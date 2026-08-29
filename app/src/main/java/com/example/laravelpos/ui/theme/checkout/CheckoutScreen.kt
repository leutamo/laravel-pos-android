package com.example.laravelpos.ui.theme.checkout

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.laravelpos.data.model.Customer
import com.example.laravelpos.data.model.CustomerAttributes
import com.example.laravelpos.data.model.CustomerLinks
import com.example.laravelpos.viewmodel.CheckoutViewModel
import com.example.laravelpos.viewmodel.HomeViewModel
@RequiresApi(Build.VERSION_CODES.O)
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
    val pagoContado by checkoutViewModel.pagoContado.collectAsState()

    // ✅ Estados del ViewModel para control y tipos de documento
    val documentTypes by checkoutViewModel.documentTypes.collectAsState()
    val isLoadingDocumentTypes by checkoutViewModel.isLoadingDocumentTypes.collectAsState()
    val isDniFieldEnabled by checkoutViewModel.isDniFieldEnabled.collectAsState() // Nuevo estado
    val selectedDocType by checkoutViewModel.selectedDocumentType.collectAsState() // Usamos el estado del ViewModel
    val customerData by checkoutViewModel.customerData.collectAsState() // Obtenemos datos del cliente
    val isLoadingCustomer by checkoutViewModel.isLoadingCustomer.collectAsState() // Añadido para resolver el error

    // ✅ Estados de carga, error y navegación desde CheckoutViewModel
    val isLoading by checkoutViewModel.isLoading.collectAsState()
    val apiError by checkoutViewModel.apiError.collectAsState()
    val navigateToSummary by checkoutViewModel.navigateToSummary.collectAsStateWithLifecycle()

    // ✅ Estado para controlar la visibilidad del modal
    var showCreateCustomerDialog by remember { mutableStateOf(false) }

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
            homeViewModel.clearCart() // ✅ Limpiar el carrito tras éxito
            checkoutViewModel.clearCheckoutData() // ✅ Limpiar datos del cliente para la próxima venta
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                        value = selectedDocType?.name ?: "Seleccionar tipo de documento",
                        onValueChange = { },
                        label = { Text("Tipo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                                    checkoutViewModel.updateSelectedDocumentType(type) // ✅ Enviamos el objeto completo
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
                        label = { Text(selectedDocType?.name ?: "DNI/RUC") },
                        modifier = Modifier.weight(1f),
                        enabled = isDniFieldEnabled && !isLoadingCustomer // Bloqueo hasta seleccionar tipo y durante carga
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { /* TODO: DNI genérico */ }) {
                        Text("Genérico")
                    }
                }

                // ✅ Nuevo botón para crear cliente
                Button(
                    onClick = { showCreateCustomerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("Crear Nuevo Cliente", color = Color.White)
                }
            }
            
            // ✅ Información del Cliente Seleccionado
            customerData?.let { customer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Datos del Cliente",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = customer.attributes.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Doc: ${customer.attributes.document_number}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (customer.attributes.email.isNotEmpty()) {
                            Text(
                                text = "Email: ${customer.attributes.email}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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

            // Ya no mostramos campos de pago ya que es una cotización
            if (!pagoContado) {
                Text("Nota: El pago a crédito se procesará según las condiciones pactadas.", 
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray)
            }

            Spacer(modifier = Modifier.weight(1f))

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
                // Datos del carrito desde HomeViewModel
                val cartItems by homeViewModel.cartItems.collectAsState()
                val itemQuantities by homeViewModel.itemQuantities.collectAsState()

                Button(
                    onClick = {
                        Log.d("CheckoutScreen", "Botón Cobrar presionado")
                        checkoutViewModel.processCheckout(
                            totalAmount = totalAmount,
                            selectedReceiptType = selectedReceiptType,
                            cartItems = cartItems,
                            itemQuantities = itemQuantities
                        )
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

            // ✅ Modal para crear nuevo cliente
            if (showCreateCustomerDialog) {
                var name by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var phone by remember { mutableStateOf("") }
                var country by remember { mutableStateOf("Perú") }
                var city by remember { mutableStateOf("Lima") }
                var address by remember { mutableStateOf("") }
                var documentNumber by remember { mutableStateOf(dniText) }
                var documentTypeId by remember { mutableIntStateOf(selectedDocType?.id ?: 1) }
                var selectedDocTypeName by remember { mutableStateOf(selectedDocType?.name ?: "DNI") }
                var docTypeExpanded by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showCreateCustomerDialog = false },
                    title = { Text("Crear Nuevo Cliente") },
                    text = {
                        Column {
                            ExposedDropdownMenuBox(
                                expanded = docTypeExpanded,
                                onExpandedChange = { docTypeExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = selectedDocTypeName,
                                    onValueChange = { },
                                    label = { Text("Tipo de Documento *") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = docTypeExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = docTypeExpanded,
                                    onDismissRequest = { docTypeExpanded = false }
                                ) {
                                    documentTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type.name) },
                                            onClick = {
                                                documentTypeId = type.id
                                                selectedDocTypeName = type.name
                                                docTypeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = documentNumber,
                                onValueChange = { documentNumber = it },
                                label = { Text("Número de Documento *") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nombre Completo *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email *") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Teléfono *") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Dirección *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (name.isBlank() || email.isBlank() || phone.isBlank()) {
                                    Toast.makeText(context, "Por favor complete los campos obligatorios", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val currentTime = java.time.OffsetDateTime.now().toString()
                                val newCustomer = Customer(
                                    type = "customers",
                                    id = 0,
                                    attributes = CustomerAttributes(
                                        name = name,
                                        email = email,
                                        phone = phone,
                                        country = country,
                                        city = city,
                                        address = address,
                                        dob = null,
                                        document_number = documentNumber,
                                        document_type_id = documentTypeId,
                                        created_at = currentTime,
                                        updated_at = currentTime
                                    ),
                                    links = CustomerLinks(self = "")
                                )
                                checkoutViewModel.createCustomer(newCustomer)
                                showCreateCustomerDialog = false
                            }
                        ) {
                            Text("Crear")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showCreateCustomerDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}