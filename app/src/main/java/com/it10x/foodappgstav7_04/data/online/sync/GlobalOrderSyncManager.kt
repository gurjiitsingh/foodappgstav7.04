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
    ) -> Unit // just UI callback
) {

    private var listener: ListenerRegistration? = null

    fun startListening() {
        if (listener != null) return

        listener = firestore.collection("waiter_orders")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val orderDoc = change.document
                        val orderId = orderDoc.id
                        val tableNo = orderDoc.getString("tableNo") ?: ""
                        val sessionId = orderDoc.getString("sessionId") ?: ""

                        firestore.collection("waiter_orders")
                            .document(orderId)
                            .collection("items")
                            .get()
                            .addOnSuccessListener { itemsSnapshot ->

                                CoroutineScope(Dispatchers.IO).launch {
                                    val now = System.currentTimeMillis()

                                    val kotItems = itemsSnapshot.documents.map { itemDoc ->
                                        PosKotItemEntity(
                                            id = UUID.randomUUID().toString(),
                                            sessionId = sessionId,
                                            kotBatchId = orderId,
                                            tableNo = tableNo,
                                            productId = itemDoc.getString("productId") ?: UUID.randomUUID().toString(),
                                            name = itemDoc.getString("productName") ?: "",
                                            categoryId = itemDoc.getString("categoryId") ?: "",
                                            categoryName = itemDoc.getString("categoryName") ?: "WAITER",
                                            parentId = null,
                                            isVariant = false,
                                            basePrice = itemDoc.getDouble("price") ?: 0.0,
                                            quantity = (itemDoc.getLong("quantity") ?: 1L).toInt(),
                                            taxRate = itemDoc.getDouble("taxRate") ?: 0.0,
                                            taxType = "exclusive",
                                            status = "DONE",
                                            note = "",
                                            modifiersJson = itemDoc.getString("modifiersJson") ?: "",
                                            isPrinted = false,
                                            createdAt = now,
                                            source = "WAITER",
                                            syncedToCloud = false,
                                            syncedFromCloud = true
                                        )
                                    }

                                    // ✅ Insert items into POS database
                                    kotProcessor.processWaiterOrder(
                                        orderId = orderId,
                                        tableNo = tableNo,
                                        sessionId = sessionId,
                                        items = kotItems.map { kotItem ->
                                            mapOf(
                                                "productId" to kotItem.productId,
                                                "productName" to kotItem.name,
                                                "quantity" to kotItem.quantity,
                                                "price" to kotItem.basePrice,
                                                "taxRate" to kotItem.taxRate,
                                                "modifiersJson" to kotItem.modifiersJson,
                                                "categoryId" to kotItem.categoryId,
                                                "categoryName" to kotItem.categoryName
                                            )
                                        }
                                    )

                                    // ✅ Notify UI only, no DB insertion here
                                    onWaiterOrderReceived(orderId, tableNo, sessionId, kotItems)

                                    // ✅ Mark Firestore order as accepted
                                    firestore.collection("waiter_orders")
                                        .document(orderId)
                                        .update("status", "ACCEPTED")
                                }
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
