# 🚀 Truequapp Central Hub - Servidor de Sincronización y Persistencia

Servidor ligero y modular para la sincronización entre dispositivos de **Truequapp (Fase 2 / Beta)**.
Organizado de forma estricta y ordenada en dos repositorios de datos desacoplados:

```
server/
├── data/
│   ├── usuarios/           📁 Base de datos de Usuarios
│   │   └── usuarios.json   # Cuentas admin (chocolatt y Vergara), saldos TC (10,000) y reputación
│   └── trueques/           📁 Base de datos de Trueques
│       ├── catalogo.json   # Catálogo global compartido de artículos
│       ├── propuestas.json # Ofertas, contraofertas y bandera 'esta_activa'
│       └── chats/          # Historial de auditoría de mensajes por propuesta
├── api/
│   └── index.py            # Manejador Serverless para Vercel
├── server.py               # Servidor local ejecutable con 1 clic en la PC
├── vercel.json             # Configuración de despliegue en Vercel
└── README.md
```

---

## 💻 1. Ejecución Local en tu Computadora

Solo requieres Python (ya instalado en tu PC). Ejecuta en PowerShell:

```powershell
python server/server.py
```

El servidor iniciará en el puerto `8080` mostrando:
- **📱 Simulador Web Móvil en el Navegador:** `http://localhost:8080/app`
- **Tablero de Monitoreo Visual:** `http://localhost:8080/dashboard`
- **API Local:** `http://localhost:8080/api/`
- **Dirección para Teléfonos en la misma Red Wi-Fi:** `http://<TU_IP_LOCAL>:8080/api/` (ejemplo: `http://192.168.1.65:8080/api/`)

---

## ☁️ 2. Despliegue en la Nube con Vercel

Este servidor incluye `vercel.json` y `api/index.py` listos para funcionar sin modificar una sola línea de código:

1. **Instalar el CLI de Vercel (si no lo tienes):**
   ```bash
   npm i -g vercel
   ```
2. **Desplegar:**
   ```bash
   cd server
   vercel
   ```
3. Vercel te entregará una URL pública HTTPS (por ejemplo `https://truequapp-hub.vercel.app`), la cual puede ser configurada en la app Android para sincronizar usuarios y trueques desde cualquier red sin depender del Wi-Fi local.

---

## 📡 3. Endpoints Principales

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Estado del servidor |
| `GET` | `/api/usuarios` | Lista de usuarios (sin hashes de contraseña) |
| `GET` | `/api/usuarios/<email_o_user>` | Información de un usuario y saldo de TruequeCoins |
| `POST` | `/api/usuarios/login` | Login por correo o por nombre de usuario |
| `POST` | `/api/usuarios/coins` | Actualización de saldo de monedas TC |
| `GET` | `/api/catalogo` | Catálogo global de artículos |
| `POST` | `/api/catalogo` | Publicar nuevo artículo con fotos WebP |
| `GET` | `/api/trueques` | Listado de propuestas activas u ofertas |
| `POST` | `/api/trueques` | Crear propuesta de trueque |
| `PUT` | `/api/trueques/<id>/status` | Cambiar estado (Aceptar, Contraofertar, Rechazar, Cancelar) |
| `GET` | `/api/trueques/<id>/chat` | Historial de mensajes del chat |
| `POST` | `/api/trueques/<id>/chat` | Enviar mensaje en tiempo real |
| `GET` | `/api/room` | Estado y motor de Room SQLite en Localhost (`room_localhost.db`) |
| `GET` | `/api/rooms` | Listado de todas las salas de chat / propuestas de trueque |
| `GET` | `/api/rooms/<id>` | Detalle y mensajes de una sala de chat específica |
| `POST` | `/api/rooms` | Crear una nueva sala de trueque con mensaje inicial opcional |

---

## 🗄️ 4. Base de Datos Room SQLite Local (`room_localhost.db`)

Para pruebas 100% locales sin conexión a internet ni a Turso en la nube, el servidor genera automáticamente `server/data/room_localhost.db` con el esquema idéntico de **Android Room**:
- `usuarios`: Cuentas, credenciales, saldo TC y reputación.
- `articulos_trueque`: Catálogo con imágenes y categorías.
- `propuestas_trueque`: Propuestas con artículos y compensación.
- `chat_mensajes`: Historial de mensajes auditados por trueque (RN-03).
- `safe_zones`: Puntos seguros de Coatzacoalcos (CETIS 79, Malecón, etc.).

Cada cambio en la API o archivos JSON se sincroniza de forma automática e inmediata con `room_localhost.db`.

