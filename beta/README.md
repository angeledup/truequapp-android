# 🚀 Truequapp Android — Versión Beta Funcional

Esta subcarpeta contiene la versión **Beta** completamente funcional e independiente de Truequapp Android, separada de lo que será el programa final.

---

## 🎯 Objetivo de la Beta
El objetivo de esta carpeta es consolidar, probar y mejorar de forma iterativa el **flujo esencial de intercambio entre usuarios**, asegurando que cada componente clave funcione de extremo a extremo sin errores de concurrencia ni bloqueos de sesión:

1. **Autenticación y Registro:**
   * Inicio de sesión clásico con correo y contraseña validado contra SQLite Room.
   * Botón de **Inicio de Sesión con Google** funcional.
   * Validación estricta de cuentas (mensajes `"No hay una cuenta vinculada al correo"` y `"Contraseña incorrecta"`).
   * Almacenamiento seguro de sesión en `UserSessionDataStore`.

2. **Catálogo de Publicaciones (Persistencia Room):**
   * Feed de marketplace con imágenes WebP y tags descriptivos.
   * Publicación de nuevos artículos persistida en la tabla `articulos_trueque` de SQLite Room.
   * Regla de validación de artículos prohibidos (RN-11).
   * Filtros por categoría y estado de condición del artículo.

3. **Motor de Trueques (Negociación Real):**
   * **Regla estricta de 2 partes (Anti-Auto-Trueque):** Bloquea que un usuario oferte sobre sus propios productos.
   * Propuestas de trueque 1:1 persistidas en Room (`propuestas_trueque`).
   * Estados de negociación: Aceptada, Rechazada, Pendiente y **Contraofertas activas**.

4. **Chat y Mensajería Transaccional:**
   * Bandeja de entrada con **"Chat ejemplo 1"** preconfigurado para pruebas inmediatas sin bloqueos RN-03.
   * Chat en tiempo real con Socket.io.
   * Acceso directo a los Puntos Seguros dentro del chat para coordinar el intercambio físico.

5. **Puntos Seguros (SafeZones en Coatzacoalcos, Veracruz):**
   * Integración de ubicaciones oficiales para entregas seguras en Coatzacoalcos (Malecón Costero, Parque Independencia, Plaza Forum, etc.).
   * Generación y lectura de código QR de intercambio presencial.

6. **Base de Datos Local SQLite con Room (`TruequappDatabase`):**
   * `UserDao`, `TradeItemDao`, `SafeZoneDao`, `TradeProposalDao`, `ChatMessageDao` y `LedgerEntryDao`.

---

## 🚫 Qué se excluyó de la Beta (Reservado para la Versión Final)
Para mantener la Beta ligera, enfocada y fácil de depurar, se reservaron las siguientes características empresariales para la entrega final:
* **`feature:reports`:** Panel de liquidez avanzado y generador de reportes PDF bancarios.
* **`feature:swipe`:** Algoritmo de afinidad y navegación tipo swipe/Tinder.
* **`feature:wallet`:** Monedero con resolución de grafos triangulares multi-nodo y tokens TC.

---

## 🛠️ Cómo compilar y probar la Beta
Desde la terminal en la raíz de esta subcarpeta:
```powershell
cd beta
.\gradlew.bat assembleDebug
```
El instalador APK de la Beta se generará en:
`beta/app/build/outputs/apk/debug/app-debug.apk`

