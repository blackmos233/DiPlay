package com.shilapi.xcertplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Starts the CarPlay host after boot when the user has enabled the startup option. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val launchEnabled = AirPlayPersistence.loadAutoStartOnBoot(context)
        StartupDiagnosticSnapshot.received(context, launchEnabled)
        if (!launchEnabled) return

        val silent = AirPlayPersistence.loadSilentBootConnect(context)
        if (silent && DiPlayPreferences.phoneAddress(context) != null) {
            try {
                com.shilapi.xcertplay.api.DiPlayApi.connect(context, silent = true, fastRfcomm = true)
                StartupDiagnosticSnapshot.launchResult(context)
                Log.i(TAG, "Boot auto-start initiated silent connection in background")
                return
            } catch (error: RuntimeException) {
                Log.w(TAG, "Boot silent connect failed, falling back to activity launch", error)
            }
        }

        val launch = Intent(context, DiPlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
        try {
            context.startActivity(launch)
            StartupDiagnosticSnapshot.launchResult(context)
        } catch (error: RuntimeException) {
            StartupDiagnosticSnapshot.launchResult(context, error)
            Log.w(TAG, "Boot auto-start could not launch DiPlayActivity", error)
        }
    }

    private companion object {
        const val TAG = "xcertplay-boot"
    }
}
