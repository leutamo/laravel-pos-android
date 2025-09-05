package com.example.laravelpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.Product
import com.example.laravelpos.data.repository.ProductRepository
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Estado para los ítems del carrito
    private val _cartItems = MutableStateFlow<List<Product>>(emptyList())
    val cartItems: StateFlow<List<Product>> = _cartItems.asStateFlow() // Exponemos como StateFlow

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
        fetchProducts()
    }

    private fun fetchProducts() {
        viewModelScope.launch {
            val productList = repository.getProducts()
            _products.value = productList
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.update { query }
    }

    // Función para agregar un producto al carrito
    fun addItemToCart(product: Product) {
        // Agregamos el producto a la lista actual de ítems del carrito
        _cartItems.update { currentItems ->
            currentItems + product
        }
    }
}