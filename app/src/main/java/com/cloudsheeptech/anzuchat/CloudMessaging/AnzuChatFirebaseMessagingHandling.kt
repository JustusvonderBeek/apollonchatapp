package com.cloudsheeptech.anzuchat.CloudMessaging

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.cloudsheeptech.anzuchat.MainActivity
import com.cloudsheeptech.anzuchat.R
import com.cloudsheeptech.anzuchat.database.contact.Contact
import com.cloudsheeptech.anzuchat.database.message.DisplayMessage
import com.cloudsheeptech.anzuchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Calendar

class AnzuChatFirebaseMessagingHandling : FirebaseMessagingService() {

    private lateinit var notificationManager: NotificationManager
    private var notificationCompatBuilder : NotificationCompat.Builder? = null

    override fun onNewToken(token: String) {
        Log.d("AnzuChatFirebaseMessagingHandling", "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("AnzuChatFirebaseMessagingHandling", "Message received: $message")

        // Payload?
        if (message.data.isNotEmpty()) {
            Log.d("AnzuChatFirebaseMessagingHandling", "Body found: ${message.data}")
            // Handle message
            // TODO: Connect to server, and request messages
            // TODO: Read userid from disk
            ApollonProtocolHandler.initialize(2596996162u, application) { m, c ->
                messageNotification(m, c)
            }
        }

        // Notification included?
        message.notification?.let {
            Log.d("AnzuChatFirebaseMessagingHandling", "Notification found: ${it.body}")
        }
    }

    private fun sendRegistrationToServer(token : String) {
        // TODO: For now simply print the token
    }

    private fun setupNotificationCompat(): NotificationCompat.Builder {
        // Make the notification clickable
        val chatViewIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(chatViewIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val ownPerson = Person.Builder().setName("Own").build()
        val messagingStyle = NotificationCompat.MessagingStyle(ownPerson)
        messagingStyle.isGroupConversation = false

        return NotificationCompat.Builder(applicationContext, "NOTIFICATIONS")
            .setStyle(messagingStyle)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_user)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
    }

    private fun messageNotification(message : DisplayMessage, contact : Contact) {
        // In case we receive the first notification
        if (notificationCompatBuilder == null) {
            notificationCompatBuilder = setupNotificationCompat()
        }

        // Retrieve the messaging style and add or update the style in case of new user
        val oldNotifyStyle = notificationCompatBuilder!!.build()
        val messageStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(oldNotifyStyle)
        if (messageStyle == null) {
            Log.i("MainActivity", "The message style is null, so something went very wrong!")
            return
        }

        messageStyle.addMessage(
            NotificationCompat.MessagingStyle.Message(
            message.content,
            Calendar.getInstance().timeInMillis,
            Person.Builder().setName(contact.contactName).build()
        ))
        val notification = notificationCompatBuilder!!.setStyle(messageStyle).build()

        // Send the actual notification
        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            // Because we include the whole message history update the old one (bind to contact ID)
            notify(contact.contactId.toInt(), notification)
        }
    }

}