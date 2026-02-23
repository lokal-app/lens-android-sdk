package com.behtar.lens.internal.core.activation

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Simple implementation of [Application.ActivityLifecycleCallbacks] with empty default
 * implementations for all methods.
 *
 * Subclasses only need to override the methods they're interested in.
 */
internal abstract class SimpleActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

  override fun onActivityStarted(activity: Activity) {}

  override fun onActivityResumed(activity: Activity) {}

  override fun onActivityPaused(activity: Activity) {}

  override fun onActivityStopped(activity: Activity) {}

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

  override fun onActivityDestroyed(activity: Activity) {}
}
