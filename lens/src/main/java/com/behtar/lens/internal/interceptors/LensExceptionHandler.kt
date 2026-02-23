package com.behtar.lens.internal.interceptors

import com.behtar.lens.internal.data.model.ExceptionLogEntry
import com.behtar.lens.internal.data.repository.ExceptionLogRepository
import com.behtar.lens.internal.di.LensServiceLocator
import timber.log.Timber

/**
 * Custom UncaughtExceptionHandler that captures exceptions to Lens.
 *
 * This handler wraps the existing default handler (typically Firebase Crashlytics) to also capture
 * exceptions for local debugging in Lens.
 *
 * ## Key Features:
 * - Captures all uncaught exceptions for Lens inspection
 * - Preserves and delegates to the original handler (Crashlytics, etc.)
 * - Provides manual logging API for handled exceptions
 *
 * ## Usage:
 * ```kotlin
 * // During Lens initialization
 * LensExceptionHandler.install()
 *
 * // Log handled exceptions
 * LensExceptionHandler.logHandledException(exception)
 * ```
 */
class LensExceptionHandler private constructor() : Thread.UncaughtExceptionHandler {

  private val repository: ExceptionLogRepository
    get() = LensServiceLocator.exceptionLogRepository

  private var originalHandler: Thread.UncaughtExceptionHandler? = null

  override fun uncaughtException(thread: Thread, throwable: Throwable) {
    try {
      // Log to Lens repository
      val entry =
          ExceptionLogEntry.fromThrowable(
              throwable = throwable,
              threadName = thread.name,
              isHandled = false,
              additionalInfo =
                  mapOf(
                      "Thread ID" to thread.id.toString(),
                      "Thread Priority" to thread.priority.toString()))
      repository.addEntry(entry)

      Timber.e(
          throwable, "LensExceptionHandler: Captured uncaught exception on thread '${thread.name}'")
    } catch (e: Exception) {
      // Don't let our handler crash and prevent the original handler from running
      Timber.e(e, "LensExceptionHandler: Error capturing exception")
    }

    // Always delegate to original handler (Crashlytics, etc.)
    originalHandler?.uncaughtException(thread, throwable)
  }

  companion object {
    private var instance: LensExceptionHandler? = null
    private var isInstalled = false

    /**
     * Installs the exception handler.
     *
     * The previous handler is preserved and will be called after Lens captures the exception,
     * ensuring crash reporting still works.
     */
    @Synchronized
    fun install() {
      if (isInstalled) {
        Timber.d("LensExceptionHandler: Already installed")
        return
      }

      val handler = LensExceptionHandler()

      // Save original handler (e.g., Firebase Crashlytics)
      handler.originalHandler = Thread.getDefaultUncaughtExceptionHandler()

      // Install our handler
      Thread.setDefaultUncaughtExceptionHandler(handler)
      instance = handler
      isInstalled = true

      Timber.d(
          "LensExceptionHandler: Installed (original: ${handler.originalHandler?.javaClass?.simpleName})")
    }

    /** Uninstalls the handler and restores the original. */
    @Synchronized
    fun uninstall() {
      val handler = instance ?: return
      if (!isInstalled) return

      Thread.setDefaultUncaughtExceptionHandler(handler.originalHandler)
      handler.originalHandler = null
      instance = null
      isInstalled = false

      Timber.d("LensExceptionHandler: Uninstalled")
    }

    /**
     * Manually logs an exception to Lens without crashing. Useful for logging handled exceptions
     * for debugging.
     *
     * @param throwable The exception to log
     * @param additionalInfo Optional extra context
     */
    fun logHandledException(
        throwable: Throwable,
        additionalInfo: Map<String, String> = emptyMap()
    ) {
      instance
          ?.repository
          ?.logException(throwable = throwable, isHandled = true, additionalInfo = additionalInfo)
          ?: Timber.w("LensExceptionHandler: Not installed, cannot log exception")
    }

    /** Returns whether the handler is currently installed. */
    fun isInstalled(): Boolean = isInstalled
  }
}
