package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.Customer
import com.example.laravelpos.data.repository.ProductRepository.Companion.TOKEN_KEY
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject

private const val TAG = "CustomerRepository"

// Clase para manejar la respuesta envuelta de Laravel (sendResponse)
@Serializable
data class LaravelResponse<T>(
    val success: Boolean,
    val data: T,
    val message: String
)

// Clase para manejar el recurso individual envuelto (JsonResource)
@Serializable
data class DataWrapper<T>(
    val data: T
)

class CustomerRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {
    /**
     * Busca un cliente por su número de documento.
     * Endpoint: GET /api/customers/search/{document_number}
     */
    suspend fun searchCustomer(documentNumber: String): Customer? {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get("customers/search/$documentNumber") {
                    header("Authorization", "Bearer $token")
                }
                if (response.status.isSuccess()) {
                    // El backend usa sendResponse(new CustomerResource($customer))
                    // Estructura: { "success": true, "data": { "data": { ... } }, "message": "..." }
                    val result = response.body<LaravelResponse<DataWrapper<Customer>>>()
                    result.data.data
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching customer: ${e.message}")
                null
            }
        }
    }

    /**
     * Crea un nuevo cliente.
     * Endpoint: POST /api/customers
     */
    suspend fun createCustomer(customer: Customer): Customer? {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            try {
                // Laravel espera campos planos en CreateCustomerRequest
                val payload = mapOf(
                    "name" to customer.attributes.name,
                    "email" to customer.attributes.email,
                    "phone" to customer.attributes.phone,
                    "country" to customer.attributes.country,
                    "city" to customer.attributes.city,
                    "address" to customer.attributes.address,
                    "document_number" to customer.attributes.document_number,
                    "document_type_id" to customer.attributes.document_type_id.toString()
                )
                
                val response = client.post("customers") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
                
                if (response.status.isSuccess()) {
                    // store() en Laravel devuelve el recurso directamente -> { "data": { ... } }
                    val result = response.body<DataWrapper<Customer>>()
                    result.data
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating customer: ${e.message}")
                null
            }
        }
    }
}
