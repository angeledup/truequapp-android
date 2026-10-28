# Antigravity IDE Rule: Prohibición de Push a GitHub sin Autorización Explícita (Regla 1)

## 📌 Directiva Obligatoria
En este proyecto (`truequapp-android`), **está estrictamente prohibido ejecutar `git push` o enviar cambios a GitHub de forma autónoma**.

---

## 1. Alcance de la Regla
* Todo el trabajo de desarrollo, compilación, refactorización, pruebas unitarias y respaldos debe realizarse **estrictamente de forma local en la PC del usuario**.
* El asistente/agente **nunca** subirá ramas, tags ni commits a ningún repositorio remoto (`origin` / GitHub) por iniciativa propia.

## 2. Condición de Desbloqueo
* Solo se ejecutará `git push` cuando se cumplan **ambas** condiciones:
  1. El usuario haya revisado y aprobado localmente el progreso y los resultados de las pruebas.
  2. El usuario dé la orden explícita e inequívoca de subir los cambios al repositorio remoto en GitHub (ejemplo: *"sube el código a GitHub"*, *"sincroniza con GitHub"*).

