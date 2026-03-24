package com.sharefast.services.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sharefast.R
import com.sharefast.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nm get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_COMPLETE,
                context.getString(R.string.transfer_complete_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.transfer_complete_channel_desc) }
            nm.createNotificationChannel(ch)
        }
    }

    fun notifyReceiveComplete(
        peerName: String,
        fileCount: Int,
        lastUriString: String?,
        lastFileName: String?,
    ) {
        val intent = mainIntent().apply {
            lastUriString?.let {
                putExtra(MainActivity.EXTRA_OPEN_URI, it)
                lastFileName?.let { n -> putExtra(MainActivity.EXTRA_OPEN_FILENAME, n) }
            } ?: putExtra(MainActivity.EXTRA_OPEN_TRANSFERS, true)
        }
        val pi = PendingIntent.getActivity(
            context,
            REQ_RECEIVE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = context.getString(R.string.notif_receive_done_title)
        val text = context.resources.getQuantityString(
            R.plurals.notif_receive_done_body,
            fileCount,
            fileCount,
            peerName,
        )
        show(NotifIds.RECEIVE_DONE, title, text, pi)
    }

    fun notifySendComplete(peerName: String, fileCount: Int) {
        val intent = mainIntent().apply { putExtra(MainActivity.EXTRA_OPEN_TRANSFERS, true) }
        val pi = PendingIntent.getActivity(
            context,
            REQ_SEND,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = context.getString(R.string.notif_send_done_title)
        val text = context.resources.getQuantityString(
            R.plurals.notif_send_done_body,
            fileCount,
            fileCount,
            peerName,
        )
        show(NotifIds.SEND_DONE, title, text, pi)
    }

    private fun mainIntent() = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    private fun show(id: Int, title: String, text: String, contentIntent: PendingIntent) {
        val n = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(id, n)
    }

    private object NotifIds {
        const val RECEIVE_DONE = 7101
        const val SEND_DONE = 7102
    }

    companion object {
        private const val CHANNEL_COMPLETE = "sharefast_transfer_complete"
        private const val REQ_RECEIVE = 9101
        private const val REQ_SEND = 9102
    }
}
