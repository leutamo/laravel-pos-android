package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.laravelpos.data.model.LaravelResponse
import com.example.laravelpos.data.model.WarehouseItem
import com.example.laravelpos.data.repository.ProductRepository.Companion.TOKEN_KEY
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WarehouseRepository"

@Singleton
class WarehouseRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {
    suspend fun getWarehouses(): List<WarehouseItem> {
        val token = sharedPreferences.getString(TOKEN_KEY, null) ?: return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get("m1/warehouses") {
                    header("Authorization", "Bearer $token")
                }
                val responseText = response.bodyAsText()
                Log.d(TAG, "getWarehouses response: $responseText")

                if (response.status.isSuccess()) {
                    val result = response.body<LaravelResponse<List<WarehouseItem>>>()
                    result.data
                } else {
                    Log.e(TAG, "Failed to fetch warehouses: ${response.status}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching warehouses: ${e.message}", e)
                emptyList()
            }
        }
    }

    fun saveSelectedWarehouse(warehouse: WarehouseItem) {
        sharedPreferences.edit()
            .putInt("active_warehouse_id", warehouse.id)
            .putString("active_warehouse_name", warehouse.name)
            .apply()
    }

    fun getSavedActiveWarehouseId(): Int {
        return sharedPreferences.getInt("active_warehouse_id", -1)
    }

    fun getSavedActiveWarehouseName(): String? {
        return sharedPreferences.getString("active_warehouse_name", null)
    }
}
