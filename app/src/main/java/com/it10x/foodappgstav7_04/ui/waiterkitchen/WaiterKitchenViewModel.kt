package com.it10x.foodappgstav7_04.ui.waiterkitchen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.it10x.foodappgstav7_04.data.PrinterRole
import com.it10x.foodappgstav7_04.data.pos.AppDatabaseProvider
import com.it10x.foodappgstav7_04.data.pos.entities.PosCartEntity
import com.it10x.foodappgstav7_04.data.pos.entities.PosKotBatchEntity
import com.it10x.foodappgstav7_04.data.pos.entities.PosKotItemEntity
import com.it10x.foodappgstav7_04.data.pos.repository.CartRepository
import com.it10x.foodappgstav7_04.data.pos.repository.POSOrdersRepository
import com.it10x.foodappgstav7_04.data.pos.usecase.KotToBillUseCase
import com.it10x.foodappgstav7_04.printer.PrinterManager
import com.it10x.foodappgstav7_04.ui.cart.CartViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.it10x.foodappgstav7_04.data.pos.repository.KotRepository



import com.google.firebase.firestore.FirebaseFirestore
import com.it10x.foodappgstav7_04.data.online.models.waiter.WaiterOrder
import com.it10x.foodappgstav7_04.data.online.models.waiter.WaiterOrderItem
import com.it10x.foodappgstav7_04.data.pos.repository.WaiterKitchenRepository

import kotlinx.coroutines.tasks.await
class WaiterKitchenViewModel(
    app: Application,
    private val tableId: String,
    private val tableName: String,
    private val sessionId: String,
    private val orderType: String,
    private val repository: POSOrdersRepository,
    private val waiterKitchenRepository: WaiterKitchenRepository,
    private val cartViewModel: CartViewModel,
) : AndroidViewModel(app) {

    private var kotPrintJob: Job? = null
    private val pendingKotItems = mutableListOf<PosKotItemEntity>()
    private var pendingBatchId: String? = null
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading
    private val kotItemDao =
        AppDatabaseProvider.get(app).kotItemDao()


    private val kotToBillUseCase =
        KotToBillUseCase(kotItemDao)

    val kotItems: StateFlow<List<PosKotItemEntity>> =
        kotItemDao.getAllKotItems()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )



    private val kotRepository = KotRepository(
        AppDatabaseProvider.get(app).kotBatchDao(),
        AppDatabaseProvider.get(app).kotItemDao(),
        AppDatabaseProvider.get(app).tableDao()
    )

    private val cartRepository = CartRepository(
        AppDatabaseProvider.get(app).cartDao(),
        AppDatabaseProvider.get(app).tableDao()
    )

    private val printerManager =
        PrinterManager(app.applicationContext)

    fun sendToFireStore(
        cartList: List<PosCartEntity>,
        tableNo: String,
        deviceId: String,
        deviceName: String?
    ) {
        viewModelScope.launch {

            _loading.value = true

            val success = waiterKitchenRepository.sendOrderToFireStore(
                cartList = cartList,
                tableNo = tableNo,
                sessionId = sessionId,
                orderType = orderType,
                deviceId = deviceId,
                deviceName = deviceName
            )

            if (success) {
                repository.clearCart(orderType, tableNo)
                cartRepository.syncCartCount(tableNo)
            }

            _loading.value = false
        }
    }







    fun getPendingItems(orderRef: String, orderType: String): Flow<List<PosKotItemEntity>> {


        return if (orderType == "DINE_IN") {
            kotItemDao.getPendingItemsForTable(orderRef)
        } else {
            kotItemDao.getPendingItemsForTable(orderType)
          //  kotItemDao.getPendingItemsForSession(orderRef)
        }
    }
     // ✅ POS signal: kitchen completed for table




    fun cartToBIll_KitchenPrint(
        orderType: String,
        tableNo: String,
        sessionId: String,
        paymentType: String,
        deviceId: String,
        deviceName: String?,
        appVersion: String?
    ) {
        Log.d("KITCHEN_DEBUG4", "sendToKitchen tableNo=$tableNo orderType=$orderType sessionId=$sessionId ")
        //   logAllKotItems()
        viewModelScope.launch {
            _loading.value = true

            // ✅ use sessionId as the real key for cart & KOT
            val sessionKey = sessionId
            val tableId = tableNo!!
            //     Log.d("KITCHEN_DEBUG", "Resolved sessionKey=$sessionKey")

            // ✅ FIX: Use sessionKey (for takeaway & delivery)
            //val cartList = repository.getCartItems(sessionKey, orderType).first()
            val cartList = repository.getCartItemsByTableId(tableId).first()
            //Log.d("KITCHEN_DEBUG", "Cart fetched for type=$orderType, sessionKey=$sessionKey, size=${cartList.size}")

            if (cartList.isEmpty()) {
                Log.w("KITCHEN_DEBUG4", "⚠️ No new items found for orderType=$orderType (sessionKey=$sessionKey)")
                _loading.value = false
                return@launch
            }

            try {
                val now = System.currentTimeMillis()
                val orderId = UUID.randomUUID().toString()

                //  Log.d("KITCHEN_DEBUG4", "Creating new KOT batchId=$orderId for $orderType")

                val kotSaved = saveKotOnlyToKotItem_withPrint(
                    orderType = orderType,
                    sessionId = sessionId,
                    tableNo = tableNo,
                    cartItems = cartList,
                    deviceId = deviceId,
                    deviceName = deviceName,
                    appVersion = appVersion
                )

                if (!kotSaved) {
                    Log.e("KITCHEN_DEBUG4", " saveKotOnly() failed for session=$sessionKey")
                    return@launch
                }

                //  Log.d("KITCHEN_DEBUG4", " KOT saved successfully (${cartList.size} items)")


                repository.clearCart(orderType, tableId)
                cartRepository.syncCartCount(tableId)
            } catch (e: Exception) {
                //  Log.e("KITCHEN_DEBUG", " Exception during placeOrder()", e)
            } finally {
                _loading.value = false
            }
        }
    }






    fun sendSingleItemDirectlyToBill_Print_noPrint(
        cart: PosCartEntity,
        orderType: String,
        tableNo: String,
        sessionId: String,
        print: Boolean
    ) {

        viewModelScope.launch(Dispatchers.IO) {

            val db = AppDatabaseProvider.get(getApplication())
            val kotBatchDao = db.kotBatchDao()
            val kotItemDao = db.kotItemDao()

            val now = System.currentTimeMillis()
            val batchId = UUID.randomUUID().toString()

            // 🔹 Create batch (required for consistency)
            val batch = PosKotBatchEntity(
                id = batchId,
                sessionId = sessionId,
                tableNo = tableNo ?: orderType,
                orderType = orderType,
                deviceId = "dummy",
                deviceName = "dummy",
                appVersion = "dummy",
                createdAt = now,
                sentBy = "dummy",
                syncStatus = "DONE",
                lastSyncedAt = null
            )

            kotBatchDao.insert(batch)

            // 🔹 Create SINGLE KOT item → DONE
            val kotItem = PosKotItemEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                kotBatchId = batchId,
                tableNo = tableNo ?: orderType,
                productId = cart.productId,
                name = cart.name,
                categoryId = cart.categoryId,
                categoryName = cart.categoryName,
                parentId = cart.parentId,
                isVariant = cart.isVariant,
                basePrice = cart.basePrice,
                quantity = cart.quantity,
                taxRate = cart.taxRate,
                taxType = cart.taxType,
                note = cart.note,
                modifiersJson = cart.modifiersJson,
                status = "DONE",
                isPrinted = false,
                createdAt = now
            )

            kotItemDao.insert(kotItem)
            Log.d("TABLE_DEBUG", "Cart to direct bill with print")

            //kotItemDao.getPendingItems(tableNo)

            // 🔹 Remove from cart after sending to bill
            //    cartViewModel.removeFromCart(cart.productId, tableNo)
            cartRepository.remove(cart.productId, tableNo)
            cartRepository.syncCartCount(tableNo)
            kotRepository.syncBillCount(tableNo)

           // logAllKotItems()
            // 🔹 Print if required
            if (print) {
                addItemToDebouncedKitchenPrint(kotItem, orderType)
                }
            kotItemDao.markPrinted(kotItem.id)



        }
    }

    private suspend fun saveKotOnlyToKotItem_withPrint(
        orderType: String,
        sessionId: String,
        tableNo: String?,
        cartItems: List<PosCartEntity>,
        deviceId: String,
        deviceName: String?,
        appVersion: String?
    ): Boolean = withContext(Dispatchers.IO) {

        val tableNo = tableNo?: "";
        try {
            val db = AppDatabaseProvider.get(printerManager.appContext())
            val kotBatchDao = db.kotBatchDao()
            val kotItemDao = db.kotItemDao()

            val batchId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            repository.markAllSent(tableNo)

            val batch = PosKotBatchEntity(
                id = batchId,
                sessionId = sessionId,
                tableNo = tableNo,
                orderType = orderType,
                deviceId = deviceId,
                deviceName = deviceName,
                appVersion = appVersion,
                createdAt = now,
                sentBy = null,
                syncStatus = "DONE",
                lastSyncedAt = null
            )

            kotBatchDao.insert(batch)

            val items = cartItems.map { cart ->
                PosKotItemEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    kotBatchId = batchId,
                    tableNo = tableNo,
                    productId = cart.productId,
                    name = cart.name,
                    categoryId = cart.categoryId,
                    categoryName = cart.categoryName,
                    parentId = cart.parentId,
                    isVariant = cart.isVariant,
                    basePrice = cart.basePrice,
                    quantity = cart.quantity,
                    taxRate = cart.taxRate,
                    taxType = cart.taxType,
                    note = cart.note,
                    modifiersJson = cart.modifiersJson,
                    isPrinted = false,
                    status = "DONE",
                    createdAt = now
                )
            }

            kotRepository.insertItemsAndSync(tableNo, items)

            // 🔥 PRINT (still inside same coroutine)
            val unprintedItems = kotItemDao.getUnprintedItems(tableNo)

            if (unprintedItems.isNotEmpty()) {
                printerManager.printTextKitchen(
                    PrinterRole.KITCHEN,
                    sessionKey = tableNo,
                    orderType = orderType,
                    items = unprintedItems
                )

                kotRepository.markDoneAll(tableNo)
                kotRepository.syncKinchenCount(tableNo)
                kotRepository.syncBillCount(tableNo)

                Log.d("KITCHEN_PRINT", "Done All printed for table=$tableNo")
            }

            Log.d("KOT", "✅ KOT SAVED: batch=$batchId items=${cartItems.size}")
            true

        } catch (e: Exception) {
            Log.e("KOT", "❌ Failed to save KOT", e)
            false
        }
    }

    private fun addItemToDebouncedKitchenPrint(
        item: PosKotItemEntity,
        orderType: String
    ) {
        synchronized(this) {
            pendingKotItems.add(item)
            if (pendingBatchId == null) {
                pendingBatchId = item.kotBatchId
            }
        }

        // Cancel previous timer
        kotPrintJob?.cancel()

        // Start / restart 10s timer
        kotPrintJob = viewModelScope.launch {
            delay(5_000) // ⏱️ 10 seconds

            val itemsToPrint: List<PosKotItemEntity>
            val batchId: String?

            synchronized(this@WaiterKitchenViewModel) {
                itemsToPrint = pendingKotItems.toList()
                batchId = pendingBatchId
                pendingKotItems.clear()
                pendingBatchId = null
            }

            if (itemsToPrint.isNotEmpty()) {
                printerManager.printTextKitchen(
                    PrinterRole.KITCHEN,
                    sessionKey = itemsToPrint.first().tableNo ?: batchId!!,
                    orderType = orderType,
                    items = itemsToPrint
                )

                // mark all printed
                val db = AppDatabaseProvider.get(getApplication())
                db.kotItemDao().markPrintedBatch(itemsToPrint.map { it.id })
            }
        }
    }



}





