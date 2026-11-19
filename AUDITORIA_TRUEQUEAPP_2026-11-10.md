# Auditoría en solo lectura — angeledup/truequapp-android (HEAD e50a351)

**Fecha de auditoría:** 10 de noviembre de 2026
**Repositorio:** `angeledup/truequapp-android` (público, Kotlin, rama `main`)
**Commit auditado:** `e50a35105b860cc18a289205f76f37f53fa31f69` — 2026-11-07T21:40:18Z — "feat(catalog,trade): implementar arquitectura de servidor simulado y almacenamiento local con filtro estricto por usuario (Opción A)"
**Auditoría anterior:** 4 de noviembre de 2026, commit `7c67322` (v2.1 / versionCode 2001). Esta auditoría re-verifica esos hallazgos en el código actual y audita los 10 commits nuevos del 5–7 de noviembre.
**Método:** solo lectura sobre clon local en `/tmp/truequapp-check`. No se modificó el repo, no se hizo push, no se instaló nada en producción, no se ejecutó código del repo. Se siguió la skill `pruebas_auditoria_software`: alcance por escrito, criterios definidos antes de cada prueba, evidencia con archivo:línea, y separación estricta entre lo probado y lo inferido.
**Separación:** este repo Android es un proyecto distinto del proyecto escolar de trueque del CETis (TruequeCoins internos, sin dinero real, sin teléfonos). No se mezclan en este informe.

## Veredicto

**NO APTO para Google Play ni para uso con usuarios reales o escolares en su estado actual.**

**APTO CON CONDICIONES únicamente como prototipo local, sin usuarios reales, después de corregir los bloqueadores P0.** El trabajo del 5–7 de noviembre es avance funcional real (chat persistido en Room, perfil real, repositorio de trueques, filtro por usuario), pero no tocó la seguridad: los tres P0 de la auditoría del 4 de noviembre siguen abiertos y apareció un P0 nuevo, más grave que los anteriores.

> Regla de la skill: un defecto de seguridad o de integridad de datos detiene la publicación aunque todo lo demás pase. Rendimiento y estética no compensan una fuga de datos ni una calificación inventada.

## 1. Alcance y matriz aplicada

### 1.1 Identidad y versión

| Dato | Valor verificado |
|---|---|
| Repositorio | `angeledup/truequapp-android`, público, rama `main` |
| Commit auditado | `e50a351` (2026-11-07) |
| Commits totales | 31 (del 2026-10-26 al 2026-11-07) |
| Commits nuevos desde la auditoría anterior | 10 (5–7 nov 2026) + 1 merge |
| Versión en código | `app/build.gradle.kts`: `versionCode = 2001`, `versionName = "2.1"`, `compileSdk = 34`, `targetSdk = 34`, `minSdk = 26` |
| Stack | AGP 8.5.2, Kotlin 2.0.20, Room 2.6.1, Hilt 2.52, Retrofit 2.11.0, OkHttp 4.12.0, Compose BOM 2024.09.02 (`gradle/libs.versions.toml`) |
| Tamaño | 98 archivos Kotlin en `src/main` (12.457 líneas), 33 archivos de test |

### 1.2 Módulos en el alcance

`app` (navegación, autenticación, perfil), `core:domain`, `core:common`, `core:designsystem`, `core:network`, `core:datastore`, `core:database`, `feature:catalog`, `feature:trade`, `feature:chat`, documentación `fases/` (Fase 0–5). Fuera del alcance: la carpeta `modulo 2/` (copia empaquetada del catálogo, no incluida en `settings.gradle.kts`, no compila como parte del proyecto) y cualquier backend (no existe).

### 1.3 Matriz de pruebas aplicada

Según la skill, para una **app móvil que aspira a publicarse y que maneja cuentas, dinero interno y ubicación**, las obligatorias son: funcionales, humo, robustez de entradas, seguridad completa, integridad de datos, compatibilidad y comportamiento sin conexión. Aplicado:

| Categoría | Aplicada | Cómo |
|---|---|---|
| Funcionales (sistema, por flujo) | Sí, estática | Lectura de cada flujo contra su código (sección 3) |
| Humo (arranca, login, pantalla principal) | No ejecutada | Sin JDK ni Android SDK ni emulador en esta máquina (sección 6) |
| Unitarias / integración | No ejecutadas | 33 archivos de test existen; no se pudieron correr (sección 6) |
| Robustez (entradas, borde, concurrencia) | Sí, estática | Revisión de validaciones, transacciones y condiciones de carrera en código |
| Seguridad | Sí, estática | Autenticación, autorización, secretos, exposición, dependencias |
| Integridad de datos | Sí, estática | Esquema Room, migraciones, unicidad, cascadas, valores por defecto |
| Carga / estrés / pico / soak | No aplica | No hay backend ni multiusuario real que cargar; sin ejecución posible |
| Rendimiento | Parcial, estática | Solo configuración de build (R8, recursos) |
| Usabilidad / accesibilidad | No probada | Sin ejecución en dispositivo |
| Legales (Play, privacidad, menores) | Parcial, estática | Contraste entre documentación y código; no sustituye `auditoria_legal` |

### 1.4 Criterios previos (definidos antes de leer los resultados)

- **P0 (bloqueante):** cualquier secreto expuesto, contraseña sin hash, cuenta privilegiada sembrada en código, o integridad de datos rota. Detiene la publicación.
- **Alta:** acceso sin control de autorización, pérdida o corrupción de datos posible, éxito falso anunciado al usuario, o afirmación de Play que el código contradice.
- **Media:** documentación que describe como hecho lo no implementado, configuración de build que anula una protección, o datos fabricados presentados como reales.
- **Baja:** duplicación de código muerto, deuda técnica sin impacto en seguridad ni datos.
- **Clasificación de cada hallazgo:** "defecto confirmado" solo si se ejecutó y falló; todo lo demás es "debilidad observada" (leído en código sin ejecutar). En esta auditoría **no hay defectos confirmados por ejecución**: no se pudo ejecutar nada. Todos los hallazgos son debilidades observadas con evidencia de código exacta.

## 2. Qué cambió desde la auditoría del 4 de noviembre

Diez commits nuevos (5–7 nov), 51 archivos, +5.207 / −451 líneas. Avance real y verificable:

| Commit | Cambio verificado |
|---|---|
| `929c92a` (4 nov) | Pantalla de perfil conectada a la base de datos (antes era un placeholder) |
| `000d346` (4 nov) | Siembra de usuario administrador en la inicialización de Room (**nuevo P0, D-01**) |
| `5460cb0` (6 nov) | `UserRepository` y `UserDao` con verificación y mutación atómica de coins, trueques y calificación |
| `9fd1002` (6 nov) | Todas las pantallas y ViewModels conectados a `UserRepository` como fuente única de verdad |
| `698af16` (7 nov) | Módulo de chat con menú Mensajes, `ChatMessageDao`, `ChatRepository` y persistencia en Room |
| `5487e51` (7 nov) | Eliminación de ofertas del catálogo de prueba y chats simulados en tiempo de ejecución |
| `8f1a330` (7 nov) | Detalle de oferta, activación condicional de chat, cabecera compacta de fotos |
| `328ba39` (7 nov) | Corrección de cierre abrupto al abrir el detalle (claves duplicadas) |
| `e50a351` (7 nov) | "Servidor simulado": catálogo central en JSON local + almacenamiento local con filtro estricto por usuario (Opción A) |

**Mejoras que sí se reconocen:** el chat ya no es solo memoria (ahora persiste en Room con `ChatMessageEntity`); el perfil ya muestra datos reales; los TruequeCoins ya tienen fuente única con actualizaciones atómicas por SQL (`MAX(0.0, ...)` evita saldos negativos, `UserDao.kt`); las propuestas de trueque ya se persisten en Room (`TradeRepository.kt`) con `esta_activa`; se añadieron tests (`TradeRepositoryTest`, `ChatListViewModelTest`, `UserRepositoryTest`, 33 archivos de test en total).

**Lo que no cambió:** la versión sigue en 2.1 / 2001, `targetSdk` sigue en 34, y ninguno de los 10 commits tocó autenticación, cifrado, secretos ni Play Integrity.

## 3. Resultados por flujo funcional

Criterio previo por flujo: el flujo está "implementado" solo si existe de punta a punta en código (UI → ViewModel → repositorio → persistencia) sin datos simulados en el camino; si solo existe la regla de dominio o la UI, se marca "solo regla" o "parcial".

| Flujo | Estado | Evidencia |
|---|---|---|
| Registro | Implementado (local) | `AuthRepository.kt` valida y guarda en Room; pero contraseña en texto plano (D-02) y mínima de 4 caracteres |
| Login | Implementado (local) | `AuthRepository.kt:38-56`; comparación directa (D-02); sin límite de intentos; enumera cuentas (D-08) |
| Catálogo / feed | Implementado (local) | `CatalogViewModel.kt`, `CatalogLocalStorage.kt`; el "servidor" es un JSON en `filesDir` (`UserPublicationStorage.kt:30`) |
| Publicar artículo | Implementado (local) | Publicación al JSON local con filtro por usuario (Opción A, `e50a351`) |
| Proponer trueque | Implementado (local) | `TradeViewModel.kt`, `TradeRepository.kt`; validación de valor RN-02 aplicada (`TradeViewModel.kt:363`); sin transacción (D-05) |
| Aceptar trueque | Implementado (parcial) | `acceptProposal` (`TradeViewModel.kt:587`) aplica `LockInventoryForTradeUseCase` y pasa a RESERVADO; no hay liquidación |
| Trueque triangular | Solo regla de dominio | `TriangularCycleRulesUseCase` existe en `core/domain` con tests; cero usos en `app`/`feature` (D-06) |
| Escrow / monedero | Solo regla de dominio | `EscrowHoldAndReleaseUseCase` existe en `core/domain` con tests; cero usos en `app`/`feature` (D-06) |
| Cierre / COMPLETADO | No implementado | Ninguna función lleva un trueque a COMPLETADO; sin confirmación QR ni liberación de escrow (D-06) |
| TruequeCoins | Implementado (local) | `UserRepository`/`UserDao` atómicos; pero el admin sembrado restablece su saldo a 100M en cada apertura (D-01) |
| Chat | Implementado (local) | Room + `ChatRepository.kt:32-145`; sin control de acceso RN-03 (D-04); sin servidor real (emisión por socket en `try/catch` resiliente) |
| Zonas seguras / ubicación | No implementado | Ruta `safe_zones` es `PlaceholderSectionScreen` (`TruequappNavHost.kt:309-315`); `VerifySafeZonePresenceUseCase` sin uso en `app`/`feature` |
| Perfil | Implementado (local) | `ProfileRoute`/`ProfileViewModel` con datos reales de Room |
| Borrado de cuenta | No implementado | Sin `deleteUser`/`deleteAccount` en código principal (D-13) |
| KYC / OTP / biometría | No implementado | `isKycVerified = false` siempre; sin envío ni verificación de OTP; sin `BiometricPrompt` |

## 4. Defectos ordenados por impacto

### P0 — Bloqueantes (detienen la publicación)

#### D-01 — Cuenta de administrador sembrada en código, restablecida en cada apertura de la base de datos [CRÍTICO]

- **Evidencia:** `core/database/src/main/java/com/truequapp/core/database/di/DatabaseModule.kt:36-43` (`onCreate` y `onOpen` llaman a `insertDefaultAdminUser`) y `:46-66` (la función ejecuta `INSERT OR REPLACE INTO usuarios ...` con los valores del administrador).
- **Qué contiene el seed (tipos de dato, sin reproducir valores):** correo personal del autor (línea 58), contraseña por defecto en texto plano (línea 59), nombre de usuario administrativo (línea 60), número de teléfono personal (línea 61), saldo de 100.000.000,0 TruequeCoins (línea 62), 10 trueques realizados y calificación 10,0 (líneas 63-64).
- **Impacto:** (a) cualquiera que lea el repositorio público conoce la cuenta administradora y su contraseña; (b) como corre en `onOpen` con `INSERT OR REPLACE`, **cambiar la contraseña no sirve**: la aplicación la restablece sola cada vez que abre la base de datos; (c) el saldo de 100M se restablece también, por lo que el administrador nunca puede gastar ni perder coins y el "Single Source of Truth" de `UserRepository` queda roto para esa cuenta; (d) el chat usa ese mismo correo como identidad por defecto sin sesión (D-04), así que los mensajes de un usuario sin sesión se atribuyen al administrador.
- **Clasificación:** debilidad observada (código leído, no ejecutado). La lectura del código es determinista: el callback está cableado en el builder de Room (`DatabaseModule.kt:28-66`).
- **Corrección:** eliminar el seed del código y del historial de git; rotar/invalidar esa contraseña y ese correo como credencial; no sembrar nunca cuentas con privilegios ni saldos en código; si se necesita un admin, crearlo fuera de la app con contraseña generada y hash.

#### D-02 — Contraseñas en texto plano en Room, sin hash ni cifrado [CRÍTICO]

- **Evidencia:** `core/database/src/main/java/com/truequapp/core/database/entity/UserEntity.kt:31-32` (columna `contrasena` como `String` plano); `app/src/main/java/com/truequapp/auth/data/AuthRepository.kt:51` (`if (user.contrasena != contrasena)` — comparación directa); `AuthRepository.kt:93` (mínimo de 4 caracteres); registro guarda la contraseña tal cual (`AuthRepository.kt`, construcción de `UserEntity` en `register`).
- **Sin hash ni cifrado en el proyecto:** búsqueda de `sqlcipher`, `bcrypt`, `argon`, `scrypt`, `pbkdf` y `hash` en `*.kts` y `*.toml` sin resultados. El documento de Fase 5 promete "Cifrado de la base de datos SQLite Room mediante SQLCipher con llave derivada de Android Keystore", pero **ninguna dependencia SQLCipher existe en el build**. Promesa de documento sin código.
- **Agravante:** sin límite de intentos de login (búsqueda de intentos/bloqueo/rate-limit en `app/src/main` sin resultados) y con mínimo de 4 caracteres, la fuerza bruta local es trivial.
- **Clasificación:** debilidad observada. **Sin cambio respecto a la auditoría del 4 de noviembre.**
- **Corrección:** hash con bcrypt (o Argon2) con sal, en el backend real cuando exista; mientras tanto, como mínimo, hash en el dispositivo con la contraseña nunca persistida en claro y migración de las cuentas existentes; subir el mínimo a 8 caracteres con complejidad; añadir bloqueo o retardo tras intentos fallidos.

#### D-03 — Credencial demo publicada en un repositorio público [CRÍTICO]

- **Evidencia:** `fases/FASE_5_QA_DEPLOY_GOOGLE_PLAY.md:91-94`, sección "Credenciales de Acceso para el Equipo de Revisión de Google": usuario demo (línea 92), contraseña (línea 93) y OTP de prueba fijo (línea 94). **Los valores no se reproducen en este informe.**
- **Estado:** sigue publicada, sin cambios desde la auditoría del 4 de noviembre. El repositorio es público; la credencial debe tratarse como expuesta.
- **Clasificación:** debilidad observada.
- **Corrección:** eliminarla del documento y del historial de git (reescritura con `git filter-repo` o equivalente, con force-push coordinado), rotar/invalidar cualquier cuenta o código equivalente, y no volver a publicar credenciales en el repo: las credenciales de revisión de Play van solo en Play Console, nunca en el código.

### ALTA

#### D-04 — Chat sin control de acceso: la regla RN-03 existe pero no se aplica

- **Evidencia:** `feature/chat/src/main/java/com/truequapp/feature/chat/mvi/ChatViewModel.kt:13,30` importa e instancia `ValidateDirectChatAccessUseCase`, pero **no hay ninguna llamada a ese caso de uso en el archivo**; `feature/chat/src/main/java/com/truequapp/feature/chat/data/ChatRepository.kt:54-98` (`sendMessage`) inserta cualquier mensaje con cualquier emisor y receptor, sin verificar que ambos sean parte del trueque; `ChatViewModel.kt:69` y `ChatListViewModel.kt:43` usan el correo del administrador sembrado como identidad por defecto cuando no hay sesión.
- **Impacto:** un usuario sin sesión chatea con la identidad del administrador; un usuario con sesión puede enviar mensajes en cualquier conversación si conoce su identificador, porque el repositorio no valida pertenencia. La regla de dominio existe y tiene test (`ValidateDirectChatAccessUseCaseTest.kt`), pero no está cableada.
- **Clasificación:** debilidad observada.
- **Corrección:** invocar `ValidateDirectChatAccessUseCase` antes de enviar y de leer mensajes; eliminar los correos por defecto de los ViewModels de chat (sin sesión, no hay chat); la validación debe vivir en el repositorio, no solo en la UI.

#### D-05 — Propuesta de trueque sin transacción y con éxito falso

- **Evidencia:** `feature/trade/src/main/java/com/truequapp/feature/trade/data/TradeRepository.kt` (`saveProposal`) hace tres inserciones seguidas (Oferta A, Oferta B, Trueque central) sin `@Transaction`; `TruequeDao.kt` solo tiene `@Transaction` en lecturas (líneas 40, 47, 54, 61, 68, 75). Si falla la tercera inserción, quedan ofertas huérfanas sin trueque. Además, la reducción de coins compensatorias se hace en una llamada separada posterior (`TradeViewModel.kt:441`, `userRepository?.reduceCoins(coins, userEmail)`), fuera de la misma unidad de trabajo.
- **Éxito falso:** el `catch` que envuelve la persistencia es silencioso ("Silencioso o log de debug", `TradeViewModel.kt`), mientras que al usuario se le muestra "¡Propuesta enviada exitosamente!" y se le redirige a la negociación (`TradeViewModel.kt:449-450`). Si Room falla, el usuario cree que su propuesta existe y no existe.
- **Clasificación:** debilidad observada.
- **Corrección:** envolver las tres inserciones y el movimiento de coins en una sola transacción Room; propagar el error y no mostrar éxito si la persistencia falló.

#### D-06 — Sin cierre de trueque: escrow y triangular son solo reglas de dominio

- **Evidencia:** `EscrowHoldAndReleaseUseCase` y `TriangularCycleRulesUseCase` existen en `core/domain` con tests, pero la búsqueda en `app/src/main` y `feature/*/src/main` no encuentra ningún uso. `TradeStatus` define `COMPLETADO` ("Confirmación presencial exitosa por código QR", `core/domain/.../model/Trade.kt:22`), pero ninguna función en `TradeViewModel.kt` lleva un trueque a COMPLETADO; tampoco hay liberación de escrow ni liquidación de coins al completar. `EvaluateCancellationWindowUseCase` sí se usa (`TradeViewModel.kt:758,787`) y `LockInventoryForTradeUseCase` sí se usa en `acceptProposal` (`TradeViewModel.kt:587`), pero el bloqueo es en memoria y JSON local, no transaccional.
- **Impacto:** el ciclo de trueque no se puede cerrar dentro de la app; la UI anuncia "Reservado (ACID LOCK RN-01)" (`TradeNegotiationScreen.kt:355`) cuando el bloqueo no es ACID de extremo a extremo. La descripción del repo ("Algoritmo Triangular y Monedero Escrow") describe como hecho lo que solo existe como regla.
- **Clasificación:** debilidad observada.
- **Corrección:** implementar el cierre (confirmación, liberación de escrow, liquidación de coins) o retirar de la descripción y de la UI lo que no existe.

#### D-07 — Datos fabricados presentados como reales

- **Evidencia:** todo usuario nuevo nace con 100,0 TruequeCoins (`UserEntity.kt:34`) y calificación 5,0 (`UserEntity.kt:40`) sin haber hecho nada; el administrador sembrado nace con 10 trueques y 10,0; `TradeViewModel.kt:159` usa un número de teléfono ficticio por defecto cuando no hay teléfono en la base; `TradeViewModel.kt:61-65` (`seedInitialData`) siembra ítems en memoria; el catálogo general vive en `server_marketplace_catalog.json` en el almacenamiento local y se presenta como "Servidor Central Simulado" (`UserPublicationStorage.kt:17,30`).
- **Impacto:** calificaciones y saldos que se muestran como reales no tienen origen; un usuario nuevo aparece como "5 estrellas" antes de su primer trueque. En un sistema con menores y con reputación como mecanismo de confianza, esto es un defecto de integridad, no de estética.
- **Clasificación:** debilidad observada.
- **Corrección:** calificación inicial "sin calificaciones" (no 5,0); saldo inicial documentado como bono de bienvenida, no como saldo ganado; eliminar los valores por defecto ficticios (teléfono, `usr_current`); separar datos de ejemplo de datos reales con marca visible.

#### D-08 — Enumeración de cuentas en el login

- **Evidencia:** `AuthRepository.kt:48-53` distingue `UserNotFound` ("No hay una cuenta vinculada al correo") de `InvalidPassword` ("Contraseña incorrecta"), con mensajes distintos en la UI (`LoginViewModel.kt`).
- **Impacto:** permite confirmar qué correos están registrados, probando correos uno por uno.
- **Clasificación:** debilidad observada.
- **Corrección:** un solo mensaje genérico ("Correo o contraseña incorrectos") para ambos casos.

#### D-09 — `fallbackToDestructiveMigration`: cualquier cambio de esquema borra todos los datos

- **Evidencia:** `core/database/src/main/java/com/truequapp/core/database/di/DatabaseModule.kt:34`; esquema en versión 3 (`TruequappDatabase.kt:23`, `exportSchema = false`).
- **Impacto:** al subir la versión del esquema sin migración, Room borra y recrea la base: se pierden usuarios, trueques, chats y coins sin aviso ni respaldo. No existe respaldo ni restauración en la app.
- **Clasificación:** debilidad observada.
- **Corrección:** migraciones Room explícitas por versión y exportación de esquema; añadir respaldo local (exportación) antes de cualquier migración destructiva.

#### D-10 — Tokens sin cifrar y registro de red con cuerpo completo

- **Evidencia:** `core/datastore/src/main/java/com/truequapp/core/datastore/UserSessionDataStore.kt:47-50` guarda `accessToken`/`refreshToken` en DataStore Preferences plano (sin cifrado). Hoy `saveTokens` no se llama desde el código principal (solo aparece comentado en `TokenAuthenticator.kt:50`), así que el riesgo es latente, no activo. `core/network/src/main/java/com/truequapp/core/network/di/NetworkModule.kt:35-36` fija `HttpLoggingInterceptor.Level.BODY` sin condición por tipo de build: si ese cliente llegara a usarse con credenciales o tokens, quedarían en logcat.
- **Clasificación:** debilidad observada.
- **Corrección:** DataStore cifrado o no persistir tokens en el dispositivo; limitar el registro de red a `BASIC` o a builds de depuración.

### MEDIA

#### D-11 — Preparación para Google Play: lo prometido no está en el código

- **Evidencia:** `README.md:1` ("Google Play Ready") y `README.md:33` ("Target SDK 35+"); el código usa `targetSdk = 34` (`app/build.gradle.kts`). Google Play exige API 35 para apps nuevas desde el 31 de agosto de 2025: con 34 la publicación sería rechazada o advertida. Play Integrity solo existe en el catálogo de versiones (`gradle/libs.versions.toml:18,72`, versión 1.4.0): ningún módulo lo depende ni lo usa. R8: `isMinifyEnabled = true` en release, pero `app/proguard-rules.pro` contiene una sola regla, `-keep class com.truequapp.** { *; }`, que conserva todo el código de la app (la ofuscación queda anulada), y no hay `isShrinkResources = true` pese a que la Fase 5 promete ambos. No hay `google-services.json` en `app/src/main`.
- **Clasificación:** debilidad observada.
- **Corrección:** subir a `targetSdk = 35` (y `compileSdk`); implementar Play Integrity o retirar la promesa; quitar el `-keep` global y activar `isShrinkResources`; retirar "Google Play Ready" del README hasta que sea verdad.

#### D-12 — Zonas seguras: placeholder, no módulo

- **Evidencia:** `app/src/main/java/com/truequapp/navigation/TruequappNavHost.kt:309-315` muestra `PlaceholderSectionScreen` ("Módulo de Puntos Seguros con geofencing (Fase 3)"). No existe `feature:safezones`. `VerifySafeZonePresenceUseCase` no se usa en `app`/`feature`; `ObfuscateLocationUseCase` solo se provee en `DomainModule.kt` sin uso confirmado en la UI. El manifiesto declara ubicación y cámara (`AndroidManifest.xml`: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `CAMERA`), pero la Fase 3 promete geocercas, mapas, QR y disputas que no existen.
- **Clasificación:** debilidad observada.
- **Corrección:** implementar el módulo o retirar la ruta de la navegación y las promesas de la Fase 3 y del formulario de seguridad de datos.

#### D-13 — Sin borrado de cuenta ni exportación de datos

- **Evidencia:** búsqueda de `deleteUser`, `deleteAccount`, `eliminarCuenta` y `borrarCuenta` en el código principal sin resultados (solo existe borrado de publicaciones del catálogo). El formulario de seguridad de datos de la Fase 5 afirma que el usuario puede solicitar la eliminación de su cuenta y sus datos "en cualquier momento"; en la app no hay forma de hacerlo.
- **Clasificación:** debilidad observada.
- **Corrección:** flujo de borrado de cuenta (con confirmación y borrado en cascada real) y exportación de datos del usuario.

#### D-14 — Código duplicado muerto: carpeta `modulo 2/`

- **Evidencia:** `modulo 2/` contiene una copia del catálogo (con sus propios tests) no incluida en `settings.gradle.kts`. No compila como parte del proyecto y diverge del módulo real.
- **Clasificación:** debilidad observada (deuda técnica).
- **Corrección:** eliminar la carpeta o integrarla como módulo real; no mantener dos copias.

#### D-15 — Registro con condición de carrera y excepción sin manejar

- **Evidencia:** la unicidad del correo se verifica con `countByEmail` (`UserDao.kt`) antes de insertar, pero entre el conteo y la inserción otra operación puede registrar el mismo correo; el `INSERT` usa `OnConflictStrategy.ABORT` (`UserDao.kt`), que lanza excepción, y `AuthRepository.register` no tiene `try/catch` alrededor de `insertUser`, por lo que la excepción se propagaría a la corrutina.
- **Clasificación:** debilidad observada.
- **Corrección:** capturar la excepción de conflicto y devolver `EmailAlreadyExists`; idealmente, la verificación y la inserción deben ser atómicas.

## 5. Lo que no se probó y por qué

| No probado | Motivo |
|---|---|
| Compilación (`./gradlew assembleDebug`) | No hay JDK instalado en esta máquina (`java: command not found`) ni Android SDK (`ANDROID_HOME` vacío, sin SDK visible). No se puede compilar ni empaquetar. |
| Tests unitarios (33 archivos) | Mismo motivo: sin JDK/SDK no se ejecuta `./gradlew testDebugUnitTest`. Existen tests de dominio (`EscrowHoldAndReleaseUseCaseTest`, `TriangularCycleRulesUseCaseTest`, `LedgerDoubleEntryValidatorUseCaseTest`, `GooglePlayPolicyComplianceTest`, entre otros) y de repositorios/ViewModels (`TradeRepositoryTest`, `ChatListViewModelTest`, `UserRepositoryTest`), pero su resultado es desconocido. |
| Humo y flujos en dispositivo | Sin emulador ni dispositivo físico. Ningún flujo fue ejecutado; todo lo reportado es lectura de código. |
| Carga, estrés, pico y soak | No hay backend ni multiusuario real que someter a carga; la app es local por diseño actual. |
| Seguridad dinámica (fuerza bruta real, inyección, XSS) | Sin ejecución; la revisión es estática. La ausencia de límite de intentos y la contraseña mínima de 4 caracteres se verificaron en código, no por ataque. |
| Accesibilidad y usabilidad | Sin ejecución en dispositivo. |
| Revisión legal (privacidad, menores, Play) | Fuera del alcance de esta auditoría; se recomienda la skill `auditoria_legal` antes de cualquier publicación. |
| Verificación del dominio `https://api.truequapp.com/v1/` | Solo existe como constante `BASE_URL` (`NetworkModule.kt:22`); no se verificó si el dominio existe o responde, y no hay endpoints Retrofit (`@GET`/`@POST`) en `core/network/src`. |

## 6. Veredicto y condiciones

**Veredicto: NO APTO** para Google Play ni para uso con usuarios reales o escolares.

**APTO CON CONDICIONES únicamente como prototipo local, sin usuarios reales, si se cumple esta lista cerrada (P0 primero):**

1. Eliminar el usuario administrador sembrado (`DatabaseModule.kt`) y su rastro del historial; rotar sus credenciales. (D-01)
2. Quitar la credencial demo de la Fase 5 del documento y del historial; rotarla. (D-03)
3. Hash de contraseñas (bcrypt/Argon2) con sal; subir el mínimo a 8 caracteres; límite de intentos. (D-02)
4. Aplicar el control de acceso RN-03 en el chat y eliminar los correos por defecto. (D-04)
5. Transacción única en `saveProposal` + movimiento de coins, y no anunciar éxito si la persistencia falló. (D-05)
6. Migraciones Room explícitas; quitar `fallbackToDestructiveMigration`. (D-09)
7. Subir a `targetSdk = 35` e implementar Play Integrity, o retirar "Google Play Ready" del README. (D-11)

Tras corregir, repetir como mínimo: T1 (registro/login con hash), T2 (seed eliminado: la base abre sin cuenta admin), T3 (chat con control de acceso), T4 (propuesta con fallo de persistencia simulado), T5 (migración de esquema con datos), T6 (compilación release + R8 real). La lista completa de repetición está en la sección 7.

## 7. Pruebas a repetir tras corregir

| ID | Prueba | Criterio de aprobado |
|---|---|---|
| T1 | Registro y login con contraseña hasheada | La contraseña nunca aparece en claro en la base local; login correcto acepta, incorrecto rechaza; mínimo 8 caracteres |
| T2 | Apertura de la base sin seed | Tras instalar en limpio y abrir la app varias veces, no existe ninguna cuenta administradora ni saldo de 100M |
| T3 | Chat con control de acceso | Un usuario fuera del trueque no puede leer ni enviar mensajes de esa conversación; sin sesión no se puede chatear |
| T4 | Propuesta con fallo de persistencia | Si la escritura en Room falla, el usuario ve un error, no "¡Propuesta enviada exitosamente!"; no quedan ofertas huérfanas |
| T5 | Migración de esquema | Subir la versión del esquema con datos existentes no borra usuarios, trueques ni chats |
| T6 | Compilación release | `./gradlew bundleRelease` compila; R8 ofusca (el `-keep` global eliminado); `targetSdk = 35` |
| T7 | Cierre de trueque | Un trueque llega a COMPLETADO con confirmación y liquidación de coins; el escrow se libera o se reembolsa |
| T8 | Borrado de cuenta | El usuario puede borrar su cuenta y sus datos desaparecen de la base local |
| T9 | Regresión | La suite de 33 tests existentes pasa en verde (`./gradlew testDebugUnitTest`) |
| T10 | Humo en dispositivo | Instalación, registro, login, catálogo, publicar, proponer, aceptar, chat y perfil funcionan de punta a punta |

## 8. Notas de método

- Todos los hallazgos son **debilidades observadas** (código leído, no ejecutado), conforme a la skill: sin JDK, sin SDK y sin dispositivo no se ejecutó nada del repo.
- No se reprodujo ningún secreto, contraseña, token ni número de teléfono personal en este informe: las credenciales y los datos personales se citan solo por archivo, línea y tipo de dato.
- La auditoría es de solo lectura: no se modificó el repositorio ni se instaló nada en producción.

