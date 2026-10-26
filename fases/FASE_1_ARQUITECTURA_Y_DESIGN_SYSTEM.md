# 🏗️ FASE 1: Arquitectura Base, Design System Material 3 y Capa de Infraestructura
> **Asignación en Antigravity IDE:** Usuario / Agente 2 (Core Android & UI/UX Lead)  
> **Duración estimada:** Semanas 3–4 del SDLC  
> **Rama Git:** `feature/fase-1-architecture-m3`  
> **Fuentes SWEBOK:** Diseño de Software (KA #2), Vista de Desarrollo y Vista Física de Kruchten

---

## 🎯 Objetivo de la Fase
Configurar la estructura modular del proyecto Android con soporte para Kotlin 2.0+, Jetpack Compose y Material Design 3. Implementar el patrón arquitectónico MVI (*Model-View-Intent*), el sistema de inyección de dependencias con Hilt, la navegación Compose de tipos seguros y la infraestructura de red HTTP / WebSockets.

---

## 📁 Estructura Modular del Proyecto Android

El proyecto adoptará una arquitectura modularizada por capas y características (*Modularization by Feature*), asegurando desacoplamiento, compilación incremental y escalabilidad:

```
truequapp-android/
├── gradle/
│   └── libs.versions.toml                <-- Version Catalog centralizado
├── app/                                  <-- Módulo Application (Hilt App, MainActivity)
├── core/
│   ├── model/                            <-- Modelos de dominio puros (de Fase 0)
│   ├── common/                           <-- Extensiones, Result wrappers, Dispatchers
│   ├── designsystem/                     <-- Tema M3 (Colors, Typography, Icons, Shapes)
│   ├── network/                          <-- Retrofit, OkHttp, AuthInterceptor, WebSocket Client
│   ├── database/                         <-- Room DB, DAOs, Migrations (Cifrado SQLCipher)
│   └── datastore/                        <-- Jetpack DataStore para Token JWT y UserSession
└── feature/                              <-- Módulos de pantalla / características (en Fases 2-5)
    ├── auth/
    ├── catalog/
    ├── trade/
    ├── safezones/
    └── wallet/
```

---

## 📦 Gradle Version Catalog (`gradle/libs.versions.toml`)

El desarrollador configurará las dependencias exactas y compatibles con Google Play 2026:

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.20"
composeBom = "2024.09.02"
hilt = "2.52"
ksp = "2.0.20-1.0.25"
room = "2.6.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
coil = "2.7.0"
navigationCompose = "2.8.2"
playServicesLocation = "21.3.0"
playIntegrity = "1.4.0"
mlkitBarcode = "17.3.0"
cameraX = "1.3.4"
socketIo = "2.1.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Inyección de Dependencias
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Red y Serialización
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
socket-io-client = { group = "io.socket", name = "socket.io-client", version.ref = "socketIo" }

# Google Play Services & Hardware
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
play-integrity = { group = "com.google.android.play", name = "integrity", version.ref = "playIntegrity" }
mlkit-barcode-scanning = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "mlkitBarcode" }
camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "cameraX" }
camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "cameraX" }
camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "cameraX" }
```

---

## 🎨 Design System Material 3 (`core:designsystem`)

Truequapp utiliza una paleta cromática sofisticada con tonos esmeralda y ámbar de alta confianza:

### 1. Paleta de Colores
- **Primary:** `Emerald 600` (`#059669`) — Transmite sostenibilidad, trueque ecológico y valor.
- **Secondary:** `Amber 500` (`#D97706`) — Monedero TruequeCoins y transacciones en garantía.
- **Tertiary:** `Indigo 500` (`#6366F1`) — Trueque Triangular y algoritmos de grafos.
- **Surface & Background (Dark Theme):** `Slate 900` (`#0F172A`) y `Slate 800` (`#1E293B`).
- **Surface & Background (Light Theme):** `Neutral 50` (`#F8FAFC`).

### 2. Componentes Reutilizables Requeridos
- `TruequappTopAppBar`: Barra superior con indicador de saldo de TruequeCoins y notificaciones.
- `TruequappBadge`: Indicador de insignia de verificación KYC (Nivel 1: Teléfono verificado, Nivel 2: ID Oficial).
- `SafeZoneChip`: Chip de estado para indicar si un punto está verificado y cuenta con vigilancia.
- `ConditionTag`: Etiqueta de estado del artículo (Nuevo, Como nuevo, Usado, Regular).

---

## 🏛️ Patrón Arquitectónico MVI Base (`core:common`)

Cada pantalla debe implementar una arquitectura de flujo unidireccional (*Unidirectional Data Flow - UDF*):

```kotlin
// Interfaz base para el estado de la UI
interface UiState

// Interfaz base para las acciones del usuario
interface UiEvent

// Interfaz base para efectos secundarios de un solo disparo (Snackbars, Navegación)
interface UiEffect

abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _uiEffect = Channel<F>(Channel.BUFFERED)
    val uiEffect: Flow<F> = _uiEffect.receiveAsFlow()

    abstract fun onEvent(event: E)

    protected fun setState(reducer: S.() -> S) {
        _uiState.update(reducer)
    }

    protected fun sendEffect(effect: F) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
```

---

## 🌐 Capa de Red e Interceptores (`core:network`)

1. **`AuthInterceptor`:** Inyecta automáticamente el token JWT Bearer en todas las peticiones protegidas.
2. **`TokenAuthenticator`:** Intercepta respuestas `401 Unauthorized` y utiliza el `RefreshToken` almacenado en DataStore para renovar la sesión sin interrumpir la experiencia de usuario.
3. **`SocketManager`:** Administra el túnel Socket.io para chat en vivo y eventos de notificación en tiempo real, garantizando latencias $\le 150\text{ ms}$.

---

## 📦 Entregables Verificables de la Fase 1
1. Proyecto base configurado y compilando con `gradlew assembleDebug`.
2. Módulo `core:designsystem` con temas Light/Dark y vista previa de componentes en Android Studio.
3. Navegación declarativa implementada en `app/src/main/java/.../navigation/TruequappNavHost.kt`.
4. Módulos de Red y DataStore probados con mocks unitarios.
