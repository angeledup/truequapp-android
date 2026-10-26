package com.truequapp.core.domain.compliance

/**
 * Entidades y especificación formal de cumplimiento normativo para Google Play Console (Fase 0).
 * Garantiza apego a las directrices de publicación de Google Play Store 2026.
 */
object GooglePlayPolicyCompliance {

    /**
     * 1. Política de Divisas Virtuales y Servicios Financieros.
     * Los TruequeCoins son "Closed-Loop Virtual Utility Credits":
     * - No convertibles a moneda fiduciaria (fíat).
     * - Sin pasarelas de retiro ("cash-out").
     * - Destinados exclusivamente a compensar asimetrías de valor en el trueque asíncrono.
     */
    object VirtualCurrencyPolicy {
        const val CURRENCY_NAME = "TruequeCoins"
        const val IS_CLOSED_LOOP = true
        const val ALLOWS_CASH_OUT_RETIREMENT = false
        const val ALLOWS_CRYPTO_CONVERSION = false
        const val TERMS_OF_SERVICE_URL = "https://truequapp.org/terms-community"

        fun validateTransactionPermitted(isCashOutAttempt: Boolean) {
            if (isCashOutAttempt) {
                throw UnsupportedOperationException(
                    "Operación rechazada por política Google Play: TruequeCoins no permite retiros bancarios o conversiones fíat."
                )
            }
        }
    }

    /**
     * 2. Política de Acceso a Ubicación.
     * Solo ubicación en primer plano (Foreground Location). Prohibición estricta de background location.
     */
    object LocationAccessPolicy {
        const val PERMISSION_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
        const val PERMISSION_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
        const val PROHIBITED_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"
        const val FOREGROUND_ONLY = true

        val IN_APP_RATIONALE_EXPLANATION = """
            Truequapp requiere acceso a tu ubicación únicamente mientras utilizas la app para:
            1. Calcular la distancia aproximada a los artículos ofertados en tu comunidad.
            2. Validar tu presencia física en el Punto Seguro (Safe Zone) al momento de concretar un trueque.
            Tus coordenadas exactas residenciales nunca son compartidas públicamente ni se rastrea tu ubicación en segundo plano.
        """.trimIndent()

        fun isPermissionPermitted(permission: String): Boolean {
            return permission != PROHIBITED_BACKGROUND_LOCATION
        }
    }

    /**
     * 3. Política de Permisos de Cámara.
     * Uso estrictamente transaccional: fotografías de inventario y escaneo de código QR en Safe Zone.
     */
    object CameraUsagePolicy {
        const val PERMISSION_CAMERA = "android.permission.CAMERA"
        const val PURPOSE_INVENTORY_PHOTOS = "INVENTORY_PHOTOS"
        const val PURPOSE_QR_VERIFICATION = "QR_VERIFICATION"

        fun isCameraUsageCompliant(purpose: String): Boolean {
            return purpose == PURPOSE_INVENTORY_PHOTOS || purpose == PURPOSE_QR_VERIFICATION
        }
    }

    /**
     * 4. Política de Eliminación de Cuentas (Google Play Requirement).
     */
    object AccountDeletionPolicy {
        const val IN_APP_DELETION_PATH = "Ajustes > Seguridad > Eliminar mi cuenta"
        const val EXTERNAL_DELETION_URL = "https://truequapp.org/delete-account"
        const val AUDIT_GRACE_PERIOD_DAYS = 30
    }
}
