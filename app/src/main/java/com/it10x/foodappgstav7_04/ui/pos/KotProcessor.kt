

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

        Log.d("WAITER_KOT", "==============================")
        Log.d("WAITER_KOT", "Incoming Waiter Order")
        Log.d("WAITER_KOT", "Table: $tableNo | Session: $sessionId")
        Log.d("WAITER_KOT", "Items Count: ${items.size}")
        Log.d("WAITER_KOT", "==============================")

        // 1️⃣ Insert / Update All Items First
        items.forEach { item ->

            val id = "${item.productId}_${tableNo}"

            val existingQty = kotItemDao.getItemQtyById(id) ?: 0
            val totalQty = existingQty + item.quantity

            Log.d(
                "WAITER_KOT",
                "Item: ${item.name} | IncomingQty=${item.quantity} | ExistingQty=$existingQty | FinalQty=$totalQty"
            )

            if (existingQty > 0) {
                kotItemDao.updateQuantity(id, totalQty)
                Log.d("WAITER_KOT", "Updated existing item: $id")
            } else {
                kotItemDao.insert(
                    item.copy(
                        id = id,
                        sessionId = sessionId,
                        tableNo = tableNo,
                        quantity = totalQty,
                        print = item.print,
                        status = "PENDING",
                        createdAt = System.currentTimeMillis()
                    )
                )
                Log.d("WAITER_KOT", "Inserted new item: $id")
            }
        }

        // 2️⃣ Fetch Unprinted Items
        val unprintedItems = kotItemDao.getUnprintedItems(tableNo)

        Log.d("WAITER_KOT", "------------------------------")
        Log.d("WAITER_KOT", "Unprinted Items Count: ${unprintedItems.size}")

        unprintedItems.forEach {
            Log.d(
                "WAITER_KOT_PRINT",
                "PRINT -> ${it.name} | Qty=${it.quantity} | Printed=${it.print}"
            )
        }

        Log.d("WAITER_KOT", "------------------------------")

        if (unprintedItems.isNotEmpty()) {

            // 3️⃣ Print Once
            Log.d("WAITER_KOT", "Sending items to PrinterManager...")

            printerManager.printTextKitchen(
                PrinterRole.KITCHEN,
                sessionKey = tableNo,
                orderType = orderType,
                items = unprintedItems
            )

            // 4️⃣ Mark Printed
            kotRepository.markDoneAll(tableNo)
            kotRepository.syncBillCount(tableNo)

            Log.d("WAITER_KOT", "✅ Printing Completed for table=$tableNo")
        } else {
            Log.d("WAITER_KOT", "⚠️ No items to print")
        }

        Log.d("WAITER_KOT", "==============================")
    }
}

