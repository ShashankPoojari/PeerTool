package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val peerId: String,
    val nickname: String,
    val avatarColorHex: String = "#38BDF8",
    val isFavorite: Boolean = false,
    val lastConnectedTime: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isSecurityVerified: Boolean = false,
    val safetyNumber: String? = null
)
