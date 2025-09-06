package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.repository.ProductRepository.Companion.TOKEN_KEY
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import javax.inject.Inject
private const val TAG = "QuotationRepository"
const val TOKEN_KEY = "auth_token"

// Este es el modelo para el cuerpo de la petición.
// Deberás actualizarlo con los campos reales de tu backend.
@Serializable
data class QuotationRequest(
    val exampleData: String
)

// Este es el modelo para la respuesta que te da el backend.
// Deberás actualizarlo para que coincida con tu API.
@Serializable
data class QuotationResponse(
    val success: Boolean,
    val message: String
)

class QuotationRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {

    suspend fun createQuotation(request: QuotationRequest): QuotationResponse {
        Log.d(TAG, "Starting quotation creation...")
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        Log.d(TAG, "Authorization token: ${token?.take(10)}...")

        return withContext(Dispatchers.IO) {
            if (token != null) {
                try {
                    val response = client.post("http://192.168.100.68:8000/api/quotations") {
                        header("Authorization", "Bearer $token")
                        contentType(ContentType.Application.Json) // Asegurar que se envía como JSON
                        setBody(request)
                    }

                    val responseText = response.bodyAsText()
                    Log.d(TAG, "Received response with status: ${response.status.value}")
                    Log.d(TAG, "Response body: $responseText")

                    if (response.status.isSuccess()) {
                        response.body<QuotationResponse>()
                    } else {
                        // Intentamos parsear el error del cuerpo de la respuesta
                        val errorMessage = try {
                            val errorBody = response.body<Map<String, String>>()
                            errorBody["message"] ?: responseText
                        } catch (e: Exception) {
                            responseText
                        }
                        throw Exception("Server error: $errorMessage")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating quotation: ${e.message}", e)
                    throw e // Re-lanzar la excepción para que el ViewModel la maneje
                }
            } else {
                Log.e(TAG, "No auth token found.")
                throw Exception("No se encontró el token de autenticación.")
            }
        }
    }
}