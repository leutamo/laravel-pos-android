package com.example.laravelpos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.Customer
import com.example.laravelpos.data.model.CustomerAttributes
import com.example.laravelpos.data.model.CustomerLinks
import com.example.laravelpos.data.model.DocumentType
import com.example.laravelpos.data.repository.DocumentTypeRepository
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
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val documentTypeRepository: DocumentTypeRepository
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

    private val _pagoEfectivo = MutableStateFlow("0.00")
    val pagoEfectivo: StateFlow<String> = _pagoEfectivo.asStateFlow()

    private val _pagoVisa = MutableStateFlow("0.00")
    val pagoVisa: StateFlow<String> = _pagoVisa.asStateFlow()

    private val _totalRecibido = MutableStateFlow("0.00")
    val totalRecibido: StateFlow<String> = _totalRecibido.asStateFlow()

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
                _customerData.value = customer
                _toastEvent.emit("Cliente creado: ${customer.attributes.name}")
            } catch (e: Exception) {
                Log.e("CheckoutViewModel", "Error al crear cliente: ${e.message}")
                _toastEvent.emit("Error al crear cliente: ${e.message}")
            } finally {
                _isLoadingCustomer.value = false
            }
        }
    }

    // ✅ LÓGICA PRINCIPAL (INIT) - Ajustada para simular llamada a API con Toast
    init {
        viewModelScope.launch {
            combine(dniText.debounce(500), selectedDocumentType) { dni, docType ->
                Pair(dni, docType)
            }.collect { (dni, docType) ->
                Log.d("CheckoutViewModel", "DNI recibido: $dni, Tipo: $docType") // Log de entrada

                val requiredLength = when (docType) {
                    "DNI" -> 8
                    "RUC" -> 11
                    else -> {
                        Log.d("CheckoutViewModel", "Tipo de documento no válido o null: $docType")
                        0
                    }
                }

                Log.d("CheckoutViewModel", "Required length: $requiredLength, DNI length: ${dni.length}") // Log del cálculo

                if (dni.length == requiredLength && requiredLength > 0) {
                    _isLoadingCustomer.value = true
                    Log.d("CheckoutViewModel", "Iniciando búsqueda para DNI: $dni") // Log de confirmación
                    try {
                        // Simulación de llamada a API
                        _isLoadingCustomer.value = true // Indica que está procesando
                        // Simula un retraso como si fuera una llamada a API
                        delay(1000) // Retraso de 1 segundo para simular
                        _customerData.value = Customer( // Ejemplo temporal con datos básicos
                            type = "customers",
                            id = 1, // ID simulado
                            attributes = CustomerAttributes(
                                name = "Cliente $dni",
                                email = "$dni@example.com",
                                phone = "987654321",
                                country = "Perú",
                                city = "Lima",
                                address = "Av. Siempre Viva 123",
                                document_number = dni,
                                document_type_id = if (docType == "DNI") 1 else 2, // Ejemplo de mapeo
                                created_at = OffsetDateTime.now().toString(),
                                updated_at = OffsetDateTime.now().toString()
                            ),
                            links = CustomerLinks(self = "http://api.example.com/customers/$dni")
                        )
                        _toastEvent.emit("Cliente encontrado para DNI: $dni") // Toast simulado
                    } catch (e: Exception) {
                        Log.e("CheckoutViewModel", "Error en búsqueda simulada: ${e.message}")
                        _toastEvent.emit("Error al buscar cliente: ${e.message}")
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
    fun updatePagoEfectivo(text: String) { _pagoEfectivo.value = text }
    fun updatePagoVisa(text: String) { _pagoVisa.value = text }
    fun updateTotalRecibido(text: String) { _totalRecibido.value = text }
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
    fun processCheckout(totalAmount: Double, selectedReceiptType: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Aquí llamas a tu API de checkout
                // val response = checkoutRepository.checkout(...)
                // _navigateToSummary.value = response.id
                _navigateToSummary.value = "123" // Simulación
            } catch (e: Exception) {
                _apiError.value = e.message ?: "Error desconocido"
            } finally {
                _isLoading.value = false
            }
        }
    }
}