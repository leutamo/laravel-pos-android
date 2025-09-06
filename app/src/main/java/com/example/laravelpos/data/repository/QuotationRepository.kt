package com.example.laravelpos.data.repository

import android.content.SharedPreferences
import android.util.Log
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

private const val TAG = "QuotationRepository"
const val TOKEN_KEY = "auth_token"

@Serializable
data class QuotationItem(
    @SerialName("product_id") val productId: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("product_price") val productPrice: Double,
    @SerialName("net_unit_price") val netUnitPrice: Double,
    @SerialName("tax_type") val taxType: Int,
    @SerialName("tax_value") val taxValue: Double,
    @SerialName("tax_amount") val taxAmount: Double,
    @SerialName("discount_type") val discountType: Int,
    @SerialName("discount_value") val discountValue: Double,
    @SerialName("discount_amount") val discountAmount: Double,
    @SerialName("sale_unit") val saleUnit: Int,
    @SerialName("sub_total") val subTotal: Double
)

// Definimos la clase de datos para la solicitud de cotización
@Serializable
data class QuotationRequest(
    @SerialName("date") val date: String,
    @SerialName("customer_id") val customerId: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("status") val status: String,
    @SerialName("tax_rate") val taxRate: Double,
    @SerialName("tax_amount") val taxAmount: Double,
    @SerialName("discount") val discount: Double,
    @SerialName("shipping") val shipping: Double,
    @SerialName("grand_total") val grandTotal: Double,
    @SerialName("received_amount") val receivedAmount: Double,
    @SerialName("paid_amount") val paidAmount: Double,
    @SerialName("quotation_items") val quotationItems: List<QuotationItem>
)

// Este es el modelo para la respuesta que te da el backend.
// Deberás actualizarlo para que coincida con tu API.
@Serializable
data class QuotationResponse(
    val success: Boolean,
    val message: String
)

// Modelos para la respuesta que te da el backend.
@Serializable
data class ApiResponse(
    val data: QuotationData,
)

@Serializable
data class QuotationData(
    val type: String,
    val id: Int,
    val attributes: QuotationAttributes,
    val links: Links
)

@Serializable
data class QuotationAttributes(
    val date: String,
    @SerialName("customer_id")
    val customerId: Int,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("warehouse_id")
    val warehouseId: Int,
    @SerialName("warehouse_name")
    val warehouseName: String,
    @SerialName("tax_rate")
    val taxRate: Int,
    @SerialName("tax_amount")
    val taxAmount: Double, // Corregido a Double
    val discount: Int,
    val shipping: Int,
    @SerialName("grand_total")
    val grandTotal: Double, // Corregido a Double
    @SerialName("received_amount")
    val receivedAmount: Int,
    @SerialName("paid_amount")
    val paidAmount: Int,
    val note: String?,
    val status: String,
    @SerialName("is_sale_created")
    val isSaleCreated: String?,
    @SerialName("reference_code")
    val referenceCode: String,
    @SerialName("quotation_items")
    val quotationItems: List<QuotationItemResponse>,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class QuotationItemResponse(
    val id: Int,
    @SerialName("quotation_id")
    val quotationId: Int,
    @SerialName("product_id")
    val productId: Int,
    @SerialName("product_price")
    val productPrice: Int,
    @SerialName("net_unit_price")
    val netUnitPrice: Double, // Corregido a Double
    @SerialName("tax_type")
    val taxType: Int,
    @SerialName("tax_value")
    val taxValue: Double, // Corregido a Double
    @SerialName("tax_amount")
    val taxAmount: Double, // Corregido a Double
    @SerialName("discount_type")
    val discountType: Int,
    @SerialName("discount_value")
    val discountValue: Int,
    @SerialName("discount_amount")
    val discountAmount: Int,
    @SerialName("sale_unit")
    val saleUnit: SaleUnit,
    val quantity: Int,
    @SerialName("sub_total")
    val subTotal: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class SaleUnit(
    val id: Int,
    val name: String,
    @SerialName("short_name")
    val shortName: String,
    @SerialName("base_unit")
    val baseUnit: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class Links(
    val self: String
)

@Serializable
data class QuotationApiResult(
    val success: Boolean,
    val message: String,
    val data: QuotationData? // Usa '?' para manejar el caso de que la data sea nula en errores
)

class QuotationRepository @Inject constructor(
    private val client: HttpClient,
    private val sharedPreferences: SharedPreferences
) {

    // Actualizamos el tipo de retorno a QuotationApiResult
    suspend fun createQuotation(request: QuotationRequest): QuotationApiResult {
        Log.d(TAG, "Starting quotation creation...")
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        Log.d(TAG, "Authorization token: ${token?.take(10)}...")
        Log.d(TAG, "Sending request body: $request")

        return withContext(Dispatchers.IO) {
            if (token != null) {
                try {
                    val response = client.post("http://192.168.100.68:8000/api/quotations") {
                        header("Authorization", "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                    val responseText = response.bodyAsText()
                    Log.d(TAG, "Received response with status: ${response.status.value}")
                    Log.d(TAG, "Response body: $responseText")

                    if (response.status.isSuccess()) {
                        // Si la respuesta es exitosa, deserializamos a ApiResponse, que solo contiene 'data'
                        val apiResponse = response.body<ApiResponse>()
                        QuotationApiResult(
                            success = true,
                            message = "Cotización creada exitosamente",
                            data = apiResponse.data
                        )
                    } else {
                        // Si la respuesta es un error, el cuerpo contiene 'message'
                        val errorMessage = try {
                            val errorBody = response.body<Map<String, String>>()
                            errorBody["message"] ?: responseText
                        } catch (e: Exception) {
                            responseText
                        }
                        QuotationApiResult(
                            success = false,
                            message = errorMessage,
                            data = null
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating quotation: ${e.message}", e)
                    QuotationApiResult(
                        success = false,
                        message = e.message ?: "Error desconocido",
                        data = null
                    )
                }
            } else {
                Log.e(TAG, "No auth token found.")
                QuotationApiResult(
                    success = false,
                    message = "No se encontró el token de autenticación.",
                    data = null
                )
            }
        }
    }
}