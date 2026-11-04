# 📌 Estado Actual de Versiones — Truequapp Android

Este archivo registra el ciclo de vida de versiones activas de acuerdo a la política de retención de 2 versiones ($N-1$).

---

## 🟢 Versión Activa ($V_N$)
* **Versión:** `v2.1`
* **Fase:** Fase 2 — *MVP Core: Paso 1 - Autenticación, Registro y Base de Datos SQL Room con 7 Columnas*
* **Rama Git:** `main`
* **Fecha:** 3 de Noviembre de 2026
* **Resumen:**
  * **Base de Datos SQL Nativa con Room (`:core:database`):**
    * Creación de la tabla `usuarios` conteniendo exactamente las 7 columnas requeridas por negocio:
      1. `correo` (TEXT PRIMARY KEY)
      2. `contrasena` (TEXT)
      3. `usuario` (TEXT)
      4. `numero_telefonico` (TEXT)
      5. `cantidad_trueque_coins` (REAL, default 100.0)
      6. `trueques_realizados` (INTEGER, default 0)
      7. `calificacion` (REAL, default 5.0)
    * `UserDao` con consultas SQL directas: `getUserByEmail(correo)`, `countByEmail(correo)`, `insertUser(user)` y `getAllUsers()`.
  * **Pantalla de Inicio de Sesión (`LoginScreen`):**
    * Destino inicial al ingresar a la aplicación (`startDestination = Screen.Login.route`).
    * Validación reactiva de 2 inputs contra la base de datos SQL Room:
      * Correo electrónico: Si no existe, muestra el mensaje exacto `"No hay una cuenta vinculada al correo"`.
      * Contraseña: Si el correo existe pero la contraseña no coincide, muestra el mensaje exacto `"Contraseña incorrecta"`.
    * Botón al pie `"Registrar cuenta nueva"` para navegar a la pantalla de registro.
  * **Pantalla de Registro (`RegisterScreen`):**
    * 4 inputs validados que persisten directamente en la base de datos SQL:
      1. Nombre de usuario (`usuario`)
      2. Número telefónico (`numero_telefonico`)
      3. Correo electrónico (`correo`)
      4. Contraseña (`contrasena`)
    * Validación anti-duplicados por correo y retorno asistido al Login tras registrarse con éxito.
  * **Pruebas y Compatibilidad Google Play:**
    * `versionCode = 2001`, `versionName = "2.1"`.
    * Pruebas unitarias para `AuthRepository`, `LoginViewModel` y `UserEntity` en JUnit Platform.

---

## 🟡 Versión de Respaldo ($V_{N-1}$ — Punto de Restauración Fallback)
* **Versión:** `v0.4`
* **Fase:** Fase 2 — *MVP Core Refinements: Moneda Única `truequeCoins`, Especificación RN-03, Límite 1 Foto WebP, Pestañas Activas/Ofertas, Persistencia Local y Guardado de Favoritos*
* **Rama Git:** `main`
* **Fecha:** 28 de Octubre de 2026
* **Resumen:**
  * Estandarización de `truequeCoins`, banner explicativo de auditoría 48h RN-03, compresión WebP lossy < 80 KB para 1 sola foto, pestañas Activas/Ofertas en listado de trueques y persistencia en sandbox local (`user_published_items.json` y `user_bookmarked_items.json`).

---

## 🔴 Versiones Purgadas ($V_{N-2}$)
* **Versión:** `v0.3` (Fase 2 MVP Core Inicial), `v0.2` (Fase 0 Reseñas) y `v0.1` (Fase 0 Modelado Canónico). Archivadas conforme a la política $N-1$.
