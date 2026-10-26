# 🪙 FASE 4: Algoritmo Triangular A→B→C→A, Monedero TruequeCoins y Modo Swipe
> **Asignación en Antigravity IDE:** Usuario / Agente 5 (Fintech & Algorithmic Matching Lead)  
> **Duración estimada:** Semanas 12–14 del SDLC  
> **Rama Git:** `feature/fase-4-swipe-wallet-escrow`  
> **Casos de Uso UV Cubiertos:** CU013, CU014, CU015, CU016, CU017  
> **Fuentes SWEBOK:** Modelos y Notaciones de Diseño (KA #2), Vista Lógica y de Procesos de Kruchten

---

## 🎯 Objetivo de la Fase
Superar definitivamente la barrera de la "Doble Coincidencia de Necesidades" mediante tres pilares de innovación:
1. **Modo Swipe Interactivo:** Exploración ágil tipo *card deck* con gestos táctiles fluidos que alimenta el grafo social de afinidad en tiempo real.
2. **Monedero de TruequeCoins con Escrow:** Gestión de créditos comunitarios bajo contabilidad de partida doble (RN-04) protegida por autenticación biométrica y PIN.
3. **Orquestador de Trueques Triangulares ($A \rightarrow B \rightarrow C \rightarrow A$):** Detección visual, coordinación y firma atómica tripartita con ventana de 24 horas (RN-06).

---

## 🎴 1. Exploración Rápida en Modo Swipe (CU017)

### Arquitectura de Gestos en Jetpack Compose
El Modo Swipe presenta tarjetas de artículos en pantalla completa con transiciones basadas en física y animaciones cinéticas:
* **Manejo de Gestos (`Modifier.pointerInput`):**
  - Desplazamiento horizontal con rotación proporcional al arrastre ($\theta \propto \Delta x$).
  - **Swipe a la Derecha:** Envía señal positiva de afinidad (`swipe_signals` $\rightarrow$ "Me interesa").
  - **Swipe a la Izquierda:** Descarta el artículo temporalmente ("Pasar").
* **Detección de Match Inmediato:**
  - Si el propietario del artículo ya había indicado interés en algún objeto del usuario actual, se detona una animación de *¡Coincidencia de Trueque!* (*Lottie Animation*).
  - Acceso directo con un solo toque para emitir la propuesta 1:1 precargada.
* **Alimentación del Motor de Grafos:** Cada swipe alimenta de forma asíncrona el worker de teoría de grafos del backend para descubrir ciclos multilaterales.

---

## 💰 2. Monedero Electrónico y Depósito en Garantía (*Escrow*)

### CU013 & CU014 (Activación, PIN, Biometría y Ledger Contable)
* **Activación de Billetera:** Requiere KYC Nivel 1. El usuario configura un PIN numérico de 4 dígitos (con validación de secuencias no triviales).
* **Autenticación Biométrica (`BiometricPrompt`):**
  - Integración con Android Keystore para cifrar las operaciones de autorización con huella dactilar o reconocimiento facial.
* **Libro Mayor de Partida Doble (RN-04):**
  - Pantalla de saldo con desglose:
    - **Saldo Disponible:** Créditos listos para ofertar o transferir.
    - **Saldo en Escrow (Garantía):** Créditos retenidos en intercambios pendientes.
    - **Saldo Total:** Suma contable.
  - Historial cronológico inmutable con folios de transacción, contraparte, concepto (bono inicial, depósito en garantía, liberación por entrega) y comprobante digital exportable.

### CU015 (Retención y Liquidación en Escrow)
```
[Comprador Acepte Oferta] 
   └── Deducción de Saldo Disponible de A ──> Crédito a Saldo Escrow de A (Retención)
                                                    │
                   ┌────────────────────────────────┴───────────────────────────────┐
             (Escaneo QR CU020)                                           (Cancelación CU012)
                   ▼                                                               ▼
  Deducción de Saldo Escrow de A                                Reembolso al Saldo Disponible de A
        y Acreditación al                                            (Cero pérdida financiera)
     Saldo Disponible de B
```

---

## 📐 3. Orquestador de Trueques Triangulares ($A \rightarrow B \rightarrow C \rightarrow A$ — CU016)

Resuelve el escenario donde el **Usuario A** quiere el artículo del **Usuario B**, pero el Usuario B no desea nada de A, sino un objeto del **Usuario C**, y el Usuario C está interesado en el artículo de A:

```
        ┌─────────────────────────────────────────────────────────┐
        │                                                         │
        ▼                                                         │
   [Usuario A] ──────(Entrega Artículo A)──────> [Usuario B]      │
        ▲                                             │           │
        │                                             │ (Entrega  │
        │                                             │  Artículo │
        │ (Entrega Artículo C)                        │     B)    │
        │                                             ▼           │
        └────────────────────────────────────── [Usuario C]       │
                                                      │           │
                                                      └───────────┘
```

### Flujo Operativo y UI del Ciclo Triangular:
1. **Notificación Proactiva:** Cuando el worker del servidor detecta un ciclo cerrado con compatibilidad $\ge 0.70$, se envía una notificación Push de alta prioridad a los 3 nodos.
2. **Pantalla Gráfica Tripartita:** Un Canvas interactivo en Compose modela el triángulo en tiempo real, detallando para cada usuario:
   - *Lo que tú entregas:* Imagen y datos de tu artículo.
   - *Lo que tú recibes:* Imagen y datos del artículo objetivo.
   - *Estado de confirmación de los participantes:* Badges de firma ($1/3$, $2/3$, $3/3$).
3. **Bloqueo Atómico y Ventana de 24 Horas:**
   - Cada participante tiene 24 horas para aceptar (RN-06).
   - Al registrarse la tercera firma ($3/3$), los 3 artículos quedan automáticamente en estado `RESERVADO`.
   - Se abre una sala de coordinación grupal geolocalizada en una Safe Zone común para pactar la entrega simultánea.
4. **Desistimiento Automático:** Si vence el plazo de 24 horas o un usuario declina, el ciclo se disuelve de forma transparente y sin ninguna penalización para los miembros.

---

## 📦 Entregables Verificables de la Fase 4
1. Módulo `feature:swipe` con soporte para gestos táctiles y animaciones a 60 FPS.
2. Módulo `feature:wallet` con integración de `BiometricPrompt`, vista de saldos, historial de partida doble y comprobantes.
3. Componente `TriangularTradeDialog` en Compose con visualización de grafos circulares y lógica de firma 3/3.
4. Pruebas unitarias de contabilidad validando que `saldoDisponible + saldoEscrow == saldoTotal` en todas las operaciones.
