package com.ajmarcos.multiprobador;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";

    /**
     * Se llama cuando se genera un nuevo token (ej. al reinstalar la app).
     * Debes enviar este token a tu servidor para saber a quién enviar la notif.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // Aquí llamarías a una función para enviar el token a tu backend si tuvieras uno
        // sendRegistrationToServer(token);
    }

    /**
     * Se llama cuando se recibe un mensaje y la app está en PRIMER PLANO.
     * Si la app está en segundo plano, el sistema maneja la notificación automáticamente
     * (si el mensaje tiene carga útil de "notification").
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Verificar si el mensaje contiene datos (payload de datos)
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            // Aquí puedes procesar datos silenciosos si es necesario
        }

        // Verificar si el mensaje contiene una notificación visible
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

            // Mostrar la notificación visualmente
            sendNotification(remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody());
        }
    }

    /**
     * Crea y muestra una notificación simple conteniendo el mensaje recibido.
     */
// MyFirebaseMessagingService.java (Solo el método sendNotification)

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // FLAG_IMMUTABLE es requerido por el sistema moderno
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // 1. DEFINICIÓN DEL CANAL (DEBE SER ÚNICA)
        String channelId = "fcm_channel_multiprobador"; // <--- CAMBIAR ID DE CANAL
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId) // <-- Usamos el ID de canal
                        .setSmallIcon(R.drawable.ic_launcher_foreground) // ⚠️ Reemplaza con el nombre de tu icono real
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH); // Para forzar visibilidad

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 2. 📢 CREAR EL CANAL ANTES DE MOSTRAR LA NOTIFICACIÓN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Asegúrate de que el nombre del canal es visible para el usuario
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Actualizaciones de Multiprobador",
                    NotificationManager.IMPORTANCE_DEFAULT);

            channel.setDescription("Canal para notificaciones de actualizaciones y estado.");
            notificationManager.createNotificationChannel(channel);
        }
        // ----------------------------------------------------------------------

        notificationManager.notify(0, notificationBuilder.build());
    }
}