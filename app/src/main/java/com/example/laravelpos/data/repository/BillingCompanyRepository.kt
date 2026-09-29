package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.BillingCompany
import com.example.laravelpos.data.model.LaravelResponse
import com.example.laravelpos.data.model.SetActiveCompanyRequest
import com.example.laravelpos.data.model.SetActiveCompanyResponseData
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
import javax.inject.Singleton

private const val TAG = "BillingCompanyRepo"

@Singleton
class BillingCompanyRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {
    suspend fun getBillingCompanies(): List<BillingCompany> {
        val token = sharedPreferences.getString(TOKEN_KEY, null) ?: return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get("m1/billing-companies") {
                    header("Authorization", "Bearer $token")
                }
                val responseText = response.bodyAsText()
                Log.d(TAG, "getBillingCompanies response: $responseText")

                if (response.status.isSuccess()) {
                    val result = response.body<LaravelResponse<List<BillingCompany>>>()
                    result.data
                } else {
                    Log.e(TAG, "Failed to fetch companies: ${response.status}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching billing companies: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun setActiveCompany(companyId: Int): Result<BillingCompany> {
        val token = sharedPreferences.getString(TOKEN_KEY, null)
            ?: return Result.failure(Exception("No hay token de sesión"))
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post("m1/user-active-company") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(SetActiveCompanyRequest(companyId))
                }
                val responseText = response.bodyAsText()
                Log.d(TAG, "setActiveCompany response: $responseText")

                if (response.status.isSuccess()) {
                    val result = response.body<LaravelResponse<SetActiveCompanyResponseData>>()
                    val company = result.data.company
                    if (company != null) {
                        saveSelectedCompany(company)
                        Result.success(company)
                    } else {
                        Result.failure(Exception("Respuesta sin datos de empresa"))
                    }
                } else {
                    Result.failure(Exception("Error al cambiar empresa: ${response.status.value}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting active company: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    fun saveSelectedCompany(company: BillingCompany) {
        sharedPreferences.edit()
            .putInt("active_company_id", company.id)
            .putString("active_company_name", company.name)
            .putString("active_company_ruc", company.ruc ?: "")
            .apply()
    }

    fun getSavedActiveCompanyId(): Int {
        return sharedPreferences.getInt("active_company_id", -1)
    }

    fun getSavedActiveCompanyName(): String? {
        return sharedPreferences.getString("active_company_name", null)
    }

    fun getSavedActiveCompanyRuc(): String? {
        return sharedPreferences.getString("active_company_ruc", null)
    }
}
