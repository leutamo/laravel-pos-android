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
import javax.inject.Inject

private const val TAG = "SaleRepository"

class SaleRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {
    /**
     * Crea una venta directa en el servidor.
     */
    suspend fun createSale(request: SaleRequest): QuotationApiResult {
        Log.d(TAG, "Starting sale creation...")
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            if (token != null) {
                try {
                    val response = client.post("sales") {
                        header("Authorization", "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }
                    val responseText = response.bodyAsText()
                    Log.d(TAG, "Sale response: $responseText")
                    
                    if (response.status.isSuccess()) {
                        val saleResponse = response.body<SaleResponse>()
                        // Devolvemos el ID de la venta en el mensaje para la navegación
                        QuotationApiResult(true, saleResponse.data.id.toString(), null)
                    } else {
                        QuotationApiResult(false, responseText, null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating sale: ${e.message}")
                    QuotationApiResult(false, e.message ?: "Error desconocido", null)
                }
            } else {
                QuotationApiResult(false, "No hay token", null)
            }
        }
    }

    /**
     * Obtiene los detalles de una venta específica.
     */
    suspend fun getSale(id: Int): QuotationApiResult {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        Log.d(TAG, "getSale: Fetching sale details for ID: $id")
        return withContext(Dispatchers.IO) {
            if (token != null) {
                try {
                    // Cambiamos a 'sales/$id' para obtener la estructura JSON:API formal
                    val response = client.get("sales/$id") {
                        header("Authorization", "Bearer $token")
                    }
                    val responseText = response.bodyAsText()
                    Log.d(TAG, "getSale Raw Response: $responseText")

                    if (response.status.isSuccess()) {
                        // Al usar el endpoint de recurso, la estructura es {"data": { QuotationData }}
                        val result = response.body<ApiResponse>()
                        Log.d(TAG, "getSale: Sale parsed successfully")
                        QuotationApiResult(true, "Venta cargada", result.data)
                    } else {
                        Log.e(TAG, "getSale: Server returned error ${response.status.value}")
                        QuotationApiResult(false, "Error al cargar venta: ${response.status.value}", null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getSale: Exception during fetch: ${e.message}", e)
                    QuotationApiResult(false, "Error de red o formato: ${e.message}", null)
                }
            } else {
                Log.e(TAG, "getSale: No auth token found")
                QuotationApiResult(false, "No hay token", null)
            }
        }
    }
}
