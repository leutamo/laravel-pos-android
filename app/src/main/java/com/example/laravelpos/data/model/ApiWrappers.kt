package com.example.laravelpos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LaravelResponse<T>(
    val success: Boolean,
    val data: T,
    val message: String
)

@Serializable
data class DataWrapper<T>(
    val data: T
)

@Serializable
data class ConfigData(
    val permissions: List<String> = emptyList(),
    val version: String = ""
)
