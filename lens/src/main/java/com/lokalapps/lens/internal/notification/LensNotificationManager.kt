package com.lokalapps.lens.internal.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lokalapps.lens.R
import com.lokalapps.lens.internal.di.LensServiceLocator
import com.lokalapps.lens.internal.presentation.dashboard.LensDashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages a sticky notification that shows live network request and error counts.
 *
 * Similar to Chucker's notification-based access pattern:
 * - Shows a persistent notification while Lens is active
 * - Displays request count + error count in real time
 * - Tapping opens the Lens dashboard
 * - "Clear" action resets all network logs
 *
 * On Android 13+ (API 33), the notification requires [Manifest.permission.POST_NOTIFICATIONS]. If
 * the permission is not granted, the notification is silently skipped — the floating bubble still
 * provides access to the dashboard.
 */
internal class LensNotificationManager(private val context: Context) {

  private val notificationManager = NotificationManagerCompat.from(context)
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var observeJob: Job? = null

  /** Creates the notification channel and starts observing network logs. */
  fun start() {
    createChannel()
    observeAndUpdate()
    Timber.d("Lens: Notification manager started")
  }

  /** Stops observing and dismisses the notification. */
  fun stop() {
    observeJob?.cancel()
    observeJob = null
    scope.cancel()
    notificationManager.cancel(NOTIFICATION_ID)
    Timber.d("Lens: Notification manager stopped")
  }

  /**
   * Dismisses the notification and resets counters. Called when the user taps the "Clear" action.
   */
  fun clearAndDismiss() {
    LensServiceLocator.networkLogRepository.clear()
    // Notification will auto-update via Flow observation
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
          NotificationChannel(CHANNEL_ID, "Lens Debug", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows live network request and error counts"
            setShowBadge(false)
          }
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun observeAndUpdate() {
    val networkRepo = LensServiceLocator.networkLogRepository
    val exceptionRepo = LensServiceLocator.exceptionLogRepository

    observeJob =
        scope.launch {
          combine(
                  networkRepo.logs.map { logs ->
                    val total = logs.size
                    val errors =
                        logs.count {
                          it.statusType in
                              setOf(
                                  com.lokalapps.lens.internal.data.model.NetworkLogEntry.StatusType
                                      .ERROR,
                                  com.lokalapps.lens.internal.data.model.NetworkLogEntry.StatusType
                                      .CLIENT_ERROR,
                                  com.lokalapps.lens.internal.data.model.NetworkLogEntry.StatusType
                                      .SERVER_ERROR)
                        }
                    total to errors
                  },
                  exceptionRepo.exceptions.map { it.size }) {
                      (requestCount, errorCount),
                      exceptionCount ->
                    NotificationData(requestCount, errorCount, exceptionCount)
                  }
              .distinctUntilChanged()
              .collect { data -> showNotification(data) }
        }
  }

  private fun showNotification(data: NotificationData) {
    // Check permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED) {
        return
      }
    }

    val dashboardIntent =
        Intent(context, LensDashboardActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    val dashboardPendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            dashboardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val clearIntent =
        Intent(context, LensNotificationReceiver::class.java).apply { action = ACTION_CLEAR }
    val clearPendingIntent =
        PendingIntent.getBroadcast(
            context,
            1,
            clearIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val title = buildTitle(data)
    val text = buildText(data)

    val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lens_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(dashboardPendingIntent)
            .addAction(0, "Clear", clearPendingIntent)
            .addAction(0, "Open Dashboard", dashboardPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  private fun buildTitle(data: NotificationData): String {
    return if (data.requestCount == 0 && data.exceptionCount == 0) {
      "Lens Active"
    } else {
      buildString {
        append("${data.requestCount} requests")
        if (data.errorCount > 0) {
          append(" · ${data.errorCount} errors")
        }
      }
    }
  }

  private fun buildText(data: NotificationData): String {
    return buildString {
      if (data.exceptionCount > 0) {
        append("${data.exceptionCount} exceptions captured")
      } else {
        append("Tap to open debug dashboard")
      }
    }
  }

  private data class NotificationData(
      val requestCount: Int,
      val errorCount: Int,
      val exceptionCount: Int
  )

  internal companion object {
    const val CHANNEL_ID = "lens_debug"
    const val NOTIFICATION_ID = 0x4C454E53 // "LENS" in hex
    const val ACTION_CLEAR = "com.lokalapps.lens.CLEAR_LOGS"
  }
}
