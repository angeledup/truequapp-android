# 🏷️ Política de Versionado por Fases y Retención N-1 — Truequapp Android

> **Destinatarios:** Equipo de Desarrollo Truequapp & Antigravity IDE (Agentes IA)  
> **Ámbito:** Repositorio `truequapp-android`  
> **Estándar:** Versionado Semántico Adaptado a Fases ($X.Y$) con Ventana de Retención Estricta ($N-1$) y Purga de $N-2$.

---

## 1. 📐 Esquema de Nomenclatura de Versiones (`X.Y`)

La versión del proyecto se representa formalmente con la estructura:

$$\mathbf{vX.Y}$$

Donde:
* **`X` (Dígito antes del punto — Fase del Proyecto):**  
  Representa el identificador numérico de la fase de desarrollo activa, alineado con la estructura modular de `fases/`:
  * `0`: **Fase 0** — Modelado de Dominio, Reglas de Negocio & Cumplimiento Google Play.
  * `1`: **Fase 1** — Arquitectura MVI / Clean, Design System Material 3, Red y DI.
  * `2`: **Fase 2** — MVP Core: Autenticación, Catálogo WebP, Trueques 1:1 y Chat.
  * `3`: **Fase 3** — Seguridad Geoespacial, Safe Zones, QR Dinámico y Disputas.
  * `4`: **Fase 4** — Grafos de Trueque Triangular, TruequeCoins Escrow y Modo Swipe.
  * `5`: **Fase 5** — QA E2E, Play Integrity, App Bundle (.aab) y Lanzamiento a Producción.

* **`Y` (Dígito después del punto — Contador de Actualizaciones):**  
  Representa el número correlativo de actualización o entrega funcional completada dentro de dicha fase.
  * Inicia obligatoriamente en `1` al comenzar cada fase (`X.1`).
  * Se incrementa en `+1` con cada entrega funcional, refactorización o actualización completada y verificada (`X.1` $\rightarrow$ `X.2` $\rightarrow$ `X.3` $\dots$).

### Mapeo con Android (`versionName` y `versionCode`)
Para garantizar compatibilidad con Google Play Console:
* **`versionName`**: `"X.Y"` (ejemplo: `"0.1"`, `"0.2"`, `"1.1"`).
* **`versionCode`**: Entero monótono creciente calculado mediante la fórmula:
  $$\text{versionCode} = (X \times 1000) + Y$$
  * *Ejemplo:* Versión `0.1` $\rightarrow$ `versionCode = 1`
  * *Ejemplo:* Versión `0.2` $\rightarrow$ `versionCode = 2`
  * *Ejemplo:* Versión `1.1` $\rightarrow$ `versionCode = 1001`
  * *Ejemplo:* Versión `2.3` $\rightarrow$ `versionCode = 2003`

---

## 2. 🛡️ Política de Retención y Purga de Versiones ($N-1$)

Para optimizar el almacenamiento, prevenir inconsistencias en el trabajo en equipo y mantener un punto de restauración seguro e inmediato:

### Regla Fundamental
> **En todo momento solo existirán exactamente DOS (2) versiones activas en el histórico de respaldo:**
> 1. **Versión Actual ($V_N$):** La versión recién integrada, funcional y verificada.
> 2. **Versión Anterior ($V_{N-1}$):** Red de seguridad inmediata (*Fallback / Rollback Point*) si $V_N$ presenta fallas críticas.
>
> ❌ **La versión $V_{N-2}$ (dos versiones atrás) se ELIMINA DEFINITIVAMENTE** al consolidarse la versión $V_N$.

### Matriz de Transición y Eliminación

| Evento | Versión Creada ($V_N$) | Versión de Respaldo ($V_{N-1}$) | Versión Eliminada ($V_{N-2}$) | Estado en Repositorio |
| :--- | :---: | :---: | :---: | :--- |
| **Inicio Fase 0** | `v0.1` | *(Ninguna)* | *(Ninguna)* | Solo existe `v0.1` |
| **Actualización 2** | `v0.2` | `v0.1` | *(Ninguna)* | Coexisten `v0.2` y `v0.1` |
| **Actualización 3** | `v0.3` | `v0.2` | 🗑️ **`v0.1`** | Se elimina `v0.1`. Quedan `v0.3` y `v0.2` |
| **Paso a Fase 1** | `v1.1` | `v0.3` | 🗑️ **`v0.2`** | Se elimina `v0.2`. Quedan `v1.1` y `v0.3` |
| **Actualización 2 (F1)**| `v1.2` | `v1.1` | 🗑️ **`v0.3`** | Se elimina `v0.3`. Quedan `v1.2` y `v1.1` |

---

## 3. 🤖 Protocolo Obligatorio para Antigravity IDE

Cuando Antigravity IDE o un desarrollador trabaje en el repositorio, debe seguir este flujo riguroso:

### Paso 0: Confirmación Previa Obligatoria con el Usuario
Antes de iniciar cualquier nueva fase de desarrollo:
1. Explicar en detalle al usuario el alcance, arquitectura, módulos y entregables previstos.
2. Preguntar explícitamente si esa propuesta coincide con lo solicitado y esperar su visto bueno antes de realizar cualquier cambio en el código.
*(Ver detalle en `.agents/rules/fases.md`).*

### Paso 1: Verificación de Calidad Previa
Antes de actualizar la versión, ejecutar la suite de pruebas unitarias y comprobación de sintaxis:
```powershell
./gradlew test
```
*Si alguna prueba falla, está estrictamente prohibido generar una nueva versión.*

### Paso 2: Actualización de la Versión
1. Incrementar el identificador en la configuración del proyecto o archivo de versiones:
   * Si se continúa en la misma fase: `X.(Y + 1)`
   * Si se avanza a una nueva fase: `(X + 1).1`
2. Generar el commit correspondiente:
   ```bash
   git add .
   git commit -m "feat(fase-X): resumen de cambios funcionales - bump vX.Y"
   ```

### Paso 3: Creación de la Etiqueta (Tag $V_N$)
Crear el tag Git anotado para la versión recién generada:
```bash
git tag -a vX.Y -m "Release vX.Y - Fase X Actualización Y"
```

### Paso 4: Purga Automática de la Versión $V_{N-2}$
Determinar cuál es la versión que quedó dos pasos atrás ($V_{N-2}$) y eliminarla de Git (local y remoto si aplica):
```bash
# Ejemplo: si se acaba de crear v0.3, eliminar v0.1:
git tag -d v0.1
git push origin --delete v0.1 2>$null || true
```

---

## 4. 🚨 Procedimiento de Rollback de Emergencia (Si $V_N$ falla)

Si el código de la versión actual ($V_N$) presenta un bug bloqueante, incompatibilidad o falla de compilación que impida continuar:

1. **Abortar cambios en curso no commiteados:**
   ```bash
   git reset --hard HEAD
   ```
2. **Volver al punto de restauración seguro ($V_{N-1}$):**
   ```bash
   git checkout tags/v[ANTERIOR]
   ```
3. **Restablecer la rama de trabajo al estado de $V_{N-1}$:**
   ```bash
   git branch -D feature/temp-fix 2>$null || true
   git switch -C develop tags/v[ANTERIOR]
   ```
4. **Notificar al equipo y al IDE:** Documentar la causa de la reversión antes de reintentar la actualización.

---

## 5. 📝 Bitácora de Versiones Activas (`CHANGELOG_VERSIONES.md`)

El repositorio mantiene un archivo compacto `CHANGELOG_VERSIONES.md` que únicamente registra las dos versiones vigentes:

```markdown
# 📌 Estado Actual de Versiones (Truequapp)

- **Versión Activa (N):** `v0.1` — (Fecha: [FECHA]) — *Implementación inicial de modelos de dominio y reglas de negocio RN-01 a RN-12.*
- **Versión Fallback (N-1):** *Ninguna (Versión génesis).*
- **Última Versión Purgada (N-2):** *Ninguna.*
```
