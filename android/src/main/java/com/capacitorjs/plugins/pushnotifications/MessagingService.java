package com.capacitorjs.plugins.pushnotifications;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.IOException;
import java.lang.Integer;

import android.net.Uri;
import android.os.Build;
import android.app.PendingIntent;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.service.notification.StatusBarNotification;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import android.media.RingtoneManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;


import androidx.annotation.NonNull;
import androidx.core.app.Person;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.MessagingStyle;
import androidx.core.graphics.drawable.IconCompat;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


public class MessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
	Log.e("MyTagGoesHere", "Starting function");
        if (remoteMessage.getData().size() > 0) {
	    Log.e("MyTagGoesHere", "About to unpack var");
            String sender = remoteMessage.getData().get("sender");
            String message = remoteMessage.getData().get("message");
            String sender_id = remoteMessage.getData().get("sender_id");
            String asset_id = remoteMessage.getData().get("asset_id");
            
		Log.e("MyTagGoesHere", "about to go for a notification");
            sendNotification(message, sender, sender_id, asset_id);
        }
        /*
        else if (remoteMessage.getNotification() != null) {
            //Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(remoteMessage.getNotification().getBody());
        }*/
    }

    public Notification getActiveNotification(int notificationId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] barNotifications = notificationManager.getActiveNotifications();
        for(StatusBarNotification notification: barNotifications) {
            if (notification.getId() == notificationId) {
                return notification.getNotification();
            }
        }
        return null;
    }

    public static IconCompat getIconFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            IconCompat icon = IconCompat.createWithBitmap(bitmap);
            return icon;
        } catch (IOException e) {
            //e.printStackTrace();
            return null;
        }
    } 


    private void sendNotification(String messageBody, String sender, String sender_id, String asset_id) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification existingNotification = getActiveNotification(Integer.parseInt(sender_id));
        MessagingStyle style;
        IconCompat userIcon = getIconFromURL("https://cdn.youngapply.com/asset/" + asset_id);
        
        Person.Builder personBuilder = new Person.Builder().setName(sender);

        if (userIcon != null || asset_id != ""){
            personBuilder.setIcon(userIcon);
        }

        Person user = personBuilder.build();

        Intent intent = new Intent(this, MessagingService.class).putExtra("msg",messageBody);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,PendingIntent.FLAG_IMMUTABLE);

        String channelId = "ya_direct_messages";
        String channelName = "YoungApply Direct Messages";
        String channelDescription = "Receive direct messages from other users on YoungApply.";

        long timestamp = System.currentTimeMillis();
        int color = 0xffF8B71D;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // if Android Version >= 8, then create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setVibrationPattern(new long[] {0, 200, 100, 200});
            channel.enableVibration(true);
            channel.setAllowBubbles(true);
            channel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(channel);
        }

        //if there is already a notification
        if (existingNotification != null){
            style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(existingNotification);
            style.getMessages();
            style.addMessage(messageBody, timestamp, user);
        }else{
            style = new MessagingStyle("me")
                .addMessage(messageBody, timestamp, user);
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(MessagingService.this, channelId)
                        .setSmallIcon(R.drawable.ic_notifications)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setColor(color)
                        .setStyle(style)
                        .setContentIntent(pendingIntent);
                        //.setContentTitle("2 new messages with " + sender)
                        //.setContentText("messageBody")

        notificationManager.notify(Integer.parseInt(sender_id), notificationBuilder.build());
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        PushNotificationsPlugin.onNewToken(s);
    }

}
