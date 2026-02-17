package com.it10x.foodappgstav7_04.domain.usecase

import android.util.Log
import com.it10x.foodappgstav7_04.data.pos.dao.TableDao
import com.it10x.foodappgstav7_04.data.pos.repository.CartRepository
import com.it10x.foodappgstav7_04.viewmodel.TableStatus

class TableReleaseUseCase(
    private val cartRepository: CartRepository,
    private val tableDao: TableDao
) {


    suspend fun releaseIfOrderingAndCartEmpty(tableNo: String) {

        Log.d("CART_DEBUG", "table releasing start=$tableNo")

        // 1️⃣ cart must be empty
        val isEmpty = cartRepository.isCartEmpty(tableNo)
        if (!isEmpty) return

        // 2️⃣ table must exist
        val table = tableDao.getById(tableNo) ?: return

        Log.d(
            "CART_DEBUG",
            "Before update → id=${table.id}, name=${table.tableName}, status=${table.status}"
        )

        // 3️⃣ only release ORDERING tables
        if (table.status == TableStatus.ORDERING) {

            tableDao.updateStatus(table.id, TableStatus.AVAILABLE)
          //  loadTables()
            Log.d("CART_DEBUG", "table releasing success=$tableNo")
        }

        // 🔍 DEBUG: log ALL tables after update
        val allTables = tableDao.getAllTables()
        allTables.forEach {
            Log.d(
                "CART_DEBUG",
                "id=${it.id}, name=${it.tableName}, status=${it.status}"
            )
        }
    }


    suspend fun releaseIfOrderingAndCartEmpty1(tableNo: String) {

        Log.d(
            "CART_DEBUG",
            "table releasing start=${tableNo}"
        )
        // 1️⃣ cart must be empty
        val isEmpty = cartRepository.isCartEmpty(tableNo)

        if (!isEmpty) return

        // 2️⃣ table must exist
        val table = tableDao.getById(tableNo) ?: return



        // 3️⃣ only release ORDERING tables
        if (table.status == TableStatus.ORDERING) {
            Log.d(
                "CART_DEBUG",
                "table releasing success=${tableNo}"
            )
            tableDao.updateStatus(tableNo, TableStatus.AVAILABLE)
            tableDao.updateStatusByName(tableNo, TableStatus.AVAILABLE)
        }
    }
}
