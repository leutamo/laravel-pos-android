package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import com.example.laravelpos.data.model.Product
import com.example.laravelpos.data.model.ProductResponse
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import javax.inject.Inject

class ProductRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        const val TOKEN_KEY = "auth_token" // Clave del token en SharedPreferences
    }

    @Serializable
    data class ErrorResponse(
        val success: Boolean,
        val message: String
    )

    suspend fun getProducts(): List<Product> {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            if (token != null) {
                try {
                    val response = client.get("http://192.168.100.68:8000/api/products") {
                        header("Authorization", "Bearer $token")
                    }
                    val responseText = response.bodyAsText()
                    println("Full Response: $responseText")
                    if (response.status.value == 200) {
                        response.body<ProductResponse>().data
                    } else {
                        val error = response.body<ErrorResponse>()
                        if (!error.success && error.message == "Unauthenticated.") {
                            // Desloguear al usuario si el token venció
                            with(sharedPreferences.edit()) {
                                remove(TOKEN_KEY)
                                apply()
                            }
                            emptyList() // Retorna lista vacía para evitar crash
                        } else {
                            throw Exception("Error: ${error.message}")
                        }
                    }
                } catch (e: Exception) {
                    println("Error fetching products: ${e.message}")
                    emptyList() // Manejo seguro de errores
                }
            } else {
                emptyList() // Si no hay token, retorna lista vacía
            }
        }
    }
}