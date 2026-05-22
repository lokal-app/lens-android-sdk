package com.lokalapps.lens.internal.presentation.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.api.DeepLinkProvider
import timber.log.Timber

/**
 * Deep Link Tester screen.
 *
 * Allows entering and testing deep links without leaving the app. Includes quick access buttons
 * sourced from [DeepLinkProvider] (host-app supplied) and history of recent tests.
 *
 * The Quick Links section is hidden when no [provider] is registered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepLinkTesterScreen(provider: DeepLinkProvider? = null) {
  val context = LocalContext.current
  var deepLinkUrl by remember { mutableStateOf("") }
  val history = remember { mutableStateListOf<String>() }

  // Quick links are sourced from the host app's DeepLinkProvider.
  // Empty when no provider is registered — the section is hidden in that case.
  val quickLinks = remember(provider) { provider?.getQuickLinks() ?: emptyList() }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Deep Link Tester") },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface))
      }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              // URL Input
              item {
                OutlinedTextField(
                    value = deepLinkUrl,
                    onValueChange = { deepLinkUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Deep Link URL") },
                    placeholder = { Text("Enter full URL or path (e.g., /home)") },
                    singleLine = true,
                    trailingIcon = {
                      if (deepLinkUrl.isNotEmpty()) {
                        IconButton(onClick = { deepLinkUrl = "" }) {
                          Icon(Icons.Default.Clear, "Clear")
                        }
                      }
                    })
              }

              // Test Button
              item {
                Button(
                    onClick = {
                      val url = normalizeDeepLink(context, deepLinkUrl)
                      if (url != null) {
                        testDeepLink(context, url)
                        if (!history.contains(url)) {
                          history.add(0, url)
                          if (history.size > 10) history.removeLast()
                        }
                      }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = deepLinkUrl.isNotBlank()) {
                      Icon(Icons.Default.Send, null)
                      Spacer(modifier = Modifier.padding(4.dp))
                      Text("Test Deep Link")
                    }
              }

              // Quick Links Section — only shown when the host app registered a DeepLinkProvider
              if (quickLinks.isNotEmpty()) {
                item {
                  Text(
                      text = "Quick Links",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold)
                }
              }

              if (quickLinks.isNotEmpty())
                  item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                          Column {
                            quickLinks.forEachIndexed { index, deepLink ->
                              QuickLinkRow(
                                  path = deepLink.path,
                                  label = deepLink.label,
                                  onClick = {
                                    val url = normalizeDeepLink(context, deepLink.path)
                                    if (url != null) {
                                      testDeepLink(context, url)
                                      if (!history.contains(url)) {
                                        history.add(0, url)
                                        if (history.size > 10) history.removeLast()
                                      }
                                    }
                                  })
                              if (index < quickLinks.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                              }
                            }
                          }
                        }
                  }

              // History Section
              if (history.isNotEmpty()) {
                item {
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(
                              Icons.Default.History,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.onSurfaceVariant)
                          Spacer(modifier = Modifier.padding(4.dp))
                          Text(
                              text = "Recent",
                              style = MaterialTheme.typography.titleMedium,
                              fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { history.clear() })
                      }
                }

                items(history) { url ->
                  Card(
                      modifier = Modifier.fillMaxWidth().clickable { testDeepLink(context, url) },
                      colors =
                          CardDefaults.cardColors(
                              containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(
                            text = url,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface)
                      }
                }
              }

              item { Spacer(modifier = Modifier.height(16.dp)) }
            }
      }
}

@Composable
private fun QuickLinkRow(path: String, label: String, onClick: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = path,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
      }
}

/**
 * Normalizes a deep link input to a full URL.
 * - If it starts with /, prepends the app scheme and host
 * - If it's already a full URL, uses it as-is
 *
 * Uses the correct deep link format: scheme://host/path?query_params Example:
 * sahienglish://sahienglish.ai/path
 */
private fun normalizeDeepLink(context: Context, input: String): String? {
  val trimmed = input.trim()
  if (trimmed.isBlank()) return null

  return when {
    // Already a full URL
    trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("://") ->
        trimmed

    // Path only - prepend app scheme and host
    trimmed.startsWith("/") -> {
      val (scheme, host) = getSchemeAndHost(context)
      "$scheme://$host$trimmed"
    }

    // Just a path without leading slash
    else -> {
      val (scheme, host) = getSchemeAndHost(context)
      "$scheme://$host/$trimmed"
    }
  }
}

/**
 * Derives the deep link scheme and host from the package name.
 * - com.behtar.sahienglish -> (sahienglish, sahienglish.ai)
 * - com.behtar.sahienglish -> (gyantv, gyantv.in)
 */
private fun getSchemeAndHost(context: Context): Pair<String, String> {
  val packageName = context.packageName
  return when {
    packageName.contains("sahienglish") -> "sahienglish" to "sahienglish.ai"
    packageName.contains("gyantv") -> "gyantv" to "gyantv.in"
    else -> {
      // Fallback: extract last segment of package name
      val scheme = packageName.substringAfterLast(".")
      scheme to "$scheme.com"
    }
  }
}

/** Tests a deep link by launching an intent. */
private fun testDeepLink(context: Context, url: String) {
  try {
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)
    Timber.d("DeepLinkTester: Testing $url")
  } catch (e: Exception) {
    Timber.e(e, "DeepLinkTester: Failed to test $url")
    Toast.makeText(context, "No handler found for: $url", Toast.LENGTH_LONG).show()
  }
}
