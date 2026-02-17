package com.it10x.foodappgstav7_04.com.it10x.foodappgstav7_04.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.it10x.foodappgstav7_04.data.pos.entities.PosCartEntity
import com.it10x.foodappgstav7_04.data.pos.entities.ProductEntity
import com.it10x.foodappgstav7_04.ui.cart.CartViewModel
import com.it10x.foodappgstav7_04.ui.pos.PosSessionViewModel
import com.it10x.foodappgstav7_04.ui.pos.toTitleCase
import com.it10x.foodappgstav7_04.ui.theme.*
import com.it10x.foodappgstav7_04.viewmodel.PosTableViewModel

@Composable
fun ProductListWaiter(
    filteredProducts: List<ProductEntity>,
   // variants: List<ProductEntity>,
    cartViewModel: CartViewModel,
    tableViewModel: PosTableViewModel,
    tableNo: String,
    posSessionViewModel: PosSessionViewModel,
    onProductAdded: () -> Unit
) {

    val sessionId by posSessionViewModel.sessionId.collectAsState()

    val sortedProducts = remember(filteredProducts) {
        filteredProducts.sortedBy { it.sortOrder }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(
            items = sortedProducts,
            key = { it.id }
        ) { product ->

            ParentProductCard(
                product = product,
                cartViewModel = cartViewModel,
                tableViewModel = tableViewModel,
                tableNo = tableNo,
                sessionId = sessionId,
                onProductAdded = onProductAdded
            )
        }
    }

}

@Composable
private fun ParentProductCard(
    product: ProductEntity,
    cartViewModel: CartViewModel,
    tableViewModel: PosTableViewModel,
    tableNo: String,
    sessionId: String,
    onProductAdded: () -> Unit
) {

    val cartItems by cartViewModel.cart.collectAsState()

    val currentQty = cartItems
        .filter { it.tableId == tableNo && it.productId == product.id }
        .sumOf { it.quantity }

    val productBg = MaterialTheme.colorScheme.background
    val productText = MaterialTheme.colorScheme.onSurface

    val addBg = PosTheme.accent.cartAddBg
    val addText = PosTheme.accent.cartAddText

    val removeBorder = PosTheme.accent.cartRemoveBorder
    val removeText = PosTheme.accent.cartRemoveText

    val price = when {
        product.discountPrice == null || product.discountPrice == 0.0 -> product.price
        else -> product.discountPrice
    }

    val code = product.searchCode?.trim()
    val numericCode = code?.takeIf { it.all { ch -> ch.isDigit() } }

    val displayName = numericCode?.let {
        "${product.name} $it"
    } ?: product.name

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline)),
        color = productBg,
        shape = RectangleShape
    ) {

        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // 🔹 Product name (takes max width)
            Text(
                text = toTitleCase(displayName),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                fontSize = 14.sp,
                color = productText
            )

            // 🔹 Remove button
            OutlinedButton(
                onClick = { cartViewModel.decrease(product.id, tableNo) },
                border = BorderStroke(1.5.dp, removeBorder),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RectangleShape
            ) {
                Text("−", color = removeText, fontSize = 16.sp)
            }

            // 🔹 Quantity
            Text(
                text = currentQty.toString(),
                modifier = Modifier.padding(horizontal = 6.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = productText
            )

            // ➕ Add
            IconButton(
                onClick = {
                    cartViewModel.addToCart(
                        PosCartEntity(
                            productId = product.id,
                            name = toTitleCase(product.name),
                            basePrice = price,
                            note = "",                // ✅ change here
                            modifiersJson = "",       // ✅ change here
                            quantity = 1,
                            taxRate = product.taxRate ?: 0.0,
                            taxType = product.taxType ?: "inclusive",
                            parentId = null,
                            isVariant = false,
                            categoryId = product.categoryId,
                            sessionId = sessionId,
                            tableId = tableNo
                        )
                    )
                  //  onProductAdded()
                    tableViewModel.markOrdering(tableNo)
                },
                modifier = Modifier
                    .size(width = 40.dp, height = 32.dp)
                    .background(addBg, RectangleShape)
            ) {
                Text(
                    "+",
                    color = addText,
                    fontSize = 18.sp
                )
            }
        }
    }
}

