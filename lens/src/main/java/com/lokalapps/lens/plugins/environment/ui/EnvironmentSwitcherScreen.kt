package com.lokalapps.lens.plugins.environment.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.api.Environment
import com.lokalapps.lens.api.EnvironmentProvider
import com.lokalapps.lens.api.Lens

/**
 * Path handling modes for WebView URL construction.
 *
 * This is an SDK-internal enum that mirrors the app-level [WebViewPathMode] by convention via the
 * shared key-value store. Both sides serialize/deserialize using [name]/[valueOf].
 */
private enum class WebViewPathMode {
  DEFAULT,
  CUSTOM,
  NONE
}

/** Shared key constants — must match the keys used by app-level WebViewDebugConfig. */
private const val KEY_BASE_URL = "webview_base_url_override"
private const val KEY_PATH_MODE = "webview_path_mode"
private const val KEY_CUSTOM_PATH = "webview_custom_path"
private const val KEY_SKIP_AUTH = "webview_skip_auth"

private fun getStoredPathMode(): WebViewPathMode {
  val modeName = Lens.getString(KEY_PATH_MODE)
  return try {
    modeName?.let { WebViewPathMode.valueOf(it) } ?: WebViewPathMode.DEFAULT
  } catch (e: IllegalArgumentException) {
    WebViewPathMode.DEFAULT
  }
}

/**
 * Environment switcher screen.
 *
 * Shows available environments and allows switching between them. Also provides WebView URL
 * override controls for testing different WebView environments. Requires app restart for API
 * environment changes to take effect.
 */
@Composable
fun EnvironmentSwitcherScreen(provider: EnvironmentProvider) {
  var showRestartDialog by remember { mutableStateOf(false) }
  var pendingEnvironment by remember { mutableStateOf<Environment?>(null) }

  val environments = remember { provider.getEnvironments() }
  val currentEnvironment = remember { provider.getCurrentEnvironment() }

  val webViewPresets = remember { provider.getWebViewPresets() }

  Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
    // WebView URL Section (only shown if provider supplies presets)
    if (webViewPresets.isNotEmpty()) {
      WebViewUrlSection(presets = webViewPresets)

      Spacer(modifier = Modifier.height(24.dp))

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Spacer(modifier = Modifier.height(24.dp))
    }

    // API Environment Section
    Text(
        text = "API Environment",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface)

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "App restart required after changing environment",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline)

    Spacer(modifier = Modifier.height(16.dp))

    environments.forEach { env ->
      EnvironmentCard(
          environment = env,
          isSelected = env.id == currentEnvironment.id,
          onClick = {
            if (env.id != currentEnvironment.id) {
              pendingEnvironment = env
              showRestartDialog = true
            }
          })
      Spacer(modifier = Modifier.height(8.dp))
    }
  }

  // Restart confirmation dialog
  if (showRestartDialog && pendingEnvironment != null) {
    AlertDialog(
        onDismissRequest = {
          showRestartDialog = false
          pendingEnvironment = null
        },
        icon = {
          Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Switch Environment?") },
        text = {
          Text(
              "Switching to ${pendingEnvironment?.name} requires an app restart.\n\n" +
                  "Base URL: ${pendingEnvironment?.baseUrl}")
        },
        confirmButton = {
          TextButton(
              onClick = {
                pendingEnvironment?.let { env ->
                  provider.setEnvironment(env)
                  provider.onRestartRequested()
                }
                showRestartDialog = false
                pendingEnvironment = null
              }) {
                Text("Switch & Restart")
              }
        },
        dismissButton = {
          TextButton(
              onClick = {
                showRestartDialog = false
                pendingEnvironment = null
              }) {
                Text("Cancel")
              }
        })
  }
}

/**
 * WebView URL configuration section.
 *
 * Provides comprehensive control over WebView URL construction:
 * - **Base URL**: Override the default BuildConfig URL
 * - **Path Mode**: Control how paths are appended (Default/Custom/None)
 * - **Custom Path**: Specify a custom path when using Custom mode
 * - **Skip Auth**: Bypass HMAC handshake authentication
 *
 * Changes take effect on next WebView load - no app restart required.
 */
@Composable
private fun WebViewUrlSection(presets: List<Environment>) {
  val focusManager = LocalFocusManager.current

  // Track all WebView configuration state via generic key-value store
  var baseUrlOverride by remember { mutableStateOf(Lens.getString(KEY_BASE_URL)) }
  var baseUrlInput by remember { mutableStateOf(baseUrlOverride ?: "") }
  var pathMode by remember { mutableStateOf(getStoredPathMode()) }
  var customPath by remember { mutableStateOf(Lens.getString(KEY_CUSTOM_PATH) ?: "") }
  var skipAuth by remember { mutableStateOf(Lens.getBoolean(KEY_SKIP_AUTH)) }

  // Use presets from the EnvironmentProvider
  val presetEnvironments = remember { presets.map { it.name to it.baseUrl } }

  Column {
    // Section header
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Icon(
          imageVector = Icons.Default.Language,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
          text = "WebView URL",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text =
            if (baseUrlOverride != null) {
              "Override active - changes apply on next WebView load"
            } else {
              "Using BuildConfig default - no restart needed"
            },
        style = MaterialTheme.typography.bodySmall,
        color =
            if (baseUrlOverride != null) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.outline
            })

    Spacer(modifier = Modifier.height(16.dp))

    // ===================== BASE URL SECTION =====================
    Text(
        text = "Base URL",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface)

    Spacer(modifier = Modifier.height(8.dp))

    // Preset environment chips
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      presetEnvironments.forEach { (name, url) ->
        AssistChip(
            onClick = {
              Lens.putString(KEY_BASE_URL, url)
              baseUrlOverride = url
              baseUrlInput = url
              focusManager.clearFocus()
            },
            label = { Text(name) },
            leadingIcon = {
              if (baseUrlOverride == url) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary)
              }
            })
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Custom base URL input
    OutlinedTextField(
        value = baseUrlInput,
        onValueChange = { baseUrlInput = it },
        label = { Text("Custom Base URL") },
        placeholder = { Text("https://your-server.com") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                  if (baseUrlInput.isNotBlank()) {
                    Lens.putString(KEY_BASE_URL, baseUrlInput)
                    baseUrlOverride = baseUrlInput
                  }
                  focusManager.clearFocus()
                }),
        trailingIcon = {
          if (baseUrlInput.isNotBlank()) {
            IconButton(onClick = { baseUrlInput = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        })

    Spacer(modifier = Modifier.height(8.dp))

    // Apply / Clear buttons for base URL
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      TextButton(
          onClick = {
            if (baseUrlInput.isNotBlank()) {
              Lens.putString(KEY_BASE_URL, baseUrlInput)
              baseUrlOverride = baseUrlInput
            }
            focusManager.clearFocus()
          },
          enabled = baseUrlInput.isNotBlank() && baseUrlInput != baseUrlOverride) {
            Text("Apply URL")
          }

      OutlinedButton(
          onClick = {
            Lens.putString(KEY_BASE_URL, null)
            baseUrlOverride = null
            baseUrlInput = ""
            focusManager.clearFocus()
          },
          enabled = baseUrlOverride != null) {
            Text("Clear")
          }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ===================== PATH MODE SECTION =====================
    Text(
        text = "Path Handling",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface)

    Spacer(modifier = Modifier.height(8.dp))

    // Path mode radio buttons
    Column(modifier = Modifier.selectableGroup()) {
      PathModeOption(
          mode = WebViewPathMode.DEFAULT,
          selected = pathMode == WebViewPathMode.DEFAULT,
          title = "Default (Tab Path)",
          description = "Append /learn or /ai-riya based on tab",
          onSelect = {
            pathMode = WebViewPathMode.DEFAULT
            Lens.putString(KEY_PATH_MODE, WebViewPathMode.DEFAULT.name)
          })

      PathModeOption(
          mode = WebViewPathMode.CUSTOM,
          selected = pathMode == WebViewPathMode.CUSTOM,
          title = "Custom Path",
          description = "Append a custom path you specify",
          onSelect = {
            pathMode = WebViewPathMode.CUSTOM
            Lens.putString(KEY_PATH_MODE, WebViewPathMode.CUSTOM.name)
          })

      PathModeOption(
          mode = WebViewPathMode.NONE,
          selected = pathMode == WebViewPathMode.NONE,
          title = "No Path",
          description = "Use URL exactly as entered",
          onSelect = {
            pathMode = WebViewPathMode.NONE
            Lens.putString(KEY_PATH_MODE, WebViewPathMode.NONE.name)
          })
    }

    // Custom path input (visible only in CUSTOM mode)
    if (pathMode == WebViewPathMode.CUSTOM) {
      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
          value = customPath,
          onValueChange = { input ->
            customPath = input
            Lens.putString(KEY_CUSTOM_PATH, input.ifBlank { null })
          },
          label = { Text("Custom Path") },
          placeholder = { Text("/beta-feature") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
          supportingText = { Text("Path will be appended to base URL") })
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ===================== AUTHENTICATION SECTION =====================
    Text(
        text = "Authentication",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface)

    Spacer(modifier = Modifier.height(8.dp))

    // Skip Auth toggle
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Skip Authentication",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(
                text =
                    if (skipAuth) {
                      "WebView loads directly without HMAC handshake"
                    } else {
                      "Normal auth flow via handshake API"
                    },
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (skipAuth) {
                      MaterialTheme.colorScheme.error
                    } else {
                      MaterialTheme.colorScheme.outline
                    })
          }
          Switch(
              checked = skipAuth,
              onCheckedChange = { enabled ->
                skipAuth = enabled
                Lens.putBoolean(KEY_SKIP_AUTH, enabled)
              })
        }

    Spacer(modifier = Modifier.height(20.dp))

    // ===================== RESET ALL SECTION =====================
    OutlinedButton(
        onClick = {
          // Reset all WebView settings via generic store
          Lens.putString(KEY_BASE_URL, null)
          Lens.putString(KEY_PATH_MODE, WebViewPathMode.DEFAULT.name)
          Lens.putString(KEY_CUSTOM_PATH, null)
          Lens.putBoolean(KEY_SKIP_AUTH, false)

          // Update local state
          baseUrlOverride = null
          baseUrlInput = ""
          pathMode = WebViewPathMode.DEFAULT
          customPath = ""
          skipAuth = false
          focusManager.clearFocus()
        },
        enabled =
            baseUrlOverride != null ||
                pathMode != WebViewPathMode.DEFAULT ||
                customPath.isNotBlank() ||
                skipAuth,
        modifier = Modifier.fillMaxWidth()) {
          Text("Reset All WebView Settings")
        }
  }
}

/** A single path mode option with radio button styling. */
@Composable
private fun PathModeOption(
    mode: WebViewPathMode,
    selected: Boolean,
    title: String,
    description: String,
    onSelect: () -> Unit
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
              .padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector =
                if (selected) {
                  Icons.Default.RadioButtonChecked
                } else {
                  Icons.Default.RadioButtonUnchecked
                },
            contentDescription = if (selected) "Selected" else "Not selected",
            tint =
                if (selected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.outline
                })
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
              text = title,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface)
          Text(
              text = description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline)
        }
      }
}

@Composable
private fun EnvironmentCard(environment: Environment, isSelected: Boolean, onClick: () -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  })) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector =
                      if (isSelected) {
                        Icons.Default.CheckCircle
                      } else {
                        Icons.Default.RadioButtonUnchecked
                      },
                  contentDescription = if (isSelected) "Selected" else "Not selected",
                  tint =
                      if (isSelected) {
                        MaterialTheme.colorScheme.primary
                      } else {
                        MaterialTheme.colorScheme.outline
                      })

              Spacer(modifier = Modifier.width(16.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = environment.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)

                Text(
                    text = environment.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = environment.baseUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
              }
            }
      }
}
