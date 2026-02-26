package com.it10x.foodappgstav7_04.data.pos.repository

import android.util.Log
import com.it10x.foodappgstav7_04.data.pos.dao.KotBatchDao
import com.it10x.foodappgstav7_04.data.pos.dao.KotItemDao
import com.it10x.foodappgstav7_04.data.pos.dao.TableDao
import com.it10x.foodappgstav7_04.data.pos.entities.PosKotItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class KotRepository(
    private val batchDao: KotBatchDao,
    private val kotItemDao: KotItemDao,
    private val tableDao: TableDao
) {

    fun getRunningKotsForTable(tableNo: String): Flow<Pair<List<Any>, List<Any>>> {
        return combine(
            batchDao.getBatchesForTable(tableNo),
            kotItemDao.getItemsForTable(tableNo)
        ) { batches, items ->
            batches to items
        }
    }


    suspend fun insertItemsAndSync(
        tableNo: String,
        items: List<PosKotItemEntity>
    ) {
//        Log.d("KITCHEN_T", "insertItemsAndSync called for table: $tableNo")
//        Log.d("KITCHEN_T", "Items count: ${items.size}")
//
//        items.forEach {
//            Log.d("KITCHEN_T", "Item -> productId: ${it.productId}, qty: ${it.quantity}")
//        }

        kotItemDao.insertAll(items)
      //  Log.d("KITCHEN_T", "Items inserted into database")

        syncKitchenCount(tableNo)
      //  Log.d("KITCHEN_T", "Kitchen count synced for table: $tableNo")
    }



    suspend fun markDoneAll(tableNo: String) {
        kotItemDao.markAllDone(tableNo)
        kotItemDao.markAllPrinted(tableNo)

       }

    private suspend fun syncBillCounters(tableNo: String) {
        val billCount = kotItemDao.countDoneItems(tableNo) ?: 0
        val billAmount = kotItemDao.sumDoneAmount(tableNo) ?: 0.0

        tableDao.updateBill(tableNo, billCount, billAmount)
    }

    private suspend fun syncKitchenCount(tableNo: String) {
        Log.d("TABLE_DEBUG", "syncKitchenCount() called for table = $tableNo")

        val count = kotItemDao.countKitchenPending(tableNo) ?: 0

        Log.d("TABLE_DEBUG", "Kitchen pending count from DB = $count")

        tableDao.setKitchenCount(tableNo, count)

        Log.d("TABLE_DEBUG", "Kitchen count updated in tableDao")
    }

    suspend fun syncBillCount(tableNo: String) {
        // 🔥 refresh counters after state change
        syncBillCounters(tableNo)
    }

    suspend fun syncKinchenCount(tableNo: String) {
        // 🔥 refresh counters after state change
        syncKitchenCount(tableNo)
    }

}
