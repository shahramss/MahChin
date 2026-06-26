package com.mahchin.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#2DD4BF",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
