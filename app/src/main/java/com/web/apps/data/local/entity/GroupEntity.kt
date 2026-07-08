package com.web.apps.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val groupId: Long = 0,
    val name: String,
    val colorHex: String = "#2196F3",
    val iconUri: String? = null,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)