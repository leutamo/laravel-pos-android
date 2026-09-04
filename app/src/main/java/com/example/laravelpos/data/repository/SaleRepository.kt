package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.SaleRequest
import com.example.laravelpos.data.model.SaleResponse
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "SaleRepository"

class SaleRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {
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
                        // Reutilizamos QuotationApiResult para simplicidad si la estructura es compatible
                        // O creamos uno nuevo. Por ahora devolvemos éxito.
                        QuotationApiResult(true, "Venta creada exitosamente", null)
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
}
