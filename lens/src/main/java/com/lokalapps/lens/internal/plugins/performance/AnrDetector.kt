package com.lokalapps.lens.internal.plugins.performance

import android.os.Handler
import android.os.Looper
import com.lokalapps.lens.internal.data.model.ExceptionLogEntry
import com.lokalapps.lens.internal.di.LensServiceLocator
import timber.log.Timber

/**
 * Detects Application Not Responding (ANR) conditions by posting a watchdog [Runnable] to the main
 * thread [Looper] at regular intervals.
 *
 * **How it works:**
 * 1. A background watchdog thread posts a no-op [Runnable] to the main [Handler].
 * 2. It then sleeps for [TIMEOUT_MS].
 * 3. If the Runnable has **not** executed by the time the watchdog wakes up, the main thread is
 *    blocked — an ANR condition.
 * 4. The detector captures the main thread's stack trace and logs it as an exception entry in
 *    [LensServiceLocator.exceptionLogRepository].
 *
 * This mirrors the approach used by Facebook ANR-WatchDog and LeakCanary. The default timeout is 5
 * seconds, matching Android's system ANR threshold.
 */
internal class AnrDetector(
    private val timeoutMs: Long = TIMEOUT_MS,
) {

  companion object {
    /** Default ANR timeout in milliseconds (matches Android system ANR threshold). */
    private const val TIMEOUT_MS = 5_000L

    /** Minimum cooldown between consecutive ANR reports to avoid log spam. */
    private const val COOLDOWN_MS = 10_000L
  }

  private val mainHandler = Handler(Looper.getMainLooper())

  @Volatile private var isRunning = false
  @Volatile private var responded = false

  private var watchdogThread: Thread? = null
  private var lastAnrTimestamp = 0L

  /** Start the ANR watchdog. Safe to call multiple times. */
  fun start() {
    if (isRunning) return
    isRunning = true

    watchdogThread =
        Thread(
                {
                  Timber.d("AnrDetector: Watchdog started (timeout=${timeoutMs}ms)")

                  while (isRunning && !Thread.currentThread().isInterrupted) {
                    responded = false

                    // Post a marker to the main thread
                    mainHandler.post { responded = true }

                    try {
                      Thread.sleep(timeoutMs)
                    } catch (_: InterruptedException) {
                      break
                    }

                    if (!isRunning) break

                    if (!responded) {
                      val now = System.currentTimeMillis()
                      if (now - lastAnrTimestamp >= COOLDOWN_MS) {
                        lastAnrTimestamp = now
                        reportAnr()
                      }
                    }
                  }

                  Timber.d("AnrDetector: Watchdog stopped")
                },
                "Lens-ANR-Watchdog")
            .also {
              it.isDaemon = true
              it.start()
            }
  }

  /** Stop the ANR watchdog. */
  fun stop() {
    isRunning = false
    watchdogThread?.interrupt()
    watchdogThread = null
  }

  private fun reportAnr() {
    val mainThread = Looper.getMainLooper().thread
    val stackTrace = mainThread.stackTrace

    val traceString = buildString {
      appendLine("ANR detected — main thread blocked for >${timeoutMs}ms")
      appendLine()
      appendLine("Main thread stack trace:")
      for (element in stackTrace) {
        appendLine("    at $element")
      }
    }

    Timber.w("AnrDetector: ANR detected!\n$traceString")

    // Log as an exception entry so it appears in the Exceptions plugin
    try {
      val entry =
          ExceptionLogEntry(
              threadName = mainThread.name,
              exceptionClass = "ANR (Application Not Responding)",
              message = "Main thread blocked for >${timeoutMs}ms",
              stackTrace = traceString,
              isHandled = false,
              additionalInfo =
                  mapOf(
                      "type" to "ANR",
                      "timeout_ms" to timeoutMs.toString(),
                      "main_thread_state" to mainThread.state.name,
                  ),
          )
      LensServiceLocator.exceptionLogRepository.addEntry(entry)
    } catch (e: Exception) {
      Timber.e(e, "AnrDetector: Failed to log ANR entry")
    }
  }
}
