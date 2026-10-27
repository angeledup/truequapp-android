3. ⏪ ¿Cómo volver a una versión anterior? (Procedimiento de Rollback)
Si en algún momento deseas regresar el código a una versión previa, tienes 3 formas según lo que necesites hacer:

Opción A: Solo inspeccionar o compilar la versión previa (Sin alterar el historial)
Para ver el código tal como estaba en v0.1 en tu editor:

powershell
# git checkout tags/v0.1
(Para regresar después a la versión actual: git checkout main).

Opción B: Crear una nueva rama de trabajo desde la versión previa
Si quieres experimentar o corregir algo partiendo de v0.1 sin tocar main:

powershell
# git switch -C restauracion-v0.1 tags/v0.1
Opción C: Rollback Definitivo en main (Restauración de Emergencia)
Si la versión actual tuviera un fallo grave y deseas descartar v0.2 y dejar main permanentemente en v0.1:

powershell
 -1. Forzar la rama main al commit de v0.1
# git reset --hard tags/v0.1
 -2. Si ya se había subido a GitHub, sincronizar el remoto (con precaución)
# git push origin main --force

Todo el mecanismo está probado y listo para usarse cuando lo requieras.