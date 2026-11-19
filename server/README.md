# 🚀 Truequapp Central Cloud Hub - Servidor de Sincronización y Persistencia

Servidor modular para la sincronización entre dispositivos de **Truequapp (Fase 2 / Beta)** desplegado en **Vercel** (`https://truequapp-pi.vercel.app`) y respaldado por la base de datos **Turso (libSQL Cloud SQLite)**.

Organizado de forma estricta y ordenada en repositorios de datos desacoplados:

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
├── server.py               # Servidor API Hub compatible con Vercel
├── turso_client.py         # Cliente nativo HTTP Pipeline para Turso (libSQL)
├── vercel.json             # Configuración de despliegue en Vercel
└── README.md
```

---

## ☁️ 1. Infraestructura Cloud (Vercel + Turso)

El backend opera 100% en la nube:
- **Servidor API:** Vercel Serverless Functions (`https://truequapp-pi.vercel.app`).
- **Base de Datos Cloud:** Turso Database (SQLite distribuido de alta velocidad y baja latencia).
- La app Android (`com.truequapp`) se conecta directamente a la nube mediante HTTPS, permitiendo acceso multi-dispositivo sin requerir conexión a redes locales.

---

## 📡 2. Endpoints Principales

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Estado del servidor en Vercel |
| `GET` | `/api/status` | Estado de salud y conectividad con Turso |
| `GET` | `/api/usuarios` | Lista de usuarios (sin hashes de contraseña) |
| `GET` | `/api/usuarios/<email_o_user>` | Información de un usuario y saldo de TruequeCoins |
| `POST` | `/api/usuarios/login` | Login por correo o por nombre de usuario |
| `POST` | `/api/usuarios/sync` | Registro o sincronización de cuenta de usuario |
| `POST` | `/api/usuarios/coins` | Actualización de saldo de monedas TC |
| `GET` | `/api/catalogo` | Catálogo global de artículos |
| `POST` | `/api/catalogo` | Publicar nuevo artículo con fotos WebP |
| `DELETE`| `/api/catalogo/<id>` | Eliminar publicación del catálogo |
| `GET` | `/api/trueques` | Listado de propuestas activas u ofertas |
| `POST` | `/api/trueques` | Crear propuesta de trueque |
| `PUT` | `/api/trueques/<id>/status` | Cambiar estado (Aceptar, Contraofertar, Rechazar, Cancelar) |
| `GET` | `/api/trueques/<id>/chat` | Historial de mensajes del chat |
| `POST` | `/api/trueques/<id>/chat` | Enviar mensaje en tiempo real |
| `GET` | `/api/rooms` | Listado de todas las salas de chat / propuestas de trueque |
| `GET` | `/api/rooms/<id>` | Detalle y mensajes de una sala de chat específica |
| `POST` | `/api/rooms` | Crear una nueva sala de trueque con mensaje inicial opcional |


