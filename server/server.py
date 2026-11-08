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
        path = parsed.path
        if path.startswith("/api/index.py"):
            path = path[len("/api/index.py"):] or "/"
        query = parse_qs(parsed.query)

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

        # 6. Tablero Visual HTML de Monitoreo Local
        if path == "/" or path == "/dashboard":
            self._render_dashboard()
            return

        self._send_json({"error": f"Ruta GET {path} no encontrada"}, 404)

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
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
        <div class="badge">● Activo (Port 8080)</div>
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
    </div>
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
    print(f"   - Local:    http://localhost:{port}")
    print(f"   - Red WiFi: http://0.0.0.0:{port} (Usa la IP de tu PC)")
    print(f"   - Dashboard: http://localhost:{port}/dashboard")
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

