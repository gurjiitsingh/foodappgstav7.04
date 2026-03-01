package com.it10x.foodappgstav7_04.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.it10x.foodappgstav7_04.data.pos.AppDatabaseProvider
import com.it10x.foodappgstav7_04.data.pos.entities.TableEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.it10x.foodappgstav7_04.data.pos.repository.TableRepository
import com.it10x.foodappgstav7_04.data.pos.repository.CartRepository
import com.it10x.foodappgstav7_04.data.pos.repository.KotRepository

object TableStatus {

    const val OCCUPIED = "OCCUPIED"
    const val AVAILABLE = "AVAILABLE"
    const val ORDERING = "ORDERING"
    const val KITCHEN = "KITCHEN"
    const val KITCHEN_PRINTED = "KITCHEN_PRINTED"
    const val BILL = "BILL"
    const val BILL_REQUESTED = "BILL_REQUESTED"
}

class PosTableViewModel(app: Application) : AndroidViewModel(app) {

    // ✅ FIRST — StateFlow
    private val _tables = MutableStateFlow<List<TableUiState>>(emptyList())
    val tables: StateFlow<List<TableUiState>> = _tables

    // ✅ THEN dao
    private val database = AppDatabaseProvider.get(app)

    private val dao = database.tableDao()
    private val orderDao = database.orderMasterDao()
    private val repository = TableRepository(dao)

    private val cartRepository = CartRepository(
        dao = database.cartDao(),
        tableDao = dao
    )

    private val kotRepository = KotRepository(
        batchDao = database.kotBatchDao(),
        kotItemDao = database.kotItemDao(),
        tableDao = dao
    )

    init {
        observeTables()
        refreshAllTableCounts()
    }


    data class TableUiState(
        val table: TableEntity,
        val color: TableColor,
        val isBilled: Boolean = false
    ) {
        val cartCount get() = table.cartCount
        val kitchenPendingCount get() = table.kitchenCount
        val billDoneCount get() = table.billCount
        val billAmount get() = table.billAmount
        val runningAmount get() = table.billAmount
    }

    enum class TableColor {
        GRAY,
        BLUE,
        GREEN,
        RED
    }



    private fun observeTables() {
        viewModelScope.launch {
            dao.observeAllTables().collect { tableList ->

                val uiList = tableList.map { table ->

                    val isBilled = table.billCount > 0 || table.kitchenCount > 0

                    val color = when {
                        table.billCount > 0 -> TableColor.RED
                        table.kitchenCount > 0 -> TableColor.GREEN
                        table.cartCount > 0 -> TableColor.BLUE
                        else -> TableColor.GRAY
                    }

                    TableUiState(
                        table = table,
                        color = color,
                        isBilled = isBilled
                    )
                }

                _tables.emit(uiList)
            }
        }
    }

    private fun refreshAllTableCounts() {
        viewModelScope.launch {

            val allTables = dao.getAll()

            allTables.forEach { table ->
                val tableId = table.id

                cartRepository.syncCartCount(tableId)
               // kotRepository.syncKinchenCount(tableId)
                kotRepository.syncBillCount(tableId)
            }
        }
    }


    fun loadTables() {
        viewModelScope.launch {
            try {
                val tableList = dao.getAll()

                // 🔹 Add this to print all tables and their area values
//                tableList.forEach { table ->
//                    Log.d("TABLE_DEBUG", "Table ${table.id} (${table.tableName}) → area=${table.area}")
//                }

                val uiList = tableList.map { table ->

                    val isBilled = table.billCount > 0 || table.kitchenCount > 0

                    val color = when {
                        table.billCount > 0 -> TableColor.RED
                        table.kitchenCount > 0 -> TableColor.GREEN
                        table.cartCount > 0 -> TableColor.BLUE
                        else -> TableColor.GRAY
                    }

                    TableUiState(
                        table = table,
                        color = color,
                        isBilled = isBilled
                    )
                }


                _tables.emit(uiList)

            } catch (e: Exception) {
                _tables.value = emptyList()
            }
        }
    }

    fun markOrdering(tableId: String) {
        Log.d("CART_DEBUG", "markOrdering tableId=$tableId")
        viewModelScope.launch {
            val table = dao.getById(tableId) ?: return@launch
            if (table.status == TableStatus.KITCHEN || table.status == TableStatus.BILL) return@launch
            dao.updateStatus(tableId, TableStatus.ORDERING)
          //  loadTables()
        }
    }

    fun updateStatus(tableId: String, newStatus: String) {
        viewModelScope.launch {
            dao.updateStatus(tableId, newStatus)
          //  loadTables()
        }
    }

    fun markRunning(tableId: String, orderId: String) {
        viewModelScope.launch {
            dao.setActiveOrder(tableId, orderId)
            loadTables()
        }
    }

    fun requestBill(tableId: String) {
        viewModelScope.launch {
            dao.updateStatus(tableId, TableStatus.BILL_REQUESTED)
           // loadTables()
        }
    }

    fun closeTable(tableId: String) {
        viewModelScope.launch {
            orderDao.closeTableOrders(tableId, System.currentTimeMillis())
            dao.updateStatus(tableId, TableStatus.AVAILABLE)
           // loadTables()
        }
    }

    fun occupyTable(tableId: String) {
        viewModelScope.launch {
            dao.updateStatus(tableId, TableStatus.OCCUPIED)
         //   loadTables()
        }
    }

    fun releaseTable(tableId: String) {
        viewModelScope.launch {
            orderDao.closeTableOrders(tableId, System.currentTimeMillis())
            dao.clearActiveOrder(tableId)
            dao.updateStatus(tableId, TableStatus.AVAILABLE)
           // loadTables()
        }
    }

    fun releaseIfOrderingAndCartEmpty(tableNo: String) {
        viewModelScope.launch {
            val table = dao.getById(tableNo) ?: return@launch
            if (table.status != TableStatus.ORDERING) return@launch
            dao.updateStatus(tableNo, TableStatus.AVAILABLE)
           // loadTables()
        }
    }

    fun syncTablesFromCloud() {
        viewModelScope.launch {
            repository.syncFromFirestore()
        }
    }

    // ============================
    // 🔹 NEW CODE ADDED (SAFE)
    // ============================
    // Use ONLY when you need raw counts for a single table

}
