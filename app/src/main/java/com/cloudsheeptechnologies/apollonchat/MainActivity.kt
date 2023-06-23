package com.cloudsheeptechnologies.apollonchat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.util.TimeUtils
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cloudsheeptechnologies.apollonchat.database.ApollonDatabase
import com.cloudsheeptechnologies.apollonchat.database.contact.Contact
import com.cloudsheeptechnologies.apollonchat.database.message.DisplayMessage
import com.cloudsheeptechnologies.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.cloudsheeptechnologies.apollonchat.databinding.ActivityMainBinding
import com.cloudsheeptechnologies.apollonchat.work.FetchNetworkWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private var job = Job()
    private var mainScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupNotification()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.mainActionBar)

        // This does only work when the Navigation component is inside a <fragment> tag, NOTHING ELSE!
        val navController = findNavController(R.id.navHostFragment)

        // Decide what fragments should be "top-level" and SHOULD NOT! have a "back" arrow
        val appBarConfiguration = AppBarConfiguration.Builder(setOf(R.id.navigation_chat_list, R.id.navigation_user_creation)).build()
        // The back arrow in the actionBar (title bar on top)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Starting the networking service here since it will be used throughout the whole app
        // TODO: Loading the newly created user
        val userId = runBlocking {
            loadUser()
        }
        ApollonProtocolHandler.initialize(userId, application) { m, c ->
            messageNotification(m, c)
        }

//        val builder = NotificationCompat.Builder(this, "NOTIFICATIONS")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Apollon Chat Setup Finished")
//            .setContentText("Finished the setup successfully")
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .setAutoCancel(true)
//
//        with(NotificationManagerCompat.from(this)) {
//            if (ActivityCompat.checkSelfPermission(
//                    applicationContext,
//                    Manifest.permission.POST_NOTIFICATIONS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return
//            }
////            notify(1, builder.build())
//        }
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
        val notificationManager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.navHostFragment)
        return navController.navigateUp()
    }

    private fun messageNotification(message : DisplayMessage, contact : Contact) {
        val notification = NotificationCompat.Builder(applicationContext, "NOTIFICATIONS")
            .setStyle(NotificationCompat.MessagingStyle(Person.Builder().setName(contact.contactName).build())
                .addHistoricMessage(NotificationCompat.MessagingStyle.Message(message.content, Calendar.getInstance().timeInMillis, Person.Builder().setName(contact.contactName).build()))
                .addMessage(NotificationCompat.MessagingStyle.Message(message.content, Calendar.getInstance().timeInMillis, Person.Builder().setName(contact.contactName).build()))
            )
            .setSmallIcon(R.drawable.ic_user)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup("com.cloudsheeptechnologies.apollonchat.CHAT_MESSAGE")
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            notify(message.messageId.toInt(), notification)
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