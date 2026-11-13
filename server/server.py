#!/usr/bin/env python3
"""
Truequapp Backend Server - Central Data Hub & Vercel-ready API
==============================================================
Servidor ligero y modular para la sincronización de Truequapp (Beta / Fase 2).
Organización de almacenamiento estricta en 2 carpetas principales:
  - server/data/usuarios/ -> Base de datos y gestión de cuentas, saldos TC y reputación.
  - server/data/trueques/ -> Base de datos de propuestas, catálogo global y chats de auditoría.

Puede ejecutarse:
1. De forma local en la PC: `python server/server.py [puerto]` (por defecto 8080).
2. En la nube mediante Vercel Serverless Functions (`server/api/index.py`).
"""

import json
import os
import sys
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

try:
    import turso_client
except ImportError:
    try:
        from server import turso_client
    except ImportError:
        turso_client = None

try:
    import room_sqlite
except ImportError:
    try:
        from server import room_sqlite
    except ImportError:
        room_sqlite = None

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data")
USUARIOS_DIR = os.path.join(DATA_DIR, "usuarios")
TRUEQUES_DIR = os.path.join(DATA_DIR, "trueques")
CHATS_DIR = os.path.join(TRUEQUES_DIR, "chats")

USUARIOS_FILE = os.path.join(USUARIOS_DIR, "usuarios.json")
CATALOGO_FILE = os.path.join(TRUEQUES_DIR, "catalogo.json")
PROPUESTAS_FILE = os.path.join(TRUEQUES_DIR, "propuestas.json")

# Bloqueo de concurrencia para operaciones ACID sobre archivos JSON
db_lock = threading.Lock()

def init_storage():
    """Asegura la creación de las dos carpetas de almacenamiento y archivos iniciales."""
    try:
        os.makedirs(USUARIOS_DIR, exist_ok=True)
        os.makedirs(CHATS_DIR, exist_ok=True)

        if not os.path.exists(USUARIOS_FILE):
            with open(USUARIOS_FILE, "w", encoding="utf-8") as f:
                json.dump([], f, indent=2)

        if not os.path.exists(CATALOGO_FILE):
            with open(CATALOGO_FILE, "w", encoding="utf-8") as f:
                json.dump([], f, indent=2)

        if not os.path.exists(PROPUESTAS_FILE):
            with open(PROPUESTAS_FILE, "w", encoding="utf-8") as f:
                json.dump([], f, indent=2)
    except Exception:
        pass

    # Inicializar Base de Datos SQLite Room Localhost
    if room_sqlite:
        try:
            room_sqlite.init_room_database()
        except Exception as e:
            print(f"[Room Warning] Error al inicializar Room SQLite: {e}")

    # Si Turso está configurado vía variables de entorno, inicializar y sembrar
    if turso_client and turso_client.is_turso_configured():
        try:
            turso_client.init_turso_tables()
            u_data = []
            if os.path.exists(USUARIOS_FILE):
                with open(USUARIOS_FILE, "r", encoding="utf-8") as f:
                    u_data = json.load(f)
            c_data = []
            if os.path.exists(CATALOGO_FILE):
                with open(CATALOGO_FILE, "r", encoding="utf-8") as f:
                    c_data = json.load(f)
            p_data = []
            if os.path.exists(PROPUESTAS_FILE):
                with open(PROPUESTAS_FILE, "r", encoding="utf-8") as f:
                    p_data = json.load(f)
            chats_dict = {}
            if os.path.exists(CHATS_DIR):
                for fname in os.listdir(CHATS_DIR):
                    if fname.endswith(".json"):
                        pid = fname[:-5]
                        with open(os.path.join(CHATS_DIR, fname), "r", encoding="utf-8") as f:
                            chats_dict[pid] = json.load(f)
            turso_client.sync_local_data_to_turso(u_data, c_data, p_data, chats_dict)
        except Exception as e:
            print(f"[Turso Warning] Error al inicializar/sincronizar Turso: {e}")

def read_json_file(file_path):
    if turso_client and turso_client.is_turso_configured():
        try:
            if os.path.abspath(file_path) == os.path.abspath(USUARIOS_FILE):
                return turso_client.turso_read_table("usuarios")
            if os.path.abspath(file_path) == os.path.abspath(CATALOGO_FILE):
                return turso_client.turso_read_table("catalogo")
            if os.path.abspath(file_path) == os.path.abspath(PROPUESTAS_FILE):
                return turso_client.turso_read_table("propuestas")
            if CHATS_DIR in os.path.abspath(file_path):
                pid = os.path.splitext(os.path.basename(file_path))[0]
                return turso_client.turso_read_table("chats", prop_id=pid)
        except Exception as e:
            print(f"[Turso Warning] Fallback a local tras error de lectura: {e}")

    with db_lock:
        if not os.path.exists(file_path):
            return []
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return []

def write_json_file(file_path, data):
    if turso_client and turso_client.is_turso_configured():
        try:
            if os.path.abspath(file_path) == os.path.abspath(USUARIOS_FILE):
                turso_client.turso_save_table("usuarios", data)
            elif os.path.abspath(file_path) == os.path.abspath(CATALOGO_FILE):
                turso_client.turso_save_table("catalogo", data)
            elif os.path.abspath(file_path) == os.path.abspath(PROPUESTAS_FILE):
                turso_client.turso_save_table("propuestas", data)
            elif CHATS_DIR in os.path.abspath(file_path):
                pid = os.path.splitext(os.path.basename(file_path))[0]
                turso_client.turso_save_table("chats", data, prop_id=pid)
        except Exception as e:
            print(f"[Turso Warning] Error al persistir en Turso: {e}")

    with db_lock:
        try:
            os.makedirs(os.path.dirname(file_path), exist_ok=True)
            with open(file_path, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            if room_sqlite:
                try:
                    room_sqlite.sync_json_to_sqlite()
                except Exception:
                    pass
        except Exception:
            pass

class TruequappRequestHandler(BaseHTTPRequestHandler):

    def _set_headers(self, status=200, content_type="application/json"):
        self.send_response(status)
        self.send_header("Content-Type", f"{content_type}; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.end_headers()

    def do_OPTIONS(self):
        self._set_headers(200)

    def _read_body(self):
        content_length = int(self.headers.get("Content-Length", 0))
        if content_length == 0:
            return {}
        raw_body = self.rfile.read(content_length).decode("utf-8")
        try:
            return json.loads(raw_body)
        except Exception:
            return {}

    def _send_json(self, data, status=200):
        self._set_headers(status, "application/json")
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode("utf-8"))

    def do_GET(self):
        parsed = urlparse(self.path)
        query = parse_qs(parsed.query)
        v_path = query.get("__path__", [None])[0]
        if v_path is not None:
            path = "/" + v_path.lstrip("/")
        else:
            raw_path = self.headers.get("x-matched-path") or self.headers.get("x-vercel-matched-path") or parsed.path
            path = raw_path
            if path.startswith("/api/index.py"):
                path = path[len("/api/index.py"):] or "/"

        # 1. Health Check
        if path == "/api/health" or path == "/health":
            self._send_json({"status": "ok", "service": "Truequapp API Hub", "version": "1.0.0"})
            return

        # 2. Base de datos de Usuarios
        if path == "/api/usuarios" or path == "/api/usuarios/":
            users = read_json_file(USUARIOS_FILE)
            # Ocultar hashes de contraseñas por seguridad en lecturas públicas
            safe_users = [{k: v for k, v in u.items() if k != "passwordHash"} for u in users]
            self._send_json(safe_users)
            return

        if path.startswith("/api/usuarios/"):
            identifier = path.replace("/api/usuarios/", "").strip().lower()
            users = read_json_file(USUARIOS_FILE)
            user = next((u for u in users if u.get("email", "").lower() == identifier or u.get("name", "").lower() == identifier), None)
            if user:
                safe_user = {k: v for k, v in user.items() if k != "passwordHash"}
                self._send_json(safe_user)
            else:
                self._send_json({"error": "Usuario no encontrado"}, 404)
            return

        # 3. Base de datos de Trueques: Catálogo de Artículos
        if path == "/api/catalogo" or path == "/api/catalogo/":
            items = read_json_file(CATALOGO_FILE)
            user_filter = query.get("user", [None])[0]
            if user_filter:
                u_lower = user_filter.lower()
                items = [it for it in items if it.get("ownerId", "").lower() == u_lower or it.get("ownerEmail", "").lower() == u_lower]
            self._send_json(items)
            return

        # 4. Base de datos de Trueques: Propuestas y Ofertas
        if path == "/api/trueques" or path == "/api/trueques/":
            proposals = read_json_file(PROPUESTAS_FILE)
            user_filter = query.get("user", [None])[0]
            only_active = query.get("activas", ["false"])[0].lower() == "true"
            if user_filter:
                u_lower = user_filter.lower()
                proposals = [p for p in proposals if p.get("initiatorUserId", "").lower() == u_lower or p.get("receiverUserId", "").lower() == u_lower or p.get("initiatorEmail", "").lower() == u_lower or p.get("receiverEmail", "").lower() == u_lower]
            if only_active:
                proposals = [p for p in proposals if p.get("esta_activa", True)]
            self._send_json(proposals)
            return

        # 5. Chats de Trueques
        if path.startswith("/api/trueques/") and path.endswith("/chat"):
            parts = path.strip("/").split("/")
            if len(parts) == 4:
                prop_id = parts[2]
                chat_file = os.path.join(CHATS_DIR, f"{prop_id}.json")
                messages = read_json_file(chat_file)
                self._send_json(messages)
                return

        # 5.1 Endpoint para Salas de Chat (Rooms) en Localhost
        if path == "/api/rooms" or path == "/api/rooms/":
            proposals = read_json_file(PROPUESTAS_FILE)
            rooms = []
            for p in proposals:
                pid = p.get("id")
                chat_file = os.path.join(CHATS_DIR, f"{pid}.json")
                msgs = read_json_file(chat_file) if os.path.exists(chat_file) else []
                rooms.append({
                    "roomId": pid,
                    "proposal": p,
                    "totalMessages": len(msgs),
                    "lastMessage": msgs[-1] if msgs else None
                })
            self._send_json(rooms)
            return

        if path.startswith("/api/rooms/"):
            room_id = path.replace("/api/rooms/", "").strip()
            proposals = read_json_file(PROPUESTAS_FILE)
            prop = next((p for p in proposals if p.get("id") == room_id), None)
            chat_file = os.path.join(CHATS_DIR, f"{room_id}.json")
            msgs = read_json_file(chat_file) if os.path.exists(chat_file) else []
            self._send_json({
                "roomId": room_id,
                "proposal": prop,
                "messages": msgs
            })
            return

        # 5.2 Estado de Room Localhost SQLite
        if path == "/api/room" or path == "/api/room/status":
            room_db_path = os.path.join(DATA_DIR, "room_localhost.db")
            db_exists = os.path.exists(room_db_path)
            self._send_json({
                "status": "ok",
                "roomDatabase": "room_localhost.db",
                "exists": db_exists,
                "path": room_db_path,
                "engine": "SQLite 3 (Android Room Schema)",
                "endpoints": {
                    "rooms": "/api/rooms",
                    "usuarios": "/api/usuarios",
                    "catalogo": "/api/catalogo",
                    "trueques": "/api/trueques"
                }
            })
            return

        # 6. Tablero Visual HTML de Monitoreo Local
        if path == "/" or path == "/dashboard":
            self._render_dashboard()
            return

        # 6.1 Simulador Móvil Interactivo en el Navegador
        if path == "/app" or path == "/simulator" or path == "/web":
            self._render_web_app()
            return

        self._send_json({"error": f"Ruta GET {path} no encontrada"}, 404)

    def do_POST(self):
        parsed = urlparse(self.path)
        query = parse_qs(parsed.query)
        v_path = query.get("__path__", [None])[0]
        if v_path is not None:
            path = "/" + v_path.lstrip("/")
        else:
            raw_path = self.headers.get("x-matched-path") or self.headers.get("x-vercel-matched-path") or parsed.path
            path = raw_path
            if path.startswith("/api/index.py"):
                path = path[len("/api/index.py"):] or "/"
        body = self._read_body()

        # 1. Login / Autenticación de Usuario (por correo o por nombre de usuario)
        if path == "/api/usuarios/login":
            identifier = body.get("identifier", "").strip().lower()
            password = body.get("password", "")
            users = read_json_file(USUARIOS_FILE)

            user = next((u for u in users if (u.get("email", "").lower() == identifier or u.get("name", "").lower() == identifier) and u.get("passwordHash") == password), None)
            if user:
                safe_user = {k: v for k, v in user.items() if k != "passwordHash"}
                self._send_json({"success": True, "user": safe_user})
            else:
                self._send_json({"success": False, "error": "Credenciales inválidas"}, 401)
            return

        # 2. Registrar o Sincronizar Usuario
        if path == "/api/usuarios/sync" or path == "/api/usuarios":
            users = read_json_file(USUARIOS_FILE)
            email = body.get("email", "").strip().lower()
            name = body.get("name", "").strip()

            existing_idx = next((i for i, u in enumerate(users) if u.get("email", "").lower() == email), -1)
            if existing_idx >= 0:
                users[existing_idx].update(body)
            else:
                body.setdefault("truequeCoins", 10000.0)
                body.setdefault("bayesianRating", 4.8)
                body.setdefault("completedTrades", 0)
                users.append(body)

            write_json_file(USUARIOS_FILE, users)
            self._send_json({"success": True, "message": "Usuario sincronizado correctamente"})
            return

        # 3. Actualización de Monedas (TruequeCoins)
        if path == "/api/usuarios/coins":
            identifier = body.get("identifier", "").strip().lower()
            amount = float(body.get("amount", 0))
            operation = body.get("operation", "add") # "add", "reduce", "set"

            users = read_json_file(USUARIOS_FILE)
            user = next((u for u in users if u.get("email", "").lower() == identifier or u.get("name", "").lower() == identifier or u.get("id", "").lower() == identifier), None)

            if not user:
                self._send_json({"error": "Usuario no encontrado"}, 404)
                return

            current = float(user.get("truequeCoins", 10000.0))
            if operation == "add":
                user["truequeCoins"] = current + amount
            elif operation == "reduce":
                user["truequeCoins"] = max(0.0, current - amount)
            elif operation == "set":
                user["truequeCoins"] = amount

            write_json_file(USUARIOS_FILE, users)
            self._send_json({"success": True, "newBalance": user["truequeCoins"]})
            return

        # 4. Publicar Artículo en Catálogo
        if path == "/api/catalogo":
            items = read_json_file(CATALOGO_FILE)
            item_id = body.get("id") or f"item_{int(threading.current_thread().ident)}_{len(items)+1}"
            body["id"] = item_id
            body.setdefault("status", "DISPONIBLE")

            # Evitar duplicados por id
            items = [it for it in items if it.get("id") != item_id]
            items.insert(0, body)
            write_json_file(CATALOGO_FILE, items)
            self._send_json({"success": True, "item": body}, 201)
            return

        # 5. Crear Propuesta de Trueque
        if path == "/api/trueques":
            initiator = body.get("initiatorUserId", "").lower()
            receiver = body.get("receiverUserId", "").lower()

            if initiator == receiver and initiator != "":
                self._send_json({"error": "Anti-auto-trueque: No puedes intercambiar contigo mismo"}, 400)
                return

            proposals = read_json_file(PROPUESTAS_FILE)
            prop_id = body.get("id") or f"prop_{len(proposals)+1}"
            body["id"] = prop_id
            body["esta_activa"] = True
            body.setdefault("status", "PROPUESTA_ENVIADA")

            proposals.insert(0, body)
            write_json_file(PROPUESTAS_FILE, proposals)
            self._send_json({"success": True, "proposal": body}, 201)
            return

        # 6. Enviar Mensaje a Chat de Trueque
        if path.startswith("/api/trueques/") and path.endswith("/chat"):
            parts = path.strip("/").split("/")
            if len(parts) == 4:
                prop_id = parts[2]
                chat_file = os.path.join(CHATS_DIR, f"{prop_id}.json")
                messages = read_json_file(chat_file)

                msg_id = body.get("id") or f"msg_{len(messages)+1}"
                body["id"] = msg_id
                body["proposalId"] = prop_id
                body.setdefault("timestampEpochMs", int(os.times().system * 1000))
                messages.append(body)

                write_json_file(chat_file, messages)
                self._send_json({"success": True, "message": body}, 201)
                return

        # 7. Crear o Registrar Room (Sala de Chat / Propuesta de Trueque)
        if path == "/api/rooms" or path == "/api/rooms/":
            initiator = body.get("initiatorUserId", "").lower()
            receiver = body.get("receiverUserId", "").lower()
            proposals = read_json_file(PROPUESTAS_FILE)
            prop_id = body.get("id") or body.get("roomId") or f"prop_local_{len(proposals)+1}"
            body["id"] = prop_id
            body["esta_activa"] = True
            body.setdefault("status", "PENDIENTE")
            proposals.insert(0, body)
            write_json_file(PROPUESTAS_FILE, proposals)

            initial_msg = body.get("initialMessage")
            if initial_msg:
                chat_file = os.path.join(CHATS_DIR, f"{prop_id}.json")
                msg_entry = {
                    "id": f"msg_{prop_id}_1",
                    "proposalId": prop_id,
                    "truequeId": prop_id,
                    "senderId": body.get("initiatorEmail", initiator),
                    "emisorCorreo": body.get("initiatorEmail", initiator),
                    "receptorCorreo": body.get("receiverEmail", receiver),
                    "content": initial_msg,
                    "contenido": initial_msg,
                    "timestampEpochMs": int(os.times().system * 1000),
                    "estaEntregado": True,
                    "estaLeido": False
                }
                write_json_file(chat_file, [msg_entry])

            self._send_json({"success": True, "roomId": prop_id, "room": body}, 201)
            return

        self._send_json({"error": f"Ruta POST {path} no encontrada"}, 404)

    def do_PUT(self):
        parsed = urlparse(self.path)
        path = parsed.path
        body = self._read_body()

        # Actualizar estado de una propuesta (Aceptar, Contraofertar, Rechazar, Cancelar)
        if path.startswith("/api/trueques/") and path.endswith("/status"):
            parts = path.strip("/").split("/")
            if len(parts) == 4:
                prop_id = parts[2]
                new_status = body.get("status")
                proposals = read_json_file(PROPUESTAS_FILE)
                target_prop = next((p for p in proposals if p.get("id") == prop_id), None)

                if not target_prop:
                    self._send_json({"error": "Propuesta no encontrada"}, 404)
                    return

                target_prop["status"] = new_status
                # Las propuestas rechazadas o canceladas desactivan su bandera activa
                if new_status in ["RECHAZADO", "CANCELADO", "COMPLETADO"]:
                    target_prop["esta_activa"] = False
                elif new_status in ["PROPUESTA_ENVIADA", "CONTRAOFERTADA", "ACEPTADO", "EN_ENTREGA"]:
                    target_prop["esta_activa"] = True

                write_json_file(PROPUESTAS_FILE, proposals)
                self._send_json({"success": True, "proposal": target_prop})
                return

        self._send_json({"error": f"Ruta PUT {path} no encontrada"}, 404)

    def do_DELETE(self):
        parsed = urlparse(self.path)
        query = parse_qs(parsed.query)
        v_path = query.get("__path__", [None])[0]
        if v_path is not None:
            path = "/" + v_path.lstrip("/")
        else:
            raw_path = self.headers.get("x-matched-path") or self.headers.get("x-vercel-matched-path") or parsed.path
            path = raw_path
            if path.startswith("/api/index.py"):
                path = path[len("/api/index.py"):] or "/"

        item_id = None
        if path.startswith("/api/catalogo/"):
            item_id = path[len("/api/catalogo/"):].strip()
        elif path == "/api/catalogo" or path == "/api/catalogo/":
            item_id = query.get("id", [None])[0]

        if item_id:
            items = read_json_file(CATALOGO_FILE)
            if item_id in ["all", "*"]:
                items = []
                if turso_client and turso_client.is_turso_configured():
                    try:
                        turso_client.execute_pipeline([{"sql": "DELETE FROM catalogo;"}])
                    except Exception as e:
                        print(f"[Turso Warning] Error al eliminar catálogo: {e}")
            else:
                items = [it for it in items if it.get("id") != item_id]
                if turso_client and turso_client.is_turso_configured():
                    try:
                        turso_client.turso_delete_item("catalogo", item_id)
                    except Exception as e:
                        print(f"[Turso Warning] Error al eliminar de Turso: {e}")
            write_json_file(CATALOGO_FILE, items)
            self._send_json({"success": True, "deletedId": item_id})
            return

        prop_id = None
        if path.startswith("/api/trueques/"):
            prop_id = path[len("/api/trueques/"):].strip()
        elif path == "/api/trueques" or path == "/api/trueques/":
            prop_id = query.get("id", [None])[0]

        if prop_id:
            proposals = read_json_file(PROPUESTAS_FILE)
            if prop_id in ["all", "*"]:
                proposals = []
                if turso_client and turso_client.is_turso_configured():
                    try:
                        turso_client.execute_pipeline([
                            {"sql": "DELETE FROM propuestas;"},
                            {"sql": "DELETE FROM chats;"}
                        ])
                    except Exception as e:
                        print(f"[Turso Warning] Error al eliminar propuestas: {e}")
            else:
                proposals = [p for p in proposals if p.get("id") != prop_id]
                if turso_client and turso_client.is_turso_configured():
                    try:
                        turso_client.turso_delete_item("propuestas", prop_id)
                        turso_client.execute_pipeline([{"sql": "DELETE FROM chats WHERE propuesta_id = ?;", "args": [prop_id]}])
                    except Exception as e:
                        print(f"[Turso Warning] Error al eliminar de Turso: {e}")
            write_json_file(PROPUESTAS_FILE, proposals)
            self._send_json({"success": True, "deletedProposalId": prop_id})
            return

        self._send_json({"error": f"Ruta DELETE {path} no encontrada"}, 404)

    def _render_dashboard(self):
        """Genera el tablero HTML para monitorear el servidor desde la computadora."""
        users = read_json_file(USUARIOS_FILE)
        proposals = read_json_file(PROPUESTAS_FILE)
        items = read_json_file(CATALOGO_FILE)

        html = f"""<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Truequapp Central Hub - Servidor de Sincronización</title>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #121214; color: #F6F6F8; margin: 0; padding: 24px; }}
        h1 {{ color: #E5B869; margin-bottom: 4px; }}
        .header {{ display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #2A2A30; padding-bottom: 16px; margin-bottom: 24px; }}
        .badge {{ background: #212126; color: #E5B869; border: 1px solid #E5B869; padding: 4px 12px; border-radius: 999px; font-weight: bold; font-size: 13px; }}
        .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px; }}
        .card {{ background: #1A1A1E; border: 1px solid #2A2A30; border-radius: 14px; padding: 18px; }}
        .card h2 {{ font-size: 16px; color: #E5B869; margin-top: 0; border-bottom: 1px solid #2A2A30; padding-bottom: 8px; }}
        .item {{ padding: 8px 0; border-bottom: 1px solid #222228; font-size: 13px; }}
        .item:last-child {{ border-bottom: none; }}
        .folder-tag {{ font-size: 11px; color: #8C7A6B; text-transform: uppercase; letter-spacing: 0.05em; }}
        .stat {{ font-size: 24px; font-weight: bold; color: #F0C987; }}
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1>Truequapp Central Server</h1>
            <div class="folder-tag">Servidor Local & Preparado para Vercel</div>
        </div>
        <div style="display:flex; align-items:center; gap:10px;">
            <a href="/app" style="background:#E5B869; color:#121214; padding:8px 16px; border-radius:8px; text-decoration:none; font-weight:bold; font-size:13px; display:inline-flex; align-items:center; gap:6px;">📱 Abrir App en Navegador</a>
            <div class="badge">● Activo (Port 8080)</div>
        </div>
    </div>

    <div class="grid">
        <div class="card">
            <span class="folder-tag">server/data/usuarios/</span>
            <h2>Usuarios Registrados ({len(users)})</h2>
            {"".join(f'''<div class="item">
                <strong>{u.get("name")}</strong> ({u.get("email")})<br/>
                <span style="color:#E5B869;">💰 {u.get("truequeCoins")} TC</span> • ⭐ {u.get("bayesianRating")} • {u.get("city")}
            </div>''' for u in users)}
        </div>

        <div class="card">
            <span class="folder-tag">server/data/trueques/</span>
            <h2>Propuestas de Trueque ({len(proposals)})</h2>
            {"".join(f'''<div class="item">
                <strong>{p.get("id")}</strong>: {p.get("initiatorUserId")} ➔ {p.get("receiverUserId")}<br/>
                Estado: <span style="color:#F0C987;">{p.get("status")}</span> • Activa: {p.get("esta_activa")}<br/>
                Compensación: {p.get("monetaryCompensationCoins")} TC
            </div>''' for p in proposals) if proposals else '<div class="item">Sin propuestas activas.</div>'}
        </div>

        <div class="card">
            <span class="folder-tag">server/data/trueques/catalogo.json</span>
            <h2>Artículos en Catálogo ({len(items)})</h2>
            {"".join(f'''<div class="item">
                <strong>{it.get("title")}</strong> ({it.get("condition")})<br/>
                Valor: ${it.get("estimatedValueMin")} - ${it.get("estimatedValueMax")} • Dueño: {it.get("ownerName", it.get("ownerId"))}
            </div>''' for it in items)}
        </div>

        <div class="card">
            <span class="folder-tag">server/data/room_localhost.db</span>
            <h2>Room SQLite & Chat Rooms ({len(proposals)})</h2>
            <div style="font-size:12px; color:#A1A1AA; margin-bottom:10px;">
                Motor SQLite nativo compatible con Android Room (<code>usuarios</code>, <code>articulos</code>, <code>propuestas</code>, <code>mensajes</code>).
            </div>
            {"".join(f'''<div class="item">
                <strong>Room: {p.get("id")}</strong><br/>
                Participantes: {p.get("initiatorUserId")} & {p.get("receiverUserId")}<br/>
                <span style="color:#E5B869;">💬 Mensajes: {len(read_json_file(os.path.join(CHATS_DIR, f"{p.get('id')}.json")))}</span> • <a href="/api/rooms/{p.get('id')}" target="_blank" style="color:#60A5FA; text-decoration:none;">Ver JSON</a>
            </div>''' for p in proposals) if proposals else '<div class="item">Sin rooms activos.</div>'}
        </div>
    </div>
</body>
</html>"""
        self._set_headers(200, "text/html")
        self.wfile.write(html.encode("utf-8"))

    def _render_web_app(self):
        """Genera el cliente web móvil interactivo de Truequapp para ejecutarse en el navegador."""
        html = """<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
    <title>Truequapp Web — Simulador Móvil Oficial</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', -apple-system, sans-serif; }
        body { background: #0c0c0e; color: #f4f4f5; display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 12px; }
        .phone-frame {
            width: 100%; max-width: 440px; height: 90vh; max-height: 860px;
            background: #141418; border-radius: 36px; border: 3px solid #27272e;
            display: flex; flex-direction: column; overflow: hidden;
            box-shadow: 0 25px 60px -15px rgba(0,0,0,0.8), 0 0 0 1px rgba(255,255,255,0.06);
            position: relative;
        }
        /* Top App Bar */
        .top-bar {
            background: #18181e; padding: 16px 18px 12px; border-bottom: 1px solid #27272e;
            display: flex; flex-direction: column; gap: 10px;
        }
        .brand-row { display: flex; justify-content: space-between; align-items: center; }
        .logo { font-size: 18px; font-weight: 700; color: #E5B869; display: flex; align-items: center; gap: 8px; }
        .user-switch {
            background: #23232b; color: #e4e4e7; border: 1px solid #3f3f4e;
            padding: 5px 10px; border-radius: 8px; font-size: 12px; font-weight: 600; outline: none;
        }
        .stats-row {
            display: flex; justify-content: space-between; align-items: center;
            background: #202028; padding: 8px 12px; border-radius: 10px; font-size: 13px;
        }
        .coins-badge { color: #E5B869; font-weight: 700; display: flex; align-items: center; gap: 5px; }
        /* Main View Container */
        .content-area { flex: 1; overflow-y: auto; padding: 16px; position: relative; }
        /* Bottom Navigation Bar */
        .nav-bar {
            background: #18181e; border-top: 1px solid #27272e; display: flex;
            justify-content: space-around; padding: 10px 0 14px;
        }
        .nav-item {
            display: flex; flex-direction: column; align-items: center; gap: 4px;
            color: #71717a; text-decoration: none; font-size: 11px; font-weight: 600;
            cursor: pointer; transition: color 0.15s ease;
        }
        .nav-item.active { color: #E5B869; }
        .nav-icon { font-size: 20px; }
        /* Components */
        .card {
            background: #1e1e26; border: 1px solid #2c2c38; border-radius: 16px;
            padding: 14px; margin-bottom: 14px;
        }
        .card img {
            width: 100%; height: 160px; object-fit: cover; border-radius: 12px; margin-bottom: 10px;
        }
        .card-title { font-size: 15px; font-weight: 700; margin-bottom: 6px; color: #fafafa; }
        .card-desc { font-size: 13px; color: #a1a1aa; line-height: 1.4; margin-bottom: 10px; }
        .tag-row { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 10px; }
        .tag { background: #2a2a38; color: #e4e4e7; font-size: 11px; padding: 3px 8px; border-radius: 6px; }
        .val-badge { color: #F0C987; font-weight: 700; font-size: 13px; }
        .btn {
            background: #E5B869; color: #121214; border: none; padding: 8px 14px;
            border-radius: 10px; font-size: 13px; font-weight: 700; cursor: pointer;
            transition: opacity 0.15s ease; text-align: center; width: 100%; display: block;
        }
        .btn:hover { opacity: 0.9; }
        .btn-secondary {
            background: #2a2a36; color: #f4f4f5; border: 1px solid #3f3f50;
        }
        /* Chat view */
        .chat-container { display: flex; flex-direction: column; height: 100%; }
        .msg-stream { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding-bottom: 10px; }
        .msg-bubble {
            max-width: 80%; padding: 10px 14px; border-radius: 16px; font-size: 13px; line-height: 1.4;
        }
        .msg-sent {
            align-self: flex-end; background: #E5B869; color: #121214; font-weight: 500;
            border-bottom-right-radius: 4px;
        }
        .msg-received {
            align-self: flex-start; background: #272733; color: #f4f4f5;
            border-bottom-left-radius: 4px; border: 1px solid #363646;
        }
        .msg-sender { font-size: 10px; opacity: 0.7; margin-bottom: 3px; font-weight: 700; }
        .chat-input-row {
            display: flex; gap: 8px; padding-top: 10px; border-top: 1px solid #27272e;
        }
        .chat-input {
            flex: 1; background: #202028; border: 1px solid #363644; border-radius: 12px;
            padding: 10px 14px; color: #fff; font-size: 13px; outline: none;
        }
        .status-badge {
            display: inline-block; padding: 4px 10px; border-radius: 999px;
            font-size: 11px; font-weight: 700; text-transform: uppercase;
        }
        .status-PENDIENTE { background: #3b2d18; color: #facc15; border: 1px solid #ca8a04; }
        .status-ACEPTADO { background: #143224; color: #4ade80; border: 1px solid #16a34a; }
    </style>
</head>
<body>
    <div class="phone-frame">
        <!-- Top App Bar -->
        <div class="top-bar">
            <div class="brand-row">
                <div class="logo">🔄 Truequapp Beta</div>
                <select id="userSelector" class="user-switch" onchange="onUserChanged()">
                    <option value="chocolattcorreo@gmail.com">👤 Chocolatt</option>
                    <option value="Vergaracorreo@gmail.com">👤 Vergara</option>
                    <option value="usuario@truequapp.com">👤 Demo Coatzacoalcos</option>
                </select>
            </div>
            <div class="stats-row">
                <span id="userCity">📍 Coatzacoalcos, Ver.</span>
                <span class="coins-badge">🪙 <span id="userCoins">10,000</span> TC</span>
            </div>
        </div>

        <!-- Content Area -->
        <div id="contentArea" class="content-area">
            <!-- Renderizado dinámicamente según la pestaña -->
        </div>

        <!-- Bottom Navigation Bar -->
        <div class="nav-bar">
            <div class="nav-item active" onclick="switchTab('catalogo')">
                <span class="nav-icon">🛍️</span>
                <span>Catálogo</span>
            </div>
            <div class="nav-item" onclick="switchTab('trueques')">
                <span class="nav-icon">🤝</span>
                <span>Trueques</span>
            </div>
            <div class="nav-item" onclick="switchTab('chat')">
                <span class="nav-icon">💬</span>
                <span>Chat</span>
            </div>
            <div class="nav-item" onclick="switchTab('perfil')">
                <span class="nav-icon">👤</span>
                <span>Perfil</span>
            </div>
        </div>
    </div>

    <script>
        let currentUser = {
            email: "chocolattcorreo@gmail.com",
            name: "chocolatt",
            coins: 10000.0
        };
        let currentTab = "catalogo";
        let activeRoomId = "prop_localhost_01";
        let chatInterval = null;

        async function init() {
            await loadUserData();
            switchTab('catalogo');
        }

        async function loadUserData() {
            const email = document.getElementById("userSelector").value;
            try {
                const res = await fetch(`/api/usuarios/${encodeURIComponent(email)}`);
                if (res.ok) {
                    const u = await res.json();
                    currentUser = {
                        email: u.email || email,
                        name: u.name || email.split('@')[0],
                        coins: u.truequeCoins || 10000.0,
                        city: u.city || "Coatzacoalcos",
                        rating: u.bayesianRating || 5.0
                    };
                    document.getElementById("userCoins").innerText = Number(currentUser.coins).toLocaleString();
                    document.getElementById("userCity").innerText = `📍 ${currentUser.city}`;
                }
            } catch (e) {
                console.error("Error loading user", e);
            }
        }

        async function onUserChanged() {
            await loadUserData();
            if (currentTab === "chat") {
                renderChat();
            } else if (currentTab === "perfil") {
                renderPerfil();
            } else if (currentTab === "catalogo") {
                renderCatalogo();
            } else if (currentTab === "trueques") {
                renderTrueques();
            }
        }

        function switchTab(tab) {
            currentTab = tab;
            if (chatInterval) { clearInterval(chatInterval); chatInterval = null; }

            const navs = document.querySelectorAll(".nav-item");
            const tabs = ["catalogo", "trueques", "chat", "perfil"];
            navs.forEach((n, idx) => {
                if (tabs[idx] === tab) n.classList.add("active");
                else n.classList.remove("active");
            });

            if (tab === "catalogo") renderCatalogo();
            if (tab === "trueques") renderTrueques();
            if (tab === "chat") renderChat();
            if (tab === "perfil") renderPerfil();
        }

        async function renderCatalogo() {
            const area = document.getElementById("contentArea");
            area.innerHTML = '<div style="text-align:center; padding:30px; color:#a1a1aa;">Cargando catálogo...</div>';
            try {
                const res = await fetch("/api/catalogo");
                const items = await res.json();
                let html = '<h3 style="margin-bottom:12px; color:#E5B869; font-size:16px;">Catálogo Comunitario CETIS 79</h3>';
                items.forEach(it => {
                    const isMine = (it.ownerEmail || it.ownerId || '').toLowerCase() === currentUser.email.toLowerCase();
                    html += `
                        <div class="card">
                            <img src="${(it.imagesWebP && it.imagesWebP[0]) || 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62'}" alt="Foto"/>
                            <div class="card-title">${it.title}</div>
                            <div class="card-desc">${it.description || ''}</div>
                            <div class="tag-row">
                                <span class="tag">Condición: ${it.condition || 'BUENO'}</span>
                                <span class="tag">Dueño: ${it.ownerName || it.ownerId || ''}</span>
                            </div>
                            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
                                <span class="val-badge">💰 $${it.estimatedValueMin} - $${it.estimatedValueMax}</span>
                                <span style="font-size:11px; color:#71717a;">📍 ${it.city || 'Coatzacoalcos'}</span>
                            </div>
                            ${isMine ? 
                                '<div style="text-align:center; font-size:12px; color:#E5B869; padding:8px; border:1px dashed #E5B869; border-radius:10px;">Tu propia publicación (RN-02)</div>' : 
                                `<button class="btn" onclick="proponerTrueque('${it.id}', '${it.ownerEmail || it.ownerId}')">🤝 Proponer Trueque</button>`
                            }
                        </div>
                    `;
                });
                area.innerHTML = html;
            } catch (e) {
                area.innerHTML = `<div style="color:#ef4444;">Error al cargar catálogo: ${e.message}</div>`;
            }
        }

        async function renderTrueques() {
            const area = document.getElementById("contentArea");
            area.innerHTML = '<div style="text-align:center; padding:30px; color:#a1a1aa;">Cargando trueques...</div>';
            try {
                const res = await fetch(`/api/trueques?user=${encodeURIComponent(currentUser.email)}`);
                const props = await res.json();
                let html = '<h3 style="margin-bottom:12px; color:#E5B869; font-size:16px;">Tus Negociaciones Activas</h3>';
                if (!props || props.length === 0) {
                    html += '<div style="text-align:center; padding:40px; color:#71717a;">No tienes propuestas activas por ahora.</div>';
                } else {
                    props.forEach(p => {
                        html += `
                            <div class="card">
                                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                                    <span style="font-size:13px; font-weight:700; color:#E5B869;">Trueque ${p.id}</span>
                                    <span class="status-badge status-${p.status}">${p.status}</span>
                                </div>
                                <div style="font-size:13px; margin-bottom:6px;"><strong>Iniciador:</strong> ${p.initiatorEmail || p.initiatorUserId}</div>
                                <div style="font-size:13px; margin-bottom:6px;"><strong>Receptor:</strong> ${p.receiverEmail || p.receiverUserId}</div>
                                <div style="font-size:13px; margin-bottom:12px; color:#F0C987;">🪙 Compensación: ${p.monetaryCompensationCoins || 0} TC</div>
                                <button class="btn" onclick="abrirChatDePropuesta('${p.id}')">💬 Abrir Chat de Negociación</button>
                            </div>
                        `;
                    });
                }
                area.innerHTML = html;
            } catch (e) {
                area.innerHTML = `<div style="color:#ef4444;">Error al cargar trueques: ${e.message}</div>`;
            }
        }

        async function proponerTrueque(targetItemId, ownerEmail) {
            const promptCoins = prompt("Ingresa las TruequeCoins de compensación que deseas ofrecer (opcional, ej: 0 o 50):", "0");
            if (promptCoins === null) return;
            const coins = parseFloat(promptCoins) || 0.0;
            const body = {
                id: `prop_local_${Date.now()}`,
                initiatorUserId: currentUser.name,
                initiatorEmail: currentUser.email,
                receiverUserId: ownerEmail.split('@')[0],
                receiverEmail: ownerEmail,
                targetItemId: targetItemId,
                offeredItemIds: ["item_oferta_simulada"],
                monetaryCompensationCoins: coins,
                status: "PENDIENTE",
                esta_activa: true,
                createdAtEpochMs: Date.now()
            };
            try {
                const res = await fetch("/api/trueques", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(body)
                });
                if (res.ok) {
                    alert("¡Propuesta enviada con éxito! Abriendo chat de negociación.");
                    activeRoomId = body.id;
                    switchTab('chat');
                } else {
                    const err = await res.json();
                    alert("Error: " + (err.error || "No se pudo crear"));
                }
            } catch (e) {
                alert("Error de red: " + e.message);
            }
        }

        function abrirChatDePropuesta(propId) {
            activeRoomId = propId;
            switchTab('chat');
        }

        async function renderChat() {
            const area = document.getElementById("contentArea");
            area.innerHTML = `
                <div class="chat-container">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                        <span style="font-size:14px; font-weight:700; color:#E5B869;">💬 Sala: ${activeRoomId}</span>
                        <span style="font-size:11px; color:#4ade80;">● En Vivo</span>
                    </div>
                    <div id="msgStream" class="msg-stream">
                        <div style="text-align:center; padding:20px; color:#71717a;">Conectando a la sala...</div>
                    </div>
                    <div class="chat-input-row">
                        <input id="chatInput" class="chat-input" type="text" placeholder="Escribe un mensaje..." onkeydown="if(event.key==='Enter') sendChatMessage()"/>
                        <button class="btn" style="width:auto; padding:10px 16px;" onclick="sendChatMessage()">Enviar</button>
                    </div>
                </div>
            `;
            await fetchMessages();
            chatInterval = setInterval(fetchMessages, 2000);
        }

        async function fetchMessages() {
            const stream = document.getElementById("msgStream");
            if (!stream) return;
            try {
                const res = await fetch(`/api/trueques/${activeRoomId}/chat`);
                if (res.ok) {
                    const msgs = await res.json();
                    if (!msgs || msgs.length === 0) {
                        stream.innerHTML = '<div style="text-align:center; padding:40px; color:#71717a;">No hay mensajes aún en esta sala. ¡Comienza la conversación!</div>';
                        return;
                    }
                    let html = '';
                    msgs.forEach(m => {
                        const emisor = m.emisorCorreo || m.senderId || '';
                        const isMine = emisor.toLowerCase() === currentUser.email.toLowerCase();
                        html += `
                            <div class="msg-bubble ${isMine ? 'msg-sent' : 'msg-received'}">
                                <div class="msg-sender">${isMine ? 'Tú' : (m.senderName || emisor.split('@')[0])}</div>
                                <div>${m.contenido || m.content || ''}</div>
                            </div>
                        `;
                    });
                    const wasAtBottom = (stream.scrollHeight - stream.scrollTop) <= (stream.clientHeight + 50);
                    stream.innerHTML = html;
                    if (wasAtBottom) {
                        stream.scrollTop = stream.scrollHeight;
                    }
                }
            } catch (e) {
                console.error("Error fetching messages", e);
            }
        }

        async function sendChatMessage() {
            const input = document.getElementById("chatInput");
            if (!input) return;
            const txt = input.value.trim();
            if (!txt) return;

            input.value = "";
            const body = {
                id: `msg_web_${Date.now()}`,
                proposalId: activeRoomId,
                emisorCorreo: currentUser.email,
                senderId: currentUser.email,
                senderName: currentUser.name,
                contenido: txt,
                content: txt,
                fechaEnvioMs: Date.now(),
                timestampEpochMs: Date.now()
            };

            try {
                await fetch(`/api/trueques/${activeRoomId}/chat`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(body)
                });
                await fetchMessages();
            } catch (e) {
                console.error("Error sending message", e);
            }
        }

        async function renderPerfil() {
            const area = document.getElementById("contentArea");
            area.innerHTML = `
                <div style="text-align:center; padding:20px 0;">
                    <div style="font-size:64px; margin-bottom:10px;">👤</div>
                    <h2 style="color:#fafafa; font-size:20px; margin-bottom:4px;">${currentUser.name}</h2>
                    <div style="color:#a1a1aa; font-size:13px; margin-bottom:16px;">${currentUser.email}</div>
                    <div style="background:#1e1e26; border:1px solid #2c2c38; border-radius:16px; padding:16px; margin-bottom:16px; text-align:left;">
                        <div style="display:flex; justify-content:space-between; margin-bottom:10px;">
                            <span style="color:#a1a1aa;">Monedero TruequeCoins:</span>
                            <span style="color:#E5B869; font-weight:700;">${Number(currentUser.coins).toLocaleString()} TC</span>
                        </div>
                        <div style="display:flex; justify-content:space-between; margin-bottom:10px;">
                            <span style="color:#a1a1aa;">Reputación Bayesiana:</span>
                            <span style="color:#F0C987; font-weight:700;">⭐ ${currentUser.rating} / 5.0</span>
                        </div>
                        <div style="display:flex; justify-content:space-between;">
                            <span style="color:#a1a1aa;">Ubicación:</span>
                            <span style="color:#fafafa; font-weight:600;">${currentUser.city}</span>
                        </div>
                    </div>
                    <div style="font-size:12px; color:#71717a; line-height:1.4;">
                        Estás conectado al simulador web en tiempo real sincronizado con el servidor local y la base SQLite de Room.
                    </div>
                </div>
            `;
        }

        init();
    </script>
</body>
</html>"""
        self._set_headers(200, "text/html")
        self.wfile.write(html.encode("utf-8"))

def run_server(port=8080):
    init_storage()
    server_address = ("", port)
    httpd = HTTPServer(server_address, TruequappRequestHandler)
    print("=" * 60)
    print(f"🚀 Truequapp Central Server corriendo en:")
    print(f"   - 📱 App Web:   http://localhost:{port}/app")
    print(f"   - 📊 Dashboard: http://localhost:{port}/dashboard")
    print(f"   - 🌐 Red WiFi:  http://0.0.0.0:{port} (Usa la IP de tu PC)")
    print(f"📁 Almacenamiento desacoplado en:")
    print(f"   - Usuarios: {USUARIOS_DIR}")
    print(f"   - Trueques: {TRUEQUES_DIR}")
    print("=" * 60)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nDeteniendo servidor...")
        httpd.server_close()

if __name__ == "__main__":
    port = 8080
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            pass
    run_server(port)

