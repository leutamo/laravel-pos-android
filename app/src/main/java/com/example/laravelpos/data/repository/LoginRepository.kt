package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.LoginRequest
import com.example.laravelpos.data.model.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sharedPreferences: SharedPreferences
) {
    suspend fun login(request: LoginRequest): LoginResponse {
        return try {
            val response: LoginResponse = httpClient.post("http://192.168.100.68:8000/api/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            if (response.data != null) {
                sharedPreferences.edit()
                    .putString("auth_token", response.data.token)
                    .putString("user_name", response.data.user.first_name)
                    .apply()
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

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getString("auth_token", null) != null
    }

    fun getUserName(): String? {
        return sharedPreferences.getString("user_name", null)
    }

    fun logout() {
        sharedPreferences.edit()
            .remove("auth_token")
            .remove("user_name")
            .apply()
    }
}