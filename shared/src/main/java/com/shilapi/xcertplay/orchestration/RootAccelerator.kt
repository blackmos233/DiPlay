package com.shilapi.xcertplay.orchestration

import android.os.Process
import android.util.Log
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Root-level system and kernel performance booster.
 *
 * Accelerates startup on rooted devices by:
 * 1. Elevating CPU governor to 'performance' and pinning task affinity to big cores (e.g. A73 on Kirin 710)
 * 2. Elevating process priority (renice -20) and OOM score (-1000)
 * 3. Tuning kernel TCP low-latency parameters (tcp_low_latency=1, tcp_slow_start_after_idle=0)
 * 4. Disabling Wi-Fi power save on p2p and wlan interfaces
 *
 * Gracefully degrades to a no-op if root (su) is unavailable or not granted.
 */
object RootAccelerator {
    private const val TAG = "RootAccelerator"
    private val isRootAvailable = AtomicBoolean(false)
    private val boosted = AtomicBoolean(false)
    private val SU_BINARIES = listOf(
        "su",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/adb/ksu/bin/su",
        "/data/adb/ap/bin/su",
    )

    /**
     * Checks or actively requests root permission.
     * Does not permanently lock false on rejection so user can grant at any time.
     */
    fun checkRoot(): Boolean {
        if (isRootAvailable.get()) return true
        return try {
            val output = runSuCommand("id")
            val hasSu = output?.contains("uid=0") == true
            if (hasSu) {
                isRootAvailable.set(true)
                Log.i(TAG, "Root permission granted! ($output)")
            }
            hasSu
        } catch (e: Exception) {
            Log.w(TAG, "Root check failed: ${e.message}")
            false
        }
    }

    /**
     * Applies system-wide TCP low-latency parameters and OOM immunity once at startup.
     */
    fun applySystemTuning(log: (String) -> Unit = {}) {
        Thread({
            try {
                if (!checkRoot()) {
                    log("root access not granted or unavailable; running in standard mode")
                    return@Thread
                }
                log("root access confirmed (uid=0); applying system performance tuning")
                val pid = Process.myPid()
                val script = "echo 1 > /proc/sys/net/ipv4/tcp_low_latency; " +
                    "echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle; " +
                    "echo 4194304 > /proc/sys/net/core/rmem_max; " +
                    "echo 4194304 > /proc/sys/net/core/wmem_max; " +
                    "echo -1000 > /proc/$pid/oom_score_adj"
                runSuCommand(script)
                log("root system tuning applied (TCP low-latency & OOM immunity)")
            } catch (t: Throwable) {
                Log.w(TAG, "Root system tuning failed: ${t.message}")
            }
        }, "root-system-tuning").apply { isDaemon = true; start() }
    }

    /**
     * Boosts CPU and task priority to maximum during connection negotiation.
     */
    fun boost(log: (String) -> Unit = {}) {
        if (boosted.getAndSet(true)) return
        Thread({
            try {
                if (!checkRoot()) return@Thread
                val pid = Process.myPid()
                val script = "for gov in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \"${'$'}gov\"; done; " +
                    "taskset -p f0 $pid; " +
                    "renice -n -20 -p $pid; " +
                    "iw dev p2p0 set power_save off 2>/dev/null; " +
                    "iw dev wlan0 set power_save off 2>/dev/null"
                runSuCommand(script)
                log("root performance boost applied (CPU performance, big-core affinity, Wi-Fi PS off)")
            } catch (t: Throwable) {
                Log.w(TAG, "Root boost failed: ${t.message}")
            }
        }, "root-boost").apply { isDaemon = true; start() }
    }

    /**
     * Restores power-efficient governors after connection is established and video is stable.
     */
    fun restore(log: (String) -> Unit = {}) {
        if (!boosted.getAndSet(false)) return
        Thread({
            try {
                if (!checkRoot()) return@Thread
                val script = "for gov in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo schedutil > \"${'$'}gov\"; done"
                runSuCommand(script)
                log("root performance restored to schedutil")
            } catch (t: Throwable) {
                Log.w(TAG, "Root restore failed: ${t.message}")
            }
        }, "root-restore").apply { isDaemon = true; start() }
    }

    private fun runSuCommand(command: String): String? {
        for (su in SU_BINARIES) {
            try {
                val process = ProcessBuilder(su, "-c", command)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val finished = process.waitFor(5, TimeUnit.SECONDS)
                val code = if (finished) process.exitValue() else -1
                Log.d(TAG, "su candidate '$su' exited with code $code, output='$output'")
                if (finished && code == 0) {
                    return output.trim()
                }
            } catch (e: Exception) {
                Log.d(TAG, "su candidate '$su' failed: ${e.message}")
            }
        }
        return null
    }
}
