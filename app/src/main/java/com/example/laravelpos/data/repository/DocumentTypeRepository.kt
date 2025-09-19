package com.example.laravelpos.data.repository
import javax.inject.Inject

import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.example.laravelpos.data.model.DocumentType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class DocumentTypeRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        const val TOKEN_KEY = "auth_token"
        private const val TAG = "DocumentTypeRepo"
    }

    @Serializable
    data class ErrorResponse(
        val success: Boolean,
        val message: String
    )

    suspend fun getDocumentTypes(): List<DocumentType> = withContext(Dispatchers.IO) {
        val token = sharedPreferences.getString(TOKEN_KEY, null)

        if (token == null) {
            Log.d(TAG, "No token available for document types request")
            return@withContext emptyList()
        }

        return@withContext try {
            val response = client.get("http://192.168.100.68:8000/api/document-types") {
                header("Authorization", "Bearer $token")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<List<DocumentType>>()
                }
                else -> {
                    val error = response.body<ErrorResponse>()
                    if (!error.success && error.message == "Unauthenticated.") {
                        sharedPreferences.edit().remove(TOKEN_KEY).apply()
                        emptyList()
                    } else {
                        Log.e(TAG, "Server error: ${error.message}")
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, " $e Error fetching document types")
            emptyList()
        }
    }

    // 👇 Solo para pruebas unitarias, si las haces
    @VisibleForTesting
    fun setHttpClientForTest(client: HttpClient) {
        // Puedes exponer setter si necesitas inyectar un mock
    }
}