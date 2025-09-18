package com.example.estudapp.domain.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.estudapp.R
import com.example.estudapp.domain.GeofenceManager // Importe o GeofenceManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent nulo no BroadcastReceiver.")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = "Erro no Geofence: ${geofencingEvent.errorCode}"
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        val geofenceId = triggeringGeofences?.firstOrNull()?.requestId

        // --- ATUALIZAÇÃO AQUI ---
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.i(TAG, "Entrou em: $geofenceId")
            GeofenceManager.updateCurrentLocation(geofenceId) // Atualiza o estado
            sendNotification(context, geofenceId ?: "Local de Estudo", "Você entrou em um local de estudo!")
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.i(TAG, "Saiu de: $geofenceId")
            GeofenceManager.updateCurrentLocation(null) // Limpa o estado
            sendNotification(context, geofenceId ?: "Local de Estudo", "Você saiu de um local de estudo.")
        } else {
            Log.e(TAG, "Tipo de transição inválido: $geofenceTransition")
        }
    }

    private fun sendNotification(context: Context, title: String, content: String) {
        // (O resto do seu código de notificação permanece o mesmo)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "geofence_channel"
        val channelName = "Notificações de Geofence"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}