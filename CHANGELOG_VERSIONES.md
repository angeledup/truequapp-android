# 📌 Estado Actual de Versiones — Truequapp Android

Este archivo registra el ciclo de vida de versiones activas de acuerdo a la política de retención de 2 versiones ($N-1$).

---

## 🟢 Versión Activa ($V_N$)
* **Versión:** `v0.9.0-Ch`
* **Fase:** Beta / Producción CETIS 79 — *Notificaciones interactivas, Sincronización Multidispositivo en la Nube, Confirmación de Publicación y Depuración de Datos Demo*
* **Rama Git:** `beta`
* **Fecha:** 8 de Noviembre de 2026
* **Resumen de Módulos y Capacidades Implementadas:**
  * **Menú Interactivo de Notificaciones (`:core:designsystem`):**
    * Integración de `DropdownMenu` anclado a la campana de `TruequappTopAppBar` con diseño Warm Studio (obsidian y acentos dorados).
    * Notificaciones en tiempo real: balance activo de TruequeCoins ($TC$), recordatorios de Punto Seguro CETIS 79 (Caseta y Domo Cívico) y estado de sincronización.
    * Acción de "Marcar leídas" para reiniciar el contador visual y ocultar el indicador de novedades.
  * **Confirmación Inmediata de Publicación (`:feature:catalog`):**
    * Diálogo Material 3 (`AlertDialog`) con confirmación visual de éxito y `Toast` nativo de Android al publicar artículos escolares.
    * Redirección automática con snackbar de feedback al regresar al catálogo escolar.
  * **Sincronización Multidispositivo en la Nube (`:app`, `:core:network`):**
    * Autenticación híbrida en `AuthRepository`: registro local en Room + envío inmediato a la nube (`/api/usuarios/sync`).
    * Inicio de sesión multi-dispositivo con recuperación automática de credenciales y balance desde Turso/Vercel.
  * **Depuración de Datos de Demostración (`:core:database`, `:feature:trade`, `server`):**
    * Eliminación definitiva de ofertas y chats de demostración predeterminados.
    * Base de datos Room limpia para intercambios reales de alumnos del CETIS 79.

---

## 🟡 Versión de Respaldo ($V_{N-1}$ — Punto de Restauración Fallback)
* **Versión:** `v0.8.7`
* **Fase:** Beta — *Pre-carga de items escolares CETIS 79, Punto seguro y paleta Warm Commerce Studio*
* **Rama Git:** `beta`
* **Fecha:** 7 de Noviembre de 2026
* **Resumen:**
  * Actualización de items escolares CETIS 79 precargados, configuración de Puntos Seguros y diseño Warm Obsidian Gold.

---

## 🔴 Versiones Purgadas ($V_{N-2}$)
* **Versiones:** `v0.8.6`, `v0.8.5` y anteriores. Archivadas conforme a la política de retención $N-1$.
