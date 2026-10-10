package com.example.laravelpos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.CartItem
import com.example.laravelpos.data.model.Customer
import com.example.laravelpos.data.model.CustomerLinks
import com.example.laravelpos.data.model.DocumentType
import com.example.laravelpos.data.model.Product
import com.example.laravelpos.data.model.QuotationItem
import com.example.laravelpos.data.model.QuotationRequest
import com.example.laravelpos.data.model.SaleItem
import com.example.laravelpos.data.model.SaleRequest
import com.example.laravelpos.data.repository.BillingCompanyRepository
import com.example.laravelpos.data.repository.CustomerRepository
import com.example.laravelpos.data.repository.CustomerResult
import com.example.laravelpos.data.repository.DocumentTypeRepository
import com.example.laravelpos.data.repository.LoginRepository
import com.example.laravelpos.data.repository.QuotationRepository
import com.example.laravelpos.data.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val documentTypeRepository: DocumentTypeRepository,
    private val quotationRepository: QuotationRepository,
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val loginRepository: LoginRepository,
    private val billingCompanyRepository: BillingCompanyRepository
) : ViewModel() {

    // Estado para tipos de documento
    private val _documentTypes = MutableStateFlow<List<DocumentType>>(emptyList())
    val documentTypes: StateFlow<List<DocumentType>> = _documentTypes.asStateFlow()

    // ✅ ESTADO PARA EL TIPO DE DOCUMENTO SELECCIONADO (Objeto completo)
    private val _selectedDocumentType = MutableStateFlow<DocumentType?>(null)
    val selectedDocumentType: StateFlow<DocumentType?> = _selectedDocumentType.asStateFlow()

    // ✅ NUEVO: Estado para controlar si el campo DNI está habilitado
    private val _isDniFieldEnabled = MutableStateFlow(false)
    val isDniFieldEnabled: StateFlow<Boolean> = _isDniFieldEnabled.asStateFlow()

    // ✅ NUEVOS ESTADOS PARA BUSCAR CLIENTES
    private val _customerData = MutableStateFlow<Customer?>(null)
    val customerData: StateFlow<Customer?> = _customerData.asStateFlow()

    private val _isLoadingCustomer = MutableStateFlow(false)
    val isLoadingCustomer: StateFlow<Boolean> = _isLoadingCustomer.asStateFlow()

    private val _isLoadingDocumentTypes = MutableStateFlow(false)
    val isLoadingDocumentTypes: StateFlow<Boolean> = _isLoadingDocumentTypes.asStateFlow()

    // Estado del formulario
    private val _dniText = MutableStateFlow("")
    val dniText: StateFlow<String> = _dniText.asStateFlow()

    private val _pagoContado = MutableStateFlow(true)
    val pagoContado: StateFlow<Boolean> = _pagoContado.asStateFlow()

    // ✅ Cargar tipos de documento
    fun loadDocumentTypes() {
        viewModelScope.launch {
            _isLoadingDocumentTypes.value = true
            try {
                val types = documentTypeRepository.getDocumentTypes()
                _documentTypes.value = types
                Log.d("CheckoutViewModel", "Tipos de documento cargados: $types")
            } catch (e: Exception) {
                Log.e("CheckoutViewModel", "Error al cargar tipos de documento: ${e.message}")
            } finally {
                _isLoadingDocumentTypes.value = false
            }
        }
    }

    // ✅ Actualizar tipo de documento seleccionado
    fun updateSelectedDocumentType(type: DocumentType?) {
        Log.d("CheckoutViewModel", "Actualizando tipo de documento a: ${type?.name}")
        _selectedDocumentType.value = type
        _isDniFieldEnabled.value = type != null
    }

    // ✅ Crear cliente real usando el repositorio mejorado
    fun createCustomer(customer: Customer) {
        viewModelScope.launch {
            _isLoadingCustomer.value = true
            try {
                when (val result = customerRepository.createCustomer(customer)) {
                    is CustomerResult.Success -> {
                        _customerData.value = result.customer
                    }
                    is CustomerResult.Error -> {
                        _apiError.value = result.message
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckoutViewModel", "Error inesperado al crear cliente: ${e.message}")
                _apiError.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoadingCustomer.value = false
            }
        }
    }

    // ✅ Búsqueda real de clientes en el backend
    init {
        viewModelScope.launch {
            combine(dniText.debounce(500), selectedDocumentType) { dni, docType ->
                Pair(dni, docType)
            }.collect { (dni, docType) ->
                val requiredLength = when (docType?.name) {
                    "DNI" -> 8
                    "RUC" -> 11
                    "CARNET EXT." -> 12 // Ajustar según sea necesario
                    else -> 0
                }

                if (dni.length == requiredLength && requiredLength > 0) {
                    // Solo buscamos si el DNI es diferente al del cliente ya cargado
                    // para evitar bucles o sobreescritura al usar el botón "Genérico"
                    if (_customerData.value?.attributes?.document_number != dni) {
                        _isLoadingCustomer.value = true
                        try {
                            val customer = customerRepository.searchCustomer(dni)
                            if (customer != null) {
                                _customerData.value = customer
                            } else {
                                _customerData.value = null
                            }
                        } catch (e: Exception) {
                            Log.e("CheckoutViewModel", "Error al buscar cliente: ${e.message}")
                        } finally {
                            _isLoadingCustomer.value = false
                        }
                    }
                } else if (dni.isEmpty()) {
                    _customerData.value = null
                }
            }
        }
    }

    // Funciones para actualizar
    fun updateDni(text: String) { _dniText.value = text }
    fun updatePagoContado(isContado: Boolean) { _pagoContado.value = isContado }

    /**
     * Selecciona al primer cliente de la base de datos como cliente genérico
     */
    fun selectGenericCustomer() {
        viewModelScope.launch {
            _isLoadingCustomer.value = true
            try {
                val customer = customerRepository.getFirstCustomer()
                if (customer != null) {
                    Log.d("CheckoutViewModel", "Cliente genérico obtenido: ${customer.attributes.name}")
                    
                    // Buscamos el tipo de documento correspondiente en nuestra lista PRIMERO
                    val docType = _documentTypes.value.find { it.id == customer.attributes.document_type_id }
                    
                    // ✅ USAMOS LA FUNCIÓN para que se habilite el campo DNI automáticamente
                    updateSelectedDocumentType(docType)
                    
                    // Luego el DNI y el objeto completo
                    _dniText.value = customer.attributes.document_number
                    _customerData.value = customer
                }
            } catch (e: Exception) {
                Log.e("CheckoutViewModel", "Error al seleccionar cliente genérico: ${e.message}")
            } finally {
                _isLoadingCustomer.value = false
            }
        }
    }

    // Estado para errores y navegación
    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _navigateToSummary = MutableStateFlow<String?>(null)
    val navigateToSummary: StateFlow<String?> = _navigateToSummary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun clearApiError() { _apiError.value = null }
    fun onSummaryNavigated() { _navigateToSummary.value = null }

    /**
     * Limpia la selección del cliente actual para permitir una nueva búsqueda
     */
    fun clearCustomerSelection() {
        _dniText.value = ""
        _customerData.value = null
    }

    /**
     * Limpia todos los datos del checkout para una nueva venta
     */
    fun clearCheckoutData() {
        _dniText.value = ""
        _customerData.value = null
        _selectedDocumentType.value = null
        _isDniFieldEnabled.value = false
        _pagoContado.value = true
        _apiError.value = null
        _navigateToSummary.value = null
    }

    fun getActiveCompanyName(): String? = billingCompanyRepository.getSavedActiveCompanyName()
    fun getActiveCompanyRuc(): String? = billingCompanyRepository.getSavedActiveCompanyRuc()

    // Lógica de procesar checkout
    fun processCheckout(
        totalAmount: Double,
        selectedReceiptType: String?,
        cartItems: List<CartItem>
    ) {
        val customerId = _customerData.value?.id ?: 6 
        val permissions = loginRepository.getUserPermissions()
        val canManageSale = permissions.contains("manage_sale")

        viewModelScope.launch {
            _isLoading.value = true
            _apiError.value = null
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                val currentDate = isoFormat.format(Date())

                // Cálculo dinámico por ítem según la configuración del producto enviada por el servidor
                var calculatedTotalTax = 0.0
                var calculatedGrandTotal = 0.0

                val saleItems = cartItems.map { cartItem ->
                    val product = cartItem.product
                    val quantity = cartItem.quantity
                    val unitPrice = cartItem.unitPrice
                    val taxRate = product.attributes.parsedOrderTax
                    val taxType = product.attributes.parsedTaxType

                    val netUnitPrice: Double
                    val itemTaxAmount: Double
                    val itemSubTotal: Double

                    if (taxRate <= 0.0) {
                        netUnitPrice = unitPrice
                        itemTaxAmount = 0.0
                        itemSubTotal = unitPrice * quantity
                    } else if (taxType == 2) { // Inclusive (IGV Incluido)
                        netUnitPrice = unitPrice / (1.0 + taxRate / 100.0)
                        val taxPerUnit = unitPrice - netUnitPrice
                        itemTaxAmount = taxPerUnit * quantity
                        itemSubTotal = unitPrice * quantity
                    } else { // Exclusive (IGV No Incluido)
                        netUnitPrice = unitPrice
                        val taxPerUnit = unitPrice * (taxRate / 100.0)
                        itemTaxAmount = taxPerUnit * quantity
                        itemSubTotal = (unitPrice + taxPerUnit) * quantity
                    }

                    calculatedTotalTax += itemTaxAmount
                    calculatedGrandTotal += itemSubTotal

                    SaleItem(
                        productId = product.id,
                        quantity = quantity,
                        productPrice = String.format(Locale.US, "%.2f", unitPrice),
                        netUnitPrice = String.format(Locale.US, "%.2f", netUnitPrice),
                        taxType = taxType,
                        taxValue = String.format(Locale.US, "%.2f", taxRate),
                        taxAmount = String.format(Locale.US, "%.2f", itemTaxAmount),
                        discountType = 2,
                        discountValue = "0.00",
                        discountAmount = "0.00",
                        saleUnit = cartItem.selectedConversion?.toUnitId ?: product.attributes.sale_unit_name.id,
                        subTotal = String.format(Locale.US, "%.2f", itemSubTotal)
                    )
                }

                if (canManageSale) {
                    val activeCompanyId = billingCompanyRepository.getSavedActiveCompanyId()
                    val companyId = if (activeCompanyId > 0) activeCompanyId else null

                    val request = SaleRequest(
                        date = currentDate,
                        customerId = customerId,
                        warehouseId = 1,
                        companyId = companyId,
                        taxRate = "0.00",
                        taxAmount = String.format(Locale.US, "%.2f", calculatedTotalTax),
                        discount = "0.00",
                        shipping = "0.00",
                        grandTotal = String.format(Locale.US, "%.2f", calculatedGrandTotal),
                        receivedAmount = "0.00",
                        paidAmount = "0.00",
                        paymentType = 1,
                        status = 1,
                        paymentStatus = 2,
                        note = "Venta directa desde App Android",
                        saleItems = saleItems
                    )

                    val result = saleRepository.createSale(request)
                    if (result.success) {
                        val saleId = result.message 
                        _navigateToSummary.value = "sale|$saleId" 
                    } else {
                        _apiError.value = result.message
                    }
                } else {
                    // REALIZAR COTIZACIÓN
                    val quotationItems = cartItems.map { cartItem ->
                        val product = cartItem.product
                        val quantity = cartItem.quantity
                        val unitPrice = cartItem.unitPrice
                        val taxRate = product.attributes.parsedOrderTax
                        val taxType = product.attributes.parsedTaxType

                        val netUnitPrice = if (taxRate > 0.0 && taxType == 2) {
                            unitPrice / (1.0 + taxRate / 100.0)
                        } else {
                            unitPrice
                        }
                        val itemTaxAmount = if (taxRate > 0.0) {
                            if (taxType == 2) (unitPrice - netUnitPrice) * quantity else (unitPrice * (taxRate / 100.0)) * quantity
                        } else 0.0
                        val itemSubTotal = if (taxRate > 0.0 && taxType == 1) {
                            (unitPrice + unitPrice * (taxRate / 100.0)) * quantity
                        } else {
                            unitPrice * quantity
                        }

                        QuotationItem(
                            productId = product.id,
                            quantity = quantity,
                            productPrice = String.format(Locale.US, "%.2f", unitPrice),
                            netUnitPrice = String.format(Locale.US, "%.2f", netUnitPrice),
                            taxType = taxType,
                            taxValue = String.format(Locale.US, "%.2f", taxRate),
                            taxAmount = String.format(Locale.US, "%.2f", itemTaxAmount),
                            discountType = 2,
                            discountValue = "0.00",
                            discountAmount = "0.00",
                            saleUnit = cartItem.selectedConversion?.toUnitId ?: product.attributes.sale_unit_name.id,
                            subTotal = String.format(Locale.US, "%.2f", itemSubTotal)
                        )
                    }

                    val request = QuotationRequest(
                        date = currentDate,
                        customerId = customerId,
                        warehouseId = 1, 
                        status = 1,
                        taxRate = "0.00",
                        taxAmount = String.format(Locale.US, "%.2f", calculatedTotalTax),
                        discount = "0.00",
                        shipping = "0.00",
                        grandTotal = String.format(Locale.US, "%.2f", calculatedGrandTotal),
                        receivedAmount = 0.0,
                        paidAmount = 0.0,
                        note = "Cotización desde App Android",
                        quotationItems = quotationItems
                    )

                    val result = quotationRepository.createQuotation(request)
                    if (result.success) {
                        val quotationId = result.data?.id.toString()
                        _navigateToSummary.value = "quotation|$quotationId"
                    } else {
                        _apiError.value = result.message
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckoutViewModel", "Error en checkout: ${e.message}", e)
                _apiError.value = "Error al procesar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
