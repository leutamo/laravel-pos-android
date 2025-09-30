package com.example.laravelpos.data.model

import kotlinx.serialization.Serializable

// Representa la respuesta completa de la API, que contiene una lista de clientes.
@Serializable
data class CustomerResponse(
    val data: List<Customer>
)

// Representa un solo cliente de la API con sus metadatos (type, id, links)
// y los atributos anidados.
@Serializable
data class Customer(
    val type: String,
    val id: Int,
    val attributes: CustomerAttributes,
    val links: CustomerLinks
)

// Contiene los datos reales del cliente, que vienen anidados dentro del objeto 'attributes'.
@Serializable
data class CustomerAttributes(
    val name: String,
    val email: String,
    val phone: String,
    val country: String,
    val city: String,
    val address: String,
    val dob: String? = null,

    // ✅ Campos de la migración de Laravel
    val document_number: String,
    val document_type_id: Int,

    // Campos de marca de tiempo (timestamps) de la API
    val created_at: String,
    val updated_at: String
)

// Representa los enlaces de la API, si los hay.
@Serializable
data class CustomerLinks(
    val self: String
)
