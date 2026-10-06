package com.example.laravelpos.ui.theme.summary

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.laravelpos.viewmodel.CheckoutViewModel
import com.example.laravelpos.viewmodel.HomeViewModel
import com.example.laravelpos.viewmodel.SummaryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    navController: NavController,
    homeViewModel: HomeViewModel,
    type: String,
    id: Int,
    summaryViewModel: SummaryViewModel = hiltViewModel(),
    checkoutViewModel: CheckoutViewModel = hiltViewModel()
) {
    val state by summaryViewModel.state.collectAsState()
    val quotation = state.quotation
    val customerData by checkoutViewModel.customerData.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(id, type) {
        summaryViewModel.loadData(type, id)
    }

    var phoneText by remember { mutableStateOf("") }
    var isSendingWhatsapp by remember { mutableStateOf(false) }

    // Auto-poblar número de teléfono si el cliente lo tiene registrado
    LaunchedEffect(customerData, quotation) {
        if (phoneText.isEmpty()) {
            val phone = customerData?.attributes?.phone
                ?: quotation?.attributes?.customerPhone
            if (!phone.isNullOrBlank()) {
                phoneText = phone
            }
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (quotation != null) {
        val attr = quotation.attributes
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val titleText = if (type == "sale") "Venta" else "Cotización"
                            Text(
                                text = "$titleText #${attr.referenceCode}",
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val message = "Comprobante #${attr.referenceCode} por un total de S/ ${String.format("%.2f", attr.grandTotal)}"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            shareIntent.type = "text/plain"
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir comprobante"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Título de totales
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Total cobrado",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "S/ ${String.format("%.2f", attr.grandTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Encabezados de la tabla
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "PRODUCTO", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                    Text(text = "PRECIO", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(text = "#", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold)
                    Text(text = "UNIDAD", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(text = "TOTAL", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                // Lista de productos
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(attr.items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Producto ${item.productId}",
                                modifier = Modifier.weight(2f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(text = "S/ ${String.format("%.2f", item.productPrice)}", modifier = Modifier.weight(1f))
                            Text(text = "${item.quantity}", modifier = Modifier.weight(0.5f))
                            Text(text = item.saleUnit.shortName, modifier = Modifier.weight(1f))
                            Text(text = "S/ ${String.format("%.2f", item.subTotal)}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Sección de totales y vuelto
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Sub Total", modifier = Modifier.weight(1f))
                        Text(text = "S/ ${String.format("%.2f", attr.grandTotal - attr.taxAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "IGV (${attr.taxRate}%)", modifier = Modifier.weight(1f))
                        Text(text = "S/ ${String.format("%.2f", attr.taxAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "TOTAL", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(text = "S/ ${String.format("%.2f", attr.grandTotal)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Pago:", modifier = Modifier.weight(1f))
                        Text(text = "S/ ${String.format("%.2f", attr.receivedAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Vuelto:", modifier = Modifier.weight(1f))
                        Text(text = "S/ ${String.format("%.2f", attr.receivedAmount - attr.grandTotal)}", fontWeight = FontWeight.Bold)
                    }
                }

                // Sección de Teléfono/WhatsApp y botones de acción
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("Número de WhatsApp / Teléfono") },
                        placeholder = { Text("Ej. 987654321") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Icono Teléfono WhatsApp",
                                tint = Color(0xFF25D366) // Color verde WhatsApp
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = {
                                var cleanPhone = phoneText.trim().replace(" ", "").replace("-", "")
                                if (cleanPhone.isEmpty()) {
                                    Toast.makeText(context, "Por favor ingrese un número de teléfono", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (cleanPhone.length == 9 && !cleanPhone.startsWith("+") && !cleanPhone.startsWith("51")) {
                                    cleanPhone = "51$cleanPhone"
                                }

                                val refCode = attr.referenceCode
                                val grandTotal = String.format("%.2f", attr.grandTotal)
                                val fullNum = attr.electronicDocument?.fullNumber
                                val docInfo = if (!fullNum.isNullOrEmpty()) " ($fullNum)" else ""
                                
                                // Determinar la URL del PDF (usar la de attr o la pública por defecto)
                                val rawPdfUrl = attr.electronicDocument?.pdfUrl
                                val pdfUrlToUse = if (!rawPdfUrl.isNullOrBlank()) {
                                    summaryViewModel.serverConfig.getFullImageUrl(rawPdfUrl)
                                } else {
                                    val baseUrl = summaryViewModel.serverConfig.getBaseUrl()
                                    "${baseUrl}sales/${quotation.id}/sunat-pdf"
                                }

                                val message = "Hola, le enviamos su comprobante de venta$docInfo (#$refCode) por un total de S/ $grandTotal. ¡Gracias por su preferencia!"

                                // Detectar WhatsApp o WhatsApp Business instalado
                                val whatsappPkg = try {
                                    context.packageManager.getPackageInfo("com.whatsapp", 0)
                                    "com.whatsapp"
                                } catch (e: Exception) {
                                    try {
                                        context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
                                        "com.whatsapp.w4b"
                                    } catch (e2: Exception) {
                                        null
                                    }
                                }

                                scope.launch {
                                    isSendingWhatsapp = true
                                    try {
                                        var contentUri: Uri? = null

                                        withContext(Dispatchers.IO) {
                                            try {
                                                val url = URL(pdfUrlToUse)
                                                val connection = url.openConnection() as HttpURLConnection
                                                connection.connectTimeout = 10000
                                                connection.readTimeout = 10000
                                                val token = summaryViewModel.getAuthToken()
                                                if (!token.isNullOrEmpty()) {
                                                    connection.setRequestProperty("Authorization", "Bearer $token")
                                                }
                                                connection.connect()

                                                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                                                    val file = File(context.cacheDir, "Comprobante_${refCode}.pdf")
                                                    file.outputStream().use { output ->
                                                        connection.inputStream.use { input ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                    contentUri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                } else {
                                                    Log.e("SummaryScreen", "Error descargando PDF HTTP: ${connection.responseCode}")
                                                }
                                            } catch (e: Exception) {
                                                Log.e("SummaryScreen", "Excepción descargando PDF: ${e.message}", e)
                                            }
                                        }

                                        if (contentUri != null) {
                                            // Enviar con archivo PDF adjunto a WhatsApp
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                                putExtra(Intent.EXTRA_TEXT, message)
                                                putExtra("jid", "$cleanPhone@s.whatsapp.net")
                                                if (!whatsappPkg.isNullOrEmpty()) {
                                                    setPackage(whatsappPkg)
                                                }
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            shareIntent.type = "application/pdf"
                                            try {
                                                context.startActivity(shareIntent)
                                            } catch (e: Exception) {
                                                val chooser = Intent.createChooser(shareIntent, "Enviar comprobante por WhatsApp")
                                                context.startActivity(chooser)
                                            }
                                        } else {
                                            // Fallback con URL en texto si no se pudo descargar el archivo
                                            val fullMsg = "$message\n\nVer PDF: $pdfUrlToUse"
                                            val encodedMsg = URLEncoder.encode(fullMsg, "UTF-8")
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
                                            )
                                            if (!whatsappPkg.isNullOrEmpty()) {
                                                intent.setPackage(whatsappPkg)
                                            }
                                            context.startActivity(intent)
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al enviar por WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSendingWhatsapp = false
                                    }
                                }
                            },
                            enabled = !isSendingWhatsapp,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // Verde WhatsApp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSendingWhatsapp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enviando...", fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enviar", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                checkoutViewModel.clearCheckoutData() // ✅ Limpiar datos del cliente anterior
                                homeViewModel.clearCart() // ✅ Por si acaso
                                homeViewModel.selectReceiptType(null)
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Finalizar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = state.error ?: "Error desconocido")
        }
    }
}
