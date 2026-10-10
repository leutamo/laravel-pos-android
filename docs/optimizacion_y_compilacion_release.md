# Guía de Optimización de Rendimiento y Compilación Release (Producción)

## 1. Resumen de Optimizaciones de Rendimiento Aplicadas

Para garantizar que la aplicación corra de forma fluida y rápida incluso en dispositivos Android de gama de entrada (como procesadores MediaTek Helio G36 / Unisoc / 3GB-4GB RAM), se aplicaron las siguientes mejoras en el código:

### A. Animación en RenderThread de GPU (`LoginScreen.kt`)
- **Antes:** La animación del icono usaba un bucle `while(true)` cambiando estado en `Dp`, lo que provocaba que Jetpack Compose midiera, calculara y redibujara la pantalla entera **60 veces por segundo**.
- **Ahora:** Se utiliza `.graphicsLayer { translationY = translateY }` con `rememberInfiniteTransition`. Esta técnica delega el movimiento al hilo gráfico de la GPU (**RenderThread**), requiriendo **0 recomposiciones en la CPU**.

### B. Reducción de Overhead en Logs de Red (`NetworkModule.kt`)
- **Antes:** Ktor usaba `LogLevel.ALL`, lo que obligaba a formatear e imprimir en consola (Logcat) miles de líneas de texto JSON por cada solicitud de productos.
- **Ahora:** Se ajustó a `LogLevel.INFO` en modo Debug y `LogLevel.NONE` en Release.

### C. Claves Únicas de Recomposición (`HomeScreen.kt`)
- La lista de productos en `LazyVerticalGrid` utiliza identificadores estables `key = { it.id }`, permitiendo que Compose recicle tarjetas de productos eficientemente durante el scroll.

---

## 2. Diferencia entre Compilación Debug vs. Release

- **APK Debug (`Build -> Build APK(s)` o `./gradlew assembleDebug`):**
  Incluye metadatos de inspección de código, trazabilidad de Compose y deshabilita R8. Es pesado y lento en celulares gama baja.

- **APK Release (`./gradlew assembleRelease`):**
  Aplica la minificación y optimización de código mediante **R8/ProGuard**. Elimina código no utilizado y optimiza el bytecode de Kotlin, haciendo que la app se ejecute entre **5x a 10x más rápido**.

---

## 3. Configuración de ProGuard / R8 (`app/proguard-rules.pro`)

Se configuraron las reglas necesarias en `app/proguard-rules.pro` para permitir la minificación R8 sin errores de compilación para **Ktor, SLF4J, Kotlinx Serialization, Coil y Hilt**:

```proguard
# SLF4J / Ktor Logging
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Ktor Client
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*,ElementValueAttribute,Signature
-keepnames class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# Hilt / Dagger
-dontwarn dagger.**
-keep class dagger.** { *; }

# Models Data Classes
-keep class com.example.laravelpos.data.model.** { *; }
```

---

## 4. Guía para Generar el APK de Producción (Release)

### Opción A: Mediante la Terminal de Comandos

Abre la terminal en la raíz del proyecto Android y ejecuta:

```bash
./gradlew assembleRelease
```

Al finalizar la compilación exitosa (`BUILD SUCCESSFUL`), la ubicación del APK optimizado listo para instalar o distribuir es:

```
app/build/outputs/apk/release/app-release.apk
```

---

### Opción B: Desde el Menú de Android Studio

1. Ve al menú superior: **Build -> Generate Signed Bundle / APK...**
2. Selecciona **APK** y presiona **Next**.
3. Selecciona tu clave/keystore de firma.
4. En **Build Variants**, selecciona **`release`**.
5. Presiona **Create / Finish**.
