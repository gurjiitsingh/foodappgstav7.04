package com.it10x.foodappgstav7_04.data.pos.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.it10x.foodappgstav7_04.data.online.models.waiter.WaiterOrder
import com.it10x.foodappgstav7_04.data.online.models.waiter.WaiterOrderItem
import com.it10x.foodappgstav7_04.data.pos.entities.PosCartEntity
import kotlinx.coroutines.tasks.await


class WaiterKitchenRepository(
    private val firestore: FirebaseFirestore
) {

    suspend fun sendOrderToFireStore(
        cartList: List<PosCartEntity>,
        tableNo: String,
        sessionId: String,
        orderType: String,
        deviceId: String,
        deviceName: String?
    ): Boolean {

        return try {

            if (cartList.isEmpty()) return false

            val orderId = firestore.collection("waiter_orders").document().id
            val batch = firestore.batch()

            val orderRef = firestore
                .collection("waiter_orders")
                .document(orderId)

            val order = WaiterOrder(
                orderId = orderId,
                tableNo = tableNo,
                sessionId = sessionId,
                orderType = orderType,
                deviceId = deviceId,
                deviceName = deviceName,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            batch.set(orderRef, order)

            cartList.forEach { cartItem ->

                val itemRef = orderRef
                    .collection("items")
                    .document()

                val orderItem = WaiterOrderItem(
                    productId = cartItem.productId,
                    productName = cartItem.name,
                    quantity = cartItem.quantity,
                    price = cartItem.basePrice,
                    taxRate = cartItem.taxRate,
                    tableNo = tableNo,
                    sessionId = sessionId
                )

                batch.set(itemRef, orderItem)
            }

            batch.commit().await()

            Log.d("WAITER_FIRESTORE", "Order uploaded with ${cartList.size} items")

            true

        } catch (e: Exception) {
            Log.e("WAITER_FIRESTORE", "Upload failed", e)
            false
        }
    }

}



//class WaiterKitchenRepository {
//
//    private val firestore: FirebaseFirestore =
//        FirebaseFirestore.getInstance()
//
//    suspend fun uploadItems(
//        cartItems: List<PosCartEntity>,
//        tableNo: String,
//        sessionId: String,
//        orderType: String,
//        deviceId: String,
//        deviceName: String?,
//        sendToKitchen: Boolean
//    ): Boolean {
//
//        return try {
//
//            val batch = firestore.batch()
//
//            cartItems.forEach { cart ->
//
//                val docRef = firestore
//                    .collection("waiter_orders")
//                    .document()
//
//                val data = hashMapOf(
//                    "productId" to cart.productId,
//                    "name" to cart.name,
//                    "categoryId" to cart.categoryId,
//                    "categoryName" to cart.categoryName,
//                    "parentId" to cart.parentId,
//                    "isVariant" to cart.isVariant,
//                    "quantity" to cart.quantity,
//                    "basePrice" to cart.basePrice,
//                    "taxRate" to cart.taxRate,
//                    "taxType" to cart.taxType,
//                    "note" to cart.note,
//                    "modifiersJson" to cart.modifiersJson,
//
//                    "tableNo" to tableNo,
//                    "sessionId" to sessionId,
//                    "orderType" to orderType,
//
//                    "deviceId" to deviceId,
//                    "deviceName" to deviceName,
//
//                    "sendToKitchen" to sendToKitchen,
//                    "status" to "PENDING",
//                    "createdAt" to System.currentTimeMillis()
//                )
//
//                batch.set(docRef, data)
//            }
//
//            batch.commit().await()
//
//            Log.d("WAITER_FIRESTORE", "Uploaded ${cartItems.size} items")
//            true
//
//        } catch (e: Exception) {
//            Log.e("WAITER_FIRESTORE", "Upload failed", e)
//            false
//        }
//    }
//}
