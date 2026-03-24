package com.sharefast.services.transfer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sharefast.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TransferForegroundService : Service() {

    @Inject
    lateinit var engine: TcpTransferEngine

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground(NOTIFICATION_ID, buildNotification(getString(com.sharefast.R.string.transfer_running)))
            engine.ensureServerRunning()
        } catch (_: Throwable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching { engine.ensureServerRunning() }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        createChannel()
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.sharefast.R.string.app_name))
            .setContentText(text)
            .setSmallIcon(com.sharefast.R.drawable.ic_stat_sharefast)
            .setContentIntent(launch)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(com.sharefast.R.string.transfer_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        mgr.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "sharefast_transfers"
        private const val NOTIFICATION_ID = 42
        fun start(context: Context) {
            val i = Intent(context, TransferForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                @Suppress("DEPRECATION")
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TransferForegroundService::class.java))
        }
    }
}
