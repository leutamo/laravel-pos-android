package com.example.laravelpos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val data: List<Product>
)

@Serializable
data class ErrorResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class Product(
    val type: String,
    val id: Int,
    val attributes: ProductAttributes,
    val links: ProductLinks
)

@Serializable
data class ProductAttributes(
    val name: String,
    val code: String,
    val product_code: String,
    val main_product_id: Int,
    val product_category_id: Int,
    val brand_id: Int,
    val product_cost: Double,
    val product_price: Double,
    val product_unit: String,
    val sale_unit: String,
    val purchase_unit: String,
    val stock_alert: String,
    val quantity_limit: String?,
    val order_tax: Int,
    val tax_type: String,
    val notes: String?,
    val images: List<String>, // Podría ser una lista vacía o más compleja, ajusta según necesites
    val product_category_name: String,
    val brand_name: String,
    val barcode_image_url: String,
    val barcode_symbol: Int,
    val created_at: String,
    val product_unit_name: UnitName,
    val purchase_unit_name: UnitName,
    val sale_unit_name: UnitName,
    val stock: Stock?,
    val warehouse: List<Warehouse>,
    val barcode_url: String,
    val in_stock: Int,
    val variation_product: VariationProduct? = null // Opcional, ya que no todos los productos lo tienen
)

@Serializable
data class UnitName(
    val id: Int,
    val name: String,
    val short_name: String? = null,
    val base_unit: Int? = null,
    val is_default: Int? = null,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class Stock(
    val id: Int,
    val warehouse_id: Int,
    val product_id: Int,
    val quantity: Int,
    val created_at: String,
    val updated_at: String,
    val alert: Int
)

@Serializable
data class Warehouse(
    val total_quantity: Int,
    val name: String
)

@Serializable
data class VariationProduct(
    val main_product_id: Int,
    val product_id: Int,
    val variation_id: Int,
    val variation_type_id: Int,
    val variation_name: String,
    val variation_type_name: String
)

@Serializable
data class ProductLinks(
    val self: String
)