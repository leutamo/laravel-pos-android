# Documentación del Flujo Multi-Empresa, Impuestos y Envío de PDF por WhatsApp (Laravel POS Android)

## 1. Visión General

Se ha integrado el soporte para **Multi-Empresa de Facturación Electrónica**, el **Cálculo Dinámico de Impuestos por Producto** y la funcionalidad de **Envío de Comprobantes PDF por WhatsApp** en la aplicación móvil Android. Esta solución permite que los cajeros o usuarios del POS puedan seleccionar con cuál empresa emisora desean facturar sus ventas (Boletas / Facturas / Notas de Venta), cobren el impuesto exacto configurado en la plataforma para cada producto y envíen el archivo PDF físico correspondiente a sus clientes.

---

## 2. Endpoints de la API Integrados (API M1)

La app móvil se comunica con el backend mediante los siguientes endpoints autenticados con Bearer Token:

### A. Obtener Lista de Empresas Emisoras
- **Ruta:** `GET /api/m1/billing-companies`
- **Cabeceras:**
  - `Authorization: Bearer {TOKEN}`
  - `Accept: application/json`

---

### B. Establecer Empresa Activa del Usuario
- **Ruta:** `POST /api/m1/user-active-company`
- **Cuerpo (Request):** `{"company_id": 2}`

---

### C. Emisión de Venta Especificando Empresa e Impuestos Dinámicos
- **Ruta:** `POST /api/m1/sales` (o `POST /api/sales`)
- **Estructura del Payload:**
```json
{
  "date": "2026-09-28T17:00:00.000Z",
  "customer_id": 1,
  "warehouse_id": 1,
  "company_id": 2,
  "tax_rate": "0.00",
  "tax_amount": "1.80",
  "grand_total": "11.80",
  "sale_items": [
    {
      "product_id": 15,
      "quantity": 1,
      "product_price": "11.80",
      "net_unit_price": "10.00",
      "tax_type": 2,
      "tax_value": "18.00",
      "tax_amount": "1.80",
      "sub_total": "11.80"
    }
  ]
}
```

---

## 3. Lógica de Cálculo Dinámico de Impuestos (`order_tax` y `tax_type`)

Anteriormente la aplicación móvil tenía configurados impuestos estáticos de 18% Exclusive a nivel global, lo cual provocaba que el backend recalculara y **sumara doble impuesto** a productos no gravados (`order_tax = 0`).

### A. Atributos del Producto leídos desde la Plataforma
Cada producto devuelto por `GET /api/products` contiene:
- `order_tax`: Porcentaje de impuesto configurado en la plataforma (ej. `0.00` para exonerado/inafecto o `18.00` para IGV).
- `tax_type`: Tipo de impuesto (`1` = Exclusive / Impuesto no incluido, `2` = Inclusive / Impuesto incluido).

### B. Reglas de Cálculo en la App (`Product.kt`, `HomeViewModel.kt`, `CheckoutViewModel.kt`)

1. **Si `order_tax <= 0.0` (Producto sin Impuesto / 0% / Exonerado)**:
   - `netUnitPrice` = `productPrice`
   - `taxAmount` = `0.00`
   - `subTotal` = `productPrice * quantity`
   - No se agrega ningún impuesto adicional.

2. **Si `tax_type == 2` (Inclusive - IGV Incluido en el precio)**:
   - `netUnitPrice` = `productPrice / (1 + order_tax / 100)`
   - `taxAmount` = `(productPrice - netUnitPrice) * quantity`
   - `subTotal` = `productPrice * quantity`

3. **Si `tax_type == 1` (Exclusive - IGV No incluido)**:
   - `netUnitPrice` = `productPrice`
   - `taxAmount` = `(productPrice * order_tax / 100) * quantity`
   - `subTotal` = `(productPrice + taxAmountPerUnit) * quantity`

4. **Nivel Global de la Venta (`SaleRequest`)**:
   - `tax_rate` se envía como `"0.00"` para evitar que la plataforma vuelva a calcular un impuesto global adicional sobre el total.
   - `tax_amount` es la suma exacta de los `tax_amount` individuales de cada producto.

---

## 4. Lógica de Descarga y Envío de PDF por WhatsApp

Se ha implementado un flujo en `SummaryScreen.kt` (`downloadDocumentPdf`) para enviar el archivo PDF físico correspondiente como documento adjunto a través de WhatsApp (`com.whatsapp` / `com.whatsapp.w4b`).

### A. Diferenciación de Tipos de Documentos y Resolutores de PDF

1. **Comprobantes Electrónicos SUNAT (Boleta / Factura)**:
   - Utiliza `attr.electronicDocument.pdfUrl` devuelta por el servidor o la ruta pública `/api/sales/{sale_id}/sunat-pdf`.

2. **Notas de Venta Internas**:
   - Invoca el endpoint interno de generación de PDF: `GET /api/sale-pdf-download/{sale_id}`.
   - Extrae la propiedad `data.sale_pdf_url` de la respuesta JSON para descargar el archivo.

3. **Cotizaciones**:
   - Invoca `GET /api/quotation-pdf-download/{quotation_id}` y extrae `data.quotation_pdf_url`.

---

### B. Proceso de Descarga y Envío Adjunto en Android

1. **Descarga en Caché Local**: Descarga los bytes a `cacheDir/Documento_SA_XXXX.pdf`.
2. **FileProvider**: Expone el URI seguro `content://com.example.laravelpos.fileprovider/pdf_cache/...`.
3. **Intent de Envío a WhatsApp**: Asigna el MIME `application/pdf`, pasa el URI mediante `EXTRA_STREAM`, incluye la leyenda con `caption` y `EXTRA_TEXT`, y fija el paquete `com.whatsapp` / `com.whatsapp.w4b`.
