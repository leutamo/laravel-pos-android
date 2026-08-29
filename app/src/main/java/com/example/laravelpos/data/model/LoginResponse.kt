package com.example.laravelpos.data.model

import kotlinx.serialization.SerialName
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
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String? = null,
    val email: String,
    val phone: String? = null,
    @SerialName("email_verified_at") val emailVerifiedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val status: Int,
    val language: String,
    @SerialName("image_url") val imageUrl: String,
    val media: List<Media> = emptyList(),
    val role: List<Role> = emptyList()
)

@Serializable
data class Role(
    val id: Int,
    val name: String,
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
data class Media(
    val id: Int? = null,
    val url: String? = null
)

@Serializable
data class UserProfileResponse(
    val data: UserProfileData
)

@Serializable
data class UserProfileData(
    val type: String,
    val id: Int,
    val attributes: UserAttributes
)

@Serializable
data class UserAttributes(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String? = null,
    val email: String,
    val phone: String? = null,
    val role: List<Role> = emptyList()
)