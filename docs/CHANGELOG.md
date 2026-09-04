# Changelog - Laravel POS Android

Este archivo documenta los hitos y cambios importantes realizados en la aplicación móvil.

## [2026-08-29] - Integración de Negocio y Robustez

### Añadido
- **Gestión de Roles y Permisos**:
  - Conexión con el endpoint `/api/config` para obtener capacidades del usuario.
  - Nueva pantalla **"Mis Permisos"** en el menú lateral para auditoría de usuario.
  - Lógica inteligente en Checkout: elige automáticamente entre **Venta Directa** o **Cotización** basándose en el permiso `manage_sale`.
- **Gestión de Clientes Real**:
  - Búsqueda por número de documento vinculada a la base de datos Laravel.
  - Creación de nuevos clientes con validaciones del servidor y selector de tipo de documento (DNI, RUC, etc.).
  - Botón **"Genérico"** que selecciona automáticamente al primer cliente registrado en el sistema.
- **Visualización de Perfil**:
  - Carga dinámica del nombre y rol del usuario en el Drawer.
- **Sistema de Documentación**:
  - Creación de carpeta `/docs` para seguimiento del proyecto.

### Mejoras y Fixes
- **Robustez de API**:
  - Manejo de campos nulos en `order_tax`, `tax_type` y `last_name` para evitar cierres inesperados.
  - Implementación de `ProductImagesSerializer` para manejar inconsistencias en el formato de imágenes (Array vs Object).
- **UX/UI**:
  - Sincronización del buscador de productos entre pantallas.
  - Limpieza automática de datos de cliente al finalizar una transacción.
  - Eliminación de mensajes `Toast` para una interfaz más limpia.
  - Botón de borrado rápido ("X") en la tarjeta de cliente seleccionado.
- **Resumen Dinámico**:
  - La pantalla de resumen ahora carga los datos directamente desde el servidor usando el ID del documento generado.

## [Anteriores] - Cimientos y Navegación
- Implementación de IP dinámica del servidor.
- Menú lateral (Drawer) y navegación básica.
- Integración con Ktor para peticiones HTTP.
- Visualización de productos con imágenes desde almacenamiento local/remoto de Laravel.
