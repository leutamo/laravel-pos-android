# Documentación del Flujo Multi-Empresa, Impuestos, Clientes Just-In-Time y WhatsApp (Laravel POS Android)

## 1. Visión General

Se han integrado las siguientes funcionalidades clave en la aplicación móvil Android:
- **Multi-Empresa de Facturación Electrónica**: Selección de la empresa emisora activa en el menú lateral.
- **Consulta DNI/RUC y Registro Just-In-Time ("Al Cobrar")**: Búsqueda externa RENIEC/SUNAT y creación automática del cliente antes de emitir la venta.
- **Cálculo Dinámico de Impuestos por Producto**: Mapeo de `order_tax` y `tax_type` desde la plataforma para prevenir doble cobro de IGV.
- **Envío de PDF por WhatsApp**: Descarga física de comprobantes A4 SUNAT o Notas de Venta y envío como archivo `.pdf` adjunto.

---

## 2. Consulta DNI/RUC y Guardado Just-In-Time ("Al Cobrar")

### A. Búsqueda de Clientes (`GET /api/customers/search/{documentNumber}`)
1. **DNI (8 dígitos) o RUC (11 dígitos)**:
   - **Cliente en BD local (`customer.id > 0`)**: Recupera el cliente registrado en la tienda.
   - **Cliente de Consulta Externa RENIEC/SUNAT (`customer.id == 0`)**: El backend devuelve `id: 0` con los datos de nombre/razón social, documento, dirección y ciudad en `customer.attributes`.
   - **Error / No Encontrado (HTTP 422)**: Se captura el mensaje retornado (`message`), se muestra una alerta o mensaje al cajero y se permite continuar o ingresar un nuevo cliente.

2. **Indicador Visual en la App (`CheckoutScreen.kt`)**:
   - Cuando se selecciona un cliente de consulta externa (`id == 0`), la tarjeta de datos muestra el distintivo **"Consulta RENIEC/SUNAT (Se guardará al cobrar)"**.

---

### B. Proceso de Guardado "Al Cobrar" (`CheckoutViewModel.kt`)

1. **Si `customer.id == 0`**:
   - Al presionar **"Cobrar"**, la app ejecuta primero la petición `POST /api/customers` (o `/api/m1/customers`) enviando:
     ```json
     {
       "name": customer.attributes.name,
       "document_type_id": customer.attributes.document_type_id,
       "document_number": customer.attributes.document_number,
       "email": customer.attributes.email.ifBlank { null },
       "phone": customer.attributes.phone.ifBlank { null },
       "address": customer.attributes.address.ifBlank { null },
       "city": customer.attributes.city.ifBlank { null },
       "country": "Perú"
     }
     ```
   - El backend guarda el cliente en la base de datos de la tienda y devuelve el nuevo `id > 0`.
   - La app asigna este nuevo `id` como `customer_id` en el JSON de la venta (`SaleRequest`).

2. **Si `customer.id > 0`**:
   - Usa directamente `customer.id` para registrar la venta.

3. **Flexibilidad de Campos y Limpieza**:
   - `email` y `phone` son totalmente opcionales.
   - Si el cajero cancela la venta antes de cobrar, el cliente con `id == 0` se descarta de la memoria del teléfono sin haber guardado nada en el servidor.

---

## 3. Endpoints de la API Integrados (API M1)

### A. Obtener Lista de Empresas Emisoras
- `GET /api/m1/billing-companies`

### B. Establecer Empresa Activa del Usuario
- `POST /api/m1/user-active-company` -> `{"company_id": 2}`

### C. Emisión de Venta
- `POST /api/m1/sales` (o `/api/sales`) enviando `company_id` y `voucher_type` (`"nota_venta"`, `"03"`, `"01"`).

---

## 4. Lógica de Cálculo Dinámico de Impuestos (`order_tax` y `tax_type`)

Cada producto se evalúa según sus atributos `order_tax` y `tax_type` devueltos por la plataforma:
- **`order_tax <= 0.0` (Sin Impuesto / Exonerado)**: `taxAmount = 0.00`, `netUnitPrice = unitPrice`.
- **`tax_type == 2` (Inclusive - IGV Incluido)**: `netUnitPrice = unitPrice / (1 + order_tax / 100)`.
- **`tax_type == 1` (Exclusive - IGV No incluido)**: `netUnitPrice = unitPrice`, `taxAmount = unitPrice * (order_tax / 100)`.
- En `SaleRequest`, `taxRate` se envía como `"0.00"` para evitar recálculos globales.

---

## 5. Lógica de Descarga y Envío de PDF por WhatsApp

- **Comprobante SUNAT (Boleta/Factura)**: Descarga desde `/api/sales/{id}/sunat-pdf`.
- **Nota de Venta**: Descarga desde `/api/sale-pdf-download/{id}`.
- **Cotización**: Descarga desde `/api/quotation-pdf-download/{id}`.
- La app valida la firma binaria `%PDF` del archivo descargado en `cacheDir` antes de entregarlo a WhatsApp con `FileProvider`.
