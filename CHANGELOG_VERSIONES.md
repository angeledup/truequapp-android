# 📌 Estado Actual de Versiones — Truequapp Android

Este archivo registra el ciclo de vida de versiones activas de acuerdo a la política de retención de 2 versiones ($N-1$).

---

## 🟢 Versión Activa ($V_N$)
* **Versión:** `v0.3`
* **Fase:** Fase 0 — *Modelado de Dominio, Reglas de Negocio (RN-01 a RN-12) y Cumplimiento Google Play*
* **Fecha:** 26 de Octubre de 2026
* **Resumen:**
  * Configuración y distribución del Gradle Wrapper oficial 8.9 (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) para ejecución autónoma de pruebas en CLI y entornos locales.
  * Ajuste de `.gitignore` para exclusión recursiva de compilados (`**/build/`) y respaldos locales (`backups/`).
  * Verificación y validación de ejecución de pruebas unitarias de dominio y cumplimiento de políticas de Google Play.

---

## 🟡 Versión de Respaldo ($V_{N-1}$ — Punto de Restauración Fallback)
* **Versión:** `v0.2`
* **Fase:** Fase 0 — *Modelado de Dominio, Reglas de Negocio (RN-01 a RN-12) y Cumplimiento Google Play*
* **Fecha:** 26 de Octubre de 2026
* **Resumen:**
  * Modificación de la regla invariante **RN-12**: sustitución del mecanismo de doble ciego por **Transparencia y Visibilidad Pública Inmediata de Reseñas**.
  * Se permite a cualquier usuario consultar el historial de reseñas de una contraparte sin requerir haber interactuado previamente con ella.
  * Publicación inmediata de reseñas al completarse el trueque (sin ventana de ocultamiento de 7 días).
  * Creación de `EvaluateReviewVisibilityUseCase` y actualización de contratos en `TradeReview`, `ReviewVisibilityStatus` y `EvaluateDoubleBlindReviewUseCaseTest`.

---

## 🔴 Versiones Purgadas ($V_{N-2}$)
* **Versión:** `v0.1` (Génesis inicial del proyecto, purgada al consolidar v0.3 bajo política $N-1$).
