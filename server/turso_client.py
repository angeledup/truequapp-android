"""
Turso (libSQL) Cloud Database Client for Truequapp
==================================================
Conector nativo HTTP pipeline (v2) para bases de datos Turso SQLite.
No requiere dependencias externas (usa urllib y json nativos de Python).
Funciona tanto de forma local como en entornos serverless de Vercel.
"""

import os
import json
import urllib.request
import urllib.error
import logging

logger = logging.getLogger("TruequappTurso")

def get_turso_config():
    """Obtiene la URL y el Token de Turso desde variables de entorno."""
    raw_url = os.environ.get("TURSO_DATABASE_URL", "").strip()
    token = os.environ.get("TURSO_AUTH_TOKEN", "").strip()

    if not raw_url or not token:
        return None, None

    # Normalizar URL: reemplazar libsql:// por https://
    http_url = raw_url
    if http_url.startswith("libsql://"):
        http_url = "https://" + http_url[len("libsql://"):]
    elif not http_url.startswith("http://") and not http_url.startswith("https://"):
        http_url = "https://" + http_url

    http_url = http_url.rstrip("/")
    if not http_url.endswith("/v2/pipeline"):
        http_url = f"{http_url}/v2/pipeline"

    return http_url, token

def is_turso_configured() -> bool:
    url, token = get_turso_config()
    return bool(url and token)

def execute_pipeline(statements: list) -> list:
    """
    Ejecuta una lista de sentencias SQL en Turso vía el endpoint /v2/pipeline.
    Cada sentencia puede ser un string SQL o un dict con {"sql": ..., "args": [...]}.
    """
    pipeline_url, token = get_turso_config()
    if not pipeline_url:
        raise ValueError("Turso no está configurado (faltan TURSO_DATABASE_URL o TURSO_AUTH_TOKEN).")

    requests_payload = []
    for stmt in statements:
        if isinstance(stmt, str):
            requests_payload.append({
                "type": "execute",
                "stmt": {"sql": stmt}
            })
        elif isinstance(stmt, dict):
            formatted_args = []
            for arg in stmt.get("args", []):
                if arg is None:
                    formatted_args.append({"type": "null"})
                elif isinstance(arg, bool):
                    formatted_args.append({"type": "integer", "value": "1" if arg else "0"})
                elif isinstance(arg, int):
                    formatted_args.append({"type": "integer", "value": str(arg)})
                elif isinstance(arg, float):
                    formatted_args.append({"type": "float", "value": float(arg)})
                else:
                    formatted_args.append({"type": "text", "value": str(arg)})

            requests_payload.append({
                "type": "execute",
                "stmt": {
                    "sql": stmt["sql"],
                    "args": formatted_args
                }
            })

    requests_payload.append({"type": "close"})

    body_bytes = json.dumps({"requests": requests_payload}).encode("utf-8")
    req = urllib.request.Request(
        pipeline_url,
        data=body_bytes,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            results = data.get("results", [])
            output = []
            for res in results:
                if res.get("type") == "ok":
                    response_obj = res.get("response", {})
                    result_obj = response_obj.get("result", {})
                    cols = [c["name"] for c in result_obj.get("cols", [])]
                    rows_raw = result_obj.get("rows", [])
                    parsed_rows = []
                    for row in rows_raw:
                        row_dict = {}
                        for i, col_name in enumerate(cols):
                            val_obj = row[i] if i < len(row) else None
                            val = val_obj.get("value") if isinstance(val_obj, dict) else val_obj
                            row_dict[col_name] = val
                        parsed_rows.append(row_dict)
                    output.append({
                        "ok": True,
                        "rows": parsed_rows,
                        "affected": result_obj.get("affected_row_count", 0)
                    })
                elif res.get("type") == "error":
                    output.append({
                        "ok": False,
                        "error": res.get("error", {}).get("message", "Error desconocido de Turso")
                    })
            return output
    except Exception as e:
        logger.error(f"Error al conectar con Turso pipeline: {e}")
        raise e

def init_turso_tables():
    """Inicializa las 4 tablas principales en Turso si aún no existen."""
    if not is_turso_configured():
        return

    statements = [
        """
        CREATE TABLE IF NOT EXISTS usuarios (
            correo TEXT PRIMARY KEY,
            contrasena TEXT NOT NULL,
            usuario TEXT NOT NULL,
            numero_telefonico TEXT,
            cantidad_trueque_coins REAL DEFAULT 10000.0,
            trueques_realizados INTEGER DEFAULT 0,
            calificacion REAL DEFAULT 5.0,
            data_json TEXT
        );
        """,
        """
        CREATE TABLE IF NOT EXISTS catalogo (
            id TEXT PRIMARY KEY,
            owner_id TEXT,
            title TEXT,
            status TEXT DEFAULT 'DISPONIBLE',
            data_json TEXT
        );
        """,
        """
        CREATE TABLE IF NOT EXISTS propuestas (
            id TEXT PRIMARY KEY,
            initiator_user_id TEXT,
            receiver_user_id TEXT,
            esta_activa INTEGER DEFAULT 1,
            status TEXT DEFAULT 'PROPUESTA_ENVIADA',
            data_json TEXT
        );
        """,
        """
        CREATE TABLE IF NOT EXISTS chats (
            id TEXT PRIMARY KEY,
            propuesta_id TEXT,
            emisor_correo TEXT,
            data_json TEXT,
            timestamp_ms INTEGER
        );
        """
    ]
    execute_pipeline(statements)

def turso_read_table(table_name: str, prop_id: str = None) -> list:
    """Lee registros desde una tabla de Turso deserializando el JSON."""
    if not is_turso_configured():
        return []

    if table_name == "chats" and prop_id:
        res = execute_pipeline([{
            "sql": "SELECT data_json FROM chats WHERE propuesta_id = ? ORDER BY timestamp_ms ASC;",
            "args": [prop_id]
        }])
        if res and res[0].get("ok"):
            messages = []
            for r in res[0].get("rows", []):
                raw = r.get("data_json")
                if raw:
                    try:
                        messages.append(json.loads(raw))
                    except Exception:
                        pass
            return messages
        return []

    sql = f"SELECT data_json FROM {table_name};"
    res = execute_pipeline([{"sql": sql}])
    if res and res[0].get("ok"):
        items = []
        for r in res[0].get("rows", []):
            raw = r.get("data_json")
            if raw:
                try:
                    items.append(json.loads(raw))
                except Exception:
                    pass
        return items
    return []

def turso_save_table(table_name: str, items: list, prop_id: str = None):
    """Guarda o actualiza registros en Turso con soporte para inserciones/actualizaciones."""
    if not is_turso_configured() or not items:
        return

    stmts = []
    if table_name == "usuarios":
        for u in items:
            correo = u.get("email") or u.get("correo") or ""
            pwd = u.get("passwordHash") or u.get("contrasena") or ""
            nombre = u.get("name") or u.get("usuario") or ""
            phone = u.get("phoneNumber") or u.get("numeroTelefonico") or ""
            coins = float(u.get("truequeCoins") or u.get("cantidadTruequeCoins") or 10000.0)
            trades = int(u.get("completedTrades") or u.get("truequesRealizados") or 0)
            rating = float(u.get("bayesianRating") or u.get("calificacion") or 5.0)
            stmts.append({
                "sql": """INSERT INTO usuarios (correo, contrasena, usuario, numero_telefonico, cantidad_trueque_coins, trueques_realizados, calificacion, data_json)
                          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                          ON CONFLICT(correo) DO UPDATE SET
                            contrasena=excluded.contrasena,
                            usuario=excluded.usuario,
                            numero_telefonico=excluded.numero_telefonico,
                            cantidad_trueque_coins=excluded.cantidad_trueque_coins,
                            trueques_realizados=excluded.trueques_realizados,
                            calificacion=excluded.calificacion,
                            data_json=excluded.data_json;""",
                "args": [correo, pwd, nombre, phone, coins, trades, rating, json.dumps(u, ensure_ascii=False)]
            })
    elif table_name == "catalogo":
        for it in items:
            it_id = it.get("id") or ""
            owner = it.get("ownerId") or it.get("ownerEmail") or ""
            title = it.get("title") or ""
            status = it.get("status") or "DISPONIBLE"
            stmts.append({
                "sql": """INSERT INTO catalogo (id, owner_id, title, status, data_json)
                          VALUES (?, ?, ?, ?, ?)
                          ON CONFLICT(id) DO UPDATE SET
                            owner_id=excluded.owner_id,
                            title=excluded.title,
                            status=excluded.status,
                            data_json=excluded.data_json;""",
                "args": [it_id, owner, title, status, json.dumps(it, ensure_ascii=False)]
            })
    elif table_name == "propuestas":
        for p in items:
            p_id = p.get("id") or ""
            initiator = p.get("initiatorUserId") or p.get("initiatorEmail") or ""
            receiver = p.get("receiverUserId") or p.get("receiverEmail") or ""
            activa = 1 if p.get("esta_activa", True) else 0
            status = p.get("status") or "PROPUESTA_ENVIADA"
            stmts.append({
                "sql": """INSERT INTO propuestas (id, initiator_user_id, receiver_user_id, esta_activa, status, data_json)
                          VALUES (?, ?, ?, ?, ?, ?)
                          ON CONFLICT(id) DO UPDATE SET
                            initiator_user_id=excluded.initiator_user_id,
                            receiver_user_id=excluded.receiver_user_id,
                            esta_activa=excluded.esta_activa,
                            status=excluded.status,
                            data_json=excluded.data_json;""",
                "args": [p_id, initiator, receiver, activa, status, json.dumps(p, ensure_ascii=False)]
            })
    elif table_name == "chats" and prop_id:
        for m in items:
            m_id = m.get("id") or ""
            emisor = m.get("emisorCorreo") or m.get("senderId") or ""
            ts = int(m.get("fechaEnvioMs") or m.get("timestamp") or 0)
            stmts.append({
                "sql": """INSERT INTO chats (id, propuesta_id, emisor_correo, data_json, timestamp_ms)
                          VALUES (?, ?, ?, ?, ?)
                          ON CONFLICT(id) DO UPDATE SET
                            data_json=excluded.data_json,
                            timestamp_ms=excluded.timestamp_ms;""",
                "args": [m_id, prop_id, emisor, json.dumps(m, ensure_ascii=False), ts]
            })

    if stmts:
        execute_pipeline(stmts)

def turso_delete_item(table_name: str, item_id: str):
    """Elimina un registro de la tabla en Turso por su identificador primario."""
    if not is_turso_configured():
        return
    stmts = [{"sql": f"DELETE FROM {table_name} WHERE id = ?;", "args": [item_id]}]
    execute_pipeline(stmts)

def sync_local_data_to_turso(usuarios_data: list, catalogo_data: list, propuestas_data: list, chats_dict: dict):
    """Sincroniza datos precargados si Turso se encuentra vacío."""
    if not is_turso_configured():
        return

    res = execute_pipeline([{"sql": "SELECT COUNT(*) as count FROM usuarios;"}])
    count = 0
    if res and res[0].get("ok"):
        rows = res[0].get("rows", [])
        if rows:
            count = int(rows[0].get("count", 0))

    if count == 0:
        logger.info("Turso se encuentra vacío. Sembrando usuarios, artículos escolares CETIS 79 y propuestas...")
        if usuarios_data:
            turso_save_table("usuarios", usuarios_data)
        if catalogo_data:
            turso_save_table("catalogo", catalogo_data)
        if propuestas_data:
            turso_save_table("propuestas", propuestas_data)
        if chats_dict:
            for prop_id, msgs in chats_dict.items():
                turso_save_table("chats", msgs, prop_id=prop_id)
