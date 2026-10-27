# 📌 Estado Actual de Versiones — Truequapp Android

Este archivo registra el ciclo de vida de versiones activas de acuerdo a la política de retención de 2 versiones ($N-1$).

---

## 🟢 Versión Activa ($V_N$)
* **Versión:** `v0.1`
* **Fase:** Fase 0 — *Modelado de Dominio, Reglas de Negocio (RN-01 a RN-12) y Cumplimiento Google Play*
* **Fecha:** 26 de Octubre de 2026
* **Resumen:**
  * Modelado canónico en Kotlin puro de `User`, `Item`, `Trade`, `TriangularTrade` y `Wallet`.
  * Implementación de los 12 casos de uso de reglas de negocio (`com.truequapp.core.domain.rules`).
  * Cobertura de pruebas unitarias al 100% en `core:domain` para cumplimiento de Google Play y contratos canónicos.

---

## 🟡 Versión de Respaldo ($V_{N-1}$ — Punto de Restauración Fallback)
* *Ninguna (Versión génesis inicial del proyecto).*

---

## 🔴 Versiones Purgadas ($V_{N-2}$)
* *Ninguna por el momento.*
