package com.behtar.lens.internal.presentation.bubble

import android.app.Activity
import android.app.Application
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.behtar.lens.internal.core.activation.SimpleActivityLifecycleCallbacks
import com.behtar.lens.internal.presentation.dashboard.LensDashboardActivity
import java.util.Collections
import java.util.WeakHashMap
import timber.log.Timber

/**
 * Injects the Lens bubble into every Activity's content view AND DialogFragment windows.
 *
 * This approach doesn't require SYSTEM_ALERT_WINDOW permission because we're adding the bubble to
 * the Activity's/Dialog's own view hierarchy, not as a system overlay.
 *
 * The bubble is automatically:
 * - Added when an Activity resumes
 * - Added when a DialogFragment resumes (on top of the dialog)
 * - Removed when an Activity/DialogFragment is paused/destroyed
 * - Skipped for the Lens dashboard itself
 *
 * **Why DialogFragment support is needed:** DialogFragments create their own Window that floats
 * above the Activity's content. Without injecting into the dialog's window, the bubble would be
 * hidden behind the dialog.
 *
 * This is the recommended pattern for in-app debugging tools as it:
 * - Requires no permissions
 * - Works immediately (no user action needed)
 * - Is more performant than a system service
 * - Follows the Activity/Fragment lifecycle properly
 */
internal class LensBubbleInjector(
    private val application: Application,
    private val onBubbleClick: () -> Unit
) {
  private val activityBubbleViews: MutableMap<Activity, ComposeView> =
      Collections.synchronizedMap(WeakHashMap())
  private val dialogBubbleViews: MutableMap<DialogFragment, ComposeView> =
      Collections.synchronizedMap(WeakHashMap())

  /** Starts listening for Activity and Fragment lifecycle events and injecting bubbles. */
  fun start() {
    // Listen for Activity lifecycle to inject bubble into Activities
    application.registerActivityLifecycleCallbacks(
        object : SimpleActivityLifecycleCallbacks() {
          override fun onActivityCreated(
              activity: Activity,
              savedInstanceState: android.os.Bundle?
          ) {
            // Register fragment lifecycle callbacks for this activity
            registerFragmentCallbacks(activity)
          }

          override fun onActivityResumed(activity: Activity) {
            injectBubbleIntoActivity(activity)
          }

          override fun onActivityPaused(activity: Activity) {
            removeBubbleFromActivity(activity)
          }

          override fun onActivityDestroyed(activity: Activity) {
            removeBubbleFromActivity(activity)
          }
        })

    Timber.d("Lens: Bubble injector started (Activity + DialogFragment support)")
  }

  /** Registers FragmentLifecycleCallbacks to detect DialogFragments. */
  private fun registerFragmentCallbacks(activity: Activity) {
    if (activity !is FragmentActivity) return
    if (activity is LensDashboardActivity) return

    activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
        object : FragmentManager.FragmentLifecycleCallbacks() {
          override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
            if (fragment is DialogFragment) {
              injectBubbleIntoDialog(fragment)
            }
          }

          override fun onFragmentPaused(fm: FragmentManager, fragment: Fragment) {
            if (fragment is DialogFragment) {
              removeBubbleFromDialog(fragment)
            }
          }
        },
        true // recursive - also catches child fragment managers
        )

    Timber.d("Lens: Fragment callbacks registered for ${activity.javaClass.simpleName}")
  }

  /**
   * Injects the bubble into an Activity's content view.
   *
   * Only works with ComponentActivity (AppCompatActivity, FragmentActivity, etc.) because we need
   * proper Compose lifecycle support.
   */
  private fun injectBubbleIntoActivity(activity: Activity) {
    Timber.d("Lens: injectBubbleIntoActivity called for ${activity.javaClass.simpleName}")

    // Skip if already injected
    if (activityBubbleViews.containsKey(activity)) {
      Timber.d("Lens: Bubble already injected for ${activity.javaClass.simpleName}")
      return
    }

    // Skip Lens dashboard to avoid recursive bubbles
    if (activity is LensDashboardActivity) {
      Timber.d("Lens: Skipping bubble for LensDashboardActivity")
      return
    }

    // Only inject into ComponentActivity which has proper Compose lifecycle support
    if (activity !is ComponentActivity) {
      Timber.w("Lens: Skipping bubble for non-ComponentActivity: ${activity.javaClass.simpleName}")
      return
    }

    // Use the content view (android.R.id.content) which has proper lifecycle setup
    val contentView =
        activity.findViewById<FrameLayout>(android.R.id.content)
            ?: run {
              Timber.w("Lens: Could not get content view for ${activity.javaClass.simpleName}")
              return
            }

    try {
      // Create ComposeView and set up lifecycle owners BEFORE adding to hierarchy
      val bubbleView =
          ComposeView(activity).apply {
            // Explicitly set the lifecycle and saved state registry owners
            setViewTreeLifecycleOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)

            // Now it's safe to set content
            setContent { LensBubble(onClick = onBubbleClick) }
          }

      // Use FrameLayout.LayoutParams to cover the full screen
      val params =
          FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

      contentView.addView(bubbleView, params)
      activityBubbleViews[activity] = bubbleView

      Timber.d("Lens: Bubble injected into Activity ${activity.javaClass.simpleName}")
    } catch (e: Exception) {
      Timber.e(e, "Lens: Failed to inject bubble into ${activity.javaClass.simpleName}")
    }
  }

  /** Removes the bubble from an Activity. */
  private fun removeBubbleFromActivity(activity: Activity) {
    val bubbleView = activityBubbleViews.remove(activity) ?: return

    try {
      (bubbleView.parent as? ViewGroup)?.removeView(bubbleView)
      Timber.d("Lens: Bubble removed from Activity ${activity.javaClass.simpleName}")
    } catch (e: Exception) {
      Timber.w(e, "Lens: Error removing bubble from ${activity.javaClass.simpleName}")
    }
  }

  /**
   * Injects the bubble into a DialogFragment's window.
   *
   * DialogFragments have their own Window that floats above the Activity. We inject into the
   * dialog's decor view so the bubble appears on top of the dialog.
   */
  private fun injectBubbleIntoDialog(dialogFragment: DialogFragment) {
    Timber.d("Lens: injectBubbleIntoDialog called for ${dialogFragment.javaClass.simpleName}")

    // Skip if already injected
    if (dialogBubbleViews.containsKey(dialogFragment)) {
      Timber.d("Lens: Bubble already injected for dialog ${dialogFragment.javaClass.simpleName}")
      return
    }

    val dialog =
        dialogFragment.dialog
            ?: run {
              Timber.w("Lens: Dialog is null for ${dialogFragment.javaClass.simpleName}")
              return
            }

    val window =
        dialog.window
            ?: run {
              Timber.w("Lens: Dialog window is null for ${dialogFragment.javaClass.simpleName}")
              return
            }

    val decorView =
        window.decorView as? ViewGroup
            ?: run {
              Timber.w(
                  "Lens: Could not get decor view for dialog ${dialogFragment.javaClass.simpleName}")
              return
            }

    // Get the fragment's activity for lifecycle owners
    val activity =
        dialogFragment.activity as? ComponentActivity
            ?: run {
              Timber.w("Lens: DialogFragment's activity is not ComponentActivity")
              return
            }

    try {
      val bubbleView =
          ComposeView(activity).apply {
            setViewTreeLifecycleOwner(dialogFragment.viewLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(dialogFragment)

            setContent { LensBubble(onClick = onBubbleClick) }
          }

      val params =
          FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

      decorView.addView(bubbleView, params)
      dialogBubbleViews[dialogFragment] = bubbleView

      Timber.d("Lens: Bubble injected into DialogFragment ${dialogFragment.javaClass.simpleName}")
    } catch (e: Exception) {
      Timber.e(
          e, "Lens: Failed to inject bubble into dialog ${dialogFragment.javaClass.simpleName}")
    }
  }

  /** Removes the bubble from a DialogFragment. */
  private fun removeBubbleFromDialog(dialogFragment: DialogFragment) {
    val bubbleView = dialogBubbleViews.remove(dialogFragment) ?: return

    try {
      (bubbleView.parent as? ViewGroup)?.removeView(bubbleView)
      Timber.d("Lens: Bubble removed from DialogFragment ${dialogFragment.javaClass.simpleName}")
    } catch (e: Exception) {
      Timber.w(e, "Lens: Error removing bubble from dialog ${dialogFragment.javaClass.simpleName}")
    }
  }
}
