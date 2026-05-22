package com.lokalapps.lens.internal.presentation.cache

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.DecimalFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Cache Manager screen.
 *
 * Shows cache sizes and allows clearing various app caches:
 * - Image cache (Glide disk cache directory)
 * - App internal cache
 * - WebView cache
 * - External storage cache
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheManagerScreen() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var cacheInfo by remember { mutableStateOf<CacheInfo?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var clearingCache by remember { mutableStateOf<String?>(null) }

  // Load cache info
  fun refreshCacheInfo() {
    scope.launch {
      isLoading = true
      cacheInfo = withContext(Dispatchers.IO) { calculateCacheInfo(context) }
      isLoading = false
    }
  }

  LaunchedEffect(Unit) { refreshCacheInfo() }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Cache Manager") },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface),
            actions = {
              IconButton(onClick = { refreshCacheInfo() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
              }
            })
      }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              // Total cache size
              item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                      Column(
                          modifier = Modifier.fillMaxWidth().padding(16.dp),
                          horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Cache", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isLoading) {
                              CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            } else {
                              Text(
                                  text = cacheInfo?.totalFormatted ?: "0 B",
                                  style = MaterialTheme.typography.headlineLarge,
                                  fontWeight = FontWeight.Bold)
                            }
                          }
                    }
              }

              // Individual caches
              item {
                Text(
                    text = "Cache Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
              }

              // Image cache
              item {
                CacheCard(
                    name = "Image Cache",
                    description = "Cached images from network (Glide)",
                    size = cacheInfo?.imageCacheFormatted ?: "-",
                    isClearing = clearingCache == "image",
                    onClear = {
                      clearingCache = "image"
                      scope.launch {
                        withContext(Dispatchers.IO) {
                          val imageCacheDir = File(context.cacheDir, "image_manager_disk_cache")
                          clearDirectory(imageCacheDir)
                        }
                        clearingCache = null
                        refreshCacheInfo()
                      }
                    })
              }

              // App cache
              item {
                CacheCard(
                    name = "App Cache",
                    description = "Temporary app data",
                    size = cacheInfo?.appCacheFormatted ?: "-",
                    isClearing = clearingCache == "app",
                    onClear = {
                      clearingCache = "app"
                      scope.launch {
                        withContext(Dispatchers.IO) {
                          // Clear cache but preserve known subdirectories
                          clearDirectoryExcept(
                              context.cacheDir, setOf("image_manager_disk_cache", "WebView"))
                        }
                        clearingCache = null
                        refreshCacheInfo()
                      }
                    })
              }

              // WebView cache
              item {
                CacheCard(
                    name = "WebView Cache",
                    description = "Cached web content",
                    size = cacheInfo?.webCacheFormatted ?: "-",
                    isClearing = clearingCache == "web",
                    onClear = {
                      clearingCache = "web"
                      scope.launch {
                        withContext(Dispatchers.IO) {
                          val webViewCache = File(context.cacheDir, "WebView")
                          clearDirectory(webViewCache)
                        }
                        clearingCache = null
                        refreshCacheInfo()
                      }
                    })
              }

              // External cache
              item {
                CacheCard(
                    name = "External Cache",
                    description = "Cached data on external storage",
                    size = cacheInfo?.externalCacheFormatted ?: "-",
                    isClearing = clearingCache == "external",
                    onClear = {
                      clearingCache = "external"
                      scope.launch {
                        withContext(Dispatchers.IO) {
                          context.externalCacheDir?.let { clearDirectory(it) }
                        }
                        clearingCache = null
                        refreshCacheInfo()
                      }
                    })
              }

              // Clear all button
              item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                      clearingCache = "all"
                      scope.launch {
                        withContext(Dispatchers.IO) {
                          try {
                            // Clear all cache directories
                            clearDirectory(context.cacheDir)
                            context.externalCacheDir?.let { clearDirectory(it) }
                          } catch (e: Exception) {
                            Timber.e(e, "Failed to clear caches")
                          }
                        }
                        clearingCache = null
                        refreshCacheInfo()
                      }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error),
                    enabled = clearingCache == null) {
                      if (clearingCache == "all") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError)
                      } else {
                        Icon(Icons.Default.Delete, null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Clear All Caches")
                      }
                    }
              }

              item { Spacer(modifier = Modifier.height(16.dp)) }
            }
      }
}

@Composable
private fun CacheCard(
    name: String,
    description: String,
    size: String,
    isClearing: Boolean,
    onClear: () -> Unit
) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = size,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
              }

              Button(
                  onClick = onClear,
                  enabled = !isClearing,
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.secondaryContainer,
                          contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    if (isClearing) {
                      CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                      Text("Clear")
                    }
                  }
            }
      }
}

/** Data class holding cache size information. */
private data class CacheInfo(
    val imageCacheSize: Long,
    val appCacheSize: Long,
    val webCacheSize: Long,
    val externalCacheSize: Long
) {
  val totalSize: Long
    get() = imageCacheSize + appCacheSize + webCacheSize + externalCacheSize

  val totalFormatted: String
    get() = formatFileSize(totalSize)

  val imageCacheFormatted: String
    get() = formatFileSize(imageCacheSize)

  val appCacheFormatted: String
    get() = formatFileSize(appCacheSize)

  val webCacheFormatted: String
    get() = formatFileSize(webCacheSize)

  val externalCacheFormatted: String
    get() = formatFileSize(externalCacheSize)
}

/** Calculates cache sizes for all cache types. */
private fun calculateCacheInfo(context: Context): CacheInfo {
  val imageCacheDir = File(context.cacheDir, "image_manager_disk_cache")
  val webCacheDir = File(context.cacheDir, "WebView")

  val totalCacheSize = getDirectorySize(context.cacheDir)
  val imageCacheSize = getDirectorySize(imageCacheDir)
  val webCacheSize = getDirectorySize(webCacheDir)

  return CacheInfo(
      imageCacheSize = imageCacheSize,
      appCacheSize = (totalCacheSize - imageCacheSize - webCacheSize).coerceAtLeast(0),
      webCacheSize = webCacheSize,
      externalCacheSize = context.externalCacheDir?.let { getDirectorySize(it) } ?: 0)
}

/** Recursively calculates the size of a directory. */
private fun getDirectorySize(dir: File): Long {
  if (!dir.exists()) return 0
  if (dir.isFile) return dir.length()

  var size = 0L
  dir.listFiles()?.forEach { file ->
    size +=
        if (file.isDirectory) {
          getDirectorySize(file)
        } else {
          file.length()
        }
  }
  return size
}

/** Recursively deletes all files in a directory. */
private fun clearDirectory(dir: File) {
  if (!dir.exists()) return
  dir.listFiles()?.forEach { file ->
    if (file.isDirectory) {
      clearDirectory(file)
    }
    file.delete()
  }
}

/** Clears a directory except for specified subdirectories. */
private fun clearDirectoryExcept(dir: File, except: Set<String>) {
  if (!dir.exists()) return
  dir.listFiles()?.forEach { file ->
    if (file.name !in except) {
      if (file.isDirectory) {
        clearDirectory(file)
      }
      file.delete()
    }
  }
}

/** Formats a file size in bytes to a human-readable string. */
private fun formatFileSize(size: Long): String {
  if (size <= 0) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB")
  val digitGroups =
      (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
  val formatter = DecimalFormat("#,##0.#")
  return "${formatter.format(size / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
