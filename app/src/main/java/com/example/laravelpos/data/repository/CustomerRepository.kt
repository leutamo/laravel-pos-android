package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.*
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

private const val TAG = "CustomerRepository"

@Serializable
data class CustomerCreateRequest(
    val name: String,
    @SerialName("document_type_id") val documentTypeId: Int,
    @SerialName("document_number") val documentNumber: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = "Perú"
)

sealed class SearchCustomerResult {
    data class Success(val customer: Customer) : SearchCustomerResult()
    data class Error(val message: String) : SearchCustomerResult()
}

sealed class CustomerResult {
    data class Success(val customer: Customer) : CustomerResult()
    data class Error(val message: String) : CustomerResult()
}

class CustomerRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {
    /**
     * Busca un cliente por su número de documento (DB o RENIEC/SUNAT).
     */
    suspend fun searchCustomer(documentNumber: String): SearchCustomerResult {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get("customers/search/$documentNumber") {
                    header("Authorization", "Bearer $token")
                }
                val responseText = response.bodyAsText()
                Log.d(TAG, "searchCustomer response: $responseText")

                if (response.status.isSuccess()) {
                    val result = response.body<LaravelResponse<DataWrapper<Customer>>>()
                    SearchCustomerResult.Success(result.data.data)
                } else {
                    val errorMessage = try {
                        val json = Json.parseToJsonElement(responseText).jsonObject
                        json["message"]?.toString()?.removeSurrounding("\"") ?: "Documento no encontrado"
                    } catch (e: Exception) {
                        "Error del servidor (${response.status.value})"
                    }
                    SearchCustomerResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching customer: ${e.message}")
                SearchCustomerResult.Error(e.message ?: "Error de red al buscar cliente")
            }
        }
    }

    /**
     * Obtiene el primer cliente registrado (considerado genérico para esta etapa).
     */
    suspend fun getFirstCustomer(): Customer? {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            try {
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
     * Crea un nuevo cliente en el backend.
     */
    suspend fun createCustomer(customer: Customer): CustomerResult {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            try {
                val attr = customer.attributes
                val request = CustomerCreateRequest(
                    name = attr.name,
                    documentTypeId = attr.document_type_id,
                    documentNumber = attr.document_number,
                    email = attr.email?.ifBlank { null },
                    phone = attr.phone?.ifBlank { null },
                    address = attr.address?.ifBlank { null },
                    city = attr.city?.ifBlank { null },
                    country = attr.country?.ifBlank { "Perú" } ?: "Perú"
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
                        val message = json["message"]?.toString()?.removeSurrounding("\"") ?: "Error al registrar cliente"
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
