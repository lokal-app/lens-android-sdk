package com.behtar.lens.internal.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.behtar.lens.internal.di.LensServiceLocator
import timber.log.Timber

/**
 * Handles the "Clear" action from the Lens notification.
 *
 * When the user taps "Clear" on the notification, this receiver clears all network logs. The
 * notification updates automatically via the Flow observation in [LensNotificationManager].
 */
internal class LensNotificationReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action == LensNotificationManager.ACTION_CLEAR) {
      Timber.d("Lens: Clear action from notification")
      LensServiceLocator.networkLogRepository.clear()
      LensServiceLocator.webSocketLogRepository.clear()
      LensServiceLocator.exceptionLogRepository.clear()
    }
  }
}
