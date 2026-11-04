# 📦 Módulo 2: Catálogo y Pipeline Nativo WebP (`feature:catalog`)

Este directorio contiene la implementación completa, probada y modularizada del **Módulo 2** de la Fase 2 (Truequapp Android), desarrollado para el trabajo colaborativo en equipo sobre la versión base `v2.1` (`main`).

---

## 📂 Estructura del Módulo 2

```text
modulo 2/
├── README.md                               <-- Este documento descriptivo
├── build.gradle.kts                        <-- Configuración Gradle del módulo
├── consumer-rules.pro                      <-- Reglas de ProGuard para el consumidor
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/com/truequapp/feature/catalog/
    │       ├── compressor/
    │       │   └── WebPImageCompressor.kt  <-- Pipeline de compresión nativo WebP (< 80 KB)
    │       ├── mvi/
    │       │   ├── CatalogContract.kt      <-- Contrato MVI (State, Events, Effects)
    │       │   └── CatalogViewModel.kt     <-- Lógica reactiva, paginación y moderación RN-11
    │       ├── storage/
    │       │   └── CatalogLocalStorage.kt  <-- Persistencia en sandbox local (JSON)
    │       └── ui/
    │           ├── CatalogFeedScreen.kt    <-- Feed marketplace en cuadrícula con filtros
    │           ├── ItemCard.kt             <-- Tarjeta con Coil, badge multi-foto y distancia
    │           └── ItemPublishScreen.kt    <-- Selector nativo (hasta 6 fotos) y formulario
    └── test/
        └── java/com/truequapp/feature/catalog/
            ├── CatalogViewModelTest.kt     <-- Tests unitarios de ViewModel y RN-11
            └── WebPImageCompressorTest.kt  <-- Tests de sub-muestreo inteligente
```

---

## 🚀 Componentes Implementados

### 1. Pipeline Nativo de Compresión WebP (`WebPImageCompressor.kt`)
* **Corrutinas en `Dispatchers.Default`:** Procesa las imágenes sin bloquear el hilo principal de Compose.
* **Sub-muestreo inteligente (`inSampleSize`):** Inspecciona dimensiones con `inJustDecodeBounds = true` y aplica escala en potencias de 2 (`||`) para proteger la memoria heap contra `OutOfMemoryError`.
* **Limpieza de metadatos EXIF:** Remueve atributos de privacidad con `ExifInterface`:
  * Coordenadas GPS (`TAG_GPS_LATITUDE`, `TAG_GPS_LONGITUDE`, `TAG_GPS_ALTITUDE`).
  * Atributos de fecha de captura (`TAG_DATETIME`, `TAG_DATETIME_ORIGINAL`).
* **Bucle dinámico de calidad lossy:** Comprime iterando calidades (`80 → 70 → 60 → 50...`) y reescalando dimensiones hasta asegurar estrictamente un peso **$< 80\text{ KB}$** ($< 81,920$ bytes).
* **Procesamiento por lote:** Permite comprimir hasta 6 fotos simultáneamente vía `compressMultipleUris()`.

### 2. Selector Nativo de Imágenes (`ItemPublishScreen.kt`)
* Emplea `rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6))`.
* **Cumplimiento Google Play:** Accede a la galería nativa de Android sin solicitar permisos invasivos de almacenamiento (`READ_MEDIA_IMAGES`).
* **Galería interactiva:** Muestra miniaturas en carrusel con badge **"Principal"** para la primera foto y botón **"X"** para eliminar imágenes individuales.

### 3. Formulario de Publicación y Reglas de Negocio
* **Taxonomía:** Selector con chips de categorías de alta liquidez.
* **Condición física:** Selector con `ConditionTag` (`NUEVO`, `COMO_NUEVO`, `BUEN_ESTADO`, `USADO`).
* **Rango de valor (RN-02):** Inputs de Min/Max MXN para garantizar trueques con equivalencia económica razonable.
* **Wishlist:** Etiquetas dinámicas interactivas.
* **Moderación RN-11:** Ejecuta `ValidateProhibitedItemUseCase` para rechazar armas, drogas o divisas ilícitas.

### 4. Marketplace Feed Paginado con Coil (`CatalogFeedScreen.kt` + `ItemCard.kt`)
* **`LazyVerticalGrid`:** Cuadrícula de 2 columnas con scroll infinito y paginación incremental (`pageSize = 10`, `LoadNextPage`).
* **`ItemCard`:** Integra **Coil** (`SubcomposeAsyncImage`) para carga asíncrona de WebP, badge de fotos múltiples (`📷 X`) y distancia en km (`📍 X.X km`).
* **Filtros avanzados:** Distancia geográfica (1 a 50 km), categorías, condición física, rango de valor, y pestañas de "Mis publicaciones" y "Favoritos".

---

## 🧪 Pruebas Unitarias Aprobadas
* `WebPImageCompressorTest.kt`: 100% aprobado.
* `CatalogViewModelTest.kt`: 100% aprobado (14 tests).
* Suite global del proyecto: 223 tareas ejecutadas exitosamente con cero errores (`BUILD SUCCESSFUL`).
