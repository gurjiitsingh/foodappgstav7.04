

package com.it10x.foodappgstav7_04.data.pos

import android.util.Log
import com.it10x.foodappgstav7_04.data.PrinterRole
import com.it10x.foodappgstav7_04.data.pos.dao.KotItemDao
import com.it10x.foodappgstav7_04.data.pos.entities.PosKotItemEntity
import com.it10x.foodappgstav7_04.data.pos.repository.KotRepository
import com.it10x.foodappgstav7_04.printer.PrinterManager

class KotProcessor(
    private val kotItemDao: KotItemDao,
    private val kotRepository: KotRepository,
    private val printerManager: PrinterManager
) {

    // ✅ Already suspend — NO scope.launch here
    suspend fun processWaiterOrder(
        tableNo: String,
        sessionId: String,
        orderType: String,
        items: List<PosKotItemEntity>
    ) {

//        Log.d("WAITER_KOT", "==============================")
//        Log.d("WAITER_KOT", "Incoming Waiter Order")
//        Log.d("WAITER_KOT", "Table: $tableNo | Session: $sessionId")
//        Log.d("WAITER_KOT", "Items Count: ${items.size}")
//        Log.d("WAITER_KOT", "==============================")

        // 1️⃣ Insert / Update All Items First
        items.forEach { item ->

            val id = "${item.productId}_${tableNo}"

            val existingQty = kotItemDao.getItemQtyById(id) ?: 0
            val totalQty = existingQty + item.quantity

//            Log.d(
//                "WAITER_KOT",
//                "Item: ${item.name} |  kitchenPrintReq=${item.kitchenPrintReq} | kitchenPrinted=${item.kitchenPrinted}"
//            )

            if (existingQty > 0) {
                kotItemDao.updateQuantity(id, totalQty)
               // Log.d("WAITER_KOT", "Updated existing item: $id")
            } else {
                kotItemDao.insert(
                    item.copy(
                        id = id,
                        sessionId = sessionId,
                        tableNo = tableNo,
                        quantity = totalQty,
                        kitchenPrintReq = item.kitchenPrintReq,
                        kitchenPrinted = item.kitchenPrinted,
                        status = "PENDING",
                        createdAt = System.currentTimeMillis()
                    )
                )
                //Log.d("WAITER_KOT", "Inserted new item: $id")
            }
        }
      //  kotItemDao.clearForTableAll()

        val allItems = kotItemDao.getItemsAll(tableNo)
        allItems.forEach {

            Log.d(
                "KOT",
                "All tableNo Item-> ${it.name} | Printed=${it.kitchenPrinted}  kReq=${it.kitchenPrintReq} status=${it.status} Qty=${it.quantity} |"
            )
        }

        // 2️⃣ Fetch Unprinted Items
        val unprintedItems = kotItemDao.getItemsToPrintForKitchen(tableNo)


       // Log.d("WAITER_KOT1", "Unprinted Items Count: ${unprintedItems.size}")

        unprintedItems.forEach {

            Log.d(
                "KOT",
                "To be PRINT -> ${it.name} | Qty=${it.quantity} | kitchenPrintReq=${it.kitchenPrintReq}"
            )
        }

        if (unprintedItems.isNotEmpty()) {

            printerManager.printTextKitchen(
                PrinterRole.KITCHEN,
                sessionKey = tableNo,
                orderType = orderType,
                items = unprintedItems
            )

            // ✅ MARK AS PRINTED IMMEDIATELY
            val ids = unprintedItems.map { it.id }
            kotItemDao.markKitchenPrinted(ids)

            kotRepository.markDoneAll(tableNo)
            kotRepository.syncBillCount(tableNo)
        }


    }
}

