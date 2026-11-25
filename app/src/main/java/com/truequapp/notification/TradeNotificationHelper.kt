package com.truequapp.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.truequapp.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TradeNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    init {
        createNotificationChannel(context)
    }

    /**
     * Muestra una notificación en la bandeja del sistema Android cuando llega una oferta de trueque.
     */
    fun notifyNewTradeOffer(
        proposalId: String,
        senderNameOrEmail: String,
        targetItemId: String,
        coins: Double
    ) {
        // En Android 13+ (API 33+) se debe comprobar el permiso en tiempo de ejecución
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return
            }
        }

        val displayName = if (senderNameOrEmail.contains("@")) {
            senderNameOrEmail.substringBefore("@")
        } else {
            senderNameOrEmail
        }

        val extraCoinsText = if (coins > 0) " (+${coins.toInt()} TC)" else ""
        val message = "El usuario $displayName te ha enviado una propuesta de trueque$extraCoinsText."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAV_DESTINATION", "trades")
            putExtra("PROPOSAL_ID", proposalId)
        }

        val requestCode = proposalId.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("¡Nueva oferta de trueque recibida!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        } catch (_: SecurityException) {
            // Silencioso ante revocación intempestiva de permisos
        }
    }

    companion object {
        const val CHANNEL_ID = "truequapp_trade_offers_channel"
        const val CHANNEL_NAME = "Ofertas y Trueques"
        const val CHANNEL_DESCRIPTION = "Notificaciones de nuevas ofertas recibidas en Truequapp"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }
}
