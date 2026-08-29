package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.LoginRequest
import com.example.laravelpos.data.model.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sharedPreferences: SharedPreferences
) {
    suspend fun login(request: LoginRequest): LoginResponse {
        return try {
            val response: LoginResponse = httpClient.post("login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            if (response.data != null) {
                sharedPreferences.edit()
                    .putString("auth_token", response.data.token)
                    .putString("user_name", response.data.user.firstName)
                    .apply()
                
                // No bloqueamos el login esperando el perfil. 
                // Lo lanzamos en segundo plano.
                CoroutineScope(Dispatchers.IO).launch {
                    fetchProfile(response.data.token)
                }
            }
            response
        } catch (e: HttpRequestTimeoutException) {
            Log.e("LoginRepository", "Login timed out: ${e.message}", e)
            LoginResponse(data = null, message = "Request timed out")
        } catch (e: Exception) {
            Log.e("LoginRepository", "Login failed: ${e.message}", e)
            LoginResponse(data = null, message = e.message)
        }
    }

    suspend fun fetchProfile(token: String? = null): Boolean {
        val authToken = token ?: sharedPreferences.getString("auth_token", null) ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.get("edit-profile") {
                    header("Authorization", "Bearer $authToken")
                }
                val responseText = response.bodyAsText()
                Log.d("LoginRepository", "Edit profile raw response: $responseText")
                
                if (response.status.isSuccess()) {
                    try {
                        val profileResponse = response.body<com.example.laravelpos.data.model.UserProfileResponse>()
                        val userAttributes = profileResponse.data.attributes
                        
                        val roleObj = userAttributes.role.firstOrNull()
                        val roleName = roleObj?.displayName ?: roleObj?.name ?: "Sin rol"
                        
                        Log.d("LoginRepository", "Profile parsed successfully. Role: $roleName")
                        
                        sharedPreferences.edit()
                            .putString("user_name", userAttributes.firstName)
                            .putString("user_role", roleName)
                            .apply()
                        true
                    } catch (parseError: Exception) {
                        Log.e("LoginRepository", "JSON parsing error in profile: ${parseError.message}", parseError)
                        sharedPreferences.edit().putString("user_role", "Error de formato").apply()
                        false
                    }
                } else {
                    Log.e("LoginRepository", "Fetch profile failed with status: ${response.status}")
                    false
                }
            } catch (e: Exception) {
                Log.e("LoginRepository", "Error fetching profile: ${e.message}", e)
                // Si falla, al menos quitamos el "Cargando..." de la preferencia si existiera
                if (sharedPreferences.getString("user_role", null) == "Cargando...") {
                    sharedPreferences.edit().putString("user_role", "Error al cargar rol").apply()
                }
                false
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getString("auth_token", null) != null
    }

    fun getUserName(): String? {
        return sharedPreferences.getString("user_name", null)
    }

    fun getUserRole(): String? {
        return sharedPreferences.getString("user_role", "Cargando...")
    }

    fun logout() {
        sharedPreferences.edit()
            .remove("auth_token")
            .remove("user_name")
            .remove("user_role")
            .apply()
    }
}
