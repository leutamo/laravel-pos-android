# Documentación del Flujo Multi-Empresa y Envío de PDF por WhatsApp (Laravel POS Android)

## 1. Visión General

Se ha integrado el soporte para **Multi-Empresa de Facturación Electrónica** y la funcionalidad de **Envío de Comprobantes PDF por WhatsApp** en la aplicación móvil Android. Esta solución permite que los cajeros o usuarios del POS puedan seleccionar con cuál empresa emisora desean facturar sus ventas (Boletas / Facturas / Notas de Venta) y enviar el archivo PDF físico correspondiente a sus clientes.

---

## 2. Endpoints de la API Integrados (API M1)

La app móvil se comunica con el backend mediante los siguientes endpoints autenticados con Bearer Token:

### A. Obtener Lista de Empresas Emisoras
- **Ruta:** `GET /api/m1/billing-companies`
- **Cabeceras:**
  - `Authorization: Bearer {TOKEN}`
  - `Accept: application/json`
- **Respuesta:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Bezaleel",
      "razon_social": "BEZALEEL S.A.C.",
      "ruc": "20000000001",
      "address": "C-303, Mall, Lima",
      "phone": "999888777",
      "email": "ventas@bezaleel.pe",
      "invoice_series": "F001",
      "boleta_series": "B001",
      "is_default": true,
      "is_active": true
    },
    {
      "id": 2,
      "name": "Empresa B (Secundaria)",
      "razon_social": "EMPRESA B COMERCIAL S.A.C.",
      "ruc": "20600000002",
      "invoice_series": "F002",
      "boleta_series": "B002",
      "is_default": false,
      "is_active": true
    }
  ],
  "message": "Empresas emisoras obtenidas exitosamente"
}
```

---

### B. Establecer Empresa Activa del Usuario
- **Ruta:** `POST /api/m1/user-active-company`
- **Cabeceras:**
  - `Authorization: Bearer {TOKEN}`
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Cuerpo (Request):**
```json
{
  "company_id": 2
}
```
- **Respuesta:**
```json
{
  "success": true,
  "data": {
    "user_id": 1,
    "default_company_id": 2,
    "company": {
      "id": 2,
      "name": "Empresa B (Secundaria)",
      "razon_social": "EMPRESA B COMERCIAL S.A.C.",
      "ruc": "20600000002",
      "invoice_series": "F002",
      "boleta_series": "B002"
    }
  },
  "message": "Empresa activa cambiada a Empresa B (Secundaria) correctamente"
}
```

---

### C. Emisión de Venta Especificando Empresa
- **Ruta:** `POST /api/m1/sales` (o `POST /api/sales`)
- **Campo añadido en el JSON payload:**
```json
{
  "date": "2026-09-28T17:00:00.000Z",
  "customer_id": 1,
  "warehouse_id": 1,
  "company_id": 2,
  "grand_total": "50.00",
  "sale_items": [...]
}
```
*Nota:* Si se omite `company_id`, el backend tomará automáticamente la empresa activa del usuario (`user.default_company_id`).

---

## 3. Arquitectura y Componentes Creados / Modificados

### Modelos (`data/model/`)
- `BillingCompany.kt`:
  - `BillingCompany`: Mapeo de la entidad de empresa emisora (`id`, `name`, `razon_social`, `ruc`, `invoice_series`, `boleta_series`, `is_default`, `is_active`).
  - `SetActiveCompanyRequest` y `SetActiveCompanyResponseData`: Mapeos para el cambio de empresa activa.
- `LoginResponse.kt`:
  - `UserAttributes` actualizado con `default_company_id` y `default_company`.
- `Sale.kt`:
  - `SaleRequest` actualizado agregando la propiedad opcional `@SerialName("company_id") val companyId: Int? = null`.
- `Quotation.kt`:
  - `QuotationAttributes` actualizado agregando `customerPhone` y `electronicDocument` (`ElectronicDocumentData`).

### Repositorio (`data/repository/`)
- `BillingCompanyRepository.kt`:
  - `getBillingCompanies()`: Consulta el catálogo de empresas activas.
  - `setActiveCompany(companyId)`: Invoca la API para cambiar la empresa activa en la base de datos y la guarda localmente en `SharedPreferences`.
  - Guardado/recuperación local en `SharedPreferences` (`active_company_id`, `active_company_name`, `active_company_ruc`).
- `LoginRepository.kt`:
  - Al cargar el perfil (`fetchProfile`), almacena automáticamente la empresa por defecto configurada en el perfil del usuario.

### ViewModels (`viewmodel/`)
- `LoginViewModel.kt`:
  - Expone los flujos de estado `billingCompanies`, `activeCompany`, `isLoadingCompanies` y `companyError`.
  - Métodos `loadBillingCompanies()` y `selectActiveCompany(company)`.
  - Observador en tiempo real de `SharedPreferences` para cerrar sesión automáticamente ante respuestas HTTP 401 Unauthenticated.
- `CheckoutViewModel.kt`:
  - Accede a `BillingCompanyRepository` para recuperar el `company_id` activo.
  - Al procesar la venta (`processCheckout`), adjunta `companyId` al `SaleRequest`.
  - Proporciona métodos helper `getActiveCompanyName()` y `getActiveCompanyRuc()`.

### Vistas UI (`ui/theme/`)
- `HomeScreen.kt` (Menú Hamburguesa / Navigation Drawer):
  - Agregado en el `ModalDrawerSheet` la sección **Empresa Emisora**.
  - Permite visualizar la empresa seleccionada con su RUC y desplegar un `DropdownMenu` con las empresas disponibles para cambiar la selección.
  - Agregado el indicador visual de carga cuando abre la app.
- `CheckoutScreen.kt` (Pantalla de Cobro):
  - Muestra una tarjeta informativa indicando la **Empresa Emisora** que emitirá el comprobante antes de presionar "Procesar Venta".
- `SummaryScreen.kt` (Resumen de Venta / Cotización):
  - Muestra el campo de texto para **Número de WhatsApp / Teléfono** con auto-completado del teléfono registrado del cliente.
  - Botón verde con estilo e icono de **WhatsApp** para procesar y adjuntar la descarga del PDF oficial.

---

## 4. Lógica de Descarga y Envío de PDF por WhatsApp (SUNAT vs. Normal)

Se ha implementado un flujo inteligente en `SummaryScreen.kt` (`downloadDocumentPdf`) para enviar el archivo PDF físico correspondiente como documento adjunto a través de WhatsApp (`com.whatsapp` / `com.whatsapp.w4b`).

### A. Diferenciación de Tipos de Documentos y Resolutores de PDF

1. **Comprobantes Electrónicos SUNAT (Boleta / Factura - `voucher_type: "01"` / `"03"`)**:
   - **Texto del Mensaje:** `"Hola, le enviamos su Comprobante de Venta B001-XXXX (#SA_XXXX) por S/ XX.XX. ¡Gracias por su preferencia!"`
   - **Obtención del PDF:**
     - Si la respuesta trae `electronic_document.pdf_url`, se descarga desde esa ruta.
     - Fallback dinámico a la ruta pública SUNAT: `GET /api/sales/{sale_id}/sunat-pdf`.

2. **Notas de Venta Internas (`voucher_type: "nota_venta"` / `"00"`)**:
   - **Texto del Mensaje:** `"Hola, le enviamos su Nota de Venta #SA_XXXX por S/ XX.XX. ¡Gracias por su preferencia!"`
   - **Obtención del PDF:**
     1. Invoca el endpoint interno de generación de PDF de venta: `GET /api/sale-pdf-download/{sale_id}`.
     2. Extrae la propiedad `data.sale_pdf_url` de la respuesta JSON.
     3. Si la respuesta trae la URL del PDF generado, lo descarga a la caché del teléfono.
     4. Fallback dinámico a `/api/sales/{sale_id}/sunat-pdf`.

3. **Cotizaciones (`type == "quotation"`)**:
   - **Texto del Mensaje:** `"Hola, le enviamos su Cotización #QT_XXXX por S/ XX.XX. ¡Gracias por su preferencia!"`
   - **Obtención del PDF:**
     1. Invoca `GET /api/quotation-pdf-download/{quotation_id}`.
     2. Extrae la propiedad `data.quotation_pdf_url` de la respuesta JSON.

---

### B. Proceso de Descarga y Envío Adjunto en Android

1. **Descarga en Caché Local (`downloadDocumentPdf`)**:
   - Se descargan los bytes del archivo PDF en la carpeta temporal `context.cacheDir/Documento_SA_XXXX.pdf`.
   - Transmite las credenciales mediante la cabecera `Authorization: Bearer {token}` si la ruta requiere autenticación.

2. **Proveedor Seguro de Archivos (`FileProvider`)**:
   - Configurado en `AndroidManifest.xml` mediante `file_paths.xml` para exponer el URI seguro `content://com.example.laravelpos.fileprovider/pdf_cache/Documento_SA_XXXX.pdf`.

3. **Intent de Envío a WhatsApp (`Intent.ACTION_SEND`)**:
   - **MIME Type:** `application/pdf`
   - **Extra Stream:** `contentUri` (Archivo PDF físico `.pdf`)
   - **Extra Text:** Mensaje personalizado con el número de comprobante y total
   - **Target Package:** Asigna `setPackage("com.whatsapp")` o `"com.whatsapp.w4b"`, con el parámetro de destinatario `jid = "{cleanPhone}@s.whatsapp.net"`.

4. **Mecanismo de Respaldo (Fallback)**:
   - Si por algún motivo el archivo no pudiera descargarse, se activa la apertura mediante la URL web de WhatsApp API (`https://api.whatsapp.com/send?phone=...&text=...`) incluyendo el enlace de descarga del documento en el mensaje.
