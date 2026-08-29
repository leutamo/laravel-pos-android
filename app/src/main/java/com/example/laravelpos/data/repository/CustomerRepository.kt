package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.Customer
import com.example.laravelpos.data.model.CustomerResponse
import com.example.laravelpos.data.repository.ProductRepository.Companion.TOKEN_KEY
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
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

@Serializable
data class CustomerCreateRequest(
    val name: String,
    val email: String,
    val phone: String,
    val country: String,
    val city: String,
    val address: String,
    @SerialName("document_number") val documentNumber: String,
    @SerialName("document_type_id") val documentTypeId: Int
)

sealed class CustomerResult {
    data class Success(val customer: Customer) : CustomerResult()
    data class Error(val message: String) : CustomerResult()
}

class CustomerRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {
    /**
     * Busca un cliente por su número de documento.
     */
    suspend fun searchCustomer(documentNumber: String): Customer? {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get("customers/search/$documentNumber") {
                    header("Authorization", "Bearer $token")
                }
                if (response.status.isSuccess()) {
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
     * Obtiene el primer cliente registrado (considerado genérico para esta etapa)
     */
    suspend fun getFirstCustomer(): Customer? {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            try {
                // Solicitamos la lista de clientes, limitada a 1 para eficiencia
                val response = client.get("customers?page[size]=1") {
                    header("Authorization", "Bearer $token")
                }
                if (response.status.isSuccess()) {
                    val result = response.body<CustomerResponse>()
                    result.data.firstOrNull()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching first customer: ${e.message}")
                null
            }
        }
    }

    /**
     * Crea un nuevo cliente.
     */
    suspend fun createCustomer(customer: Customer): CustomerResult {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            try {
                val request = CustomerCreateRequest(
                    name = customer.attributes.name,
                    email = customer.attributes.email,
                    phone = customer.attributes.phone,
                    country = customer.attributes.country,
                    city = customer.attributes.city,
                    address = customer.attributes.address,
                    documentNumber = customer.attributes.document_number,
                    documentTypeId = customer.attributes.document_type_id
                )
                
                val response = client.post("customers") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                
                val responseText = response.bodyAsText()
                Log.d(TAG, "Create Customer Response: $responseText")

                if (response.status.isSuccess()) {
                    val result = response.body<DataWrapper<Customer>>()
                    CustomerResult.Success(result.data)
                } else {
                    val errorMessage = try {
                        val json = Json.parseToJsonElement(responseText).jsonObject
                        val message = json["message"]?.toString()?.removeSurrounding("\"") ?: "Error desconocido"
                        val errors = json["errors"]?.jsonObject
                        if (errors != null) {
                            val firstError = errors.values.firstOrNull()?.jsonArray?.firstOrNull()?.toString()?.removeSurrounding("\"")
                            if (firstError != null) "$message: $firstError" else message
                        } else {
                            message
                        }
                    } catch (e: Exception) {
                        "Error del servidor (${response.status.value})"
                    }
                    CustomerResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating customer: ${e.message}")
                CustomerResult.Error(e.message ?: "Error de conexión")
            }
        }
    }
}
