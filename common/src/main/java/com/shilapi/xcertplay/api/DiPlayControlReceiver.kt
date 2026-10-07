package com.shilapi.xcertplay.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver receiving external commands from custom launchers and third-party tools.
 */
class DiPlayControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "DiPlayControlReceiver received action=$action")

        when (action) {
            DiPlayApi.ACTION_CONNECT -> {
                val silent = intent.getBooleanExtra(DiPlayApi.EXTRA_SILENT, false)
                val fastRfcomm = intent.getBooleanExtra(DiPlayApi.EXTRA_FAST_RFCOMM, true)
                DiPlayApi.connect(context, silent = silent, fastRfcomm = fastRfcomm)
            }
            DiPlayApi.ACTION_DISCONNECT -> {
                DiPlayApi.disconnect(context)
            }
            DiPlayApi.ACTION_OPEN_CARPLAY -> {
                DiPlayApi.openCarPlay(context)
            }
            DiPlayApi.ACTION_GET_STATUS -> {
                DiPlayApi.broadcastCurrentStatus(context)
            }
        }
    }

    private companion object {
        const val TAG = "DiPlayControlReceiver"
    }
}
