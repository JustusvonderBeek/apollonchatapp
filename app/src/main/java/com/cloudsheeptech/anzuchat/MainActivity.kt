package com.cloudsheeptech.anzuchat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cloudsheeptech.anzuchat.database.ApollonDatabase
import com.cloudsheeptech.anzuchat.database.contact.Contact
import com.cloudsheeptech.anzuchat.database.message.DisplayMessage
import com.cloudsheeptech.anzuchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.cloudsheeptech.anzuchat.databinding.ActivityMainBinding
import com.cloudsheeptech.anzuchat.work.FetchNetworkWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var notificationManager: NotificationManager
    private var notificationCompatBuilder : NotificationCompat.Builder? = null

    private var job = Job()
    private var mainScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNotification()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // No need to setup the action bar here, as each fragment now needs to handle the back button etc.
//        setSupportActionBar(binding.mainActionBar)

        // This does only work when the Navigation component is inside a <fragment> tag, NOTHING ELSE!
//        val navController = findNavController(R.id.navHostFragment)
//
//        // Decide what fragments should be "top-level" and SHOULD NOT! have a "back" arrow
//        val appBarConfiguration = AppBarConfiguration.Builder(setOf(R.id.navigation_chat_list, R.id.navigation_user_creation)).build()
//        // The back arrow in the actionBar (title bar on top)
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Starting the networking service here since it will be used throughout the whole app
        // TODO: Loading the newly created user
        val userId = runBlocking {
            loadUser()
        }
        ApollonProtocolHandler.initialize(userId, application) { m, c ->
            messageNotification(m, c)
        }
    }

    override fun onResume() {
        super.onResume()
        // Remove notifications when user comes back to the app
        notificationManager.cancelAll()
    }

    private suspend fun loadUser() : UInt {
        val userDB = ApollonDatabase.getInstance(this.application).userDao()
        var userId = 0u
        withContext(Dispatchers.IO) {
            val user = userDB.getUser()
            if (user != null)
                userId = user.userId.toUInt()
        }
        return userId
    }

    private fun setupNotification() {
        // No need to check version as SDK is always 26+
        val name = getString(R.string.notification_channel)
        val descriptionText = getString(R.string.notification_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("NOTIFICATIONS", name, importance).apply {
            description = descriptionText
        }
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        // Close all notifications on opening up the app
        notificationManager.cancelAll()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.navHostFragment)
        return navController.navigateUp()
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

        messageStyle.addMessage(NotificationCompat.MessagingStyle.Message(
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

    override fun onDestroy() {
        // Schedule the work manager to run background tasks
        Log.i("MainActivity", "Starting scheduling of background work")
        val repeatingFetch = PeriodicWorkRequestBuilder<FetchNetworkWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(FetchNetworkWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, repeatingFetch)
        super.onDestroy()
    }
}