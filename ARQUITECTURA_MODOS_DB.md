# Arquitectura y Guía de Modos de Validación de Base de Datos: `db-Local` vs `db-Online`

Este documento describe la especificación técnica, el funcionamiento operativo, los casos de uso con y sin conexión a internet, las frecuencias de actualización, la subida de archivos, el ciclo de vida de propuestas de trueque con cola de validación (**"Enviando"** → **"Pendiente"**) y la serie de pasos para integrar la conmutación entre **`db-Local`** y **`db-Online`** en **Truequapp**.

---

## 1. Nomenclatura Oficial

A partir de esta implementación, la aplicación adopta oficialmente los siguientes dos nombres para sus motores de base de datos y validación:

| Nombre Oficial | Tipo de Base de Datos | Ubicación | Descripción |
| :--- | :--- | :--- | :--- |
| **`db-Local`** | SQLite / Room + Preferences DataStore | En el almacenamiento interno del dispositivo | Gestiona y valida la información localmente en el teléfono. Máxima velocidad e inmediatez. Permite operar 100% sin internet. |
| **`db-Online`** | libSQL (Turso Cloud) vía API REST Vercel | En la nube (servidores distribuidos de Turso) | Gestiona y valida la información contra la base de datos central en la nube. Coherencia multi-dispositivo y sincronía global en tiempo real. |

---

## 2. Diagrama General de Arquitectura y Conmutación

```mermaid
flowchart TD
    subgraph UI["Apartado de Ajustes (Pantalla del Usuario)"]
        User["Usuario en Ajustes"] -->|Selecciona modo| Selector["Selector de Modo de Validación"]
        Selector -->|Activa db-Online| FuncOnline["enableDbOnlineValidation()"]
        Selector -->|Activa db-Local| FuncLocal["enableDbLocalValidation()"]
        FuncOnline --> Popup["Pop-up Informativo (AlertDialog)"]
        FuncLocal --> Popup
    end

    subgraph CoreEngine["Motor de Decisión (DatabaseValidationModeManager)"]
        FuncOnline --> Manager["ValidationModeManager"]
        FuncLocal --> Manager
        Manager -->|Persiste selección| Prefs[("DataStore: validation_mode")]
        Manager -->|Emite StateFlow en memoria| ModeFlow["validationMode: StateFlow"]
    end

    subgraph AppFlows["Operaciones que requieren Validación"]
        V1["Iniciar Sesión (Login)"]
        V2["Registrar Nuevo Usuario"]
        V3["Modificar Credenciales en Ajustes"]
        V4["Validar y Crear Propuesta de Trueque"]
    end

    AppFlows --> Router{"¿Modo Activo en ModeFlow?"}
    ModeFlow -.-> Router

    subgraph OnlineBranch["db-Online (Turso Cloud)"]
        Router -->|db-Online| CheckNet{"¿Hay Conexión?"}
        CheckNet -->|Sí| TursoReq["Llamada HTTP a /api/*"]
        TursoReq --> TursoDB[("Turso Cloud libSQL")]
        TursoDB --> SyncDown["Sincroniza y valida con la nube en tiempo real"]
        CheckNet -->|No| ErrNet["Error controlado: 'Sin conexión a db-Online' (Sugerir db-Local)"]
    end

    subgraph LocalBranch["db-Local (Dispositivo)"]
        Router -->|db-Local| RoomReq["Consulta directa a UserDao / TruequeDao"]
        RoomReq --> SQLiteDB[("SQLite Local (Room)")]
        SQLiteDB --> LocalVal["Validación instantánea en el dispositivo (< 5ms)"]
    end
```

---

## 3. Comportamiento Detallado: `db-Local`

Cuando el usuario activa el modo **`db-Local`**, las validaciones de usuario, contraseñas y datos del perfil se resuelven contra la base de datos interna SQLite/Room alojada en el teléfono.

```mermaid
flowchart LR
    subgraph Request["Acción del Usuario"]
        Action["Validar Usuario / Contraseña / Perfil"]
    end

    subgraph Execution["Ejecución en db-Local"]
        Action --> Query["Consulta SQL a Room Local"]
        Query --> DBLocal[("SQLite en Teléfono")]
        DBLocal --> FastResp["Respuesta inmediata (< 5 ms)"]
    end

    subgraph InternetCondition["Estado de Internet"]
        FastResp --> HasInternet{"¿Hay Internet?"}
        HasInternet -->|Con Conexión| SyncBg["Background Sync (cada 15s) actualiza catálogo sin bloquear"]
        HasInternet -->|Sin Conexión| OfflineOk["Operación exitosa en modo 100% offline"]
    end
```

### 3.1. ¿Cómo funciona `db-Local` CON conexión a internet?
- **Validación instantánea:** Al iniciar sesión, registrarse o modificar datos de la cuenta, la comprobación de credenciales, balance de monedas y existencia se realiza directamente contra la base de datos interna SQLite (`db-Local`). La respuesta toma milisegundos (< 5 ms) y no sufre por lentitud o latencia de red.
- **Sincronización en segundo plano no bloqueante:** Como hay conexión a internet, el motor de sincronización (`DatabaseSyncManager`) continúa ejecutándose en segundo plano cada 15 segundos para descargar publicaciones nuevas o cambios del catálogo general de Turso hacia la caché local, pero **ninguna validación se bloquea** esperando respuesta del servidor.
- **Ventaja clave:** Fluidez absoluta de la interfaz sin consumir datos móviles para cada validación.

### 3.2. ¿Cómo funciona `db-Local` SIN conexión a internet (Modo 100% Offline)?
- **Operatividad total garantizada:** La aplicación sigue funcionando con normalidad. Todas las consultas (`UserDao`, `TruequeDao`, `UserPublicationStorage`) se ejecutan contra los archivos locales del teléfono.
- **Cero errores de red en pantalla:** No se producen excepciones de tipo `UnknownHostException` ni pantallas de espera infinitas para validar contraseñas o datos del perfil.
- **Manejo del ciclo de sincronización:** `DatabaseSyncManager` detecta de forma limpia la falta de conexión y, en lugar de alertar al usuario con diálogos molestos, mantiene el estado en reposo (`SyncState.ConnectionError`) de forma silenciosa o con un banner discreto en la parte inferior, permitiendo al usuario continuar su navegación sin interrupciones.
- **Ventaja clave:** La app nunca se queda inaccesible si el usuario está en el metro, en carretera o en zonas sin señal.

---

## 4. Comportamiento Detallado: `db-Online`

Cuando el usuario activa el modo **`db-Online`**, las validaciones se dirigen a los servidores centrales de Turso Cloud a través de la API en Vercel (`/api/*`).

### 4.1. ¿Cómo funciona `db-Online` CON conexión a internet?
- **Validación global en tiempo real:** Cuando el usuario valida un correo, contraseña o propuesta, la consulta va directamente a la nube. Si otro usuario en otro dispositivo acaba de registrar un correo idéntico hace un segundo o modificó un trueque, `db-Online` lo detecta inmediatamente evitando colisiones.
- **Replicación local inmediata:** Una vez que Turso aprueba la validación o modificación (código HTTP 200), los datos actualizados se replican de inmediato en `db-Local` y en el `UserSessionDataStore`, manteniendo ambas bases en sincronía absoluta.
- **Ventaja clave:** Integridad multi-usuario y multi-dispositivo sin inconsistencias.

### 4.2. ¿Cómo funciona `db-Online` SIN conexión a internet?
- **Detección segura del fallo:** Si el dispositivo no tiene acceso a internet o Turso no responde, la aplicación captura la excepción de conexión (`SocketTimeoutException`, `UnknownHostException`).
- **Resiliencia y continuidad operativa:** La app no se congela ni crashea. Muestra un mensaje claro:
  > *"Error de conexión con la base de datos en línea (db-Online). Puedes conmutar a db-Local en el apartado de Ajustes para operar sin internet."*
- **Acceso a Ajustes garantizado:** La pantalla de **Ajustes** permanece siempre accesible para que el usuario pueda cambiar a `db-Local` con un solo toque y continuar trabajando de inmediato.

---

## 5. Ciclo de Validaciones y Frecuencias

Es fundamental distinguir entre los dos tipos de ciclos que operan en la aplicación:

```mermaid
flowchart LR
    subgraph Ciclo1["1. Ciclo Transaccional (Bajo Demanda)"]
        ActionUser["Acción del Usuario (Click en botón)"] --> ValidacionInmediata["Validación Inmediata (0 segundos)"]
        ValidacionInmediata --> RespInmediata["Respuesta en Pantalla (Éxito / Error)"]
    end

    subgraph Ciclo2["2. Ciclo Periódico de Reconciliación"]
        Timer["Temporizador de Fondo (delay 15s)"] --> SyncJob["DatabaseSyncManager.syncDatabase()"]
        SyncJob --> FetchNube["Descarga novedades de Turso"]
        FetchNube --> RevalidaCola["Revalida propuestas en espera (Outbox)"]
    end
```

### 5.1. Ciclo Transaccional (Bajo Demanda / En el momento de la acción)
- **Frecuencia:** **Inmediata (0 segundos de espera)**.
- **Cuándo ocurre:** Cada vez que el usuario realiza una acción interactiva:
  - Al pulsar *"Iniciar Sesión"* → Se valida correo y contraseña en ese instante.
  - Al pulsar *"Registrarse"* → Se valida unicidad del correo en ese instante.
  - Al pulsar *"Guardar Cambios"* en Ajustes → Se valida el nuevo nombre, contraseña y correo en ese instante.
  - Al pulsar *"Enviar Propuesta de Trueque"* → Se ejecuta la validación de inventario y saldo de monedas en ese instante.
- **Tiempo de respuesta:**
  - En `db-Local`: **1 a 5 milisegundos**.
  - En `db-Online`: **150 a 600 milisegundos** (dependiendo de la latencia de red).

### 5.2. Ciclo Periódico de Reconciliación y Sincronización
- **Frecuencia:** **Cada 15 segundos exactos** (`15_000L ms`).
- **Dónde corre:** En una corrutina de fondo (`CoroutineScope`) gestionada por `DatabaseSyncManager`.
- **Qué hace cada 15 segundos:**
  1. Consulta a Turso (`GET /api/catalogo`) para actualizar el feed global de artículos.
  2. Consulta a Turso (`GET /api/trueques`) para actualizar las ofertas recibidas y enviadas.
  3. Consulta a Turso (`GET /api/usuarios/login` o balance) para mantener al día los TruequeCoins y calificaciones.
  4. **Procesa la cola de trueques en espera:** Revalida cualquier propuesta que se haya creado en modo local (estado *"Enviando"*) contra el servidor central para pasarla a *"Pendiente"*.

---

## 6. ¿Cómo se suben los archivos a `db-Online`?

La subida de información a `db-Online` (Turso Cloud) maneja dos tipos de datos: **datos relacionales/estructurados** y **archivos multimedia (imágenes WebP)**.

```mermaid
flowchart TD
    subgraph Client["Cliente Android (Dispositivo)"]
        ImgRaw["Imagen de la Galería o Cámara (JPG / PNG)"] --> Compressor["WebPImageCompressor"]
        Compressor --> WebP["Imagen WebP Optimizada (< 300 KB)"]
        WebP --> Serializer["Serialización JSON (Base64 / URL Payload)"]
        
        DataFields["Campos de Texto (Título, Descripción, Precios, Categoría)"] --> Serializer
        Serializer --> HTTPClient["HTTP POST /api/catalogo"]
    end

    subgraph Server["Backend Vercel (API Serverless)"]
        HTTPClient --> APIEndpoint["api/catalogo (server.py / index.py)"]
        APIEndpoint --> TursoDriver["turso_client.execute()"]
    end

    subgraph Storage["Base de Datos Turso (db-Online)"]
        TursoDriver --> TursoTable[("Tabla 'catalogo' en Turso libSQL")]
    end
```

### 6.1. Subida de Datos Estructurados (Textos, números, estados, fechas)
- La aplicación construye un objeto `JSONObject` con los metadatos correspondientes (título, descripción, condición, categoría, valor estimado, etc.).
- Se realiza una petición HTTP `POST` o `PUT` con encabezado `Content-Type: application/json; charset=utf-8` hacia los endpoints del backend en Vercel (`/api/catalogo`, `/api/usuarios`, `/api/trueques`).
- El backend en Python / libSQL SDK ejecuta consultas SQL parametrizadas (`INSERT INTO ... VALUES (?, ?, ...)`), protegiendo la base de datos contra inyecciones SQL y garantizando transacciones atómicas.

### 6.2. Subida de Archivos Multimedia e Imágenes
1. **Compresión previa en el dispositivo (`WebPImageCompressor`):**
   - Para no saturar el ancho de banda ni la base de datos, toda fotografía capturada por la cámara o seleccionada de la galería se convierte a formato **WebP**.
   - Se reduce la resolución máxima y el peso del archivo a menos de 300 KB por imagen, manteniendo excelente fidelidad visual.
2. **Almacenamiento y persistencia en Turso:**
   - Las imágenes optimizadas en WebP se transmiten dentro del payload JSON en el campo `images` de la publicación.
   - En Turso (`db-Online`), la tabla `catalogo` almacena el array de imágenes (`images TEXT`) asociado al identificador único de la publicación (`id`).
   - El catálogo descarga estas imágenes de forma perezosa (*lazy loading*) y las guarda en la caché local del dispositivo para evitar descargas repetidas.

---

## 7. Protocolo de Trueques en `db-Local`: Cola de Validación ("Enviando" → "Pendiente")

Para evitar que un usuario en modo `db-Local` oferte por una publicación que **ya no se encuentra disponible o activa** en el servidor central (porque otro usuario la intercambió previamente en la red), se implementa el patrón de **Cola de Salida (Outbox Pattern)** con estados controlados:

```mermaid
stateDiagram-v2
    [*] --> ENVIANDO: Usuario crea propuesta en db-Local
    
    state ENVIANDO {
        [*] --> GuardadoLocal: Se guarda en SQLite (esta_activa = 0)
        GuardadoLocal --> BloqueoItems: Bloquea ítems propios localmente (RN-01)
        BloqueoItems --> BadgeUI: Muestra etiqueta 'Enviando... (En espera de validación)'
        BadgeUI --> EsperaSync: Espera validación con db-Online
    }

    ENVIANDO --> ValidandoConOnline: Ciclo de Sincronización (cada 15s o conexión activa)
    
    state ValidandoConOnline {
        [*] --> CheckDisponibilidad: GET /api/catalogo (¿targetItem activo?)
        CheckDisponibilidad --> CheckReceptor: GET /api/usuarios (¿Receptor activo?)
    }

    ValidandoConOnline --> PENDIENTE: Validación con db-Online EXITOSA
    ValidandoConOnline --> RECHAZADA_POR_CONFLICTO: Publicación objetivo ya inactiva o borrada

    state PENDIENTE {
        [*] --> RegistradaTurso: Insertada formalmente en Turso (esta_activa = 1)
        RegistradaTurso --> VisibleReceptor: Visible para el Usuario Receptor
        VisibleReceptor --> BadgePendiente: Muestra etiqueta 'Pendiente' en UI
    }

    state RECHAZADA_POR_CONFLICTO {
        [*] --> DesbloqueoItems: Libera inventario propio local
        DesbloqueoItems --> NotificarUsuario: Muestra alerta 'Publicación ya no disponible en el servidor'
    }
```

### 7.1. Detalle de los Estados y Transiciones

#### 1. Estado Inicial: `"ENVIANDO"` (En espera de validación)
- Al presionar *"Enviar Propuesta"* estando en modo `db-Local`:
  - La propuesta se almacena en la tabla `trueques` de Room con `estado = "ENVIANDO"` y bandera temporal `esta_activa = 0`.
  - **Bloqueo de inventario (RN-01):** Los artículos ofertados por el usuario quedan bloqueados localmente para evitar que intente ofertarlos en otro trueque simultáneo.
  - **Experiencia de Usuario en la App:**
    - En la pestaña de *Mis Trueques Enviados*, la propuesta aparece con una tarjeta distintiva y un badge ámbar:
      > ⏳ **"Enviando... (En espera de validación con el servidor)"**
    - **No se envía todavía al usuario receptor:** La propuesta aún no es visible para el otro usuario, evitando notificarlo sobre ofertas no confirmadas.

#### 2. Proceso de Validación con `db-Online`
- En el siguiente ciclo de sincronización (cada 15 segundos o cuando se detecte conexión activa a internet):
  - El despachador consulta a `db-Online` mediante la API de Vercel:
    1. **¿El artículo objetivo (`targetItemId`) sigue teniendo `isAvailable = 1` y no ha sido eliminado en Turso?**
    2. **¿El usuario receptor sigue existiendo y activo?**

#### 3. Transición a `"PENDIENTE"` (Validación Exitosa)
- Si la publicación objetivo sigue disponible y activa en `db-Online`:
  - Se registra la propuesta en Turso (`POST /api/trueques`).
  - En la base de datos local `db-Local`, el estado se actualiza automáticamente a:
    - `estado = "PENDIENTE"` (o `PROPUESTA_ENVIADA`).
    - `esta_activa = 1`.
  - **En la interfaz del usuario emisor:** La tarjeta cambia al badge azul/verde:
    > 📬 **"Pendiente (Esperando respuesta del usuario receptor)"**
  - **En la interfaz del usuario receptor:** La propuesta aparece inmediatamente en su bandeja de *Ofertas Recibidas* para que pueda Aceptar o Rechazar.

#### 4. Transición a `"RECHAZADA_POR_CONFLICTO"` (Validación Fallida / Publicación Inactiva)
- Si al validar con `db-Online` se descubre que el artículo objetivo ya no está disponible (fue intercambiado por otra persona o eliminado por su dueño):
  - La propuesta **no se envía** a Turso.
  - En `db-Local`, la propuesta cambia a `estado = "CONFLICTO_NO_DISPONIBLE"` o se cancela.
  - **Liberación inmediata:** Los artículos ofertados por el usuario emisor se desbloquean al instante para que pueda usarlos en otros trueques.
  - **Notificación en la app:** Se muestra un aviso al usuario:
    > ⚠️ *"Tu propuesta no pudo enviarse porque la publicación seleccionada ya no se encuentra activa en el servidor."*

---

## 8. Serie de Pasos para la Integración

A continuación se detalla la serie de pasos requeridos para implementar esta funcionalidad en el código fuente:

```mermaid
flowchart TD
    P1["Paso 1: Enum DatabaseValidationMode (DB_LOCAL / DB_ONLINE)"] --> P2["Paso 2: DatabaseValidationModeManager con DataStore"]
    P2 --> P3["Paso 3: Implementar enableDbOnlineValidation() y enableDbLocalValidation()"]
    P3 --> P4["Paso 4: Integrar Selector en SettingsScreen y SettingsViewModel"]
    P4 --> P5["Paso 5: Diseñar el Pop-up Informativo (AlertDialog) con diferencias"]
    P5 --> P6["Paso 6: Enrutar validaciones en AuthRepository"]
    P6 --> P7["Paso 7: Implementar Cola Outbox en TradeViewModel (Enviando -> Pendiente)"]
    P7 --> P8["Paso 8: Ajustar DatabaseSyncManager (cada 15s con revalidación de cola)"]
    P8 --> P9["Paso 9: Pruebas Unitarias al 100% y Commit en Git"]
```

### Paso 1: Definir el Enumerador de Modos
Crear el modelo en `core/datastore/model/DatabaseValidationMode.kt`:
```kotlin
enum class DatabaseValidationMode {
    DB_LOCAL,   // Modo db-Local: Validaciones en el dispositivo
    DB_ONLINE   // Modo db-Online: Validaciones en Turso Cloud
}
```

### Paso 2: Crear el Gestor `DatabaseValidationModeManager`
Crear un componente `@Singleton` inyectable por Hilt con persistencia en `DataStore<Preferences>`:
```kotlin
interface DatabaseValidationModeManager {
    val validationMode: StateFlow<DatabaseValidationMode>
    fun getValidationMode(): DatabaseValidationMode
    suspend fun enableDbOnlineValidation()
    suspend fun enableDbLocalValidation()
    suspend fun setValidationMode(mode: DatabaseValidationMode)
}
```

### Paso 3: Implementar las Funciones de Conmutación
- **`enableDbOnlineValidation()`:**
  - Guarda la preferencia `DB_ONLINE` en `DataStore`.
  - Emite el nuevo valor al `StateFlow`.
  - Configura los endpoints de red activos (`cloudBaseUrl`).
- **`enableDbLocalValidation()`:**
  - Guarda la preferencia `DB_LOCAL` en `DataStore`.
  - Emite el nuevo valor al `StateFlow`.
  - Ordena a los repositorios consultar únicamente los DAOs locales de Room.

### Paso 4: Añadir la Sección en la Pantalla de "Ajustes" (`SettingsScreen.kt`)
- Incorporar una tarjeta interactiva con dos opciones visuales claras:
  - 🌐 **db-Online (Turso Cloud)**
  - 📱 **db-Local (Dispositivo)**
- Marcar visualmente con un check cuál es la opción actualmente seleccionada.

### Paso 5: Implementar el Pop-Up Informativo (`AlertDialog`)
Al presionar o cambiar a cualquiera de las dos opciones, desplegar un cuadro de diálogo modal que contenga:
1. **Título:** *"Modo db-Online Activado"* o *"Modo db-Local Activado"*.
2. **Explicación del modo activo:** Detalle de cómo valida la base de datos seleccionada.
3. **Diferencia con el otro modo:** Comparativa clara sobre conectividad, velocidad, consumo de datos y alcance.
4. **Botón:** *"Entendido"*.

### Paso 6: Conectar las Validaciones en `AuthRepository`
- En [`AuthRepository.kt`](file:///c:/Users/angel/truequapp-android-beta/truequapp-android-beta/app/src/main/java/com/truequapp/auth/data/AuthRepository.kt):
  - En `login()`:
    - Si `mode == DB_ONLINE`: Valida con `POST /api/usuarios/login` en Turso.
    - Si `mode == DB_LOCAL`: Valida con `userDao.getUserByEmail()` y `contrasena` en Room local.
  - En `register()`:
    - Si `mode == DB_ONLINE`: Valida unicidad en Turso y replica en local.
    - Si `mode == DB_LOCAL`: Valida unicidad con `userDao.countByEmail()` en Room local.
  - En `updateProfile()`:
    - Si `mode == DB_ONLINE`: Valida y actualiza en Turso y luego en Room.
    - Si `mode == DB_LOCAL`: Valida y actualiza exclusivamente en `userDao.updateUserCredentials()`.

### Paso 7: Implementar la Cola de Validación de Trueques en `TradeViewModel`
- En [`TradeViewModel.kt`](file:///c:/Users/angel/truequapp-android-beta/truequapp-android-beta/feature/trade/src/main/java/com/truequapp/feature/trade/mvi/TradeViewModel.kt):
  - Al enviar una propuesta en modo `db-Local`, guardarla con `estado = "ENVIANDO"`.
  - Bloquear localmente los ítems ofertados.
  - Desplegar en la UI el estado *"Enviando... (En espera de validación)"*.
  - En el ciclo de reconciliación, comprobar la disponibilidad del artículo en Turso antes de cambiar a *"Pendiente"* o alertar de conflicto si ya no está disponible.

### Paso 8: Ajustar `DatabaseSyncManager`
- Mantener la ejecución periódica cada 15 segundos (`delay(15_000L)`).
- Añadir la rutina `reconcilePendingTradeProposals()` para validar propuestas en estado *"ENVIANDO"* con Turso y efectuar la transición a *"PENDIENTE"*.

### Paso 9: Pruebas Unitarias y Verificación
- Crear pruebas unitarias en `SettingsViewModelTest.kt`, `TradeViewModelTest.kt` y `DatabaseValidationModeManagerTest.kt`:
  - Prueba de activación de `enableDbOnlineValidation()` y persistencia en DataStore.
  - Prueba de activación de `enableDbLocalValidation()`.
  - Prueba del pop-up informativo.
  - Prueba de transición de propuesta de trueque: `"ENVIANDO"` → `"PENDIENTE"` al validar exitosamente con la nube.
  - Prueba de rechazo y liberación de ítems si el artículo en Turso ya no está activo.
- Ejecutar `./gradlew :app:testDebugUnitTest` verificando 100% de éxito.

---

## 9. Matriz Comparativa Resumida

| Criterio | `db-Local` | `db-Online` |
| :--- | :--- | :--- |
| **Origen de Validación** | SQLite / Room en el teléfono | libSQL en Turso Cloud |
| **¿Funciona sin conexión a internet?** | **Sí, 100% funcional** | No (alerta que requiere conexión) |
| **Comportamiento si hay internet** | Valida localmente de forma instantánea y sincroniza en segundo plano sin bloquear. | Valida directamente contra la nube y replica el resultado en local. |
| **Velocidad de Respuesta** | Ultrarrápida (< 5 ms) | Sujeta a latencia de red (150 - 600 ms) |
| **Consumo de Datos Móviles** | Cero consumo para validaciones | Consume datos en cada validación |
| **Alcance de los Datos** | Confinado a la memoria del dispositivo actual | Global, accesible y visible por todos los usuarios |
| **Ciclo de Validación Transaccional** | Instantáneo (0s) en SQLite Room | Instantáneo (0s) en Turso + réplica local |
| **Ciclo de Reconciliación Periódica** | Cada 15 segundos (secundario si hay red) | Cada 15 segundos (primario con la nube) |
| **Manejo de Trueques Creados** | Estado inicial **"Enviando"** (en cola) hasta verificar en Turso que la publicación sigue activa → cambia a **"Pendiente"** | Pasa directo a **"Pendiente"** en tiempo real tras validar en Turso |
| **Subida de Imágenes** | Guarda WebP en almacenamiento local | Comprime a WebP (< 300 KB) y envía por API a Turso |
