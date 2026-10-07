package com.diplay.tester

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TesterActivity : Activity() {

    private lateinit var statusBadge: TextView
    private lateinit var stageText: TextView
    private lateinit var logContainer: LinearLayout
    private lateinit var logScrollView: ScrollView

    private var lastStatusText: String = "⚪ 等待接收状态..."
    private var lastStatusBgColor: Int = Color.rgb(60, 60, 65)
    private var lastStageText: String = "当前阶段: 尚未连接"
    private val logHistory = mutableListOf<String>()

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getStringExtra("status") ?: "UNKNOWN"
            val stage = intent.getStringExtra("stage") ?: ""
            val silent = intent.getBooleanExtra("silent", false)
            val isWireless = intent.getBooleanExtra("is_wireless", true)

            updateStatusUi(status, stage, silent, isWireless)
            addLog("收到广播: status=$status, silent=$silent, stage=[$stage]")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())

        val filter = IntentFilter("com.shihab.diplay.action.STATUS_CHANGED")
        registerReceiver(statusReceiver, filter)

        // Query status upon opening
        sendStatusBroadcast("com.shihab.diplay.action.GET_STATUS")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(statusReceiver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(buildUi())
    }

    private fun updateStatusUi(status: String, stage: String, silent: Boolean, isWireless: Boolean) {
        val (text, bgColor) = when (status) {
            "CONNECTED" -> "🟢 CONNECTED (已连接)" to Color.rgb(38, 166, 91)
            "CONNECTING" -> "🟡 CONNECTING (连接中...)" to Color.rgb(230, 126, 34)
            "DISCONNECTED" -> "🔴 DISCONNECTED (已断开)" to Color.rgb(217, 30, 24)
            "IDLE" -> "⚪ IDLE (空闲/未连接)" to Color.rgb(108, 122, 137)
            else -> status to Color.rgb(108, 122, 137)
        }

        lastStatusText = text
        lastStatusBgColor = bgColor
        lastStageText = if (stage.isNotBlank()) "当前阶段: $stage" else "当前阶段: 无"

        if (::statusBadge.isInitialized) {
            statusBadge.text = text
            statusBadge.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(8).toFloat()
            }
        }
        if (::stageText.isInitialized) {
            stageText.text = lastStageText
        }
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val entry = "[$time] $message"
        logHistory.add(entry)
        if (logHistory.size > 200) {
            logHistory.removeAt(0)
        }
        appendLogView(entry)
    }

    private fun appendLogView(entry: String) {
        if (!::logContainer.isInitialized) return
        val logView = TextView(this).apply {
            text = entry
            setTextColor(Color.rgb(200, 200, 200))
            textSize = 12f
            setPadding(0, dp(2), 0, dp(2))
        }
        logContainer.addView(logView)
        if (::logScrollView.isInitialized) {
            logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun sendStatusBroadcast(action: String, silent: Boolean = false, fastRfcomm: Boolean = true) {
        val intent = Intent(action).apply {
            setPackage("com.shihab.diplay")
            putExtra("silent", silent)
            putExtra("fast_rfcomm", fastRfcomm)
        }
        sendBroadcast(intent)
        addLog("已发送广播: $action (silent=$silent, fast_rfcomm=$fastRfcomm)")
    }

    private fun launchTrampolineActivity(className: String, silent: Boolean = false) {
        val intent = Intent().apply {
            component = ComponentName("com.shihab.diplay", className)
            putExtra("silent", silent)
            putExtra("fast_rfcomm", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            startActivity(intent)
            overridePendingTransition(0, 0)
            addLog("已启动路由 Activity: $className (silent=$silent)")
        }.onFailure {
            addLog("启动路由失败: ${it.message}")
        }
    }

    private fun buildUi(): View {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return if (isLandscape) buildLandscapeUi() else buildPortraitUi()
    }

    private fun buildLandscapeUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(18, 18, 20))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        // Left Panel (Scrollable Controls)
        val leftScroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val leftContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, dp(10), 0)
        }

        val title = TextView(this).apply {
            text = "DiPlay 控制与调试面板"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        }
        leftContent.addView(title)

        // Status Card
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.rgb(30, 30, 34))
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        statusBadge = TextView(this).apply {
            text = lastStatusText
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = GradientDrawable().apply {
                setColor(lastStatusBgColor)
                cornerRadius = dp(8).toFloat()
            }
        }
        statusCard.addView(statusBadge)

        stageText = TextView(this).apply {
            text = lastStageText
            setTextColor(Color.rgb(220, 220, 220))
            textSize = 12f
            setPadding(0, dp(4), 0, 0)
        }
        statusCard.addView(stageText)
        leftContent.addView(statusCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        // Section 1: Cold start
        val coldLabel = TextView(this).apply {
            text = "❄️ 冷启动唤醒（主程序被杀/开机时用）："
            setTextColor(Color.rgb(241, 196, 15))
            textSize = 12f
            setPadding(0, dp(8), 0, dp(2))
        }
        leftContent.addView(coldLabel)

        val btnRowCold = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnActivityConnect = createButton("⚡ 冷启动静默 (Activity)", Color.rgb(41, 128, 185)) {
            launchTrampolineActivity("com.shilapi.xcertplay.api.ConnectActivity", silent = true)
        }
        val btnActivityOpen = createButton("📱 拉起界面 (Activity)", Color.rgb(39, 174, 96)) {
            launchTrampolineActivity("com.shilapi.xcertplay.api.OpenCarPlayActivity")
        }
        btnRowCold.addView(btnActivityConnect, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(6) })
        btnRowCold.addView(btnActivityOpen, LinearLayout.LayoutParams(0, dp(40), 1f))
        leftContent.addView(btnRowCold)

        // Section 2: Hot control
        val hotLabel = TextView(this).apply {
            text = "🔥 热控制广播（主程序存活时使用，零抢焦）："
            setTextColor(Color.rgb(180, 180, 180))
            textSize = 12f
            setPadding(0, dp(8), 0, dp(2))
        }
        leftContent.addView(hotLabel)

        val btnRowHot1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnSilentConnect = createButton("🚀 后台静默连 (广播)", Color.rgb(52, 152, 219)) {
            sendStatusBroadcast("com.shihab.diplay.action.CONNECT", silent = true, fastRfcomm = true)
        }
        val btnOpenCarPlay = createButton("📱 打开 CarPlay (广播)", Color.rgb(46, 204, 113)) {
            sendStatusBroadcast("com.shihab.diplay.action.OPEN_CARPLAY")
        }
        btnRowHot1.addView(btnSilentConnect, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(6) })
        btnRowHot1.addView(btnOpenCarPlay, LinearLayout.LayoutParams(0, dp(40), 1f))
        leftContent.addView(btnRowHot1)

        val btnRowHot2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnDisconnect = createButton("🛑 断开连接 (广播)", Color.rgb(231, 76, 60)) {
            sendStatusBroadcast("com.shihab.diplay.action.DISCONNECT")
        }
        val btnQuery = createButton("🔄 刷新状态 (GET_STATUS)", Color.rgb(52, 73, 94)) {
            sendStatusBroadcast("com.shihab.diplay.action.GET_STATUS")
        }
        btnRowHot2.addView(btnDisconnect, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(6) })
        btnRowHot2.addView(btnQuery, LinearLayout.LayoutParams(0, dp(40), 1f))
        leftContent.addView(btnRowHot2, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        leftScroll.addView(leftContent, ViewGroup.LayoutParams(-1, -2))
        root.addView(leftScroll, LinearLayout.LayoutParams(0, -1, 1.15f))

        // Right Panel (Log Panel)
        val rightPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }

        val logLabel = TextView(this).apply {
            text = "接收到的实时广播与事件日志:"
            setTextColor(Color.rgb(180, 180, 180))
            textSize = 13f
            setPadding(0, 0, 0, dp(4))
        }
        rightPanel.addView(logLabel)

        logContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        logScrollView = ScrollView(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.rgb(26, 26, 28))
                cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(8), dp(6), dp(8), dp(6))
            addView(logContainer, ViewGroup.LayoutParams(-1, -2))
        }
        rightPanel.addView(logScrollView, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(rightPanel, LinearLayout.LayoutParams(0, -1, 0.85f))

        for (log in logHistory) {
            appendLogView(log)
        }

        return root
    }

    private fun buildPortraitUi(): View {
        val scrollRoot = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(18, 18, 20))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val title = TextView(this).apply {
            text = "DiPlay 外部调用链 & 后台静默连接调试器"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
        }
        content.addView(title)

        // Status Card
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.rgb(30, 30, 34))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        statusBadge = TextView(this).apply {
            text = lastStatusText
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(lastStatusBgColor)
                cornerRadius = dp(8).toFloat()
            }
        }
        statusCard.addView(statusBadge)

        stageText = TextView(this).apply {
            text = lastStageText
            setTextColor(Color.rgb(220, 220, 220))
            textSize = 14f
            setPadding(0, dp(8), 0, 0)
        }
        statusCard.addView(stageText)
        content.addView(statusCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        // Section 1: Cold start
        val coldLabel = TextView(this).apply {
            text = "❄️ 冷启动唤醒（主程序被杀/开机时用）："
            setTextColor(Color.rgb(241, 196, 15))
            textSize = 12f
            setPadding(0, dp(12), 0, dp(2))
        }
        content.addView(coldLabel)

        val btnRowCold = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnActivityConnect = createButton("⚡ 冷启动静默 (Activity)", Color.rgb(41, 128, 185)) {
            launchTrampolineActivity("com.shilapi.xcertplay.api.ConnectActivity", silent = true)
        }
        val btnActivityOpen = createButton("📱 拉起界面 (Activity)", Color.rgb(39, 174, 96)) {
            launchTrampolineActivity("com.shilapi.xcertplay.api.OpenCarPlayActivity")
        }
        btnRowCold.addView(btnActivityConnect, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(8) })
        btnRowCold.addView(btnActivityOpen, LinearLayout.LayoutParams(0, dp(44), 1f))
        content.addView(btnRowCold)

        // Section 2: Hot control
        val hotLabel = TextView(this).apply {
            text = "🔥 热控制广播（主程序存活时使用，零抢焦）："
            setTextColor(Color.rgb(180, 180, 180))
            textSize = 12f
            setPadding(0, dp(10), 0, dp(2))
        }
        content.addView(hotLabel)

        val btnRowHot1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnSilentConnect = createButton("🚀 后台静默连 (广播)", Color.rgb(52, 152, 219)) {
            sendStatusBroadcast("com.shihab.diplay.action.CONNECT", silent = true, fastRfcomm = true)
        }
        val btnOpenCarPlay = createButton("📱 打开 CarPlay (广播)", Color.rgb(46, 204, 113)) {
            sendStatusBroadcast("com.shihab.diplay.action.OPEN_CARPLAY")
        }
        btnRowHot1.addView(btnSilentConnect, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(8) })
        btnRowHot1.addView(btnOpenCarPlay, LinearLayout.LayoutParams(0, dp(44), 1f))
        content.addView(btnRowHot1)

        val btnRowHot2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnDisconnect = createButton("🛑 断开连接 (广播)", Color.rgb(231, 76, 60)) {
            sendStatusBroadcast("com.shihab.diplay.action.DISCONNECT")
        }
        val btnQuery = createButton("🔄 刷新状态 (GET_STATUS)", Color.rgb(52, 73, 94)) {
            sendStatusBroadcast("com.shihab.diplay.action.GET_STATUS")
        }
        btnRowHot2.addView(btnDisconnect, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(8) })
        btnRowHot2.addView(btnQuery, LinearLayout.LayoutParams(0, dp(44), 1f))
        content.addView(btnRowHot2, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        // Log Panel
        val logLabel = TextView(this).apply {
            text = "接收到的实时广播与事件日志:"
            setTextColor(Color.rgb(180, 180, 180))
            textSize = 13f
            setPadding(0, dp(14), 0, dp(4))
        }
        content.addView(logLabel)

        logContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        logScrollView = ScrollView(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.rgb(26, 26, 28))
                cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(10), dp(8), dp(10), dp(8))
            addView(logContainer, ViewGroup.LayoutParams(-1, -2))
        }
        content.addView(logScrollView, LinearLayout.LayoutParams(-1, dp(180)).apply { topMargin = dp(4) })

        scrollRoot.addView(content, ViewGroup.LayoutParams(-1, -2))

        for (log in logHistory) {
            appendLogView(log)
        }

        return scrollRoot
    }

    private fun createButton(title: String, color: Int, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 12.5f
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(color)
            setOnClickListener { onClick() }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
