package com.example.laravelpos.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillingCompany(
    val id: Int,
    val name: String,
    @SerialName("razon_social") val razonSocial: String? = null,
    val ruc: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("invoice_series") val invoiceSeries: String? = null,
    @SerialName("invoice_next_correlative") val invoiceNextCorrelative: Int? = null,
    @SerialName("boleta_series") val boletaSeries: String? = null,
    @SerialName("boleta_next_correlative") val boletaNextCorrelative: Int? = null,
    @SerialName("is_default") val isDefault: Boolean? = false,
    @SerialName("is_active") val isActive: Boolean? = true
)

@Serializable
data class SetActiveCompanyRequest(
    @SerialName("company_id") val companyId: Int
)

@Serializable
data class SetActiveCompanyResponseData(
    @SerialName("user_id") val userId: Int? = null,
    @SerialName("default_company_id") val defaultCompanyId: Int? = null,
    val company: BillingCompany? = null
)
