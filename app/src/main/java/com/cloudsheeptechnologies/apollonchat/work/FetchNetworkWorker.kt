package com.cloudsheeptechnologies.apollonchat.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cloudsheeptechnologies.apollonchat.database.ApollonDatabase
import com.cloudsheeptechnologies.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchNetworkWorker(context : Context, params : WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "FetchNetworkWork"
    }

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            // Periodically fetch the network
            val db = ApollonDatabase.getInstance(context = applicationContext)
            val userDb = db.userDao()
            val user = userDb.getUser() ?: return@withContext Result.failure()
            // Should not overwrite the existing running thingy
            ApollonProtocolHandler.initialize(user.userId.toUInt(), applicationContext)
            // This is bad. Fix the way handling incoming request is done
            Thread.sleep(1000)
        }
        return Result.success()
    }

}