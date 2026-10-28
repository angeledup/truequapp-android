# 📌 Estado Actual de Versiones — Truequapp Android

Este archivo registra el ciclo de vida de versiones activas de acuerdo a la política de retención de 2 versiones ($N-1$).

---

## 🟢 Versión Activa ($V_N$)
* **Versión:** `v0.3`
* **Fase:** Fase 2 — *MVP Core: Usuarios, Catálogo WebP, Trueques 1:1 y Chat Transaccional*
* **Rama Git:** `feature/fase-2-mvp-core`
* **Fecha:** 27 de Octubre de 2026
* **Resumen:**
  * **Módulo Autenticación y Perfil (`:feature:auth`):**
    * Verificación SMS OTP con elevación a KYC Nivel 1 (`TELEFONO_SMS_NIVEL_1`) y activación de monedero inicial de 100 TC (CU001).
    * Gestión de sesión, login con DataStore y cierre de sesión seguro (CU023, CU024).
    * Perfil de usuario con cálculo de Reputación Bayesiana neutral inicial (RN-09) y difuminado geoespacial en radio de 500 metros (RN-10).
  * **Módulo de Catálogo e Inventario WebP (`:feature:catalog`):**
    * Pipeline asíncrono de compresión WebP Lossy garantizando tamaño estrictamente `< 80 KB` (81,920 bytes) y purga de metadatos GPS EXIF (CU005).
    * Feed de marketplace con filtros dinámicos por radio geográfico (1 a 50 km) y taxonomía de categorías (CU008).
    * Validación estricta contra publicación de armas, pirotecnia y sustancias ilícitas (RN-11).
    * Protección de inventario que impide pausar o eliminar artículos en estado `RESERVADO` (RN-01).
  * **Motor de Propuestas de Trueque (`:feature:trade`):**
    * Propuestas directas e híbridas con obligatoriedad de al menos 1 artículo de catálogo propio más diferencial opcional en TruequeCoins (CU009 - Opción A).
    * Validación en tiempo real de la banda mínima de equivalencia del 40% del valor del bien objetivo (RN-02).
    * Aceptación con bloqueo atómico ACID: reserva exclusiva de artículos (`RESERVADO`) y cancelación automática de propuestas concurrentes (CU010, RN-01).
    * Emisión de contraofertas reajustando artículos y diferencial (CU011).
    * Cancelación con evaluación de ventana de 4 horas previas a la cita en Safe Zone, aplicando advertencia y deducción del 5% en la tasa de asistencia si es tardía (CU012, RN-07).
  * **Módulo de Chat Privado Transaccional (`:feature:chat`):**
    * Chat en tiempo real vía WebSockets con Socket.io (CU018).
    * Apertura condicionada a propuestas formales activas; acceso denegado a terceros o fuera de trueques (RN-03).
    * Indicadores visuales de doble verificación (*checks* de entrega y lectura) e indicador animado de escritura.
    * Modo solo lectura durante una ventana de auditoría de 48 horas tras finalizar o cancelar el trueque (RN-03).
  * **Integración y Navegación (`:app`):**
    * Conexión fluida en `TruequappNavHost.kt` entre feed, publicación, negociación, chat y perfil.
    * Módulo de inyección Hilt `DomainModule.kt` para conectar las reglas puras de dominio.
    * Generación de instalador depurable `app-debug.apk` verificado.

---

## 🟡 Versión de Respaldo ($V_{N-1}$ — Punto de Restauración Fallback)
* **Versión:** `v0.2`
* **Fase:** Fase 0 — *Modelado de Dominio, Reglas de Negocio (RN-01 a RN-12) y Cumplimiento Google Play*
* **Rama Git:** `feature/fase-0-business-rules`
* **Fecha:** 26 de Octubre de 2026
* **Resumen:**
  * Modificación de la regla invariante **RN-12**: sustitución del mecanismo de doble ciego por **Transparencia y Visibilidad Pública Inmediata de Reseñas**.
  * Se permite a cualquier usuario consultar el historial de reseñas de una contraparte sin requerir haber interactuado previamente con ella.
  * Publicación inmediata de reseñas al completarse el trueque (sin ventana de ocultamiento de 7 días).
  * Creación de `EvaluateReviewVisibilityUseCase` y actualización de contratos en `TradeReview`, `ReviewVisibilityStatus` y `EvaluateDoubleBlindReviewUseCaseTest`.

---

## 🔴 Versiones Purgadas ($V_{N-2}$)
* **Versión:** `v0.1` (Fase 0 - Modelado Canónico Inicial). Reemplazada y archivada conforme a la política $N-1$.
