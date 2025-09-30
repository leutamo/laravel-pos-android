package com.example.laravelpos.ui.theme.checkout

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.laravelpos.viewmodel.CheckoutViewModel
import com.example.laravelpos.viewmodel.HomeViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    homeViewModel: HomeViewModel,
    checkoutViewModel: CheckoutViewModel // ✅ ¡Nuevo ViewModel inyectado!
) {
    val context = LocalContext.current
    // Datos que aún vienen de HomeViewModel (porque quizás se calculan en el carrito)
    val totalAmount by homeViewModel.totalAmount.collectAsState()
    val selectedReceiptType by homeViewModel.selectedReceiptType.collectAsState()

    // ✅ Estados del formulario desde CheckoutViewModel
    val dniText by checkoutViewModel.dniText.collectAsStateWithLifecycle()
    val pagoEfectivo by checkoutViewModel.pagoEfectivo.collectAsState()
    val pagoVisa by checkoutViewModel.pagoVisa.collectAsState()
    val totalRecibido by checkoutViewModel.totalRecibido.collectAsState()
    val pagoContado by checkoutViewModel.pagoContado.collectAsState()

    // ✅ Estados del ViewModel para control y tipos de documento
    val documentTypes by checkoutViewModel.documentTypes.collectAsState()
    val isLoadingDocumentTypes by checkoutViewModel.isLoadingDocumentTypes.collectAsState()
    val isDniFieldEnabled by checkoutViewModel.isDniFieldEnabled.collectAsState() // Nuevo estado
    val selectedDocType by checkoutViewModel.selectedDocumentType.collectAsState() // Usamos el estado del ViewModel
    val isLoadingCustomer by checkoutViewModel.isLoadingCustomer.collectAsState() // Añadido para resolver el error

    // ✅ Estados de carga, error y navegación desde CheckoutViewModel
    val isLoading by checkoutViewModel.isLoading.collectAsState()
    val apiError by checkoutViewModel.apiError.collectAsState()
    val navigateToSummary by checkoutViewModel.navigateToSummary.collectAsStateWithLifecycle()

    // 2. Lanza un efecto para escuchar los eventos del ViewModel
    LaunchedEffect(Unit) {
        // Collecta el flujo de eventos del Toast
        checkoutViewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Cargar tipos de documento al iniciar
    LaunchedEffect(Unit) {
        checkoutViewModel.loadDocumentTypes()
    }

    // ✅ Navegar al resumen cuando se emite un ID
    LaunchedEffect(navigateToSummary) {
        navigateToSummary?.let { id ->
            navController.navigate("summary_screen/$id")
            checkoutViewModel.onSummaryNavigated()
        }
    }

    // ✅ Mostrar diálogo de error si existe
    if (apiError != null) {
        AlertDialog(
            onDismissRequest = { checkoutViewModel.clearApiError() },
            title = { Text(text = "Error del Servidor", color = Color.Red) },
            text = { Text(text = apiError!!) },
            confirmButton = {
                Button(onClick = { checkoutViewModel.clearApiError() }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // ✅ Mostrar pantalla de carga inicial si es necesario
    if (isLoadingDocumentTypes && documentTypes.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando tipos de documento...")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = selectedReceiptType ?: "Finalizar Compra") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                        homeViewModel.selectReceiptType(null)
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Chat */ }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Chat",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { /* TODO: Usuarios */ }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Usuarios",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sección de total a cobrar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total a cobrar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "S/ ${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Selector de tipo de documento y DNI
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedDocType ?: "Seleccionar tipo de documento",
                        onValueChange = { },
                        label = { Text("Tipo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        documentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    checkoutViewModel.updateSelectedDocumentType(type.name) // Usamos name en lugar de code
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dniText,
                        onValueChange = { checkoutViewModel.updateDni(it) }, // ✅ Actualiza en ViewModel
                        label = { Text(selectedDocType ?: "DNI/RUC") },
                        modifier = Modifier.weight(1f),
                        enabled = isDniFieldEnabled && !isLoadingCustomer // Bloqueo hasta seleccionar tipo y durante carga
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { /* TODO: DNI genérico */ }) {
                        Text("Genérico")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Radio buttons para Contado y Crédito
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = pagoContado,
                    onClick = { checkoutViewModel.updatePagoContado(true) } // ✅
                )
                Text("Contado", modifier = Modifier.weight(1f))
                RadioButton(
                    selected = !pagoContado,
                    onClick = { checkoutViewModel.updatePagoContado(false) } // ✅
                )
                Text("Crédito", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Campos de pago
            if (pagoContado) {
                Column {
                    OutlinedTextField(
                        value = pagoEfectivo,
                        onValueChange = { checkoutViewModel.updatePagoEfectivo(it) }, // ✅
                        label = { Text("Pago efectivo") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pagoVisa,
                        onValueChange = { checkoutViewModel.updatePagoVisa(it) }, // ✅
                        label = { Text("Pago Visa") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = totalRecibido,
                        onValueChange = { checkoutViewModel.updateTotalRecibido(it) }, // ✅
                        label = { Text("Total recibido") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            } else {
                // TODO: Implementar campos para pago a crédito
                Text("Campos de crédito pendientes...")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Total y Vuelto — por ahora fijo, luego lo calculamos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vuelto:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "S/ ${String.format("%.2f", 0.0)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Botones de Atrás y Cobrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        navController.popBackStack()
                        homeViewModel.selectReceiptType(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Atrás")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        Log.d("CheckoutScreen", "Botón Cobrar presionado")
                        checkoutViewModel.processCheckout(totalAmount, selectedReceiptType) // ✅ ¡Nuevo!
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Cobrar")
                    }
                }
            }
        }
    }
}