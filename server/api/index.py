import sys
import os

# Asegurar que el directorio raíz del servidor esté en sys.path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from server import TruequappRequestHandler, init_storage

# Inicializar carpetas y archivos / conexión a Turso
init_storage()

# Vercel Python runtime requiere explícitamente una clase llamada 'handler'
class handler(TruequappRequestHandler):
    pass


