package com.shilapi.xcertplay.api

import android.app.Activity
import android.os.Bundle

/**
 * Trampoline Activity for external apps/launchers to trigger connection via Activity Intent.
 */
class ConnectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        val silent = intent.getBooleanExtra(DiPlayApi.EXTRA_SILENT, false)
        val fastRfcomm = intent.getBooleanExtra(DiPlayApi.EXTRA_FAST_RFCOMM, true)
        DiPlayApi.connect(this, silent = silent, fastRfcomm = fastRfcomm)
        finish()
        overridePendingTransition(0, 0)
    }
}

/**
 * Trampoline Activity for external apps/launchers to disconnect via Activity Intent.
 */
class DisconnectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        DiPlayApi.disconnect(this)
        finish()
        overridePendingTransition(0, 0)
    }
}

/**
 * Trampoline Activity for external apps/launchers to open CarPlay UI via Activity Intent.
 */
class OpenCarPlayActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        DiPlayApi.openCarPlay(this)
        finish()
        overridePendingTransition(0, 0)
    }
}
