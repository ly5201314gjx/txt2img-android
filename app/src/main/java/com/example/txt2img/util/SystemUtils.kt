package com.example.txt2img.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * 系统能力工具：通知权限 / 电池优化豁免。
 */
object SystemUtils {

    /** 是否拥有通知权限（Android 13 以下恒为 true）。 */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** 是否已被允许忽略电池优化（false = 后台受限）。 */
    fun isBatteryOptimized(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (pm == null) return true
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** 请求忽略电池优化（弹系统授权框），失败则跳到电池设置页。 */
    fun requestBatteryExemption(context: Context) {
        if (!isBatteryOptimized(context)) return
        try {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}
