# Verificación funcional en despliegue — TruequeApp

**Proyecto:** `angeledup/truequapp-android` — versión 2.1 / versionCode 2001, targetSdk 34
**Commit de referencia:** `e50a351` (7-nov-2026)
**Fecha:** 10 de noviembre de 2026
**Estado:** lista para correr sobre la app instalada, en un dispositivo o emulador real, antes de considerar cualquier despliegue. Aún no se ha ejecutado: no hay JDK ni Android SDK ni dispositivo en la máquina de auditoría, por lo que todos los pasos están en estado PENDIENTE.

**Cómo usarla:** correr los pasos en orden, con dos cuentas de prueba (Usuario A y Usuario B) y datos sintéticos. Cada paso tiene un resultado esperado y un criterio de aprobado/reprobado. Un solo paso bloqueador en REPROBADO detiene el despliegue. Anotar en cada paso: dispositivo, fecha, resultado (APROBADO / REPROBADO / PENDIENTE) y evidencia (captura o registro).

**Antes de empezar (precondiciones):**
- P0 de la auditoría del 10-nov-2026 corregidos (ver [AUDITORIA_TRUEQUEAPP_2026-11-10.md](AUDITORIA_TRUEQUEAPP_2026-11-10.md) y sección "Bloqueadores que impiden el despliegue").
- APK/AAB construido desde el commit verificado, sin datos reales.
- Dos cuentas de prueba creadas con datos sintéticos (nunca datos personales reales).
- Permisos concedidos o denegados según indique cada paso.

---

## 1. Compilación e instalación

### 1.1 Compilación debug
- **Paso:** ejecutar `./gradlew assembleDebug` desde la raíz del repo.
- **Resultado esperado:** la tarea termina en éxito y genera `app/build/outputs/apk/debug/app-debug.apk`.
- **Aprobado si:** compilación en verde sin errores. **Reprobado si:** cualquier error de compilación o advertencia que impida generar el APK.

### 1.2 Compilación release
- **Paso:** ejecutar `./gradlew bundleRelease`.
- **Resultado esperado:** genera el `.aab` firmado con la clave de subida; R8 activo y sin el `-keep` global.
- **Aprobado si:** el `.aab` se genera y el código está ofuscado (verificar que las clases no conservan sus nombres originales). **Reprobado si:** no compila, no firma, o el código sale sin ofuscar.

### 1.3 Instalación en limpio
- **Paso:** instalar el APK en un dispositivo o emulador sin datos previos (`adb install` o instalación manual).
- **Resultado esperado:** la app instala sin errores y abre en la pantalla de login.
- **Aprobado si:** instala y abre. **Reprobado si:** error de instalación, cierre al abrir, o pantalla en blanco.

### 1.4 Primera apertura sin cuenta administradora sembrada
- **Paso:** tras instalar en limpio, abrir la app y revisar la base local (o intentar entrar con la cuenta administradora que existía en versiones anteriores).
- **Resultado esperado:** no existe ninguna cuenta administradora preexistente ni saldo de 100M TruequeCoins.
- **Aprobado si:** no hay cuenta admin ni saldo sembrado. **Reprobado si:** aparece cualquier cuenta o saldo que no haya creado el probador. **(Bloqueador, ver D-01.)**

## 2. Humo

### 2.1 Recorrido mínimo
- **Paso:** abrir la app, ir a login, entrar con la cuenta de prueba A, ver el catálogo y abrir el detalle de un artículo.
- **Resultado esperado:** todas las pantallas cargan sin cierres ni errores visibles.
- **Aprobado si:** el recorrido completo funciona. **Reprobado si:** cualquier cierre, pantalla en blanco o error.

## 3. Registro

### 3.1 Registro válido
- **Paso:** crear la cuenta de prueba A con correo, usuario, teléfono y contraseña de 8 o más caracteres (datos sintéticos).
- **Resultado esperado:** cuenta creada, mensaje de éxito, y redirección al login.
- **Aprobado si:** la cuenta queda creada y se puede usar para entrar. **Reprobado si:** no se crea o no permite entrar después.

### 3.2 Contraseña corta rechazada
- **Paso:** intentar registrar con una contraseña de menos de 8 caracteres.
- **Resultado esperado:** rechazo con mensaje claro, sin crear la cuenta.
- **Aprobado si:** se rechaza. **Reprobado si:** se crea la cuenta con contraseña corta.

### 3.3 Correo duplicado rechazado
- **Paso:** intentar registrar dos veces el mismo correo.
- **Resultado esperado:** rechazo con "ya existe una cuenta registrada con ese correo", sin crear duplicado.
- **Aprobado si:** se rechaza el segundo registro y sigue habiendo una sola cuenta con ese correo. **Reprobado si:** se crea duplicado o la app se cierra con error.

### 3.4 Contraseña no visible en claro
- **Paso:** registrar la cuenta A y revisar la base local (`usuarios`).
- **Resultado esperado:** la contraseña está hasheada; en ningún campo aparece en texto plano.
- **Aprobado si:** hash presente, texto plano ausente. **Reprobado si:** aparece en claro. **(Bloqueador, ver D-02.)**

## 4. Login

### 4.1 Login correcto
- **Paso:** entrar con la cuenta A y su contraseña correcta.
- **Resultado esperado:** entra al catálogo y la sesión queda guardada (al cerrar y reabrir, sigue la sesión o pide login según el diseño).
- **Aprobado si:** entra y la sesión se comporta como está diseñado. **Reprobado si:** no entra con credenciales correctas.

### 4.2 Login incorrecto
- **Paso:** entrar con correo correcto y contraseña incorrecta, y luego con un correo que no existe.
- **Resultado esperado:** en ambos casos el mismo mensaje genérico ("Correo o contraseña incorrectos"), sin revelar cuál de los dos falló.
- **Aprobado si:** el mensaje es idéntico en los dos casos. **Reprobado si:** distingue "cuenta no existe" de "contraseña incorrecta". **(Ver D-08.)**

### 4.3 Intentos sin límite
- **Paso:** fallar el login 10 veces seguidas con la misma cuenta.
- **Resultado esperado:** tras varios intentos, bloqueo temporal, retardo o captcha.
- **Aprobado si:** hay alguna limitación. **Reprobado si:** permite intentos ilimitados sin fricción.

### 4.4 Cierre de sesión
- **Paso:** cerrar sesión desde el perfil.
- **Resultado esperado:** vuelve al login y la sesión queda invalidada (al reabrir no entra directo).
- **Aprobado si:** la sesión se cierra de verdad. **Reprobado si:** sigue entrando sin pedir credenciales.

## 5. Catálogo

### 5.1 Feed del catálogo
- **Paso:** con la cuenta A, abrir el catálogo.
- **Resultado esperado:** se ven los artículos publicados (los de la cuenta A y los del catálogo general), con foto, título y valor.
- **Aprobado si:** el feed carga y muestra los artículos. **Reprobado si:** feed vacío con artículos existentes, imágenes rotas o cierre.

### 5.2 Filtro por usuario (Opción A)
- **Paso:** publicar un artículo con la cuenta A, cerrar sesión, entrar con la cuenta B y revisar "Mis publicaciones" y el archivo local.
- **Resultado esperado:** B no ve las publicaciones de A en "Mis publicaciones", y el archivo local de B no contiene artículos de A.
- **Aprobado si:** el filtro por usuario se cumple en pantalla y en archivo. **Reprobado si:** se mezclan artículos de otros usuarios.

### 5.3 Detalle de artículo
- **Paso:** abrir el detalle de un artículo del catálogo.
- **Resultado esperado:** detalle con fotos, descripción, valor y dueño, sin cierre abrupto (la corrección del commit `328ba39`).
- **Aprobado si:** abre y muestra todo. **Reprobado si:** cierre, claves duplicadas o datos faltantes.

## 6. Publicar artículo

### 6.1 Publicación con fotos
- **Paso:** con la cuenta A, publicar un artículo con título, descripción, valor y hasta 6 fotos.
- **Resultado esperado:** el artículo aparece en el catálogo y en "Mis publicaciones"; las fotos se comprimen a WebP (<80 KB según el diseño).
- **Aprobado si:** publica y aparece en ambos lugares. **Reprobado si:** no aparece, no guarda las fotos o se cierra.

### 6.2 Publicación sin conexión
- **Paso:** desactivar la red y publicar un artículo.
- **Resultado esperado:** la app guarda localmente y avisa que se publicará al recuperar conexión, o rechaza con mensaje claro; nunca anuncia éxito falso.
- **Aprobado si:** el comportamiento es claro y no miente. **Reprobado si:** dice "publicado" y el artículo no existe.

## 7. Proponer trueque

### 7.1 Propuesta 1:1
- **Paso:** con la cuenta A, proponer un trueque por un artículo de la cuenta B (artículos propios + coins compensatorias si aplica).
- **Resultado esperado:** la propuesta aparece como PENDIENTE para B, con los artículos y las coins correctas.
- **Aprobado si:** B la ve con los datos correctos. **Reprobado si:** no aparece, datos incorrectos o coins mal descontadas.

### 7.2 Validación de valor (RN-02)
- **Paso:** proponer un trueque cuya oferta combinada represente menos del 40% del valor objetivo.
- **Resultado esperado:** rechazo con el mensaje de RN-02 y el porcentaje real.
- **Aprobado si:** se rechaza con el mensaje correcto. **Reprobado si:** se acepta una oferta por debajo del mínimo.

### 7.3 Propuesta con fallo de persistencia
- **Paso:** (prueba asistida) forzar un fallo de escritura en la base local durante el envío de una propuesta.
- **Resultado esperado:** la app muestra un error y no anuncia éxito; no quedan ofertas huérfanas sin trueque.
- **Aprobado si:** error visible y base consistente. **Reprobado si:** anuncia "¡Propuesta enviada exitosamente!" sin haber guardado. **(Ver D-05.)**

### 7.4 Contraoferta
- **Paso:** con la cuenta B, contraofertar la propuesta de A cambiando artículos o coins.
- **Resultado esperado:** la propuesta pasa a CONTRAOFERTADA y A ve los cambios.
- **Aprobado si:** el estado y los datos se actualizan para ambas partes. **Reprobado si:** no se actualiza o se pierde la propuesta original.

## 8. Aceptar trueque

### 8.1 Aceptación y bloqueo (RN-01)
- **Paso:** con la cuenta B, aceptar la propuesta de A.
- **Resultado esperado:** la propuesta pasa a RESERVADO, los artículos quedan bloqueados ("Reservado") y las demás propuestas en conflicto se cancelan o pausan.
- **Aprobado si:** estado RESERVADO, artículos bloqueados y conflictos resueltos. **Reprobado si:** se puede seguir ofreciendo un artículo ya reservado.

### 8.2 Cierre del trueque (COMPLETADO)
- **Paso:** completar el trueque (confirmación presencial / QR según el diseño) y revisar coins y contadores.
- **Resultado esperado:** el trueque llega a COMPLETADO, las coins se liquidan según lo acordado, los contadores de trueques de ambas cuentas suben en 1 y el escrow se libera.
- **Aprobado si:** cierre completo con liquidación correcta. **Reprobado si:** no existe el flujo de cierre o las coins no se mueven. **(Hoy no implementado, ver D-06.)**

### 8.3 Cancelación dentro de la ventana
- **Paso:** cancelar una propuesta dentro de la ventana permitida y luego intentar cancelar fuera de ella.
- **Resultado esperado:** dentro de la ventana se cancela; fuera, se rechaza con el motivo.
- **Aprobado si:** ambas respuestas son correctas. **Reprobado si:** se cancela fuera de ventana o no se puede cancelar dentro.

## 9. Chat

### 9.1 Chat de una propuesta
- **Paso:** con la cuenta A, abrir el chat de una propuesta activa y enviar un mensaje; con la cuenta B, leerlo y responder.
- **Resultado esperado:** los mensajes llegan en orden, persisten al cerrar y reabrir la app, y se marcan como leídos.
- **Aprobado si:** conversación completa y persistente. **Reprobado si:** mensajes perdidos, desordenados o que no llegan.

### 9.2 Control de acceso (RN-03)
- **Paso:** con una tercera cuenta C (que no es parte del trueque), intentar abrir o enviar mensajes en la conversación de A y B.
- **Resultado esperado:** acceso denegado, sin leer ni enviar.
- **Aprobado si:** C no puede entrar a la conversación. **Reprobado si:** C lee o escribe en ella. **(Bloqueador, ver D-04.)**

### 9.3 Chat sin sesión
- **Paso:** cerrar sesión y tratar de abrir cualquier conversación.
- **Resultado esperado:** no se puede chatear sin sesión; no se atribuyen mensajes a ninguna cuenta.
- **Aprobado si:** sin sesión no hay chat. **Reprobado si:** los mensajes se atribuyen a otra cuenta (por ejemplo, a la del administrador).

## 10. TruequeCoins

### 10.1 Saldo inicial y movimientos
- **Paso:** crear una cuenta nueva y revisar su saldo; luego proponer un trueque con coins compensatorias y revisar el saldo después.
- **Resultado esperado:** saldo inicial documentado como bono de bienvenida (no presentado como saldo ganado); al proponer, las coins se descuentan una sola vez y por el monto exacto.
- **Aprobado si:** saldos correctos y sin duplicados. **Reprobado si:** descuento doble, monto incorrecto o saldo negativo.

### 10.2 Calificación inicial
- **Paso:** crear una cuenta nueva y revisar su calificación visible.
- **Resultado esperado:** "sin calificaciones" o equivalente, no 5,0 estrellas sin haber hecho ningún trueque.
- **Aprobado si:** no muestra reputación inventada. **Reprobado si:** aparece con 5,0 estrellas de inicio. **(Ver D-07.)**

### 10.3 Saldo del administrador
- **Paso:** verificar que no exista ninguna cuenta con saldo de 100M TruequeCoins.
- **Resultado esperado:** ninguna cuenta tiene ese saldo.
- **Aprobado si:** no existe. **Reprobado si:** existe. **(Bloqueador, ver D-01.)**

## 11. Zona segura

### 11.1 Pantalla de puntos seguros
- **Paso:** abrir la sección de Puntos Seguros.
- **Resultado esperado:** lista de puntos seguros con ubicación, horarios y verificación por QR (o el alcance que se haya definido como implementado).
- **Aprobado si:** el módulo existe y funciona. **Reprobado si:** solo aparece el texto de placeholder "Módulo de Puntos Seguros con geofencing (Fase 3)". **(Hoy es placeholder, ver D-12.)**

### 11.2 Permisos de ubicación
- **Paso:** denegar el permiso de ubicación y usar la app.
- **Resultado esperado:** la app funciona sin ubicación (o pide el permiso solo cuando se necesita, con explicación en la app).
- **Aprobado si:** no exige ubicación para lo que no la necesita ni se cierra. **Reprobado si:** exige ubicación siempre o se cierra al denegarla.

## 12. Comportamiento sin conexión

### 12.1 Uso sin red
- **Paso:** activar el modo avión y recorrer: catálogo, mis publicaciones, trueques y chat.
- **Resultado esperado:** todo lo local funciona (catálogo, propuestas, mensajes guardados); lo que necesita red avisa que no hay conexión, sin cierres.
- **Aprobado si:** funciona lo local y avisa lo que no. **Reprobado si:** cierres, pantallas en blanco o éxito falso.

### 12.2 Recuperación de red
- **Paso:** con la red de vuelta, verificar que los datos locales siguen consistentes (sin duplicados por reintento).
- **Resultado esperado:** sin duplicados ni pérdidas.
- **Aprobado si:** consistencia mantenida. **Reprobado si:** duplicados o datos perdidos.

## 13. Borrado de cuenta y datos

### 13.1 Borrado de cuenta
- **Paso:** desde el perfil, solicitar el borrado de la cuenta de prueba.
- **Resultado esperado:** confirmación, borrado de la cuenta y de sus datos locales, y cierre de sesión.
- **Aprobado si:** los datos desaparecen. **Reprobado si:** no existe el flujo o los datos permanecen. **(Hoy no implementado, ver D-13.)**

---

## Bloqueadores que impiden el despliegue

Ningún despliegue (ni siquiera a pruebas cerradas) mientras alguno de estos siga abierto. Referencias a la auditoría del 10-nov-2026 ([AUDITORIA_TRUEQUEAPP_2026-11-10.md](AUDITORIA_TRUEQUEAPP_2026-11-10.md)):

| # | Bloqueador | Referencia |
|---|---|---|
| B1 | Cuenta de administrador sembrada en código, restablecida en cada apertura, con 100M TruequeCoins | D-01 |
| B2 | Contraseñas en texto plano, sin hash ni cifrado | D-02 |
| B3 | Credencial demo publicada en el repositorio público (Fase 5) | D-03 |
| B4 | Chat sin control de acceso (RN-03 no aplicada; identidad por defecto del administrador) | D-04 |
| B5 | Propuestas sin transacción y con éxito falso si falla la persistencia | D-05 |
| B6 | Sin flujo de cierre de trueque (sin COMPLETADO, sin escrow, sin liquidación) | D-06 |
| B7 | `targetSdk = 34` (Play exige 35 para apps nuevas) y Play Integrity no implementado | D-11 |
| B8 | `fallbackToDestructiveMigration`: un cambio de esquema borra todos los datos | D-09 |

**Nota:** los pasos 8.2, 11.1 y 13.1 corresponden a funcionalidad hoy no implementada; se mantienen en la lista para que el despliegue no se declare completo sin ellos o sin una decisión explícita de sacarlos del alcance.

