package com.example.laravelpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.DocumentType
import com.example.laravelpos.data.repository.DocumentTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val documentTypeRepository: DocumentTypeRepository
) : ViewModel() {

    // Estado para tipos de documento
    private val _documentTypes = MutableStateFlow<List<DocumentType>>(emptyList())
    val documentTypes: StateFlow<List<DocumentType>> = _documentTypes.asStateFlow()

    private val _isLoadingDocumentTypes = MutableStateFlow(false)
    val isLoadingDocumentTypes: StateFlow<Boolean> = _isLoadingDocumentTypes.asStateFlow()

    // ✅ FUNCIÓN CORREGIDA — ¡IMPLEMENTADA!
    fun loadDocumentTypes() {
        viewModelScope.launch {
            _isLoadingDocumentTypes.value = true
            val types = documentTypeRepository.getDocumentTypes()
            _documentTypes.value = types
            _isLoadingDocumentTypes.value = false
        }
    }

    // Estado del formulario (¡esto es nuevo y valioso!)
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

    // Lógica de procesar checkout (aquí iría tu llamada al API)
    fun processCheckout(totalAmount: Double, selectedReceiptType: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Aquí llamas a tu API de checkout
                // val response = checkoutRepository.checkout(...)
                // _navigateToSummary.value = response.id

                // Por ahora simulamos:
                _navigateToSummary.value = "123"
            } catch (e: Exception) {
                _apiError.value = e.message ?: "Error desconocido"
            } finally {
                _isLoading.value = false
            }
        }
    }
}