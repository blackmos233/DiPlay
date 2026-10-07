package com.shilapi.xcertplay.api

import android.content.Context
import android.content.Intent
import android.util.Log
import com.shilapi.xcertplay.AirPlayPersistence
import com.shilapi.xcertplay.CarPlayBackgroundSession
import com.shilapi.xcertplay.CarPlayHostActivity
import com.shilapi.xcertplay.DiPlayPreferences
import com.shilapi.xcertplay.DiPlaySessionService

/**
 * Public API Controller for DiPlay external invocation chain and state distribution.
 */
object DiPlayApi {
    private const val TAG = "DiPlayApi"

    const val ACTION_CONNECT = "com.shihab.diplay.action.CONNECT"
    const val ACTION_DISCONNECT = "com.shihab.diplay.action.DISCONNECT"
    const val ACTION_OPEN_CARPLAY = "com.shihab.diplay.action.OPEN_CARPLAY"
    const val ACTION_GET_STATUS = "com.shihab.diplay.action.GET_STATUS"

    const val ACTION_STATUS_CHANGED = "com.shihab.diplay.action.STATUS_CHANGED"

    const val EXTRA_STATUS = "status"
    const val EXTRA_STAGE = "stage"
    const val EXTRA_SILENT = "silent"
    const val EXTRA_FAST_RFCOMM = "fast_rfcomm"
    const val EXTRA_IS_WIRELESS = "is_wireless"
    const val EXTRA_PHONE_ADDRESS = "phone_address"
    const val EXTRA_TIMESTAMP = "timestamp"

    const val STATUS_IDLE = "IDLE"
    const val STATUS_CONNECTING = "CONNECTING"
    const val STATUS_CONNECTED = "CONNECTED"
    const val STATUS_DISCONNECTED = "DISCONNECTED"

    @Volatile var currentStatus: String = STATUS_IDLE
    @Volatile var currentStage: String = ""
    @Volatile var currentIsWireless: Boolean = true
    @Volatile var isSilentSession: Boolean = false

    fun notifyStatus(context: Context, status: String, stage: String, isWireless: Boolean = true) {
        currentStatus = status
        currentStage = stage
        currentIsWireless = isWireless
        Log.i(TAG, "DiPlay Status changed -> status=$status, stage=$stage")

        val phoneAddress = DiPlayPreferences.phoneAddress(context)
        val intent = Intent(ACTION_STATUS_CHANGED).apply {
            putExtra(EXTRA_STATUS, status)
            putExtra(EXTRA_STAGE, stage)
            putExtra(EXTRA_IS_WIRELESS, isWireless)
            putExtra(EXTRA_SILENT, isSilentSession)
            putExtra(EXTRA_PHONE_ADDRESS, phoneAddress)
            putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        }
        // Broadcast to any interested app / launcher
        context.sendBroadcast(intent)
    }

    fun broadcastCurrentStatus(context: Context) {
        val phoneAddress = DiPlayPreferences.phoneAddress(context)
        val intent = Intent(ACTION_STATUS_CHANGED).apply {
            putExtra(EXTRA_STATUS, currentStatus)
            putExtra(EXTRA_STAGE, currentStage)
            putExtra(EXTRA_IS_WIRELESS, currentIsWireless)
            putExtra(EXTRA_SILENT, isSilentSession)
            putExtra(EXTRA_PHONE_ADDRESS, phoneAddress)
            putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        }
        context.sendBroadcast(intent)
    }

    fun connect(context: Context, silent: Boolean = false, fastRfcomm: Boolean = true) {
        Log.i(TAG, "API connect called: silent=$silent, fastRfcomm=$fastRfcomm")
        isSilentSession = silent

        if (CarPlayBackgroundSession.hasSession() && CarPlayBackgroundSession.active) {
            Log.i(TAG, "CarPlay session already active")
            notifyStatus(context, STATUS_CONNECTED, "CarPlay 会话已在运行", isWireless = true)
            if (!silent) {
                openCarPlay(context)
            }
            return
        }

        AirPlayPersistence.saveWirelessEnabled(context, true)
        AirPlayPersistence.saveFastRfcommEnabled(context, fastRfcomm)

        val intent = Intent(context, CarPlayHostActivity::class.java).apply {
            putExtra(EXTRA_SILENT, silent)
            putExtra(EXTRA_FAST_RFCOMM, fastRfcomm)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        context.startActivity(intent)
    }

    fun disconnect(context: Context) {
        Log.i(TAG, "API disconnect called")
        isSilentSession = false
        CarPlayBackgroundSession.stop {
            notifyStatus(context, STATUS_DISCONNECTED, "已断开连接", isWireless = true)
        }
        context.sendBroadcast(Intent(DiPlaySessionService.ACTION_STOP).setPackage(context.packageName))
    }

    fun openCarPlay(context: Context) {
        Log.i(TAG, "API openCarPlay called")
        isSilentSession = false
        val intent = Intent(context, CarPlayHostActivity::class.java).apply {
            putExtra(EXTRA_SILENT, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        context.startActivity(intent)
    }
}
