package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.*
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import javax.inject.Inject

private const val TAG = "QuotationRepository"
const val TOKEN_KEY = "auth_token"

@Serializable
data class QuotationApiResult(
    val success: Boolean,
    val message: String,
    val data: QuotationData?
)

class QuotationRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {

    suspend fun createQuotation(request: QuotationRequest): QuotationApiResult {
        Log.d(TAG, "Starting quotation creation...")
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            if (token != null) {
                try {
                    val response = client.post("quotations") {
                        header("Authorization", "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }
                    val responseText = response.bodyAsText()
                    if (response.status.isSuccess()) {
                        val apiResponse = response.body<ApiResponse>()
                        QuotationApiResult(true, "Cotización creada exitosamente", apiResponse.data)
                    } else {
                        QuotationApiResult(false, responseText, null)
                    }
                } catch (e: Exception) {
                    QuotationApiResult(false, e.message ?: "Error", null)
                }
            } else {
                QuotationApiResult(false, "No se encontró el token de autenticación.", null)
            }
        }
    }

    suspend fun getQuotation(id: Int): QuotationApiResult {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            if (token != null) {
                try {
                    val response = client.get("quotations/$id") {
                        header("Authorization", "Bearer $token")
                    }
                    if (response.status.isSuccess()) {
                        val apiResponse = response.body<ApiResponse>()
                        QuotationApiResult(true, "Éxito", apiResponse.data)
                    } else {
                        QuotationApiResult(false, "Error al obtener cotización", null)
                    }
                } catch (e: Exception) {
                    QuotationApiResult(false, e.message ?: "Error", null)
                }
            } else {
                QuotationApiResult(false, "No hay token", null)
            }
        }
    }
}
