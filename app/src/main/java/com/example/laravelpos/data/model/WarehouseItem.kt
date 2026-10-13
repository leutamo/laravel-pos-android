package com.example.laravelpos.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WarehouseItem(
    val id: Int,
    val name: String,
    @SerialName("is_default") val isDefault: Boolean = false
)
