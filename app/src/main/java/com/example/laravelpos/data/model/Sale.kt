package com.example.laravelpos.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaleItem(
    @SerialName("product_id") val productId: Int,
    @SerialName("product_price") val productPrice: String,
    @SerialName("net_unit_price") val netUnitPrice: String,
    @SerialName("tax_type") val taxType: Int,
    @SerialName("tax_value") val taxValue: String,
    @SerialName("tax_amount") val taxAmount: String,
    @SerialName("discount_type") val discountType: Int,
    @SerialName("discount_value") val discountValue: String,
    @SerialName("discount_amount") val discountAmount: String,
    @SerialName("sale_unit") val saleUnit: Int,
    @SerialName("quantity") val quantity: Int,
    @SerialName("sub_total") val subTotal: String
)

@Serializable
data class SaleRequest(
    @SerialName("date") val date: String,
    @SerialName("customer_id") val customerId: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("tax_rate") val taxRate: String,
    @SerialName("tax_amount") val taxAmount: String,
    @SerialName("discount") val discount: String,
    @SerialName("shipping") val shipping: String,
    @SerialName("grand_total") val grandTotal: String,
    @SerialName("received_amount") val receivedAmount: String,
    @SerialName("paid_amount") val paidAmount: String,
    @SerialName("payment_type") val paymentType: Int,
    @SerialName("status") val status: Int,
    @SerialName("payment_status") val paymentStatus: Int,
    @SerialName("note") val note: String,
    @SerialName("sale_items") val saleItems: List<SaleItem>
)

@Serializable
data class SaleResponse(
    val data: SaleData
)

@Serializable
data class SaleData(
    val id: Int,
    val attributes: SaleAttributes
)

@Serializable
data class SaleAttributes(
    @SerialName("reference_code") val referenceCode: String,
    @SerialName("grand_total") val grandTotal: Double,
    @SerialName("created_at") val createdAt: String
)
