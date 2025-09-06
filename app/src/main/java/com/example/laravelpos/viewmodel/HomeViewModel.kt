package com.example.laravelpos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.Product
import com.example.laravelpos.data.repository.ProductRepository
import com.example.laravelpos.data.repository.QuotationItem
import com.example.laravelpos.data.repository.QuotationRepository
import com.example.laravelpos.data.repository.QuotationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val quotationRepository: QuotationRepository
) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Estado para los ítems del carrito
    private val _cartItems = MutableStateFlow<List<Product>>(emptyList())
    val cartItems: StateFlow<List<Product>> = _cartItems.asStateFlow() // Exponemos como StateFlow

    // PAra completar la navegacion
    private val _navigateToSummary = MutableStateFlow<Int?>(null)
    val navigateToSummary: StateFlow<Int?> = _navigateToSummary.asStateFlow()

    // Mapa para almacenar la cantidad de cada producto en el carrito
    private val _itemQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val itemQuantities: StateFlow<Map<String, Int>> = _itemQuantities.asStateFlow()

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


    val filteredProducts: StateFlow<List<Product>>
        get() = combine(_products, _searchQuery) { products, query ->
            if (query.isBlank()) products
            else products.filter { it.attributes.name.contains(query, ignoreCase = true) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        Log.d(TAG, "ViewModel initialized, starting fetchProducts")
        fetchProducts()
    }

    fun fetchProducts() {
        viewModelScope.launch {
            Log.d(TAG, "fetchProducts: Fetching products from repository...")
            val productList = repository.getProducts()
            Log.d(TAG, "Server responded with ${productList.size} products")
            _products.value = productList
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.update { query }
    }


    // Función para agregar un producto al carrito
    fun addItemToCart(product: Product) {
        _cartItems.update { currentItems ->
            // La condición `if (currentItems.any { it.id == product.id })`
            // evita que un producto se añada a esta lista si ya existe.
            // Por eso la lista de productos únicos no aumenta, causando el "mismatch".
            if (currentItems.any { it.id == product.id }) {
                currentItems
            } else {
                // Solo llegas a esta línea la primera vez que añades el producto.
                currentItems + product
            }
        }
        _itemQuantities.update { currentQuantities ->
            val productIdAsString = product.id.toString() // Convertimos el ID a String
            val currentCount = currentQuantities[productIdAsString] ?: 0
            currentQuantities + (productIdAsString to currentCount + 1)
        }
    }

    fun incrementProduct(product: Product) {
        _itemQuantities.update { currentQuantities ->
            val productIdAsString = product.id.toString() // Convertimos el ID a String
            val currentCount = currentQuantities[productIdAsString] ?: 0
            currentQuantities + (productIdAsString to currentCount + 1)
        }
    }

    fun decrementProduct(product: Product) {
        _itemQuantities.update { currentQuantities ->
            val productIdAsString = product.id.toString() // Convertimos el ID a String
            val currentCount = currentQuantities[productIdAsString] ?: 0
            if (currentCount > 1) {
                currentQuantities + (productIdAsString to currentCount - 1)
            } else {
                val newQuantities = currentQuantities.toMutableMap()
                newQuantities.remove(productIdAsString) // Usamos el ID como String para eliminar
                _cartItems.update { it.filter { item -> item.id.toString() != productIdAsString } } // Filtramos por ID como String
                newQuantities
            }
        }
    }

    // Esta función debe usar el ID del producto como String para buscar en el mapa
    fun getProductCount(product: Product): Int {
        return _itemQuantities.value[product.id.toString()] ?: 0 // Buscamos con el ID como String
    }

    // Esta función utiliza product.attributes.product_price que ya es Double
    fun calculateItemTotal(product: Product): Double {
        val count = getProductCount(product)
        return product.attributes.product_price * count
    }

    // Variables para calcular el total y el IGV
    // Calculamos los totales. Aquí es importante que product_price sea Double
    val totalAmount: StateFlow<Double> = combine(
        cartItems, _itemQuantities
    ) { items, quantities ->
        items.sumOf { item ->
            val productIdAsString =
                item.id.toString() // Convertimos el ID a String para buscar en el mapa
            val count = quantities[productIdAsString] ?: 0
            item.attributes.product_price * count
        }
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
        _itemQuantities.value = emptyMap()
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
                val quantities = _itemQuantities.value

                // Construir la lista de items para la petición
                val quotationItems = items.map { product ->
                    val quantity = quantities[product.id.toString()] ?: 0
                    val subTotal = product.attributes.product_price * quantity
                    QuotationItem(
                        productId = product.id.toString(),
                        quantity = quantity,
                        netUnitPrice = product.attributes.product_price,
                        taxType = 2,
                        taxValue = 0.0,
                        taxAmount = 0.0,
                        discountType = 2,
                        discountValue = 0.0,
                        discountAmount = 0.0,
                        saleUnit = 1,
                        subTotal = subTotal,
                        productPrice = product.attributes.product_price,
                    )
                }

                // Obtener la fecha actual en el formato YYYY-MM-DD
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                // Construir el cuerpo de la petición
                val requestBody = QuotationRequest(
                    date = currentDate,
                    customerId = 1, //TODO: CAmbiar a un id de cliente correcto
                    quotationItems = quotationItems,
                    warehouseId = 1, // TODO: Cambiar con el almacen desde el server,
                    status = "Pendiente", // TODO: Verificar el status
                    // Agregamos los campos que faltan con valores de prueba
                    taxRate = 0.0,
                    taxAmount = 0.0,
                    discount = 0.0,
                    shipping = 0.0,
                    grandTotal = 0.0,
                    receivedAmount = 0.0,
                    paidAmount = 0.0,
                )

                // Realizar la llamada a la API
                val response = quotationRepository.createQuotation(requestBody)

                // Manejar la respuesta
                if (response.success) {
                    Log.d(TAG, "Respuesta de la API exitosa: La cotización fue creada con el ID: ${response.data?.id}")
                    _navigateToSummary.value = response.data?.id
                } else {
                    _apiError.value = response.message
                    Log.e(TAG, "Error de la API: ${response.message}")
                }
            } catch (e: Exception) {
                _apiError.value = e.message
                Log.e(TAG, "Error en la petición: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSummaryNavigated() {
        _navigateToSummary.value = null
    }
}