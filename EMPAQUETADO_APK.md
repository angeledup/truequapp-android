## 1. Comando para generar el APK
Para compilar y empaquetar el APK en modo depuración (Debug), ejecuta en tu terminal PowerShell directamente desde la raíz del proyecto:

```powershell
.\gradlew.bat assembleDebug
```
> **Nota**: Usamos `gradlew.bat` porque ya tiene configurada automáticamente la detección del JDK 17 y el Android SDK de tu equipo.

---

## 2. Ubicación del APK Generado
Una vez completada la compilación, encontrarás el APK en:
- `app\build\outputs\apk\debug\app-debug.apk`

---

## 3. ¿Cómo probarlo en tu teléfono o emulador?
Tienes dos opciones para instalarlo:

### Opción A: Mediante cable USB / Emulador (ADB)
Con la depuración USB habilitada en tu teléfono o tu emulador abierto, ejecuta:

```powershell
.\gradlew.bat installDebug
```

O directamente con `adb`:
```powershell
$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

### Opción B: Copia directa al teléfono
Copia el archivo `app-debug.apk` a la memoria de tu teléfono (vía WhatsApp, Drive, cable o explorador de archivos).
Toca el archivo en tu teléfono y presiona **Instalar**.

---

## 3. Lista de Verificación y Pruebas
Una vez instalado el APK, sigue los pasos de prueba funcional documentados en:
* [VERIFICACION_FUNCIONAL_DESPLIEGUE.md](VERIFICACION_FUNCIONAL_DESPLIEGUE.md)