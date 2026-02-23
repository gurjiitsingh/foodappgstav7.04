package com.it10x.foodappgstav7_04.data.pos

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.it10x.foodappgstav7_04.data.pos.dao.KotItemDao
import com.it10x.foodappgstav7_04.data.pos.entities.PosKotItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import java.util.UUID

class KotProcessor(
    private val kotItemDao: KotItemDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun processWaiterOrder(
        orderId: String,
        tableNo: String,
        sessionId: String,
        items: List<Map<String, Any>>
    ) {
        scope.launch {
            // ✅ Prevent duplicates
            val alreadyExists = kotItemDao.isOrderAlreadyProcessed(orderId)
            if (alreadyExists) return@launch

            val now = System.currentTimeMillis()

            val kotItems = items.map { itemMap ->
                val productId = itemMap["productId"] as? String ?: UUID.randomUUID().toString()
                val name = itemMap["productName"] as? String ?: ""
                val quantity = (itemMap["quantity"] as? Long ?: 1L).toInt()
                val price = itemMap["price"] as? Double ?: 0.0
                val taxRate = itemMap["taxRate"] as? Double ?: 0.0
                val modifiersJson = itemMap["modifiersJson"] as? String ?: ""
                val categoryId = itemMap["categoryId"] as? String ?: ""
                val categoryName = itemMap["categoryName"] as? String ?: "WAITER"

                PosKotItemEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    kotBatchId = orderId,
                    tableNo = tableNo,
                    productId = productId,
                    name = name,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    parentId = null,
                    isVariant = false,
                    basePrice = price,
                    quantity = quantity,
                    taxRate = taxRate,
                    taxType = "exclusive",
                    status = "DONE",
                    note = "",
                    modifiersJson = modifiersJson,
                    isPrinted = false,
                    createdAt = now,
                    source = "WAITER",
                    syncedToCloud = false,
                    syncedFromCloud = true
                )
            }

            kotItemDao.insertAll(kotItems)
        }
    }
}
