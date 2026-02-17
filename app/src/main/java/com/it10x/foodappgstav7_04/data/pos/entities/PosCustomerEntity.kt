package com.it10x.foodappgstav7_04.data.pos.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pos_customers",
    indices = [
        Index(value = ["phone"], unique = true),
        Index(value = ["syncStatus"])
    ]
)
data class PosCustomerEntity(

    // =====================================================
    // CORE IDENTIFIERS
    // =====================================================
    @PrimaryKey
    val id: String,                 // UUID (same as Firestore doc id)

    val ownerId: String,
    val outletId: String,

    // =====================================================
    // CUSTOMER BASIC INFO
    // =====================================================
    val name: String?,
    val phone: String,
    val countryCode: String? = "+91",
    val email: String? = null,

    // =====================================================
    // ADDRESS (DEFAULT)
    // =====================================================
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipcode: String? = null,
    val landmark: String? = null,

    // =====================================================
    // CREDIT CONTROL (VERY IMPORTANT)
    // =====================================================
    val creditLimit: Double = 0.0,
    val currentDue: Double = 0.0,

    // =====================================================
    // CUSTOMER META
    // =====================================================
    val isActive: Boolean = true,
    val notes: String? = null,

    // =====================================================
    // TIMING
    // =====================================================
    val createdAt: Long,
    val updatedAt: Long? = null,

    // =====================================================
    // SYNC CONTROL
    // =====================================================
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long? = null
)
