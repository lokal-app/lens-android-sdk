@file:OptIn(com.behtar.lens.api.LensExperimental::class)

package com.behtar.lens.internal.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.behtar.lens.api.ComposableLensPlugin
import com.behtar.lens.api.LensPlugin
import com.behtar.lens.api.ViewLensPlugin
import com.behtar.lens.internal.export.LensExporter

/**
 * Main dashboard screen for Lens.
 *
 * Shows a list of registered plugins and allows navigation to each.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensDashboardScreen(plugins: List<LensPlugin>, onClose: () -> Unit) {
  var selectedPlugin by remember { mutableStateOf<LensPlugin?>(null) }
  val context = LocalContext.current

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = selectedPlugin?.name ?: "Lens",
                  style = MaterialTheme.typography.titleLarge)
            },
            navigationIcon = {
              if (selectedPlugin != null) {
                IconButton(onClick = { selectedPlugin = null }) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Back")
                }
              }
            },
            actions = {
              if (selectedPlugin == null) {
                IconButton(onClick = { LensExporter.shareAllLogsAsJson(context) }) {
                  Icon(imageVector = Icons.Default.Share, contentDescription = "Export logs")
                }
                IconButton(onClick = onClose) {
                  Icon(imageVector = Icons.Default.Close, contentDescription = "Close Lens")
                }
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface))
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // Plugin list (main screen)
          AnimatedVisibility(
              visible = selectedPlugin == null,
              enter = slideInHorizontally { -it },
              exit = slideOutHorizontally { -it }) {
                PluginListScreen(plugins = plugins, onPluginSelected = { selectedPlugin = it })
              }

          // Plugin detail (selected plugin screen)
          AnimatedVisibility(
              visible = selectedPlugin != null,
              enter = slideInHorizontally { it },
              exit = slideOutHorizontally { it }) {
                selectedPlugin?.let { plugin -> PluginContent(plugin) }
              }
        }
      }
}

/** List of available plugins. */
@Composable
private fun PluginListScreen(plugins: List<LensPlugin>, onPluginSelected: (LensPlugin) -> Unit) {
  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Text(
        text = "Debug Tools",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface)

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Tap a tool to open it",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline)

    Spacer(modifier = Modifier.height(16.dp))

    LazyColumn {
      items(plugins) { plugin ->
        PluginCard(plugin = plugin, onClick = { onPluginSelected(plugin) })
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

/** Card for a single plugin. */
@Composable
private fun PluginCard(plugin: LensPlugin, onClick: () -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  painter = painterResource(id = plugin.icon),
                  contentDescription = null,
                  modifier = Modifier.size(40.dp),
                  tint = MaterialTheme.colorScheme.primary)

              Spacer(modifier = Modifier.width(16.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)

                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
      }
}

/**
 * Renders plugin content based on plugin type.
 * - [ComposableLensPlugin]: Renders Compose UI directly
 * - [ViewLensPlugin]: Wraps Android View in [AndroidView]
 * - Base [LensPlugin]: Shows unsupported message
 */
@Composable
private fun PluginContent(plugin: LensPlugin) {
  when (plugin) {
    is ComposableLensPlugin -> plugin.Content()
    is ViewLensPlugin -> {
      val context = LocalContext.current
      AndroidView(factory = { plugin.createView(context) }, modifier = Modifier.fillMaxSize())
    }
    else -> {
      Text(
          text = "Plugin \"${plugin.name}\" does not provide a UI",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.outline,
          modifier = Modifier.padding(16.dp))
    }
  }
}
