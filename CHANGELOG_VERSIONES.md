# 📌 Estado Actual de Versiones — Truequapp Android

Este archivo registra el ciclo de vida de versiones activas de acuerdo a la política de retención de 2 versiones ($N-1$).

---

## 🟢 Versión Activa ($V_N$)
* **Versión:** `v0.5`
* **Fase:** Fases 3, 4 y 5 — *Seguridad Geoespacial, Google Maps SafeZones, QR Dinámico, Modo Swipe, Monedero Escrow, Trueque Triangular, Analítica de Liquidez, PDF Nativo, Play Integrity y R8 Hardening*
* **Rama Git:** `terminado-provicional`
* **Fecha:** 28 de Octubre de 2026
* **Resumen de Módulos y Capacidades Implementadas:**
  * **Fase 3: Seguridad Geoespacial, SafeZones y Confirmación Presencial (`:feature:safezones`):**
    * **Google Maps Compose en Puntos Seguros (CU019):** Integración interactiva de mapa con marcadores de SafeZones oficiales UNAM/UV, cálculo dinámico del centroide geodésico equidistante y configuración de API Key mediante placeholders de Gradle.
    * **Intercambio Seguro con QR Dinámico & TOTP (CU020):** Escaneo con CameraX / ML Kit y generación de token rotativo HMAC-SHA256 cada 60s, complementado con código OTP de 6 dígitos de respaldo offline para transacciones en sótanos o zonas de baja cobertura celular.
    * **Transparencia en Reseñas Públicas (RN-12):** Publicación inmediata de valoraciones bilaterales garantizando máxima reputación pública sin sesgo ni chantaje.
    * **Gestión de Disputas & Anti-Spoofing (CU022, RN-08):** Verificación de radio geográfico de 150 metros con rechazo inmediato de falsificación de GPS (*Mock Location Detection*).
    * **Baja de Cuenta y Supresión de Datos GDPR / Google Play (CU002):** Flujo de borrado irrevocable y anonimización de historial según directrices de Google Play Data Safety.
  * **Fase 4: Modo Swipe, Monedero TruequeCoins y Grafos Triangulares (`:feature:swipe`, `:feature:wallet`):**
    * **Modo Exploración Swipe (CU017):** Mazo de publicaciones con física cinética, arrastre, rotación angular, distintivos flotantes "ME INTERESA" / "PASAR" y detección automática de coincidencias (*Match bilateral*).
    * **Monedero Digital & Partida Doble (CU013, RN-04):** Arquitectura contable estricta de suma cero verificando en tiempo real que `saldoDisponible + saldoEscrow == saldoTotal`.
    * **Seguridad Biométrica y PIN de Operaciones (CU014):** Integración nativa con `androidx.biometric.BiometricPrompt` y PIN para autorizar movimientos y retenciones.
    * **Custodia en Garantía Escrow (CU015, RN-05):** Bloqueo automático de créditos al acordar cita y transferencia garantizada al escanear QR de entrega.
    * **Trueque Triangular Multilateral A ➔ B ➔ C ➔ A (CU016, RN-06):** Visualización en Canvas de grafo tripartito dirigido, compatibilidad algorítmica, 3 de 3 firmas requeridas y ventana de cancelación de 24 horas sin penalización.
  * **Fase 5: Analítica de Liquidez, PDF, Play Integrity y R8 (`:feature:reports`, `:core:network`, `app`):**
    * **Panel de Liquidez y Cold Start (CU021):** Monitoreo en tiempo real de OAR ($\ge 45\%$), TCR ($\ge 80\%$), TTFT ($\le 72\text{ h}$), rotación mensual de TruequeCoins y densidad de SafeZones ($\ge 85\%$).
    * **Generador Nativo de Informes en PDF:** Renderizado vectorial en formato Carta (Letter 612x792 pt) con tablas y dictamen oficial utilizando `android.graphics.pdf.PdfDocument`.
    * **Google Play Integrity API:** Gestor en `:core:network` para verificar veredictos estándar anti-manipulación y anti-emulador.
    * **Hardening R8 / ProGuard:** Reglas de ofuscación, retención de anotaciones de serialización y optimización en `app/proguard-rules.pro`.

---

## 🟡 Versión de Respaldo ($V_{N-1}$ — Punto de Restauración Fallback)
* **Versión:** `v0.4`
* **Fase:** Fase 2 — *MVP Core Refinements: Moneda Única `truequeCoins`, Especificación RN-03, Límite 1 Foto WebP, Pestañas Activas/Ofertas, Persistencia Local y Guardado de Favoritos*
* **Rama Git:** `feature/fase-2-mvp-core`
* **Fecha:** 28 de Octubre de 2026
* **Resumen:**
  * Estandarización de `truequeCoins`, banner explicativo de auditoría 48h RN-03, compresión WebP lossy < 80 KB para 1 sola foto, pestañas Activas/Ofertas en listado de trueques y persistencia en sandbox local (`user_published_items.json` y `user_bookmarked_items.json`).

---

## 🔴 Versiones Purgadas ($V_{N-2}$)
* **Versión:** `v0.3` (Fase 2 MVP Core Inicial), `v0.2` (Fase 0 Reseñas) y `v0.1` (Fase 0 Modelado Canónico). Archivadas conforme a la política $N-1$.
