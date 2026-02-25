package com.it10x.foodappgstav7_04.data.online.sync

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.it10x.foodappgstav7_04.data.pos.KotProcessor
import com.it10x.foodappgstav7_04.data.pos.entities.PosKotItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class GlobalOrderSyncManager(
    private val firestore: FirebaseFirestore,
    private val kotProcessor: KotProcessor,
    private val onWaiterOrderReceived: (
        orderId: String,
        tableNo: String,
        sessionId: String,
        items: List<PosKotItemEntity>
    ) -> Unit
) {

    private var listener: ListenerRegistration? = null

    // ✅ ADD THIS (missing earlier)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startListening() {
        if (listener != null) return

        listener = firestore.collection("waiter_orders")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e("GLOBAL_SYNC", "Firestore error", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {

                        val orderDoc = change.document
                        val orderId = orderDoc.id
                        val tableNo = orderDoc.getString("tableNo") ?: ""
                        val sessionId = orderDoc.getString("sessionId") ?: ""

                        Log.d("GLOBAL_SYNC", "New waiter order: $orderId | Table=$tableNo")

                        firestore.collection("waiter_orders")
                            .document(orderId)
                            .collection("items")
                            .get()
                            .addOnSuccessListener { itemsSnapshot ->

                                val kotItems = itemsSnapshot.documents.map { itemDoc ->

                                    val productId = itemDoc.getString("productId")
                                        ?: UUID.randomUUID().toString()

                                    val name = itemDoc.getString("productName") ?: ""
                                    val quantity =
                                        (itemDoc.getLong("quantity") ?: 1L).toInt()
                                    val price =
                                        itemDoc.getDouble("price") ?: 0.0
                                    val taxRate =
                                        itemDoc.getDouble("taxRate") ?: 0.0
                                    val modifiersJson =
                                        itemDoc.getString("modifiersJson") ?: ""
                                    val categoryId =
                                        itemDoc.getString("categoryId") ?: ""
                                    val categoryName =
                                        itemDoc.getString("categoryName") ?: "WAITER"
                                    val kitchenPrintReq =
                                        itemDoc.getBoolean("kitchenPrintReq") ?: true
                                    Log.d("WAITER_KOT_PRINT", "name: $name | print=$kitchenPrintReq")
                                    PosKotItemEntity(
                                        id = "${productId}_$tableNo",
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
                                        kitchenPrintReq = kitchenPrintReq,
                                        kitchenPrinted = false,
                                        createdAt = System.currentTimeMillis(),
                                        source = "WAITER",
                                        syncedToCloud = false,
                                        syncedFromCloud = true
                                    )
                                }

                                // ✅ PROCESS USING CORRECT SCOPE
                                scope.launch {
                                    kotProcessor.processWaiterOrder(
                                        tableNo = tableNo,
                                        sessionId = sessionId,
                                        orderType = "DINE_IN",
                                        items = kotItems
                                    )
                                }

                                // ✅ Optional UI callback
                                onWaiterOrderReceived(
                                    orderId,
                                    tableNo,
                                    sessionId,
                                    kotItems
                                )

                                // ✅ Mark order accepted
                                firestore.collection("waiter_orders")
                                    .document(orderId)
                                    .update("status", "ACCEPTED")

                                Log.d("GLOBAL_SYNC", "Order marked ACCEPTED: $orderId")
                            }
                    }
                }
            }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
    }
}
