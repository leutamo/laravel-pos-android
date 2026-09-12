package com.example.laravelpos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.Product
import com.example.laravelpos.data.model.CartItem
import com.example.laravelpos.data.model.ProductConversion
import com.example.laravelpos.data.model.QuotationItem
import com.example.laravelpos.data.model.QuotationRequest
import com.example.laravelpos.data.repository.ProductRepository
import com.example.laravelpos.data.repository.QuotationRepository
import com.example.laravelpos.data.config.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.FlowPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val quotationRepository: QuotationRepository,
    private val serverConfig: ServerConfig
) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    fun getFullImageUrl(relativePath: String?): String {
        val fullUrl = serverConfig.getFullImageUrl(relativePath)
        if (!relativePath.isNullOrBlank()) {
            Log.d(TAG, "Generating image URL: $fullUrl (from original: $relativePath)")
        }
        return fullUrl
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Estado para los ítems del carrito (Refactorizado a CartItem)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // PAra completar la navegacion
    private val _navigateToSummary = MutableStateFlow<Int?>(null)
    val navigateToSummary: StateFlow<Int?> = _navigateToSummary.asStateFlow()

    // Para el modal de tipo de comprobante
    private val _selectedReceiptType = MutableStateFlow<String?>(null)
    val selectedReceiptType: StateFlow<String?> = _selectedReceiptType.asStateFlow()

    private val _showReceiptModal = MutableStateFlow(false)
    val showReceiptModal: StateFlow<Boolean> = _showReceiptModal.asStateFlow()

    // Estados para la llamada a la API
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    // La lista filtrada ahora simplemente expone lo que devuelve el servidor
    val filteredProducts: StateFlow<List<Product>> = _products.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized, starting search observer")
        // Escuchar cambios en la búsqueda con debounce (evita peticiones excesivas)
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            searchQuery.debounce(500).collect { query ->
                fetchProducts(query)
            }
        }
    }

    fun fetchProducts(search: String? = _searchQuery.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _apiError.value = null
            Log.d(TAG, "fetchProducts: Requesting products from server (search: $search)...")
            val result = repository.getProducts(search)
            
            result.onSuccess { productList ->
                Log.d(TAG, "Server responded with ${productList.size} products")
                _products.value = productList
            }.onFailure { exception ->
                Log.e(TAG, "Error fetching products: ${exception.message}")
                _apiError.value = exception.message
                _products.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.update { query }
    }


    // Función para agregar un producto al carrito
    fun addItemToCart(product: Product) {
        // Limpiamos la búsqueda al agregar un producto
        onSearchQueryChanged("")

        _cartItems.update { currentItems ->
            val existingItem = currentItems.find { it.product.id == product.id && it.selectedConversion == null }
            if (existingItem != null) {
                currentItems.map { 
                    if (it === existingItem) it.copy(quantity = it.quantity + 1) else it 
                }
            } else {
                currentItems + CartItem(product, 1)
            }
        }
    }

    fun incrementProduct(cartItem: CartItem) {
        _cartItems.update { currentItems ->
            currentItems.map { 
                if (it === cartItem) it.copy(quantity = it.quantity + 1) else it 
            }
        }
    }

    fun decrementProduct(cartItem: CartItem) {
        _cartItems.update { currentItems ->
            currentItems.mapNotNull { 
                if (it === cartItem) {
                    if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
                } else it 
            }
        }
    }
    
    fun changeItemUnit(cartItem: CartItem, conversion: ProductConversion?) {
        _cartItems.update { currentItems ->
            currentItems.map { 
                if (it === cartItem) it.copy(selectedConversion = conversion, customPrice = null) else it 
            }
        }
    }

    fun changeItemPrice(cartItem: CartItem, newPrice: Double?) {
        _cartItems.update { currentItems ->
            currentItems.map { 
                if (it === cartItem) it.copy(customPrice = newPrice) else it
            }
        }
    }

    // Compatibilidad para UI que busca por producto
    fun getProductCount(product: Product): Int {
        return _cartItems.value.filter { it.product.id == product.id }.sumOf { it.quantity }
    }

    // Esta función utiliza el subtotal calculado en CartItem
    fun calculateItemTotal(cartItem: CartItem): Double {
        return cartItem.subTotal
    }

    // Variables para calcular el total y el IGV
    val totalAmount: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.subTotal }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val igvAmount: StateFlow<Double> = totalAmount.map { total ->
        total * 0.18 // Ejemplo de IGV del 18%
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    /**
     * Modal Tipo de comprobante
     */
    fun showReceiptModal() {
        _showReceiptModal.value = true
    }

    fun hideReceiptModal() {
        _showReceiptModal.value = false
    }

    fun selectReceiptType(type: String?) {
        _selectedReceiptType.value = type
        hideReceiptModal()
    }

    // Nueva función para borrar el error
    fun clearApiError() {
        _apiError.value = null
    }


    // Nueva función para procesar el pago y hacer la llamada a la API
    fun processCheckout() {
        viewModelScope.launch {
            _isLoading.value = true
            _apiError.value = null // Limpiar errores anteriores
            try {
                // Obtener los datos del carrito
                val items = _cartItems.value
                val currentTotal = totalAmount.value
                val currentIgv = igvAmount.value

                // Construir la lista de items para la petición
                val quotationItems = items.map { cartItem ->
                    val product = cartItem.product
                    val quantity = cartItem.quantity
                    val subTotal = cartItem.subTotal
                    val unitPrice = cartItem.unitPrice
                    
                    // Calculamos el precio neto (sin IGV) para coincidir con la plataforma
                    val netUnitPrice = unitPrice / 1.18
                    val taxAmount = subTotal - (netUnitPrice * quantity)

                    QuotationItem(
                        productId = product.id,
                        quantity = quantity,
                        productPrice = String.format("%.2f", unitPrice),
                        netUnitPrice = String.format("%.2f", netUnitPrice),
                        taxType = 1, 
                        taxValue = "18.00",
                        taxAmount = String.format("%.2f", taxAmount),
                        discountType = 2,
                        discountValue = "0.00",
                        discountAmount = "0.00",
                        saleUnit = cartItem.selectedConversion?.toUnitId ?: product.attributes.sale_unit_name.id,
                        subTotal = String.format("%.2f", subTotal)
                    )
                }

                // Obtener la fecha en formato ISO (como en la plataforma)
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val currentDate = isoFormat.format(Date())

                // Construir el cuerpo de la petición basado en los screenshots
                val requestBody = QuotationRequest(
                    date = currentDate,
                    customerId = 6, 
                    warehouseId = 1, 
                    status = 1, 
                    taxRate = "18.00",
                    taxAmount = String.format("%.2f", currentIgv),
                    discount = "0.00",
                    shipping = "0.00",
                    grandTotal = String.format("%.2f", currentTotal),
                    receivedAmount = 0.0,
                    paidAmount = 0.0,
                    note = "",
                    quotationItems = quotationItems
                )

                // Realizar la llamada a la API
                val response = quotationRepository.createQuotation(requestBody)

                // Manejar la respuesta
                if (response.success) {
                    Log.d(TAG, "Cotización creada con éxito: ${response.data?.id}")
                    _navigateToSummary.value = response.data?.id
                    clearCart() // Limpiamos el carrito tras éxito
                } else {
                    _apiError.value = response.message
                    Log.e(TAG, "Error al crear cotización: ${response.message}")
                }
            } catch (e: Exception) {
                _apiError.value = e.message
                Log.e(TAG, "Error en el proceso de checkout: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSummaryNavigated() {
        _navigateToSummary.value = null
    }
}
