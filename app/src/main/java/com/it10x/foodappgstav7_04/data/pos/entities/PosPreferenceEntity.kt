package com.it10x.foodappgstav7_04.data.pos.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pos_preferences")
data class PosPreferenceEntity(

    @PrimaryKey
    val outletId: String,

    // 🔹 STARTUP SCREEN
    val defaultHomeScreen: String = "POS",
    // POS / WAITER / CLASSIC / DASHBOARD

    // 🔹 ENABLED SCREENS
    val enablePosScreen: Boolean = true,
    val enableWaiterScreen: Boolean = true,
    val enableClassicScreen: Boolean = false,
    val enableDashboardScreen: Boolean = false,
    val enableTableScreen: Boolean = false,

    // 🔹 PRODUCT DISPLAY
    val showItemCode: Boolean = false,
    val showItemPrice: Boolean = true,
    val showItemStock: Boolean = false,

    // 🔹 ORDER SETTINGS
    val allowNegativeStock: Boolean = false,
    val autoPrintBill: Boolean = true,
    val autoPrintKitchen: Boolean = true,

    // 🔹 UI SETTINGS
    val gridColumnsTablet: Int = 4,
    val gridColumnsPhone: Int = 2,

    // 🔹 SYNC FIELDS
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long? = null
)
