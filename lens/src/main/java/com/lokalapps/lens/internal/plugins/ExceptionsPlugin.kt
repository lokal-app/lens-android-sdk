package com.lokalapps.lens.internal.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import com.lokalapps.lens.R
import com.lokalapps.lens.api.ComposableLensPlugin
import com.lokalapps.lens.internal.interceptors.LensExceptionHandler
import com.lokalapps.lens.internal.presentation.exceptions.ExceptionsScreen
import timber.log.Timber

/**
 * Exceptions/Crashes inspector plugin for Lens.
 *
 * Captures and displays:
 * - Uncaught exceptions (crashes)
 * - Manually logged handled exceptions
 *
 * ## Architecture:
 * - Uses [LensServiceLocator] for [ExceptionLogRepository] access
 * - MVI pattern for UI state management
 * - Custom [UncaughtExceptionHandler] for crash capture
 *
 * ## How it works:
 * Installs a custom [Thread.UncaughtExceptionHandler] that intercepts uncaught exceptions before
 * they crash the app. The exceptions are stored for viewing in the Lens UI.
 *
 * **Important:** The original exception handler (typically Firebase Crashlytics) is preserved and
 * called after Lens captures the exception.
 *
 * ## Manual Logging:
 * You can manually log handled exceptions for debugging:
 * ```kotlin
 * try {
 *     riskyOperation()
 * } catch (e: Exception) {
 *     LensExceptionHandler.logHandledException(e)
 *     // Handle the exception...
 * }
 * ```
 */
class ExceptionsPlugin : ComposableLensPlugin {

  override val id = "exceptions"
  override val name = "Exceptions"
  override val icon = R.drawable.ic_lens_exceptions
  override val description = "View crash logs and exceptions"
  override val priority = 85

  override fun onInitialize(context: Context) {
    // Install our exception handler to capture uncaught exceptions
    LensExceptionHandler.install()
    Timber.d("ExceptionsPlugin: Exception handler installed")
  }

  override fun onDisabled() {
    // Note: We don't uninstall the handler when Lens closes
    // because we still want to capture exceptions for later viewing.
    // The handler is only uninstalled when Lens is completely disabled.
  }

  @Composable
  override fun Content() {
    ExceptionsScreen()
  }
}
