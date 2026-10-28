# 📌 Estado Actual de Versiones — Truequapp Android

Este archivo registra el ciclo de vida de versiones activas de acuerdo a la política de retención de 2 versiones ($N-1$).

---

## 🟢 Versión Activa ($V_N$)
* **Versión:** `v0.4`
* **Fase:** Fase 2 — *MVP Core Refinements: Moneda Única `truequeCoins`, Especificación RN-03, Límite 1 Foto WebP, Pestañas Activas/Ofertas, Persistencia Local y Guardado de Favoritos*
* **Rama Git:** `feature/fase-2-mvp-core`
* **Fecha:** 28 de Octubre de 2026
* **Resumen:**
  * **Unificación de Moneda Virtual (`truequeCoins`):**
    * Estandarización de la variable en contratos, estados y ViewModels (`:feature:auth`, `:feature:trade`, `:core:domain`). Eliminación de nombres heterogéneos (`walletBalanceCoins`, `compensatoryCoinsInput`, `userCoinsBalance`) a favor de `truequeCoins` y `truequeCoinsInput`, manteniendo getters retrocompatibles.
  * **Especificación y Detalle de la Regla RN-03 (`:feature:chat`, `:feature:trade`):**
    * Incorporación de diálogo explicativo y banner permanente de seguridad detallando los tres pilares de RN-03: (1) Apertura condicionada a propuestas formales activas, (2) Prevención de acoso y tratos fuera de plataforma suprimiendo mensajería directa libre previa, y (3) Ventana de auditoría de exactamente 48 horas en modo solo lectura para controversias tras culminar o cancelar el trueque.
  * **Restricción de Publicación a 1 Sola Imagen (`:feature:catalog`):**
    * Ajuste en `ItemPublishScreen` con `PickVisualMedia` limitando la subida a exactamente 1 fotografía, procesada asíncronamente a WebP lossy `< 80 KB` con eliminación de EXIF/GPS.
  * **Zona de Trueques: Pestañas 'Activas' y 'Ofertas' (`:feature:trade`):**
    * Reestructuración de `TradeListScreen`, `TradeContract` y `TradeViewModel`:
      * **Activas:** Propuestas salientes enviadas por el usuario o intercambios en curso (`RESERVADO`, `EN_ENTREGA`).
      * **Ofertas:** Ofertas entrantes recibidas de otros usuarios respecto a publicaciones propias y contraofertas recibidas sobre la propuesta inicial.
  * **Persistencia Local de Artículos Propios Publicados (`:feature:catalog`):**
    * Implementación de `CatalogLocalStorage` serializando `user_published_items.json` en el sandbox privado de la app para conservar las publicaciones entre sesiones.
  * **Marcado y Guardado de Publicaciones de Otros Usuarios (`:feature:catalog`):**
    * Marcado de favoritos persistido en `user_bookmarked_items.json` con filtro rápido en el catálogo de marketplace que solo muestra publicaciones guardadas mientras sigan activas (`ItemStatus.DISPONIBLE`).

---

## 🟡 Versión de Respaldo ($V_{N-1}$ — Punto de Restauración Fallback)
* **Versión:** `v0.3`
* **Fase:** Fase 2 — *MVP Core: Usuarios, Catálogo WebP, Trueques 1:1 y Chat Transaccional Inicial*
* **Rama Git:** `feature/fase-2-mvp-core`
* **Fecha:** 27 de Octubre de 2026
* **Resumen:**
  * Implementación inicial de KYC Nivel 1 por SMS OTP, compresión WebP, motor de trueques 1:1 e híbridos con ratio del 40% (RN-02), bloqueo ACID (RN-01), cancelación anticipada con penalización (RN-07) y chat en tiempo real Socket.io.

---

## 🔴 Versiones Purgadas ($V_{N-2}$)
* **Versión:** `v0.2` (Fase 0 - Ajuste Transparencia de Reseñas RN-12) y `v0.1` (Fase 0 - Modelado Canónico Inicial). Archivadas conforme a la política $N-1$.
