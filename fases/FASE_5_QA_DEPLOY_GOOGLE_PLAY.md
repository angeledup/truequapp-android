# 🚀 FASE 5: Analítica de Liquidez, Hardening de Seguridad, Play Integrity y Despliegue en Google Play
> **Asignación en Antigravity IDE:** Usuario / Agente 6 (QA, Security & Release Engineer)  
> **Duración estimada:** Semanas 15–16 del SDLC  
> **Rama Git:** `feature/fase-5-qa-playstore-ready`  
> **Casos de Uso UV Cubiertos:** CU021 (Métricas de Liquidez) y Auditoría Integral CU001–CU024  
> **Fuentes SWEBOK:** Calidad de Software (KA #10), Gestión de Configuración (KA #6)

---

## 🎯 Objetivo de la Fase
Garantizar la estabilidad, seguridad, rendimiento y conformidad total con las políticas de Google Play Store para el lanzamiento oficial de **Truequapp**. Incluye la implementación del panel de métricas de liquidez (CU021), generación de perfiles de inicio rápido (*Baseline Profiles*), integración de **Play Integrity API**, ofuscación con R8 y la preparación del paquete de producción (*Android App Bundle*).

---

## 📊 1. Reporteador y Analítica de Liquidez del Ecosistema (CU021)

Superar el problema del **Mercado Vacío (*Cold Start*)** requiere monitorear de manera continua la velocidad y efectividad de los trueques:

### Indicadores Clave de Liquidez (KPIs):
1. **OAR (*Offer Acceptance Rate*):** Porcentaje de ofertas emitidas que terminan siendo aceptadas o contraofertadas con éxito (Meta: $\ge 45\%$).
2. **TCR (*Trade Completion Rate*):** Porcentaje de propuestas aceptadas que concluyen exitosamente con escaneo de QR en la Safe Zone (Meta: $\ge 80\%$).
3. **TTFT (*Time To First Trade*):** Tiempo promedio en horas desde que un usuario publica su primer artículo hasta que concreta su primer intercambio (Meta: $\le 72\text{ horas}$).
4. **Velocidad de Circulación de TruequeCoins:** Frecuencia con la que los créditos cambian de manos entre miembros de la comunidad.

### Exportación a PDF Nativo
* Uso de la API nativa de Android `android.graphics.pdf.PdfDocument`.
* Generación del informe formal tamaño carta con tablas de resumen, gráficas de barras vectoriales e identidad corporativa para administradores de la comunidad universitaria.

---

## 🛡️ 2. Seguridad, Play Integrity API y Hardening

### Integración de Google Play Integrity API
Protege la plataforma contra aplicaciones modificadas, emuladores fraudulentos, ataques de *Man-In-The-Middle* e inyecciones de datos:

```kotlin
// Inyección en core:network para validar tokens de integridad
class PlayIntegrityManager @Inject constructor(
    private val integrityManager: StandardIntegrityManager
) {
    suspend fun obtainIntegrityVerdict(requestHash: String): String {
        // Solicita el veredicto criptográfico a los servidores de Google Play
        // para garantizar que la app proviene de Google Play y el dispositivo no está vulnerado (Root/Bootloader desbloqueado).
    }
}
```

### Reglas de Ofuscación y Reducción con R8 (`app/proguard-rules.pro`)
* Habilitar optimización completa (`isMinifyEnabled = true`, `isShrinkResources = true`).
* Mantener serialización de modelos de datos Kotlinx:
  ```proguard
  -keepclassmembers class * {
      @kotlinx.serialization.Serializable *;
  }
  -keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
  -dontwarn okhttp3.**
  -dontwarn io.socket.**
  ```

### Cifrado Local y Certificado TLS 1.3 Pinning
* Cifrado de la base de datos SQLite Room mediante **SQLCipher** con llave derivada de Android Keystore.
* Fijación de certificados (*Certificate Pinning*) en OkHttp para mitigar intercepciones de tráfico en redes públicas no seguras.

---

## ⚡ 3. Optimización de Rendimiento y Arranque Rápido

### Baseline Profiles (Google Play Benchmark)
* Generación de perfiles de compilación anticipada (*Ahead-Of-Time - AOT*) mediante el módulo `macrobenchmark`.
* Reduce el tiempo de arranque en frío (*Cold Start*) de la aplicación a menos de **$1.8\text{ segundos}$** (cumpliendo con creces el RNF-03 incluso en redes móviles lentas).

### Auditoría con LeakCanary
* Detección y eliminación sistemática de fugas de memoria en `CompositionScoped` de Compose, observadores de WebSockets y *listeners* de sensores de ubicación.

---

## 📋 4. Checklist Exhaustivo de Lanzamiento en Google Play Console

Antes de enviar el archivo `.aab` a revisión de producción en Google Play:

### Formulario de Seguridad de los Datos (*Data Safety Form*):
- [x] **Datos Recopilados:**
  - *Ubicación aproximada y precisa:* Para calcular distancias al inventario y validar llegada al Punto Seguro (no se comparte con terceros).
  - *Información personal:* Nombre, correo electrónico y número de teléfono (para inicio de sesión y KYC Nivel 1).
  - *Fotos y videos:* Solo las fotos que el usuario decide subir expresamente al publicar artículos.
- [x] **Seguridad:**
  - Todos los datos se transfieren mediante conexiones cifradas seguras (HTTPS / TLS 1.3).
  - El usuario puede solicitar la eliminación de su cuenta y todos sus datos en cualquier momento (CU002).

### Credenciales de Acceso para el Equipo de Revisión de Google (*App Access*):
- [x] Proveer cuenta de demostración activa en la consola:
  - **Usuario demo:** `playstore_reviewer@truequapp.org`
  - **Contraseña:** `TruequappDemo2026!`
  - **SMS OTP de prueba:** Preconfigurado en entorno Staging para aceptar `123456` sin necesidad de un chip SIM físico.
  - **Inventario precargado:** Artículos de muestra para que el revisor pueda probar el feed, el Modo Swipe y la propuesta de trueques.

### Generación del Artefacto de Producción:
```bash
./gradlew bundleRelease
```
El archivo resultante `app-release.aab` será firmado digitalmente con la clave de subida (*Upload Key*) y cargado al canal de pruebas cerradas (*Closed Testing Track*) con al menos 20 evaluadores durante 14 días conforme al reglamento de nuevas cuentas de Google Play.

---

## 📦 Entregables Verificables de la Fase 5
1. Módulo `feature:reports` con panel de control de liquidez y exportación PDF (CU021).
2. Reporte de benchmarks con tiempo de arranque $< 1.8\text{ s}$ verificado por Baseline Profiles.
3. Veredictos de Play Integrity API integrados en los endpoints críticos de autenticación y transacciones.
4. Paquete `app-release.aab` verificado sin advertencias de linting en Google Play Pre-Launch Report.
