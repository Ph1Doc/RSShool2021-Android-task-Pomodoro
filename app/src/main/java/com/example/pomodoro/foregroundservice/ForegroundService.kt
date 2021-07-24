package com.example.pomodoro.foregroundservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.WpsInfo.INVALID
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.pomodoro.MainActivity
import com.example.pomodoro.R
import kotlinx.coroutines.*

class ForegroundService : Service() {

    private var isServiceStarted = false
    private var notificationManager: NotificationManager? = null
    private var job: Job? = null
    private var runTimer = 0L

    private val builder by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pomodoro timer")
            .setGroup("Timer")
            .setGroupSummary(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setSilent(true)
            .setSmallIcon(R.drawable.ic_baseline_access_alarm_24)
    }

    private val builderFinish by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pomodoro timer")
            .setGroup("Timer")
            .setGroupSummary(false)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setSmallIcon(R.drawable.ic_baseline_access_alarm_24)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        processCommand(intent)
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun processCommand(intent: Intent?) {
        when (intent?.extras?.getString(COMMAND_ID) ?: INVALID) {
            COMMAND_START -> {
                val startTime = intent?.extras?.getLong(STARTED_TIMER_TIME_MS) ?: return
                commandStart(startTime)
            }
            COMMAND_STOP -> commandStop()
            INVALID -> return
        }
    }

    private fun commandStart(startTime: Long) {
        if (isServiceStarted) {
            return
        }
        try {
            moveToStartedState()
            if (startTime.compareTo(0) != 0) {
                startForegroundAndShowNotification()
            }
            continueTimer(startTime)
        } finally {
            isServiceStarted = true
        }
    }

    private fun continueTimer(startTime: Long) {
        runTimer = System.currentTimeMillis()
        if (startTime.compareTo(0) != 0) {
            job = GlobalScope.launch(Dispatchers.Main) {
                while ((runTimer + startTime - System.currentTimeMillis()) > 0L) {
                    notificationManager?.notify(
                        NOTIFICATION_ID,
                        getNotification(
                            (runTimer + startTime - System.currentTimeMillis()).displayTime().dropLast(3)
                        )
                    )
                    delay(INTERVAL)
                }
                if ((runTimer + startTime - System.currentTimeMillis()) <= 0L) {
                    notificationManager?.notify(
                        NOTIFICATION_ID,
                        getNotificationAboutFinish()
                    )
                }
            }
        }

    }

    private fun commandStop() {
        if (!isServiceStarted) {
            return
        }
        try {
            job?.cancel()
            stopForeground(true)
            stopSelf()
        } finally {
            isServiceStarted = false
        }
    }

    private fun moveToStartedState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, ForegroundService::class.java))
        } else {
            startService(Intent(this, ForegroundService::class.java))
        }
    }

    private fun startForegroundAndShowNotification() {
        createChannel()
        val notification = getNotification("Please start the timer")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getNotification(content: String) = builder.setContentText(content).build()
    private fun getNotificationAboutFinish() = builderFinish.setContentText("Time up").build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "pomodoro"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, channelName, importance
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    private fun getPendingIntent(): PendingIntent? {
        val resultIntent = Intent(this, MainActivity::class.java)
        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        return PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT)
    }

    private companion object {

        private const val CHANNEL_ID = "Channel_ID"
        private const val NOTIFICATION_ID = 777
        private const val INTERVAL = 1000L
    }
}