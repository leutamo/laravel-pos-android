package com.example.laravelpos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DocumentType(
    val id: Int,
    val name: String,
    val created_at: String?,
    val updated_at: String?
)