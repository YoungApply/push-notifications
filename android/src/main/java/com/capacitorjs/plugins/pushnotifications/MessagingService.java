package com.capacitorjs.plugins.pushnotifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.app.NotificationCompat.MessagingStyle;
import androidx.core.graphics.drawable.IconCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String sender = remoteMessage.getData().get("sender");
            String message = remoteMessage.getData().get("message");
            String sender_id = remoteMessage.getData().get("sender_id");
            String asset_id = remoteMessage.getData().get("asset_id");
            sendNotification(message, sender, sender_id, asset_id);
        }
    }

    public Notification getActiveNotification(int notificationId) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        StatusBarNotification[] barNotifications = notificationManager.getActiveNotifications();
        for (StatusBarNotification notification : barNotifications) {
            if (notification.getId() == notificationId) {
                return notification.getNotification();
            }
        }
        return null;
    }

    public IconCompat getIconFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return IconCompat.createWithBitmap(bitmap);
        } catch (IOException e) {
            return null;
        }
    }

    private void sendNotification(String messageBody, String sender, String sender_id, String asset_id) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification existingNotification = getActiveNotification(Integer.parseInt(sender_id));
        MessagingStyle style;
        IconCompat userIcon = getIconFromURL("https://cdn.youngapply.com/asset/" + asset_id);

        Person.Builder personBuilder = new Person.Builder().setName(sender);

        if (userIcon != null || !asset_id.isEmpty()) {
            personBuilder.setIcon(userIcon);
        }

        Person user = personBuilder.build();

        Intent intent = new Intent(this, MessagingService.class).putExtra("msg", messageBody);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String channelId = "ya_direct_messages";
        String channelName = "YoungApply Direct Messages";
        String channelDescription = "Receive direct messages from other users on YoungApply.";

        long timestamp = System.currentTimeMillis();
        int color = 0xffF8B71D;

        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setVibrationPattern(new long[] {0, 200, 100, 200});
            channel.enableVibration(true);
            channel.setAllowBubbles(true);
            channel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(channel);
        }

        if (existingNotification != null) {
            style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(existingNotification);
            style.getMessages();
            style.addMessage(messageBody, timestamp, user);
        } else {
            style = new MessagingStyle("me")
                    .addMessage(messageBody, timestamp, user);
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notifications)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setColor(color)
                        .setStyle(style)
                        .setContentIntent(pendingIntent);

        notificationManager.notify(Integer.parseInt(sender_id), notificationBuilder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        PushNotificationsPlugin.onNewToken(token);
    }
}
