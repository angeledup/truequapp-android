# 📦 FASE 2: Desarrollo del MVP Core — Usuarios, Catálogo WebP, Trueques 1:1 y Chat
> **Asignación en Antigravity IDE:** Usuario / Agente 3 (Core Features Lead)  
> **Duración estimada:** Semanas 5–8 del SDLC  
> **Rama Git:** `feature/fase-2-mvp-core`  
> **Casos de Uso UV Cubiertos:** CU001, CU003, CU004, CU005, CU006, CU007, CU008, CU009, CU010, CU011, CU012, CU018, CU023, CU024  
> **Fuentes SWEBOK:** Construcción de Software (KA #3), Diagramas de Secuencia y Actividades de Kruchten

---

## 🎯 Objetivo de la Fase
Construir el núcleo operativo funcional de la aplicación Truequapp: sistema de autenticación y verificación KYC Nivel 1 por SMS, gestión del catálogo personal con compresión nativa WebP, buscador de artículos geolocalizado, máquina de estados para ofertas de trueque 1:1 y N:1, y chat privado en tiempo real condicionado a ofertas activas.

---

## 📋 Mapeo Detallado de Casos de Uso e Implementación Android

### 1. Módulo de Autenticación y Perfil (`feature:auth`, `feature:profile`)
* **CU001 (Registro y KYC 1):**
  - Formulario en Jetpack Compose con validación reactiva de campos: Nombre, Correo, Teléfono (formato E.164) y Contraseña.
  - Diálogo modal de captura de código SMS OTP de 6 dígitos con cuenta regresiva de 5 minutos.
  - Inicialización automática del monedero del usuario al validar el SMS.
* **CU023 & CU024 (Inicio y Cierre de Sesión):**
  - Autenticación con credenciales y soporte para Google One-Tap Sign-In.
  - Cierre de sesión seguro con revocación del token en DataStore y desconexión limpia de WebSockets.
* **CU003 & CU004 (Perfil, Reputación Bayesiana y Búsqueda de Usuarios):**
  - Edición de fotografía, biografía y colonia base de referencia (radio difuso de 500 metros, RNF-08 / RN-10).
  - Visualización del perfil público con desglose de reputación bayesiana (estrellas ponderadas según RN-09), tasa de cumplimiento y catálogo activo.

### 2. Módulo de Inventario y Publicación (`feature:catalog`)
* **CU005 (Registrar Artículo con Optimización WebP):**
  - Integración con el nuevo `ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)` (PhotoPicker nativo sin permisos intrusivos de almacenamiento).
  - **Pipeline de Compresión Nativo:** Conversión asíncrona de imágenes en Coroutines a formato WebP lossy con calidad dinámica hasta asegurar tamaño $< 80\text{ KB}$ por imagen y eliminación obligatoria de metadatos EXIF (latitud/longitud de la cámara).
  - Selector de categorías jerárquicas, estado de conservación (`NUEVO`, `COMO_NUEVO`, `USADO`, `REGULAR`), rango estimado de valor y etiquetas *Wishlist* ("Qué busco a cambio").
* **CU006 & CU007 (Modificar, Pausar y Eliminar Artículo):**
  - Bloqueo de edición/eliminación si el artículo se encuentra en estado `RESERVADO` o en un ciclo de trueque activo.
  - Función de pausa para retirar temporalmente el ítem del feed público.
* **CU008 (Explorar Marketplace y Filtros PostGIS):**
  - Feed optimizado con `LazyVerticalGrid` y paginación (*Paging 3*).
  - Filtros de categoría, condición física, rango de valor y radio geográfico de proximidad ($1\text{ km}$ a $50\text{ km}$).
  - Chips de distancia calculada protegiendo la dirección real del propietario.

---

## 🔄 3. Motor de Propuestas de Trueque (`feature:trade`)

Implementación de la máquina de estados transaccional según la Figura 7.3 del documento base:

```
[DISPONIBLE] ──(Crear Oferta)──> [PENDIENTE] ──(Contraofertar)──> [CONTRAOFERTADA]
                                      │
                                 (Aceptar)
                                      ▼
                            [RESERVADO / ACID LOCK]
                                      │
                              (Pactar Safe Zone)
                                      ▼
                                [EN ENTREGA]
                                 /         \
                       (Escanear QR)     (Inasistencia)
                             /                 \
                            ▼                   ▼
                      [COMPLETADO]         [EN DISPUTA]
```

* **CU009 (Proponer Trueque Directo o Híbrido 1:1 / N:1):**
  - Pantalla interactiva que permite al proponente seleccionar 1 o más artículos de su propio inventario.
  - Campo numérico para añadir compensación monetaria en TruequeCoins.
  - Validación local en tiempo real de la regla **RN-02**: la suma combinada debe ser $\ge 40\%$ del valor del ítem solicitado.
* **CU010 (Aceptar Propuesta y Bloqueo ACID):**
  - Al presionar "Aceptar Trueque", el backend ejecuta `SELECT FOR UPDATE` y el cliente actualiza el estado visual a `RESERVADO`.
  - Los artículos quedan deshabilitados para otras negociaciones (RN-01).
* **CU011 (Emitir Contraoferta):**
  - Interfaz para reajustar los artículos solicitados o el monto diferencial de TruequeCoins, invirtiendo el turno de decisión hacia la contraparte.
* **CU012 (Rechazar o Cancelar Propuesta):**
  - Permite cancelar antes de la entrega; si se realiza a menos de 4 horas de la cita acordada, emite advertencia de penalización sobre la tasa de asistencia (RN-07).

---

## 💬 4. Módulo de Chat Privado Transaccional (`feature:chat`)
* **CU018 (Conversar en Chat Privado WebSockets):**
  - **Apertura Condicionada:** El chat únicamente se encuentra habilitado para propuestas en estado `PENDIENTE`, `CONTRAOFERTADA`, `RESERVADO` o `EN ENTREGA` (RN-03).
  - Conexión mediante `Socket.io` con eventos tipados: `join_room`, `send_message`, `receive_message`, `typing_indicator`, `message_read`.
  - Indicadores de doble verificación (*checks* de entrega y lectura).
  - Respaldo de mensajes entrantes mediante notificaciones Push con **Firebase Cloud Messaging (FCM)** si la aplicación está en segundo plano.
  - Modo solo lectura 48 horas después de finalizado o cancelado el trueque.

---

## 🧪 Pruebas Requeridas para el MVP
1. **Pruebas de ViewModel (Unit Tests):**
   - Validación del cálculo del 40% en `CreateTradeViewModelTest`.
   - Flujo de estados en `TradeNegotiationViewModelTest` (Pendiente $\rightarrow$ Aceptado).
2. **Pruebas de UI (Compose UI Tests):**
   - Renderizado del catálogo y aplicación de filtros en `CatalogScreenTest`.
   - Diálogo de ingreso de OTP en `OtpVerificationDialogTest`.
3. **Prueba de Integración WebP:**
   - Comprobación de que toda imagen procesada por el módulo de subida pese estrictamente menos de $80\text{ KB}$.
