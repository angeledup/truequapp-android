import sys
import os

# Asegurar que el directorio raíz del servidor esté en sys.path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from server import TruequappRequestHandler, init_storage

# Inicializar carpetas y archivos en el entorno efímero de Vercel (/tmp si es necesario)
init_storage()

# Exportar handler para el runtime de Vercel
handler = TruequappRequestHandler

