package com.example.myapplication

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import kotlin.concurrent.thread

class MonitorService : Service() {

    @Volatile
    private var alarmActive = false
    private var downTime: Date? = null

    private val handler = Handler(Looper.getMainLooper())
    private val checkIntervalMs = 1 * 60 * 1000L // 1 minute
    private val urlToCheck = "https://isr.freeddns.org/shares/heartbeat"

    private val checkRunnable = object : Runnable {
        override fun run() {
            thread {
                if (isInternetAvailable()) {
                    checkUrl()
                }
            }
            handler.postDelayed(this, checkIntervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        handler.post(checkRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun isInternetAvailable(): Boolean {
        return try {
            val url = URL("https://clients3.google.com/generate_204")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty("User-Agent", "Android")
            connection.setRequestProperty("Connection", "close")
            connection.connect()

            connection.responseCode == 204
        } catch (e: Exception) {
            Log.e("MonitorService", "Error checking for internet", e)
            false
        }
    }

    private fun checkUrl() {
        thread {
            try {
                val connection = URL(urlToCheck).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != 200) {
                    updateStatus(false)
                    playAlarm()
                } else {
                    updateStatus(true)
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e("MonitorService", "Error checking URL", e)
                updateStatus(false)
                playAlarm()
            }
        }
    }

    private fun updateStatus(isUp: Boolean) {
        val intent = Intent("monitor-status")
        intent.putExtra("isUp", isUp)
        intent.putExtra("lastCheck", Date().time)
        if (!isUp) {
            if (downTime == null) {
                downTime = Date()
            }
            intent.putExtra("downTime", downTime?.time)
        } else {
            downTime = null
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun playAlarm() {
        if (alarmActive) return
        alarmActive = true

        val player = MediaPlayer.create(this, R.raw.alarm)
        player?.start()

        Handler(Looper.getMainLooper()).postDelayed({
            player?.stop()
            player?.release()
            alarmActive = false
        }, 10_000)
    }

    private fun createNotification(): Notification {
        val channelId = "monitor_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "URL Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("URL Monitor Running")
            .setContentText("Checking URL every minute")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
    }
}
