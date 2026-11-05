# ==============================================================================
# TRUEQUAPP - REGLAS DE OFUSCACIÓN Y HARDENING R8 (FASE 5)
# ==============================================================================

# 1. Reglas Generales de Optimización y Atributos de Depuración
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 2. Modelos de Dominio y Serialización Kotlinx (Preservar DTOs)
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class **$$serializer {
    *;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# 3. Google Play Integrity API (Anti-tampering & Standard Verdict)
-keep class com.google.android.play.core.integrity.** { *; }
-dontwarn com.google.android.play.core.integrity.**

# 4. Google Maps Platform & SafeZones (Play Services Maps & Maps Compose)
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.compose.** { *; }
-dontwarn com.google.android.gms.maps.**

# 5. Red y WebSockets (OkHttp, Retrofit, Socket.io)
-dontwarn okhttp3.**
-dontwarn io.socket.**
-dontwarn okio.**
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }

# 6. Biometría AndroidX y Seguridad Local
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# 7. Jetpack Compose & Hilt Inyección de Dependencias
-keep class com.truequapp.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.room.RoomDatabase
