# 📋 FASE 0: Definición del Modelo de Dominio, Reglas de Negocio y Cumplimiento de Políticas Google Play
> **Asignación en Antigravity IDE:** Usuario / Agente 1 (Arquitecto de Dominio & Compliance Lead)  
> **Duración estimada:** Semanas 1–2 del SDLC  
> **Rama Git:** `feature/fase-0-business-rules`  
> **Fuentes SWEBOK:** Requisitos de Software (KA #1) y Proceso de Ciclo de Vida ISO/IEC/IEEE 12207

---

## 🎯 Objetivo de la Fase
Establecer las bases conceptuales, las reglas de negocio invariantes (RN-01 a RN-12), las estructuras de datos de dominio en Kotlin puro (sin acoplamiento a frameworks) y los requerimientos obligatorios de cumplimiento para la publicación en **Google Play Store**.

---

## ⚖️ Reglas de Negocio Invariantes a Codificar

El motor de dominio de la aplicación debe hacer cumplir de manera estricta las siguientes reglas del documento base:

| Identificador | Nombre de la Regla | Lógica de Implementación en Dominio |
| :--- | :--- | :--- |
| **RN-01** | *Exclusividad de Reserva Firme* | Un artículo puede figurar en múltiples propuestas pendientes, pero en cuanto una es aceptada (`RESERVADO`), todas las demás propuestas asociadas deben pasar a estado `PAUSADA/BLOQUEADA` de inmediato. |
| **RN-02** | *Banda Mínima de Equivalencia de Valor* | En trueques 1:1 o N:1, el valor estimado de los artículos ofrecidos debe representar como mínimo el **40%** del valor estimado del artículo objetivo para evitar ofertas abusivas (*anti-spam*). Puede compensarse con TruequeCoins. |
| **RN-03** | *Restricción de Contacto Directo* | Ningún usuario puede iniciar un chat privado sin que exista previamente una oferta formal emitida o aceptada entre ambas partes. Previene acoso y ofertas fuera de la plataforma. |
| **RN-04** | *Conservación de Suma Cero en Ledger* | El monedero opera con contabilidad de partida doble. Toda transacción de TruequeCoins debe debitar de una cuenta origen y acreditar en una cuenta destino exactamente el mismo monto. $\sum Débitos - \sum Créditos = 0$. |
| **RN-05** | *Custodia Obligatoria en Escrow* | Al aceptarse un trueque con créditos TruequeCoins, estos se sustraen del `saldo_disponible` y se transfieren a `saldo_escrow`. Solo se acreditan al vendedor tras la validación física por QR (o se reembolsan al comprador si se cancela legítimamente). |
| **RN-06** | *Desistimiento sin Penalización en Triangulares* | En trueques triangulares ($A \rightarrow B \rightarrow C \rightarrow A$), si cualquiera de los tres usuarios rechaza o no responde en 24 horas, el ciclo se aborta automáticamente sin penalizar a ninguno. |
| **RN-07** | *Ventana de Cancelación y Penalización* | Un trueque aceptado puede cancelarse sin sanción hasta **4 horas antes** de la cita acordada en el Punto Seguro. Cancelaciones con menor tiempo reducen la tasa de cumplimiento del usuario infractor. |
| **RN-08** | *Verificación de Presencia en Safe Zone* | Para reportar inasistencia de la contraparte (pasados 15 minutos de la cita), el usuario denunciante debe comprobar presencia física por GPS en un radio $\le 150\text{ m}$ del Punto Seguro. |
| **RN-09** | *Cálculo de Reputación Bayesiana* | La calificación promedio de un usuario no es un promedio simple; usa una fórmula bayesiana ponderada para evitar que cuentas nuevas con una sola calificación de 5 estrellas aparezcan por encima de veteranos con docenas de trueques. |
| **RN-10** | *Política de Privacidad Geoespacial* | En listados públicos y perfiles, las coordenadas reales del domicilio se ofuscan en un polígono difuso con radio de **500 metros** (RNF-08). Solo los Puntos Seguros muestran coordenadas exactas. |
| **RN-11** | *Prohibición de Artículos Ilícitos* | Bloqueo estricto e inmediato de publicaciones de armas, medicamentos controlados, sustancias ilícitas, animales o servicios adultos. |
| **RN-12** | *Evaluación Doble Ciego* | Las reseñas y calificaciones de un intercambio permanecen ocultas hasta que ambas partes hayan calificado o hayan transcurrido 7 días, evitando represalias en puntuaciones. |

---

## 🏛️ Modelos de Dominio en Kotlin Puro (`domain/model/`)

El desarrollador a cargo de esta fase debe crear los siguientes modelos en el módulo o paquete `core:domain`:

```kotlin
// Paquete: com.truequapp.core.domain.model

enum class TradeStatus {
    PENDIENTE,
    CONTRAOFERTADA,
    RESERVADO,
    EN_ENTREGA,
    COMPLETADO,
    CANCELADO,
    EN_DISPUTA
}

enum class ItemCondition {
    NUEVO,
    COMO_NUEVO,
    USADO,
    REGULAR
}

enum class ItemType {
    BIEN_FISICO,
    SERVICIO
}

enum class KycLevel {
    BASICO_NIVEL_0,
    TELEFONO_SMS_NIVEL_1,
    IDENTIFICACION_OFICIAL_NIVEL_2
}

data class UserProfile(
    val id: String,
    val fullName: String,
    val username: String,
    val email: String,
    val phoneNumber: String,
    val kycLevel: KycLevel,
    val bayesianRating: Double,
    val completionRate: Double,
    val fuzzyNeighborhood: String,
    val isActive: Boolean
)

data class TradeItem(
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String,
    val categoryId: Int,
    val type: ItemType,
    val condition: ItemCondition,
    val estimatedValueMin: Double,
    val estimatedValueMax: Double,
    val imagesWebP: List<String>,
    val tagsWishlist: List<String>,
    val isAvailable: Boolean
)

data class TradeProposal(
    val id: String,
    val requesterId: String,
    val receiverId: String,
    val offeredItemIds: List<String>,
    val targetItemId: String,
    val compensatoryCoins: Double,
    val status: TradeStatus,
    val expiresAtUtc: Long
) {
    fun meetsMinimumValueRatio(targetItemValue: Double, offeredTotalValue: Double): Boolean {
        val totalOffered = offeredTotalValue + compensatoryCoins
        return (totalOffered / targetItemValue) >= 0.40 // RN-02: Mínimo 40%
    }
}
```

---

## 🛡️ Checklist de Cumplimiento de Políticas de Google Play Console

Antes de iniciar la codificación de la interfaz y la integración con servicios nativos, el equipo debe garantizar el apego a las siguientes directrices de Google Play:

### 1. Política de Divisas Virtuales y Servicios Financieros
- [x] **Clasificación de TruequeCoins:** No representan criptomonedas, valores bursátiles ni tokens convertibles en moneda de curso legal (fíat).
- [x] **Sin Retiros a Cuentas Bancarias:** La aplicación no incluye pasarelas de pago de salida (*cash-out*). Son exclusivamente unidades de cuenta internas para facilitar el trueque asíncrono.
- [x] **Transparencia en el Registro:** El usuario debe aceptar expresamente los Términos de Servicio Comunitarios donde se especifica la naturaleza de los TruequeCoins.

### 2. Política de Acceso a Ubicación (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`)
- [x] **Ubicación en Primer Plano Únicamente:** No declarar ni solicitar `ACCESS_BACKGROUND_LOCATION`.
- [x] **Justificación Clara (*In-App Rationale*):** Mostrar un diálogo previo a la solicitud del permiso de Android explicando que la ubicación solo se usa para calcular la distancia a los artículos del catálogo y verificar la llegada a la Safe Zone.

### 3. Política de Permisos de Cámara (`CAMERA`)
- [x] **Uso Estrictamente Transaccional:** Utilizado únicamente para tomar fotografías del artículo en el alta de inventario y para escanear el código QR en la entrega.

### 4. Política de Eliminación de Cuentas (Google Play Requirement)
- [x] **Opción en la App (CU002):** Acceso visible en Ajustes > Seguridad > "Eliminar mi cuenta".
- [x] **Enlace Web Externo Obligatorio:** Proveer URL pública (`https://truequapp.org/delete-account`) para solicitud de borrado de cuenta sin necesidad de reinstalar la app.

---

## 📦 Entregables Verificables de la Fase 0
1. `core:domain`: Módulo Kotlin con entidades, enumeraciones y Value Objects.
2. `core:domain:rules`: Clases de validación de reglas de negocio (`ValidateTradeValueUseCase`, `EvaluateCancellationWindowUseCase`).
3. Pruebas unitarias en `test/` con JUnit 5 validando:
   - Cumplimiento de la banda del 40% (RN-02).
   - Bloqueo de artículos en múltiples ofertas (RN-01).
   - Cálculo de penalización horaria de 4 horas (RN-07).
