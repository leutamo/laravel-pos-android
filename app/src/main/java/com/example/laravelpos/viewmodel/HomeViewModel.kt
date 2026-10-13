package com.example.laravelpos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.config.ServerConfig
import com.example.laravelpos.data.model.CartItem
import com.example.laravelpos.data.model.Product
import com.example.laravelpos.data.model.ProductConversion
import com.example.laravelpos.data.model.QuotationItem
import com.example.laravelpos.data.model.QuotationRequest
import com.example.laravelpos.data.model.WarehouseItem
import com.example.laravelpos.data.repository.ProductRepository
import com.example.laravelpos.data.repository.QuotationRepository
import com.example.laravelpos.data.repository.WarehouseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val quotationRepository: QuotationRepository,
    private val warehouseRepository: WarehouseRepository,
    private val serverConfig: ServerConfig
) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    fun getFullImageUrl(relativePath: String?): String {
        return serverConfig.getFullImageUrl(relativePath)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Estado para los ítems del carrito
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _navigateToSummary = MutableStateFlow<Int?>(null)
    val navigateToSummary: StateFlow<Int?> = _navigateToSummary.asStateFlow()

    // Para el modal de tipo de comprobante
    private val _selectedReceiptType = MutableStateFlow<String?>(null)
    val selectedReceiptType: StateFlow<String?> = _selectedReceiptType.asStateFlow()

    private val _showReceiptModal = MutableStateFlow(false)
    val showReceiptModal: StateFlow<Boolean> = _showReceiptModal.asStateFlow()

    // Estados para almacenes (Warehouses)
    private val _warehouses = MutableStateFlow<List<WarehouseItem>>(emptyList())
    val warehouses: StateFlow<List<WarehouseItem>> = _warehouses.asStateFlow()

    private val _activeWarehouse = MutableStateFlow<WarehouseItem?>(null)
    val activeWarehouse: StateFlow<WarehouseItem?> = _activeWarehouse.asStateFlow()

    private val _isLoadingWarehouses = MutableStateFlow(false)
    val isLoadingWarehouses: StateFlow<Boolean> = _isLoadingWarehouses.asStateFlow()

    private val _showConfirmClearCartDialog = MutableStateFlow<WarehouseItem?>(null)
    val showConfirmClearCartDialog: StateFlow<WarehouseItem?> = _showConfirmClearCartDialog.asStateFlow()

    // Estados para la llamada a la API
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    // La lista filtrada expone los productos devueltos
    val filteredProducts: StateFlow<List<Product>> = _products.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized, loading warehouses and initial products")
        loadWarehouses()

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            searchQuery
                .drop(1)
                .debounce(500)
                .collect { query ->
                    fetchProducts(query)
                }
        }
    }

    fun loadWarehouses() {
        viewModelScope.launch {
            _isLoadingWarehouses.value = true
            try {
                val list = warehouseRepository.getWarehouses()
                _warehouses.value = list

                val savedId = warehouseRepository.getSavedActiveWarehouseId()
                var current = list.find { it.id == savedId }
                if (current == null) {
                    current = list.find { it.isDefault } ?: list.firstOrNull()
                }

                if (current != null) {
                    _activeWarehouse.value = current
                    warehouseRepository.saveSelectedWarehouse(current)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading warehouses: ${e.message}")
            } finally {
                _isLoadingWarehouses.value = false
                fetchProducts()
            }
        }
    }

    fun selectWarehouse(warehouseItem: WarehouseItem) {
        if (warehouseItem.id == _activeWarehouse.value?.id) return

        if (_cartItems.value.isNotEmpty()) {
            _showConfirmClearCartDialog.value = warehouseItem
        } else {
            confirmWarehouseChange(warehouseItem)
        }
    }

    fun confirmWarehouseChange(warehouseItem: WarehouseItem) {
        clearCart()
        _activeWarehouse.value = warehouseItem
        warehouseRepository.saveSelectedWarehouse(warehouseItem)
        _showConfirmClearCartDialog.value = null
        fetchProducts()
    }

    fun cancelWarehouseChange() {
        _showConfirmClearCartDialog.value = null
    }

    fun fetchProducts(search: String? = _searchQuery.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _apiError.value = null
            val activeWarehouseId = _activeWarehouse.value?.id
            Log.d(TAG, "fetchProducts: Requesting products (search: $search, warehouse_id: $activeWarehouseId)...")
            val result = repository.getProducts(search, activeWarehouseId)
            
            result.onSuccess { productList ->
                Log.d(TAG, "Server responded with ${productList.size} products for warehouse $activeWarehouseId")
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
        onSearchQueryChanged("")

        _cartItems.update { currentItems ->
            val existingItem = currentItems.find { it.product.id == product.id && it.selectedConversion == null && it.customPrice == null }
            if (existingItem != null) {
                currentItems.map { 
                    if (it === existingItem) it.copy(quantity = it.quantity + 1.0) else it 
                }
            } else {
                currentItems + CartItem(product, 1.0)
            }
        }
    }

    fun incrementProduct(cartItem: CartItem) {
        _cartItems.update { currentItems ->
            currentItems.map { 
                if (it === cartItem) it.copy(quantity = it.quantity + 1.0) else it 
            }
        }
    }

    fun decrementProduct(cartItem: CartItem) {
        _cartItems.update { currentItems ->
            currentItems.mapNotNull { 
                if (it === cartItem) {
                    if (it.quantity > 1.0) it.copy(quantity = it.quantity - 1.0) else null
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

    fun changeItemQuantity(cartItem: CartItem, newQuantity: Double) {
        _cartItems.update { currentItems ->
            currentItems.map { 
                if (it === cartItem) it.copy(quantity = newQuantity) else it 
            }
        }
    }

    fun getProductCount(product: Product): Double {
        return _cartItems.value.filter { it.product.id == product.id }.sumOf { it.quantity }
    }

    // Variables para calcular el total y el IGV
    val totalAmount: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { cartItem ->
            val product = cartItem.product
            val taxRate = product.attributes.parsedOrderTax
            val taxType = product.attributes.parsedTaxType
            val unitPrice = cartItem.unitPrice
            if (taxRate > 0.0 && taxType == 1) {
                (unitPrice + unitPrice * (taxRate / 100.0)) * cartItem.quantity
            } else {
                unitPrice * cartItem.quantity
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val igvAmount: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { cartItem ->
            val product = cartItem.product
            val taxRate = product.attributes.parsedOrderTax
            val taxType = product.attributes.parsedTaxType
            val unitPrice = cartItem.unitPrice
            if (taxRate <= 0.0) {
                0.0
            } else if (taxType == 2) {
                val netUnit = unitPrice / (1.0 + taxRate / 100.0)
                (unitPrice - netUnit) * cartItem.quantity
            } else {
                (unitPrice * (taxRate / 100.0)) * cartItem.quantity
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun clearCart() {
        _cartItems.value = emptyList()
    }

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

    fun clearApiError() {
        _apiError.value = null
    }

    fun processCheckout() {
        viewModelScope.launch {
            _isLoading.value = true
            _apiError.value = null
            try {
                val items = _cartItems.value
                val currentTotal = totalAmount.value
                val currentIgv = igvAmount.value

                val quotationItems = items.map { cartItem ->
                    val product = cartItem.product
                    val quantity = cartItem.quantity
                    val subTotal = cartItem.subTotal
                    val unitPrice = cartItem.unitPrice
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

                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val currentDate = isoFormat.format(Date())
                val activeWarehouseId = _activeWarehouse.value?.id ?: 1

                val requestBody = QuotationRequest(
                    date = currentDate,
                    customerId = 6, 
                    warehouseId = activeWarehouseId, 
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

                val response = quotationRepository.createQuotation(requestBody)

                if (response.success) {
                    Log.d(TAG, "Cotización creada con éxito: ${response.data?.id}")
                    _navigateToSummary.value = response.data?.id
                    clearCart()
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
