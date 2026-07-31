package com.example.txt2img.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.txt2img.MainActivity
import com.example.txt2img.R

/**
 * 通知工具：渠道创建 + 生图完成推送。
 */
object Notifier {

    const val CHANNEL_GEN = "gen_channel"
    const val CHANNEL_KEEP = "keep_channel"

    private const val GEN_NOTIF_ID = 2001
    private const val KEEP_NOTIF_ID = 2002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GEN,
                "生图完成",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "图片生成完成时推送"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_KEEP,
                "后台生成",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "生成期间的持续通知（不打扰）"
            },
        )
    }

    /** 生图完成 → 通知栏推送。无权限时静默跳过。 */
    fun notifyGenerationDone(context: Context, count: Int, prompt: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannels(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val brief = if (prompt.length > 24) prompt.take(24) + "…" else prompt
        val notification = NotificationCompat.Builder(context, CHANNEL_GEN)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("图片生成完成")
            .setContentText("已生成 $count 张 · $brief")
            .setStyle(NotificationCompat.BigTextStyle().bigText(prompt))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(GEN_NOTIF_ID, notification)
        }
    }

    /** 生成期间的前台服务通知。 */
    fun buildKeepAliveNotification(context: Context): NotificationCompat.Builder {
        val contentIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_KEEP)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("文字生图")
            .setContentText("正在后台生成图片，保持运行中…")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    const val KEEP_NOTIFICATION_ID: Int = KEEP_NOTIF_ID
}
