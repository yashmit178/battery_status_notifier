package com.example.batterypercentagenotifier

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.AlarmClock
import android.util.Log
import androidx.core.app.NotificationCompat
import android.os.Handler
import android.os.Looper

class BatteryMonitorService : Service() {
    companion object {
        private const val CHANNEL_ID = "battery_monitor_channel"
        private const val NOTIF_ID = 1001
        private const val TAG = "BatteryMonitorService"
        private const val REPEAT_NOTIF_INTERVAL = 1000L
    }

    private var batteryReceiverRegistered = false
    private val handler = Handler(Looper.getMainLooper())
    private var repeating = false
    private val repeatNotificationRunnable = object : Runnable {
        override fun run() {
            repeatDisconnectChargerNotification()
            handler.postDelayed(this, REPEAT_NOTIF_INTERVAL)
        }
    }
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            // Battery state
            val status = intent.getIntExtra("status", -1)
            val plugged = intent.getIntExtra("plugged", -1)
            val level = intent.getIntExtra("level", -1)
            val scale = intent.getIntExtra("scale", 100)
            val percent = ((level * 100f) / scale).toInt()
            Log.d(
                TAG,
                "onReceive: status=$status plugged=$plugged level=$level scale=$scale percent=$percent"
            )

            // Only care when CHARGING
            val isCharging =
                status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
            if (!isCharging) {
                Log.d(
                    TAG,
                    "Device is not charging. Skipping trigger. Clearing lastTriggeredPercent."
                )
                ThresholdPrefs.clearLastTriggeredPercent(this@BatteryMonitorService)
                // Dismiss the timer dialog if it's visible
                TimerActivity.stop(this@BatteryMonitorService)
                stopRepeatingNotifications()
                return
            }

            val threshold = ThresholdPrefs.getThreshold(this@BatteryMonitorService)
            val activeStart = threshold - 3
            val lastTriggered = ThresholdPrefs.getLastTriggeredPercent(this@BatteryMonitorService)

            Log.d(
                TAG,
                "Threshold=$threshold, activeStart=$activeStart, lastTriggered=$lastTriggered"
            )

            if (percent <= activeStart) {
                Log.d(
                    TAG,
                    "percent $percent is below activeStart $activeStart. Clearing lastTriggeredPercent and waiting for it to rise above."
                )
                ThresholdPrefs.clearLastTriggeredPercent(this@BatteryMonitorService)
                // Dismiss the timer dialog if it's visible
                TimerActivity.stop(this@BatteryMonitorService)
                stopRepeatingNotifications()
                return;
            }

            // When monitoring range
            if (percent >= threshold) {
                Log.d(TAG, "percent $percent >= threshold $threshold")
                if (lastTriggered != percent) {
                    Log.d(
                        TAG,
                        "Triggering alarm/timer at $percent (lastTriggered was $lastTriggered)"
                    )
                    triggerOneSecondTimer()
                    ThresholdPrefs.setLastTriggeredPercent(this@BatteryMonitorService, percent)
                } else {
                    Log.d(TAG, "Already triggered for percent $percent, not triggering again.")
                }
                startRepeatingNotifications()
            } else {
                Log.d(
                    TAG,
                    "percent $percent is in monitoring range but < threshold $threshold, so not triggering yet."
                )
                stopRepeatingNotifications()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerBatteryReceiver()
    }

    override fun onDestroy() {
        unregisterReceiverSafe()
        stopRepeatingNotifications()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started via onStartCommand")
        createNotificationChannel()
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Monitoring Active")
            .setContentText("Will notify when charge reaches your set threshold.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
        registerBatteryReceiver()
        return START_STICKY
    }

    private fun registerBatteryReceiver() {
        if (!batteryReceiverRegistered) {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryReceiverRegistered = true
        }
    }

    private fun unregisterReceiverSafe() {
        if (batteryReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {
            }
            batteryReceiverRegistered = false
        }
    }

    private fun triggerOneSecondTimer() {
        // Use a full-screen notification to launch TimerActivity (works on Android 14+)
        val intent = Intent(this, TimerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val channelId = "disconnect_charger_alert"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Disconnect Charger Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Disconnect Charger full screen alert"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Please disconnect your charger!")
            .setContentText("Battery has reached your set threshold.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .build()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(5432, notification) // Use a stable arbitrary ID
    }

    private fun showTimerFallbackNotification() {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Threshold Reached!")
            .setContentText("(Timer unavailable) Battery has hit your set percentage.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID + 1, notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startRepeatingNotifications() {
        if (!repeating) {
            repeating = true
            handler.post(repeatNotificationRunnable)
        }
    }

    private fun stopRepeatingNotifications() {
        if (repeating) {
            handler.removeCallbacks(repeatNotificationRunnable)
            repeating = false
        }
    }

    private fun repeatDisconnectChargerNotification() {
        // Use the same notification as triggerOneSecondTimer but without focusing the activity every time
        val channelId = "disconnect_charger_alert"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Disconnect Charger Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Disconnect Charger full screen alert"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Please disconnect your charger!")
            .setContentText("Battery has reached your set threshold.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            // No fullScreenIntent: only the first notification launches the UI
            .build()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(5432, notification) // Use a stable arbitrary ID
    }
}
