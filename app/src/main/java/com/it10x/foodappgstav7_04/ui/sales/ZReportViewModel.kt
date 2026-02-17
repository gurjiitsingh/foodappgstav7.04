package com.it10x.foodappgstav7_04.ui.sales.zreport


import com.it10x.foodappgstav7_04.data.pos.entities.PosOrderMasterEntity


data class ZReportUiState(
    val orders: List<PosOrderMasterEntity> = emptyList(),
    val grossSales: Double = 0.0,
    val taxTotal: Double = 0.0,
    val discountTotal: Double = 0.0,
    val netSales: Double = 0.0,
    val paymentBreakup: Map<String, Double> = emptyMap(),
    val from: Long = 0L,
    val to: Long = 0L,
    val isClosed: Boolean = false
)
