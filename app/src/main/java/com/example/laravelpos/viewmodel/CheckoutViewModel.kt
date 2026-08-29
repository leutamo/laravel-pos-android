package com.example.laravelpos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.Customer
import com.example.laravelpos.data.model.CustomerAttributes
import com.example.laravelpos.data.model.CustomerLinks
import com.example.laravelpos.data.model.DocumentType
import com.example.laravelpos.data.model.Product
import com.example.laravelpos.data.model.QuotationItem
import com.example.laravelpos.data.model.QuotationRequest
import com.example.laravelpos.data.repository.CustomerRepository
import com.example.laravelpos.data.repository.DocumentTypeRepository
import com.example.laravelpos.data.repository.QuotationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val documentTypeRepository: DocumentTypeRepository,
    private val quotationRepository: QuotationRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    // Estado para tipos de documento
    private val _documentTypes = MutableStateFlow<List<DocumentType>>(emptyList())
    val documentTypes: StateFlow<List<DocumentType>> = _documentTypes.asStateFlow()

    // ✅ ESTADO PARA EL TIPO DE DOCUMENTO SELECCIONADO
    private val _selectedDocumentType = MutableStateFlow<String?>(null)
    val selectedDocumentType: StateFlow<String?> = _selectedDocumentType.asStateFlow()

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

    // ✅ NUEVO: SharedFlow para enviar eventos de un solo uso (como un Toast)
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Estado del formulario
    private val _dniText = MutableStateFlow("")
    val dniText: StateFlow<String> = _dniText.asStateFlow()

    private val _pagoContado = MutableStateFlow(true)
    val pagoContado: StateFlow<Boolean> = _pagoContado.asStateFlow()

    // ✅ FUNCIÓN CORREGIDA — ¡IMPLEMENTADA!
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

    // ✅ NUEVA FUNCIÓN: Para actualizar el tipo de documento seleccionado
    fun updateSelectedDocumentType(type: String?) {
        Log.d("CheckoutViewModel", "Actualizando tipo de documento a: $type")
        _selectedDocumentType.value = type
        _isDniFieldEnabled.value = type != null // Habilita el campo DNI solo si hay un tipo seleccionado
    }
    // Nueva función para crear un cliente
    fun createCustomer(customer: Customer) {
        viewModelScope.launch {
            _isLoadingCustomer.value = true
            try {
                val createdCustomer = customerRepository.createCustomer(customer)
                if (createdCustomer != null) {
                    _customerData.value = createdCustomer
                    _toastEvent.emit("Cliente creado: ${createdCustomer.attributes.name}")
                } else {
                    _toastEvent.emit("Error al crear cliente en el servidor")
                }
            } catch (e: Exception) {
                Log.e("CheckoutViewModel", "Error al crear cliente: ${e.message}")
                _toastEvent.emit("Error al crear cliente: ${e.message}")
            } finally {
                _isLoadingCustomer.value = false
            }
        }
    }

    // ✅ LÓGICA PRINCIPAL (INIT) - Búsqueda real en la base de datos
    init {
        viewModelScope.launch {
            combine(dniText.debounce(500), selectedDocumentType) { dni, docType ->
                Pair(dni, docType)
            }.collect { (dni, docType) ->
                val requiredLength = when (docType) {
                    "DNI" -> 8
                    "RUC" -> 11
                    else -> 0
                }

                if (dni.length == requiredLength && requiredLength > 0) {
                    _isLoadingCustomer.value = true
                    try {
                        val customer = customerRepository.searchCustomer(dni)
                        if (customer != null) {
                            _customerData.value = customer
                            _toastEvent.emit("Cliente encontrado: ${customer.attributes.name}")
                        } else {
                            _customerData.value = null
                            _toastEvent.emit("Cliente no registrado")
                        }
                    } catch (e: Exception) {
                        Log.e("CheckoutViewModel", "Error al buscar cliente: ${e.message}")
                        _toastEvent.emit("Error en la búsqueda")
                    } finally {
                        _isLoadingCustomer.value = false
                    }
                } else {
                    _customerData.value = null
                }
            }
        }
    }

    // Funciones para actualizar
    fun updateDni(text: String) { _dniText.value = text }
    fun updatePagoContado(isContado: Boolean) { _pagoContado.value = isContado }

    // Estado para errores y navegación
    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _navigateToSummary = MutableStateFlow<String?>(null)
    val navigateToSummary: StateFlow<String?> = _navigateToSummary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun clearApiError() { _apiError.value = null }
    fun onSummaryNavigated() { _navigateToSummary.value = null }

    // Lógica de procesar checkout
    fun processCheckout(
        totalAmount: Double,
        selectedReceiptType: String?,
        cartItems: List<Product>,
        itemQuantities: Map<String, Int>
    ) {
        // Obtenemos el cliente seleccionado. 
        // Si no hay uno, usamos el ID 6 como cliente de prueba (según plataforma web)
        val customerId = _customerData.value?.id ?: 6 

        viewModelScope.launch {
            _isLoading.value = true
            _apiError.value = null
            try {
                // 1. Mapear items del carrito al formato de Cotización
                val quotationItems = cartItems.map { product ->
                    val quantity = itemQuantities[product.id.toString()] ?: 0
                    val subTotal = product.attributes.product_price * quantity
                    
                    // Ajuste de impuestos (IGV 18%) similar a HomeViewModel
                    val netUnitPrice = product.attributes.product_price / 1.18
                    val taxAmount = subTotal - (netUnitPrice * quantity)

                    QuotationItem(
                        productId = product.id,
                        quantity = quantity,
                        productPrice = String.format("%.2f", product.attributes.product_price),
                        netUnitPrice = String.format("%.2f", netUnitPrice),
                        taxType = 1, // Gravado
                        taxValue = "18.00",
                        taxAmount = String.format("%.2f", taxAmount),
                        discountType = 2,
                        discountValue = "0.00",
                        discountAmount = "0.00",
                        saleUnit = 1,
                        subTotal = String.format("%.2f", subTotal)
                    )
                }

                // 2. Formatear fecha ISO
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val currentDate = isoFormat.format(Date())

                // 3. Calcular impuestos totales
                val totalIgv = totalAmount - (totalAmount / 1.18)

                // 4. Construir el Request (Payload igual al de la plataforma web)
                val request = QuotationRequest(
                    date = currentDate,
                    customerId = customerId,
                    warehouseId = 1, 
                    status = 1, // Enviado
                    taxRate = "18.00",
                    taxAmount = String.format("%.2f", totalIgv),
                    discount = "0.00",
                    shipping = "0.00",
                    grandTotal = String.format("%.2f", totalAmount),
                    receivedAmount = 0.0,
                    paidAmount = 0.0,
                    note = "Cotización desde App Android",
                    quotationItems = quotationItems
                )

                // 5. Llamar al repositorio
                val result = quotationRepository.createQuotation(request)

                if (result.success) {
                    Log.d("CheckoutViewModel", "Cotización creada ID: ${result.data?.id}")
                    _toastEvent.emit("Cotización realizada con éxito")
                    _navigateToSummary.value = result.data?.id.toString()
                } else {
                    _apiError.value = result.message
                }
            } catch (e: Exception) {
                Log.e("CheckoutViewModel", "Error en checkout: ${e.message}", e)
                _apiError.value = "Error al procesar la cotización: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}