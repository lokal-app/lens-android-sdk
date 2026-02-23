package com.behtar.lens.internal.presentation.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.behtar.lens.api.Lens
import com.behtar.lens.internal.core.LensImpl
import com.behtar.lens.internal.presentation.theme.LensTheme

/**
 * Main dashboard activity for Lens.
 *
 * Shows the list of registered plugins and allows navigation to each. Launched via [Lens.open()] or
 * activation gestures.
 *
 * ViewModels are created via [LensViewModelFactory] (no Hilt required).
 */
class LensDashboardActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      LensTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          LensDashboardScreen(plugins = Lens.getPlugins(), onClose = { finish() })
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    // Notify Lens that dashboard is closed
    (Lens.getImplementation() as? LensImpl)?.onDashboardClosed()
  }
}
