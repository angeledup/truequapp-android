# Antigravity IDE Rule: Versionado por Fases y Política de Retención N-1

## 📌 Directiva General
En este proyecto (`truequapp-android`), todo el desarrollo se organiza en **Fases** (0 a 5) y se versiona estrictamente mediante la nomenclatura **`X.Y`**.

## 1. Estructura de Versión (`X.Y`)
- **`X` (Fase del Proyecto):**
  - `0`: Fase 0 (Modelado de Dominio, Reglas de Negocio, Google Play Compliance)
  - `1`: Fase 1 (Arquitectura Base MVI, Design System Material 3, Red y DI)
  - `2`: Fase 2 (MVP Core: Auth, Catálogo WebP, Trueques 1:1, Chat)
  - `3`: Fase 3 (Seguridad Geoespacial, Safe Zones, QR Dinámico, Disputas)
  - `4`: Fase 4 (Trueques Triangulares, TruequeCoins Escrow, Modo Swipe)
  - `5`: Fase 5 (QA E2E, Play Integrity, App Bundle AAB, Deploy)
- **`Y` (Número de Actualización dentro de la Fase):**
  - Comienza en `1` al iniciar cada fase (`X.1`).
  - Se incrementa en `+1` con cada entrega funcional verificada (`X.1` $\rightarrow$ `X.2` $\dots$).
- **Mapeo Android:**
  - `versionName`: `"X.Y"`
  - `versionCode`: `(X * 1000) + Y`

## 2. Política de Retención de 2 Versiones (Ventana N-1)
- **Versión Activa ($V_N$):** La versión recién integrada y verificada.
- **Versión Fallback ($V_{N-1}$):** Punto de restauración inmediato en caso de falla crítica.
- **Versión Purgada ($V_{N-2}$):** Al crear la versión $V_N$, la versión **$V_{N-2}$ (dos versiones atrás) debe ser eliminada permanentemente** de Git (`git tag -d v(N-2)`).
- En ningún momento deben conservarse más de 2 versiones en el histórico de respaldo.

## 3. Procedimiento de Rollback de Emergencia
Si la versión actual ($V_N$) rompe la compilación o introduce errores críticos:
1. Revertir cambios locales: `git reset --hard HEAD`
2. Restaurar el código a la versión previa ($V_{N-1}$): `git checkout tags/v[ANTERIOR]`
3. Recrear rama de trabajo limpia desde $V_{N-1}$.

## 4. Referencia Completa
Consulta el documento maestro: [`POLITICA_VERSIONADO.md`](file:///c:/Users/angel/truequapp-android/POLITICA_VERSIONADO.md).
