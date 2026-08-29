package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.Product
import com.example.laravelpos.data.model.ProductResponse
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

    suspend fun getProducts(): Result<List<Product>> {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        return withContext(Dispatchers.IO) {
            if (token != null) {
                try {
                    val response = client.get("products") {
                        header("Authorization", "Bearer $token")
                        parameter("per_page", "100")
                        parameter("sort", "-created_at")
                    }
                    val responseText = response.bodyAsText()
                    Log.d("ProductRepository", "Response: $responseText")

                    if (response.status.value == 200) {
                        Result.success(response.body<ProductResponse>().data)
                    } else if (response.status.value == 403) {
                        Result.failure(Exception("Sin permisos: Su rol no permite ver productos"))
                    } else {
                        // Intentar parsear el mensaje de error del servidor
                        val errorMessage = try {
                            val error = Json.decodeFromString<ErrorResponse>(responseText)
                            error.message
                        } catch (e: Exception) {
                            "Error del servidor (${response.status.value})"
                        }

                        if (errorMessage == "Unauthenticated.") {
                            with(sharedPreferences.edit()) {
                                remove(TOKEN_KEY)
                                apply()
                            }
                        }
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: Exception) {
                    Log.e("ProductRepository", "Error: ${e.message}")
                    Result.failure(e)
                }
            } else {
                Result.failure(Exception("No autenticado"))
            }
        }
    }
}