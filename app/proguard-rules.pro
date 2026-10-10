# ProGuard / R8 Rules for Laravel POS Android

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
-keepclassmembers class *$$serializer {
    *** INSTANCE;
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
