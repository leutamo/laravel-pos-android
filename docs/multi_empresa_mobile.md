# Documentación del Flujo Multi-Empresa para App Android (Laravel POS)

## 1. Visión General

Se ha integrado el soporte para **Multi-Empresa de Facturación Electrónica** en la aplicación móvil Android. Esta funcionalidad permite que los cajeros o usuarios del POS puedan seleccionar con cuál empresa emisora desean facturar sus ventas (Boletas / Facturas / Notas de Venta).

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
- `CheckoutViewModel.kt`:
  - Accede a `BillingCompanyRepository` para recuperar el `company_id` activo.
  - Al procesar la venta (`processCheckout`), adjunta `companyId` al `SaleRequest`.
  - Proporciona métodos helper `getActiveCompanyName()` y `getActiveCompanyRuc()`.

### Vistas UI (`ui/theme/`)
- `HomeScreen.kt` (Menú Hamburguesa / Navigation Drawer):
  - Agregado en el `ModalDrawerSheet` la sección **Empresa Emisora**.
  - Permite visualizar la empresa seleccionada con su RUC y desplegar un `DropdownMenu` con las empresas disponibles para cambiar la selección.
- `CheckoutScreen.kt` (Pantalla de Cobro):
  - Muestra una tarjeta informativa indicando la **Empresa Emisora** que emitirá el comprobante antes de presionar "Procesar Venta".

---

## 4. Flujo de Funcionamiento Paso a Paso

1. **Inicio de Sesión / Carga Inicial:**
   - El usuario inicia sesión. Al obtener el perfil, el sistema carga y guarda la empresa por defecto del usuario.
   - La app consulta `GET /api/m1/billing-companies` para obtener la lista actualizada de empresas emisoras disponibles.

2. **Visualización y Cambio desde el Menú Hamburguesa:**
   - El usuario abre el menú lateral (hamburguesa).
   - En la sección **Empresa Emisora**, ve la empresa seleccionada actualmente (Nombre y RUC).
   - Al tocar el selector, se abre un menú desplegable con todas las empresas emisoras activas.
   - Al seleccionar una empresa alternativa:
     - La app llama a `POST /api/m1/user-active-company` con `company_id`.
     - El backend actualiza `users.default_company_id`.
     - La app actualiza la empresa activa localmente.

3. **Cobro y Emisión de Comprobantes:**
   - El usuario agrega productos al carrito y navega a la pantalla de Cobro / Checkout.
   - En el Checkout se visualiza la tarjeta con la **Empresa Emisora** activa.
   - Al procesar la venta, se envía el campo `"company_id": ID_SELECCIONADO` al backend.
   - El backend emite la Boleta/Factura en SUNAT usando las credenciales, series y correlativos pertenecientes a dicha empresa.
