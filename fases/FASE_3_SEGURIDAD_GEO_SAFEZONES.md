# 📍 FASE 3: Geolocalización PostGIS, Safe Exchange Zones, QR Dinámico y Disputas
> **Asignación en Antigravity IDE:** Usuario / Agente 4 (Geospatial & Hardware Integration Lead)  
> **Duración estimada:** Semanas 9–11 del SDLC  
> **Rama Git:** `feature/fase-3-geo-safezones-qr`  
> **Casos de Uso UV Cubiertos:** CU002, CU019, CU020, CU022  
> **Fuentes SWEBOK:** Calidad de Software (KA #10), Métodos de Ingeniería de Software (KA #9)

---

## 🎯 Objetivo de la Fase
Eliminar el riesgo de inseguridad física y erradicar las inasistencias en los intercambios presenciales mediante la integración de:
1. **Google Maps Compose** y geocálculos para selección de Puntos de Encuentro Seguros (*Safe Exchange Zones*).
2. **CameraX y Google ML Kit** para lectura y validación criptográfica instantánea de códigos QR dinámicos.
3. **Detección de Ubicaciones Simuladas (*Mock Locations*)** y validación de geocercas ($150\text{ m}$) para reportes de inasistencia y disputas en Escrow.
4. **Mecanismo de Evaluación Doble Ciego** (RN-12) para proteger la reputación.

---

## 🗺️ 1. Puntos de Encuentro Seguros (*Safe Exchange Zones* — CU019)

### Cálculo Equidistante y Mapa Interactivo
Cuando una propuesta pasa a estado `RESERVADO`, los usuarios pueden abrir la interfaz de selección de punto de encuentro dentro del chat:
1. **Cálculo de Punto Medio:** El backend PostGIS (o el cliente local) calcula el centroide geográfico entre la colonia del Usuario A y la colonia del Usuario B.
2. **Mapa de Puntos Verificados:** Se despliegan marcadores personalizados en Google Maps Compose señalando lugares públicos vigilados (plazas comerciales, bibliotecas, campus universitarios, cafeterías aliadas) con sus horarios verificados de operación.
3. **Agendamiento:** El Usuario A selecciona fecha, hora y Safe Zone; se envía una tarjeta interactiva al chat. Al ser aceptada por el Usuario B, el trueque cambia a estado `EN ENTREGA`.
4. **Recordatorios:** Programación de notificaciones locales de aviso mediante `AlarmManager` o *WorkManager* (24 horas y 2 horas antes de la cita).

---

## 📷 2. Confirmación Presencial con Cámara y QR Dinámico (CU020)

### Componente de Generación y Lectura de QR
* **Generación Dinámica del Código (Receptor):**
  - La pantalla del usuario que entrega genera un código QR utilizando una semilla criptográfica única (`HMAC-SHA256`) vinculada al `trade_id`, con expiración temporal corta para evitar capturas de pantalla reutilizadas.
  - **Modo Offline Fallback:** Si no hay conectividad de datos en el sitio, se muestra alternativamente un código PIN numérico TOTP de 6 dígitos.
* **Escáner Ultrarrápido con CameraX + Google ML Kit (Ofertante):**
  - Implementación con `CameraX` vinculada al ciclo de vida de Compose (`DisposableEffect`).
  - Detección en tiempo real con `mlkit:barcode-scanning` sin congelar el hilo de renderizado.
  - *Feedback* táctil háptico (`HapticFeedbackType.LongPress`) y auditivo al confirmar el código.
* **Liquidación Atómica:**
  - Al validar el código, el sistema cambia el trueque a `COMPLETADO`, transfiere los artículos a `INTERCAMBIADOS` y detona la liberación de los TruequeCoins retenidos en Escrow (CU015).

---

## ⭐ 3. Sistema de Calificación Doble Ciego (RN-12)
* Finalizada la entrega presencial, ambos usuarios acceden a la pantalla de reseña:
  - Calificación de 1 a 5 estrellas.
  - Indicador de puntualidad y trato.
  - Comentario escrito.
* **Invariante RN-12:** La reseña permanece oculta para la contraparte hasta que ambos hayan calificado o hayan transcurrido 7 días naturales. Esto erradica el sesgo de calificaciones negativas por represalia.

---

## 🚨 4. Gestión de Disputas, Inasistencias y Anti-Spoofing (CU022)

### Protocolo de Inasistencia y Validación GPS (RN-08)
Si transcurren más de 15 minutos de la hora pactada y la contraparte no se presenta:
1. **Validación de Geocerca ($150\text{ m}$):** La aplicación consulta `FusedLocationProviderClient` para verificar que el usuario reportante se encuentre físicamente dentro del radio de $150\text{ metros}$ de la Safe Zone.
2. **Detección Anti-Fraude (*Mock Location*):**
   ```kotlin
   fun isMockLocationDetected(location: Location): Boolean {
       return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
           location.isMock
       } else {
           @Suppress("DEPRECATION")
           location.isFromMockProvider
       }
   }
   ```
   Si se detecta falsificación de GPS, el reporte se rechaza automáticamente y se registra una alerta de seguridad sobre la cuenta.
3. **Congelamiento de Escrow:** Si el reporte es verificado, los artículos del usuario presente se liberan a `DISPONIBLE`, los créditos en Escrow se congelan en estado `EN_DISPUTA` y se penaliza la tasa de asistencia del usuario ausente.

---

## 🗑️ 5. Cumplimiento de Baja de Usuario y Borrado de Datos (CU002)
* Interfaz dentro de Ajustes que permite la desactivación o solicitud de borrado de cuenta.
* Validación previa de ausencia de transacciones en estado `RESERVADO` o fondos en `ESCROW`.
* Revocación de sesiones activas en Redis y ofuscación de datos personales, conservando exclusivamente asientos contables inmutables para auditoría legal.

---

## 📦 Entregables Verificables de la Fase 3
1. Módulo `feature:safezones` con mapa interactivo Google Maps Compose y selección de citas.
2. Pantalla de escaneo QR con CameraX y ML Kit funcionando a 60 FPS con detección en $< 200\text{ ms}$.
3. Utilidad `LocationValidator` con detección de *Mock Locations* y cálculo de distancia esférica Haversine.
4. Módulo de quejas y disputas con captura de evidencias fotográficas.
