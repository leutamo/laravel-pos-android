package com.example.laravelpos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val data: Data? = null,
    val message: String? = null
)

@Serializable
data class Data(
    val token: String,
    val user: User,
    val permissions: List<String>
)

@Serializable
data class User(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val email: String,
    val phone: String? = null,
    val email_verified_at: String? = null,
    val created_at: String,
    val updated_at: String,
    val status: Int,
    val language: String,
    val image_url: String,
    val media: List<Media> = emptyList()
)

@Serializable
data class Media(
    val id: Int? = null, // Opcional si no siempre está presente
    val url: String? = null, // Ajusta según la estructura real
    // Agrega otros campos según la estructura de media
)