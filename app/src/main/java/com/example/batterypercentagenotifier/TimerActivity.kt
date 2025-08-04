package com.example.batterypercentagenotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.activity.OnBackPressedCallback

class TimerActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP_TIMER_DIALOG) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "batterypercentagenotifier:AlarmWakelock"
        )
        wl.acquire(5000)
        // Fullscreen & always on screen
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Block back press entirely
            }
        })

        val textView = TextView(this).apply {
            text = "Please disconnect your charger!"
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            textSize = 30f
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            setPadding(50, 300, 50, 300)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        setContentView(textView)
        // Register to receive stop signal
        val filter = IntentFilter(ACTION_STOP_TIMER_DIALOG)
        // Always specify NOT_EXPORTED for safety
        registerReceiver(stopReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        // Play alert sound
        playAlarmSound()
    }

    private fun playAlarmSound() {
        if (mediaPlayer != null) return
        try {
            // Use default alarm sound from Android system
            val uri = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@TimerActivity, uri)
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // fallback: use default notification sound
            val fallbackUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@TimerActivity, fallbackUri)
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                )
                isLooping = true
                prepare()
                start()
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(stopReceiver)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP_TIMER_DIALOG =
            "com.example.batterypercentagenotifier.STOP_TIMER_DIALOG"

        fun show(context: Context) {
            val intent = Intent(context, TimerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(ACTION_STOP_TIMER_DIALOG).apply { setPackage(context.packageName) }
            context.sendBroadcast(intent)
        }
    }
}
