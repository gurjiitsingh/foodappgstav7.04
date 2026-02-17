package com.it10x.foodappgstav7_04.ui.bill

data class BillingItemUi(
    val id: String,
    val productId: String,     // 🔥 ADD THIS
    val name: String,
    val basePrice: Double,
    val taxRate: Double,       // 🔥 ADD THIS
    val quantity: Int,
    val finalTotal: Double,
    val itemtotal: Double,
    val taxTotal: Double,
    val note: String,
    val modifiersJson: String,
)

