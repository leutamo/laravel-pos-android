package com.example.laravelpos.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class QuotationItem(
    @SerialName("product_id") val productId: Int,
    @SerialName("quantity") val quantity: Int,
    @SerialName("product_price") val productPrice: String,
    @SerialName("net_unit_price") val netUnitPrice: String,
    @SerialName("tax_type") val taxType: Int,
    @SerialName("tax_value") val taxValue: String,
    @SerialName("tax_amount") val taxAmount: String,
    @SerialName("discount_type") val discountType: Int,
    @SerialName("discount_value") val discountValue: String,
    @SerialName("discount_amount") val discountAmount: String,
    @SerialName("sale_unit") val saleUnit: Int,
    @SerialName("sub_total") val subTotal: String
)

@Serializable
data class QuotationRequest(
    @SerialName("date") val date: String,
    @SerialName("customer_id") val customerId: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("status") val status: Int,
    @SerialName("tax_rate") val taxRate: String,
    @SerialName("tax_amount") val taxAmount: String,
    @SerialName("discount") val discount: String,
    @SerialName("shipping") val shipping: String,
    @SerialName("grand_total") val grandTotal: String,
    @SerialName("received_amount") val receivedAmount: Double,
    @SerialName("paid_amount") val paidAmount: Double,
    @SerialName("note") val note: String,
    @SerialName("quotation_items") val quotationItems: List<QuotationItem>
)

@Serializable
data class QuotationResponse(
    val success: Boolean,
    val message: String
)

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
    @SerialName("customer_id") val customerId: Int,
    @SerialName("customer_name") val customerName: String,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("warehouse_name") val warehouseName: String,
    @SerialName("tax_rate") val taxRate: Double,
    @SerialName("tax_amount") val taxAmount: Double,
    val discount: Double,
    val shipping: Double,
    @SerialName("grand_total") val grandTotal: Double,
    @SerialName("received_amount") val receivedAmount: Double,
    @SerialName("paid_amount") val paidAmount: Double,
    val note: String?,
    val status: JsonElement, // Cambiado a JsonElement para aceptar Int o String
    @SerialName("is_sale_created") val isSaleCreated: JsonElement? = null,
    @SerialName("reference_code") val referenceCode: String,
    @SerialName("quotation_items") val quotationItems: List<QuotationItemResponse>? = null,
    @SerialName("sale_items") val saleItems: List<QuotationItemResponse>? = null, // Para ventas directas
    @SerialName("created_at") val createdAt: String
) {
    // Propiedad calculada para obtener los ítems sin importar el nombre del campo
    val items: List<QuotationItemResponse> get() = quotationItems ?: saleItems ?: emptyList()
}

@Serializable
data class QuotationItemResponse(
    val id: Int,
    @SerialName("quotation_id") val quotationId: Int? = null,
    @SerialName("sale_id") val saleId: Int? = null,
    @SerialName("product_id") val productId: Int,
    @SerialName("product_price") val productPrice: Double,
    @SerialName("net_unit_price") val netUnitPrice: Double,
    @SerialName("tax_type") val taxType: Int,
    @SerialName("tax_value") val taxValue: Double,
    @SerialName("tax_amount") val taxAmount: Double,
    @SerialName("discount_type") val discountType: Int,
    @SerialName("discount_value") val discountValue: Double,
    @SerialName("discount_amount") val discountAmount: Double,
    @SerialName("sale_unit") val saleUnit: SaleUnit,
    val quantity: Int,
    @SerialName("sub_total") val subTotal: Double,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class SaleUnit(
    val id: Int,
    val name: String,
    @SerialName("short_name") val shortName: String,
    @SerialName("base_unit") val baseUnit: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class Links(
    val self: String
)
