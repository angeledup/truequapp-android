#!/usr/bin/env python3
"""
Truequapp Room Localhost Database Manager
=========================================
Crea y gestiona la base de datos SQLite local ('room_localhost.db') que refleja
exactamente el esquema de Room (Android TruequappDatabase) para desarrollo,
pruebas offline y depuración en localhost (http://localhost:8080).
"""

import os
import sqlite3
import json
import logging

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data")
ROOM_DB_PATH = os.path.join(DATA_DIR, "room_localhost.db")
USUARIOS_FILE = os.path.join(DATA_DIR, "usuarios", "usuarios.json")
CATALOGO_FILE = os.path.join(DATA_DIR, "trueques", "catalogo.json")
PROPUESTAS_FILE = os.path.join(DATA_DIR, "trueques", "propuestas.json")
CHATS_DIR = os.path.join(DATA_DIR, "trueques", "chats")

logger = logging.getLogger("RoomLocalhost")

def get_connection():
    """Obtiene una conexión a la base SQLite local de Room."""
    os.makedirs(DATA_DIR, exist_ok=True)
    conn = sqlite3.connect(ROOM_DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn

def init_room_database():
    """Crea las tablas Room en SQLite local e inserta datos iniciales."""
    conn = get_connection()
    cursor = conn.cursor()

    # 1. Tabla Usuarios (esquema Room UserEntity)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS usuarios (
        correo TEXT PRIMARY KEY NOT NULL,
        contrasena TEXT NOT NULL,
        usuario TEXT NOT NULL,
        numero_telefonico TEXT,
        cantidad_trueque_coins REAL DEFAULT 10000.0,
        trueques_realizados INTEGER DEFAULT 0,
        calificacion REAL DEFAULT 5.0,
        foto_perfil_url TEXT,
        proveedor_auth TEXT DEFAULT 'EMAIL',
        barrio_ofuscado TEXT,
        ciudad TEXT,
        fecha_registro INTEGER DEFAULT 0
    );
    """)

    # 2. Tabla Catálogo / Artículos de Trueque (esquema Room TradeItemEntity)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS articulos_trueque (
        id TEXT PRIMARY KEY NOT NULL,
        titulo TEXT NOT NULL,
        descripcion TEXT NOT NULL,
        categoria_id INTEGER NOT NULL DEFAULT 1,
        estado_conservacion TEXT NOT NULL DEFAULT 'COMO_NUEVO',
        tipo_articulo TEXT NOT NULL DEFAULT 'BIEN_FISICO',
        valor_estimado_min REAL NOT NULL DEFAULT 0.0,
        valor_estimado_max REAL NOT NULL DEFAULT 0.0,
        usuario_propietario_correo TEXT NOT NULL,
        imagenes_webp_json TEXT NOT NULL DEFAULT '[]',
        etiquetas_deseos_json TEXT NOT NULL DEFAULT '[]',
        estado TEXT NOT NULL DEFAULT 'DISPONIBLE',
        ciudad TEXT NOT NULL DEFAULT 'Coatzacoalcos',
        barrio TEXT NOT NULL DEFAULT 'Centro'
    );
    """)

    # 3. Tabla Propuestas de Trueque (esquema Room TradeProposalEntity)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS propuestas_trueque (
        id TEXT PRIMARY KEY NOT NULL,
        iniciador_correo TEXT NOT NULL,
        receptor_correo TEXT NOT NULL,
        articulo_objetivo_id TEXT NOT NULL,
        articulos_ofrecidos_ids_json TEXT NOT NULL DEFAULT '[]',
        compensacion_coins REAL NOT NULL DEFAULT 0.0,
        estado TEXT NOT NULL DEFAULT 'PENDIENTE',
        esta_activa INTEGER NOT NULL DEFAULT 1,
        fecha_creacion INTEGER NOT NULL DEFAULT 0
    );
    """)

    # 4. Tabla Mensajes de Chat (esquema Room ChatMessageEntity)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS chat_mensajes (
        id TEXT PRIMARY KEY NOT NULL,
        trueque_id TEXT NOT NULL,
        emisor_correo TEXT NOT NULL,
        receptor_correo TEXT NOT NULL,
        contenido TEXT NOT NULL,
        fecha_envio_ms INTEGER NOT NULL DEFAULT 0,
        esta_entregado INTEGER NOT NULL DEFAULT 1,
        esta_leido INTEGER NOT NULL DEFAULT 0
    );
    """)

    # 5. Tabla Zonas Seguras (SafeZoneEntity)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS safe_zones (
        id TEXT PRIMARY KEY NOT NULL,
        name TEXT NOT NULL,
        addressDescription TEXT NOT NULL,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        openingHour TEXT NOT NULL,
        closingHour TEXT NOT NULL,
        hasVideoSurveillance INTEGER NOT NULL DEFAULT 1,
        isCampusUniversityPartner INTEGER NOT NULL DEFAULT 1,
        city TEXT NOT NULL DEFAULT 'Coatzacoalcos'
    );
    """)

    conn.commit()

    # Sincronizar datos iniciales desde JSON a SQLite Room
    sync_json_to_sqlite(conn)
    conn.close()
    print(f"[Room Localhost] Base de datos SQLite creada e inicializada en: {ROOM_DB_PATH}")

def sync_json_to_sqlite(conn=None):
    """Importa los usuarios, catálogo, propuestas y chats JSON a la base SQLite Room."""
    should_close = False
    if conn is None:
        conn = get_connection()
        should_close = True

    cursor = conn.cursor()

    # Usuarios
    if os.path.exists(USUARIOS_FILE):
        try:
            with open(USUARIOS_FILE, "r", encoding="utf-8") as f:
                users = json.load(f)
            for u in users:
                correo = u.get("email") or u.get("correo") or ""
                pwd = u.get("passwordHash") or u.get("contrasena") or ""
                name = u.get("name") or u.get("usuario") or ""
                phone = u.get("phoneNumber") or u.get("numero_telefonico") or ""
                coins = float(u.get("truequeCoins") or u.get("cantidad_trueque_coins") or 10000.0)
                trades = int(u.get("completedTrades") or u.get("trueques_realizados") or 0)
                rating = float(u.get("bayesianRating") or u.get("calificacion") or 5.0)
                prov = u.get("authProvider") or "EMAIL"
                barrio = u.get("obfuscatedNeighborhood") or "Centro"
                city = u.get("city") or "Coatzacoalcos"

                cursor.execute("""
                INSERT INTO usuarios (correo, contrasena, usuario, numero_telefonico, cantidad_trueque_coins, trueques_realizados, calificacion, proveedor_auth, barrio_ofuscado, ciudad)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(correo) DO UPDATE SET
                    contrasena=excluded.contrasena,
                    usuario=excluded.usuario,
                    cantidad_trueque_coins=excluded.cantidad_trueque_coins;
                """, (correo, pwd, name, phone, coins, trades, rating, prov, barrio, city))
        except Exception as e:
            logger.error(f"Error importando usuarios a SQLite Room: {e}")

    # Catálogo
    if os.path.exists(CATALOGO_FILE):
        try:
            with open(CATALOGO_FILE, "r", encoding="utf-8") as f:
                items = json.load(f)
            for it in items:
                it_id = it.get("id") or ""
                titulo = it.get("title") or it.get("titulo") or ""
                desc = it.get("description") or it.get("descripcion") or ""
                cat_id = int(it.get("categoryId") or 1)
                cond = it.get("condition") or "COMO_NUEVO"
                tipo = it.get("type") or "BIEN_FISICO"
                v_min = float(it.get("estimatedValueMin") or 0.0)
                v_max = float(it.get("estimatedValueMax") or 0.0)
                owner = it.get("ownerEmail") or it.get("ownerId") or ""
                imgs = json.dumps(it.get("imagesWebP", []))
                tags = json.dumps(it.get("tagsWishlist", []))
                status = it.get("status") or "DISPONIBLE"
                ciudad = it.get("city") or "Coatzacoalcos"
                barrio = it.get("locationNeighborhood") or "Centro"

                cursor.execute("""
                INSERT INTO articulos_trueque (id, titulo, descripcion, categoria_id, estado_conservacion, tipo_articulo, valor_estimado_min, valor_estimado_max, usuario_propietario_correo, imagenes_webp_json, etiquetas_deseos_json, estado, ciudad, barrio)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    titulo=excluded.titulo,
                    estado=excluded.estado;
                """, (it_id, titulo, desc, cat_id, cond, tipo, v_min, v_max, owner, imgs, tags, status, ciudad, barrio))
        except Exception as e:
            logger.error(f"Error importando catálogo a SQLite Room: {e}")

    # Propuestas
    if os.path.exists(PROPUESTAS_FILE):
        try:
            with open(PROPUESTAS_FILE, "r", encoding="utf-8") as f:
                proposals = json.load(f)
            for p in proposals:
                p_id = p.get("id") or ""
                u_ini = p.get("initiatorEmail") or p.get("initiatorUserId") or ""
                u_rec = p.get("receiverEmail") or p.get("receiverUserId") or ""
                t_item = p.get("targetItemId") or ""
                offered = json.dumps(p.get("offeredItemIds", []))
                coins = float(p.get("monetaryCompensationCoins") or 0.0)
                status = p.get("status") or "PENDIENTE"
                activa = 1 if p.get("esta_activa", True) else 0
                fecha = int(p.get("createdAtEpochMs") or 0)

                cursor.execute("""
                INSERT INTO propuestas_trueque (id, iniciador_correo, receptor_correo, articulo_objetivo_id, articulos_ofrecidos_ids_json, compensacion_coins, estado, esta_activa, fecha_creacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    estado=excluded.estado,
                    esta_activa=excluded.esta_activa;
                """, (p_id, u_ini, u_rec, t_item, offered, coins, status, activa, fecha))
        except Exception as e:
            logger.error(f"Error importando propuestas a SQLite Room: {e}")

    # Chats
    if os.path.exists(CHATS_DIR):
        try:
            for fname in os.listdir(CHATS_DIR):
                if fname.endswith(".json"):
                    pid = fname[:-5]
                    with open(os.path.join(CHATS_DIR, fname), "r", encoding="utf-8") as f:
                        msgs = json.load(f)
                    for m in msgs:
                        m_id = m.get("id") or ""
                        trueque_id = pid
                        emisor = m.get("emisorCorreo") or m.get("senderId") or ""
                        receptor = m.get("receptorCorreo") or ""
                        contenido = m.get("contenido") or m.get("content") or ""
                        fecha = int(m.get("fechaEnvioMs") or m.get("timestampEpochMs") or 0)
                        entregado = 1 if m.get("estaEntregado", True) else 0
                        leido = 1 if m.get("estaLeido", False) else 0

                        cursor.execute("""
                        INSERT INTO chat_mensajes (id, trueque_id, emisor_correo, receptor_correo, contenido, fecha_envio_ms, esta_entregado, esta_leido)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET
                            esta_leido=excluded.esta_leido;
                        """, (m_id, trueque_id, emisor, receptor, contenido, fecha, entregado, leido))
        except Exception as e:
            logger.error(f"Error importando chats a SQLite Room: {e}")

    conn.commit()
    if should_close:
        conn.close()

if __name__ == "__main__":
    init_room_database()

