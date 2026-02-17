package com.it10x.foodappgstav7_04.ui.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.it10x.foodappgstav7_04.data.pos.entities.PosCartEntity
import com.it10x.foodappgstav7_04.ui.cart.CartViewModel
import com.it10x.foodappgstav7_04.data.pos.viewmodel.POSOrdersViewModel

import androidx.compose.ui.text.font.FontWeight
import com.it10x.foodappgstav7_04.data.pos.AppDatabaseProvider
import com.it10x.foodappgstav7_04.data.pos.repository.POSOrdersRepository
import com.it10x.foodappgstav7_04.printer.PrinterManager
import com.it10x.foodappgstav7_04.ui.bill.BillViewModel
import com.it10x.foodappgstav7_04.ui.bill.BillViewModelFactory
import com.it10x.foodappgstav7_04.ui.kitchen.KitchenViewModel
import com.it10x.foodappgstav7_04.ui.kitchen.KitchenViewModelFactory
import com.it10x.foodappgstav7_04.viewmodel.PosTableViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SoupKitchen
import com.it10x.foodappgstav7_04.ui.cart.MiniCartRow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RightPanel(
    cartViewModel: CartViewModel,
    ordersViewModel: POSOrdersViewModel,
    tableViewModel: PosTableViewModel,
    orderType: String,
    tableNo: String,
    tableName: String,
    paymentType: String,
    onPaymentChange: (String) -> Unit,
     onOrderPlaced: () -> Unit,
    onOpenKitchen: (String) -> Unit,
    onOpenBill: (String) -> Unit,
    isMobile: Boolean,
    repository: POSOrdersRepository,
    onClose: (() -> Unit)? = null
) {

    val context = LocalContext.current



    val application = context.applicationContext as android.app.Application
    val db = AppDatabaseProvider.get(application)

    val printerManager = PrinterManager(context)


    val sessionId = cartViewModel.sessionKey.collectAsState().value ?: return

    val kitchenViewModel: KitchenViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "KitchenVM_$sessionId",
        factory = KitchenViewModelFactory(
            application,
            tableId = tableNo ?: orderType,
            tableName = tableName,
            sessionId = sessionId,
            orderType = orderType,
            repository = repository,
            cartViewModel = cartViewModel
        )
    )

    val billViewModel: BillViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "BillVM_${tableNo ?: orderType}",
        factory = BillViewModelFactory(
            application = application,
            tableId = tableNo ?: orderType,
            tableName = tableName,
            orderType = orderType,

            )
    )

    //val orderRef = if (orderType == "DINE_IN") tableNo ?: "" else cartViewModel.sessionKey.value ?: ""
    val orderRef = if (orderType == "DINE_IN") tableNo ?: "" else orderType

    val kitchenItems by kitchenViewModel
        .getPendingItems(orderRef = orderRef, orderType = orderType)
        .collectAsState(initial = null)

    val BillItems by billViewModel
        .getDoneItems(orderRef = orderRef, orderType = orderType)
        .collectAsState(initial = null)


    val hasKitchenItems = kitchenItems?.isNotEmpty() == true

    val hasBillItems = BillItems?.isNotEmpty() == true


    val cartItems: List<PosCartEntity> by
    cartViewModel.cart.collectAsState(initial = emptyList())

    // ---------------- TABLE STATE ----------------
    val tables by tableViewModel.tables.collectAsState()
    val currentTable = tables.find { it.table.id == tableNo }
    val tableStatus = currentTable?.table?.status ?: "AVAILABLE"

    val isDineIn = orderType == "DINE_IN"
    val isRunning = tableStatus == "OCCUPIED"
    val isBillRequested = tableStatus == "BILL_REQUESTED"

    // ---------------- POS DERIVED STATE ----------------
    val hasItems = cartItems.isNotEmpty()
    val hasTable = isDineIn && tableNo != null

    val canSendToKitchen =
        hasItems && (!isDineIn || hasTable)

    val canRequestBill =
        isDineIn && isRunning && cartItems.isEmpty()

//    val canOpenBill =
//        hasBillItems && when (orderType) {
//            "DINE_IN" -> isBillRequested
//            "TAKEAWAY", "DELIVERY" -> true
//            else -> false
//        }
    val canOpenBill =
        hasBillItems && when (orderType) {
            "DINE_IN" -> true
            "TAKEAWAY", "DELIVERY" -> true
            else -> false
        }

    val canOpenKitchen = hasItems
  //  val canOpenBill = hasBillItems

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMobile) {
                    Modifier.fillMaxHeight(0.88f)   // ✅ mobile bottom sheet height
                } else {
                    Modifier.widthIn(max = 320.dp).fillMaxHeight()
                }
            )
        //    .background(Color(0xFFF7F7F7))
            .padding(12.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {

        // ---------- ORDER INFO ----------


        if (isMobile) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cart",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = { onClose?.invoke() }
                ) {
                    Text("Close")
                }
            }
            Divider()
        }


        // ---------- CART ----------
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
        ) {
            items(cartItems, key = { it.id }) { item ->
                MiniCartRow(
                    item = item,
                    cartViewModel = cartViewModel,
                    tableNo = tableNo,
                    onCartActionDirectMoveToBill = { cartItem, print ->
                        kitchenViewModel.sendSingleItemDirectlyToBill_Print_noPrint(
                            cart = cartItem,
                            orderType = orderType,
                            tableNo = tableNo,
                            sessionId = sessionId,
                            print = print
                        )
                    },
                    onOpenKitchen = {
                        onOpenKitchen(tableNo ?: orderType)
                    }
                )

            }
        }

        Divider()

        OrderSummaryScreen(cartViewModel)


//        OrderSummaryScreen(cartViewModel)

        // =========================================================
// =================== POS ACTION BUTTONS ==================
// =========================================================

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp), // ⬅️ reduced top padding
            verticalArrangement = Arrangement.spacedBy(6.dp) // ⬅️ tighter spacing between rows
        ) {

            // 🔹 Row 1 — SEND TO KITCHEN (half width, left aligned)


            // 🔹 Row 2 — OPEN KITCHEN + BILL

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                // 🍳 OPEN KITCHEN
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp), // 👈 fixed compact height
                    enabled = canOpenKitchen,
                    onClick = {
                        if (!canOpenKitchen) return@Button
                        onOpenKitchen(tableNo ?: orderType)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary
)
                ) {
                    Icon(
                        Icons.Default.SoupKitchen,
                        contentDescription = "Kitchen",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Kitchen")
                }

                // 🧾 OPEN BILL
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp), // 👈 fixed compact height
                    enabled = canOpenBill,
                    onClick = {
                        if (!canOpenBill) return@Button
                        tableNo?.let { onOpenBill(it) }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondary,
    contentColor = MaterialTheme.colorScheme.onSecondary
)
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = "Bill",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Bill")
                }
            }


        }





    }
}






















