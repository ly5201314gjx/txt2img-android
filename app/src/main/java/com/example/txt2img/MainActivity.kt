package com.example.txt2img

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.txt2img.ui.GenerateScreen
import com.example.txt2img.ui.ScreenBackdrop
import com.example.txt2img.ui.theme.Palette
import com.example.txt2img.ui.theme.Txt2ImgTheme
import com.liquidglass.ui.GlassConfig
import com.liquidglass.ui.LiquidGlassHost
import com.liquidglass.ui.TopBarButtonStyle
import com.liquidglass.ui.bottombar.FloatingBottomBar
import com.liquidglass.ui.bottombar.FloatingBottomBarItem
import com.liquidglass.ui.topbar.GlassMediumFlexibleTopAppBar
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 液态玻璃自动分级测试：
 * level 0 = 只挂载层（玻璃不渲染，纯假玻璃外观）
 * level 1 = + 底部导航真玻璃（基础模糊）
 * level 2 = + 完整特效（vibrancy/lens/高光动画）
 *
 * 每次启动：崩了自动降一级重试；活了 1.2 秒自动升一级重试；
 * 自动稳定在可用最高级。全程自动重启，用户零操作。
 */
class MainActivity : ComponentActivity() {

    private companion object {
        const val LEVEL_MAX = 2
        const val SURVIVE_MS = 1200L
    }

    private lateinit var stateFile: File       // 当前级别 "0".."2"
    private lateinit var armedFile: File       // 决策后首次启动标记
    private lateinit var bootFile: File        // 看门狗
    private lateinit var safeFile: File        // 安全模式（禁用玻璃）
    private lateinit var crashLogFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateFile = File(filesDir, "glass_v8_state")
        armedFile = File(filesDir, "glass_v8_armed")
        bootFile = File(filesDir, "glass_v8_boot")
        safeFile = File(filesDir, "glass_v8_safe")
        crashLogFile = File(filesDir, "crash.log")

        // 清除旧版本遗留标志，v8+ 首次启动永远为普通模式
        for (name in listOf(
            "glass_v7_enabled", "glass_v7_boot", "glass_v6_enabled", "glass_v6_boot",
            "glass_enabled", "glass_boot_pending", "glass_safe_mode",
        )) {
            runCatching { File(filesDir, name).delete() }
        }
        // v12：清除 v10 遗留的安全模式与崩溃日志（崩溃原因：lens 特效不支持 RectangleShape，已修复）
        runCatching { safeFile.delete() }
        runCatching { crashLogFile.delete() }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                crashLogFile.writeText("Thread: ${thread.name}\n" + Log.getStackTraceString(throwable))
            }
            runCatching { safeFile.writeText("1") }
            Log.e("Txt2ImgLG", "FATAL", throwable)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        // ── 分级状态机（在任何 UI 之前）──
        // 液态玻璃默认常开（level 2 满特效）；仅当设备出现渲染崩溃时静默降级/禁用
        val sdkOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        var level = runCatching { stateFile.readText().trim().toInt() }.getOrNull() ?: LEVEL_MAX
        val glassDesired = sdkOk && !safeFile.exists()

        if (glassDesired && stateFile.exists()) {
            val armed = armedFile.exists()
            val crashed = bootFile.exists()
            when {
                armed -> {
                    // 决策后的首次测试启动：不算崩溃
                    armedFile.delete()
                }

                crashed -> {
                    // 上次玻璃启动崩溃 → 降级
                    val next = level - 1
                    uploadDiag("level=$level CRASHED -> next=$next")
                    if (next < 0) {
                        safeFile.writeText("1")
                        stateFile.delete()
                        bootFile.delete()
                        armedFile.delete()
                        uploadDiag("glass DISABLED after level $level crash")
                        restart()
                    } else {
                        level = next
                        stateFile.writeText(level.toString())
                        armedFile.writeText("1")
                        bootFile.writeText("1")
                        uploadDiag("level $level CRASHED -> retry level $next")
                        restart()
                    }
                }

                else -> {
                    // 上次存活 → 升级测试
                    if (level < LEVEL_MAX) {
                        level += 1
                        stateFile.writeText(level.toString())
                        armedFile.writeText("1")
                        bootFile.writeText("1")
                        uploadDiag("level ${level - 1} OK -> test level $level")
                        restart()
                    } else {
                        // 稳定在最高级
                        bootFile.writeText("1") // 重新武装看门狗
                        uploadDiag("STABLE level $level")
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            Txt2ImgTheme {
                var currentTab by rememberSaveable { mutableIntStateOf(0) }
                val glassOn = remember { glassDesired }
                val glassLevel = remember { if (glassOn) level else 0 }
                val crashText = remember {
                    if (crashLogFile.exists()) crashLogFile.readText().take(4000) else null
                }

                if (crashText != null) {
                    CrashDialog(
                        text = buildString {
                            append("设备：${Build.MANUFACTURER} ${Build.MODEL}\n")
                            append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                            append("\n$crashText")
                        },
                        onDismiss = {
                            runCatching { crashLogFile.delete() }
                        },
                    )
                }

                LiquidGlassHost(
                    config = GlassConfig(
                        enableBlur = false,
                        topBarButtonStyle = if (glassOn) {
                            TopBarButtonStyle.LiquidGlass
                        } else {
                            TopBarButtonStyle.Tonal
                        },
                        useFlexibleTopAppBar = false,
                        glassLevel = glassLevel,
                    ),
                    backgroundColor = Palette.BgTop,
                    background = {
                        ScreenBackdrop()
                    },
                    // 顶栏：真玻璃（位于内容层之外），按当前 Tab 渲染
                    topBar = {
                        when (currentTab) {
                            0 -> GlassMediumFlexibleTopAppBar(
                                title = "文字生图",
                                subtitle = "输入描述词，AI 帮你生成精美图片",
                            )

                            1 -> GlassMediumFlexibleTopAppBar(
                                title = "作品",
                                subtitle = "点击查看 · 长按多选 / 归类 / 删除",
                            )

                            2 -> GlassMediumFlexibleTopAppBar(
                                title = "我的",
                                subtitle = "多供应商模型服务管理",
                            )
                        }
                    },
                    content = { _ ->
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent,
                        ) {
                            GenerateScreen(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it },
                            )
                        }
                    },
                    // 浮层：底部导航玻璃组件（内容层之外）——原版 FloatingBottomBar 全动效
                    overlay = {
                        FloatingBottomBar(
                            selectedIndex = { currentTab },
                            onSelected = { currentTab = it },
                            tabsCount = 3,
                            hasCustomIcons = true,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 12.dp),
                        ) {
                            FloatingBottomBarItem(
                                onClick = { currentTab = 0 },
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Filled.Draw,
                                    contentDescription = null,
                                    tint = if (currentTab == 0) Palette.Purple else NavInactive,
                                    modifier = Modifier.size(18.dp),
                                )
                                androidx.compose.material3.Text(
                                    "生成",
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentTab == 0) Palette.Purple else NavInactive,
                                )
                            }
                            FloatingBottomBarItem(
                                onClick = { currentTab = 1 },
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    tint = if (currentTab == 1) Palette.Purple else NavInactive,
                                    modifier = Modifier.size(18.dp),
                                )
                                androidx.compose.material3.Text(
                                    "作品",
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentTab == 1) Palette.Purple else NavInactive,
                                )
                            }
                            FloatingBottomBarItem(
                                onClick = { currentTab = 2 },
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = if (currentTab == 2) Palette.Purple else NavInactive,
                                    modifier = Modifier.size(18.dp),
                                )
                                androidx.compose.material3.Text(
                                    "我的",
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentTab == 2) Palette.Purple else NavInactive,
                                )
                            }
                        }
                    },
                )

                // 存活标记：1.2 秒后清除看门狗
                if (glassOn) {
                    LaunchedEffect(Unit) {
                        delay(SURVIVE_MS)
                        runCatching { bootFile.delete() }
                    }
                }

                // 自动升级：存活 2 秒后自动重启测试下一级（用户零操作）
                if (glassOn && glassLevel < LEVEL_MAX) {
                    LaunchedEffect(Unit) {
                        delay(2500)
                        val next = glassLevel + 1
                        runCatching { stateFile.writeText(next.toString()) }
                        runCatching { armedFile.writeText("1") }
                        runCatching { bootFile.writeText("1") }
                        uploadDiag("level $glassLevel OK -> auto test level $next")
                        restart()
                    }
                }

                // 稳定在最高级：上报确认
                if (glassOn && glassLevel >= LEVEL_MAX) {
                    LaunchedEffect(Unit) {
                        delay(2500)
                        uploadDiag("STABLE level $glassLevel (max)")
                    }
                }
            }
        }
    }

    private fun restart() {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK,
        )
        startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    private fun uploadDiag(msg: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { doUpload(msg) }
            }
        }
    }

    // 独立上传：任何 Java 崩溃日志都会上传（不受玻璃开关影响）
    private fun uploadAnyCrash() {
        if (crashLogFile.exists()) {
            uploadDiag("java-crash-detected")
        }
    }

    private fun doUpload(msg: String) {
        val body = buildString {
            append("device=${Build.MANUFACTURER} ${Build.MODEL} api=${Build.VERSION.SDK_INT}\n")
            append("msg=$msg\n")
            if (crashLogFile.exists()) {
                append("crash:\n${crashLogFile.readText().take(8000)}\n")
                crashLogFile.delete()
            }
        }
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://share.lg0304.xyz/upload")
            .header("Content-Type", "text/plain")
            .header("X-File-Name", "diag-${System.currentTimeMillis()}.txt")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()
        client.newCall(request).execute().use { }
    }
}

private val NavInactive = androidx.compose.ui.graphics.Color(0xFF7E7A8F)

@Composable
private fun CrashDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("上次启动发生崩溃") },
        text = { Text(text, fontSize = 11.sp) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
}
