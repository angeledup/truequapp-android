# 📱 Truequapp — Aplicación Móvil Android (Google Play Ready)
> **Sistema Comunitario de Trueques Inteligentes con Algoritmo Triangular y Monedero Escrow**  
> *Basado en la especificación formal de Ingeniería de Software (SWEBOK v4.0, ISO/IEC/IEEE 12207 y Arquitectura 4+1 de Kruchten — Universidad Veracruzana)*

---

## 🧭 Visión General del Proyecto
**Truequapp** es una plataforma móvil nativa para Android diseñada para resolver el problema económico fundamental de la **"Doble Coincidencia de Necesidades"** en el comercio colaborativo local. Combina tres modalidades operativas:
1. **Trueque Directo (1:1 / N:1 / Híbrido con compensación)**.
2. **Monedero Electrónico Social (TruequeCoins)** respaldado por contabilidad de partida doble y custodia transaccional (*Escrow*).
3. **Trueque Triangular Algorítmico ($A \rightarrow B \rightarrow C \rightarrow A$)** con confirmación tripartita y balanceo automatizado.
4. **Puntos de Encuentro Seguros (*Safe Exchange Zones*)** con geolocalización PostGIS, validación presencial por código QR dinámico y detección de fraude.

---

## 🛠️ Stack Tecnológico Oficial (Alineado a Estándares Google Play 2026)

| Capa / Subsistema | Tecnología Seleccionada | Justificación y Cumplimiento Google Play |
| :--- | :--- | :--- |
| **Lenguaje** | Kotlin 2.0+ (K2 Compiler) | Lenguaje preferido oficial de Google; seguridad de tipos y nulos. |
| **Framework UI** | Jetpack Compose + Material Design 3 | Renderizado declarativo moderno, soporte nativo de tema dinámico y accesibilidad. |
| **Arquitectura** | Clean Architecture + MVI / MVVM | Separación estricta de capas (UI, Domain, Data) según *Modern Android Development (MAD)*. |
| **Inyección de Dependencias** | Hilt / Dagger | Estándar recomendado por Google para Android. |
| **Concurrencia y Reactividad** | Coroutines + Kotlin Flows (`StateFlow`, `SharedFlow`) | Manejo asíncrono y reactivo sin bloqueos en el hilo principal (*UI Thread*). |
| **Persistencia Local** | Jetpack Room (SQLCipher) + DataStore Preferences | Cifrado de base de datos local y almacenamiento reactivo de preferencias y tokens. |
| **Red y API REST** | Retrofit 2 + OkHttp 4 + Kotlinx Serialization | Clientes HTTP con *interceptors* de autenticación JWT y reintentos idempotentes. |
| **Tiempo Real (Chat)** | Socket.io Client / Ktor WebSockets | Latencia menor a 150 ms para negociación y mensajería en vivo. |
| **Imágenes y Multimedia** | Coil + Compresor WebP nativo | Optimización de imágenes en cliente a formato WebP (< 80 KB) antes de subir. |
| **Geolocalización y Mapas** | Google Play Services Location + Google Maps Compose | Proveedor FusedLocation, geocercas (*Geofencing*) y detección de *Mock Locations*. |
| **Cámara y QR** | CameraX + Google ML Kit Barcode Scanning | Escaneo en dispositivo ultrarrápido sin dependencias pesadas. |
| **Biometría y Seguridad** | BiometricPrompt API + Android Keystore + Play Integrity | Autorización de transferencias con huella/PIN y atestación de integridad del APK. |
| **Notificaciones** | Firebase Cloud Messaging (FCM) + Notif Channels | Canales tipificados requeridos por Android 13+ (POST_NOTIFICATIONS). |
| **Empaquetado** | Android App Bundle (.aab) + R8 / ProGuard + Target SDK 35+ | Requisito indispensable para publicación en Google Play Console. |

---

## 👥 Organización de Fases de Trabajo en Antigravity IDE

Cada fase está diseñada como un **módulo de trabajo independiente**, con su propio archivo `.md` detallando requerimientos funcionales, clases, pantallas, diagramas UML aplicables, pruebas y criterios de aceptación:

```
truequapp-android/
├── README.md                                          <-- Este documento maestro
└── fases/
    ├── FASE_0_MODELADO_Y_POLITICAS.md                 <-- Usuario / Agente 1: Reglas de Negocio y Google Play Policies
    ├── FASE_1_ARQUITECTURA_Y_DESIGN_SYSTEM.md         <-- Usuario / Agente 2: Setup Base, MVI, Material 3 y Red
    ├── FASE_2_MVP_CORE_TRUEQUES.md                    <-- Usuario / Agente 3: Auth, Catálogo WebP, Trueques 1:1 y Chat
    ├── FASE_3_SEGURIDAD_GEO_SAFEZONES.md              <-- Usuario / Agente 4: Google Maps, Safe Zones, QR y Disputas
    ├── FASE_4_GRAFOS_TRUEQUECOINS_SWIPE.md            <-- Usuario / Agente 5: Modo Swipe, TruequeCoins Escrow y Grafos
    └── FASE_5_QA_DEPLOY_GOOGLE_PLAY.md                <-- Usuario / Agente 6: Tests E2E, Play Integrity, AAB y Lanzamiento
```

---

## 🌿 Flujo de Trabajo en GitHub (GitFlow Colaborativo)

Para garantizar integración continua y evitar colisiones entre los usuarios de Antigravity IDE:

1. **Ramas Principales:**
   - `main`: Código estable listo para versión Release en Google Play (etiquetado con `v1.0.0-rc`).
   - `develop`: Rama de integración donde convergen las fases terminadas.
2. **Ramas de Fase / Función:**
   - Cada usuario creará su rama a partir de `develop`:
     - `feature/fase-0-business-rules`
     - `feature/fase-1-architecture-m3`
     - `feature/fase-2-mvp-core`
     - `feature/fase-3-geo-safezones-qr`
     - `feature/fase-4-swipe-wallet-escrow`
     - `feature/fase-5-qa-playstore-ready`
3. **Estrategia de Pull Request (PR):**
   - Cada PR debe referenciar los Casos de Uso (ej: `Fixes CU001, CU005, CU009`).
   - Requiere revisión de pares (*Code Review*) y validación de `lintKotlin` y pruebas unitarias automáticas en GitHub Actions.
   - Mezcla mediante **Squash and Merge** hacia `develop`.

---

## 📜 Cumplimiento de Políticas Críticas de Google Play

1. **Monedas y Créditos Virtuales (TruequeCoins):** Los TruequeCoins son clasificados como *Closed-Loop Virtual Utility Credits* sin posibilidad de retiro en efectivo o canje fíat externo, cumpliendo la política de *Financial Services & Cryptocurrencies* de Google Play.
2. **Permisos Sensibles de Ubicación:** Se utiliza exclusivamente ubicación en primer plano (`ACCESS_COARSE_LOCATION` y `ACCESS_FINE_LOCATION`) mientras el usuario busca en el catálogo o valida su presencia en una Safe Zone. **No se solicita ubicación en segundo plano** para asegurar aprobación inmediata.
3. **Eliminación Obligatoria de Cuenta (CU002 / CU024):** El sistema incluye tanto eliminación directa desde la app como un portal web público de solicitud de borrado de datos, conforme a la directiva de seguridad de datos de Google Play.
4. **Protección de Datos Personales (RNF-08):** La dirección física de los usuarios **nunca** se expone de forma exacta en el catálogo público; el sistema siempre aplica un difuminado en un radio de 500 metros mediante centroides PostGIS.
