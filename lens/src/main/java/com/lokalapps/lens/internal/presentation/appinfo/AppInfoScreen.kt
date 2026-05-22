package com.lokalapps.lens.internal.presentation.appinfo

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * App Info screen showing build and device information.
 *
 * All info is gathered at composition time for a static snapshot. Tap the copy button to copy all
 * info to clipboard for sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen() {
  val context = LocalContext.current
  val appInfo = remember { gatherAppInfo(context) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("App Info") },
            actions = {
              IconButton(onClick = { copyAllToClipboard(context, appInfo) }) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy all")
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface))
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              // Build Info
              InfoSection(title = "Build Info") {
                InfoRow("Version", appInfo.versionName)
                InfoRow("Version Code", appInfo.versionCode)
                InfoRow("Package", appInfo.packageName)
                InfoRow("Install Source", appInfo.installSource)
                InfoRow("First Install", appInfo.firstInstallTime)
                InfoRow("Last Update", appInfo.lastUpdateTime)
              }

              // Device Info
              InfoSection(title = "Device Info") {
                InfoRow("Model", appInfo.deviceModel)
                InfoRow("Manufacturer", appInfo.manufacturer)
                InfoRow("Brand", appInfo.brand)
                InfoRow("Android Version", appInfo.androidVersion)
                InfoRow("API Level", appInfo.apiLevel.toString())
                InfoRow("Total RAM", appInfo.totalRam)
                InfoRow("Available RAM", appInfo.availableRam)
              }

              // System Info
              InfoSection(title = "System Info") {
                InfoRow("Board", Build.BOARD)
                InfoRow("Bootloader", Build.BOOTLOADER)
                InfoRow("Device", Build.DEVICE)
                InfoRow("Hardware", Build.HARDWARE)
                InfoRow("Product", Build.PRODUCT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                  InfoRow("Security Patch", Build.VERSION.SECURITY_PATCH)
                }
              }

              Spacer(modifier = Modifier.height(16.dp))
            }
      }
}

@Composable
private fun InfoSection(title: String, content: @Composable () -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(12.dp))
          content()
        }
      }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}

/** Data class holding all app info. */
private data class AppInfo(
    // Build
    val versionName: String,
    val versionCode: String,
    val packageName: String,
    val installSource: String,
    val firstInstallTime: String,
    val lastUpdateTime: String,
    // Device
    val deviceModel: String,
    val manufacturer: String,
    val brand: String,
    val androidVersion: String,
    val apiLevel: Int,
    val totalRam: String,
    val availableRam: String
)

/** Gathers all app info from various sources. */
private fun gatherAppInfo(context: Context): AppInfo {
  val packageInfo =
      try {
        context.packageManager.getPackageInfo(context.packageName, 0)
      } catch (e: PackageManager.NameNotFoundException) {
        null
      }

  val versionCode =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "Unknown"
      } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "Unknown"
      }

  val installSource =
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
              ?: "Unknown"
        } else {
          @Suppress("DEPRECATION")
          context.packageManager.getInstallerPackageName(context.packageName) ?: "Unknown"
        }
      } catch (e: Exception) {
        "Unknown"
      }

  // RAM info
  val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
  val memInfo = ActivityManager.MemoryInfo()
  activityManager.getMemoryInfo(memInfo)

  return AppInfo(
      // Build
      versionName = packageInfo?.versionName ?: "Unknown",
      versionCode = versionCode,
      packageName = context.packageName,
      installSource = formatInstallSource(installSource),
      firstInstallTime = formatTimestamp(packageInfo?.firstInstallTime ?: 0),
      lastUpdateTime = formatTimestamp(packageInfo?.lastUpdateTime ?: 0),
      // Device
      deviceModel = Build.MODEL,
      manufacturer = Build.MANUFACTURER,
      brand = Build.BRAND,
      androidVersion = Build.VERSION.RELEASE,
      apiLevel = Build.VERSION.SDK_INT,
      totalRam = formatBytes(memInfo.totalMem),
      availableRam = formatBytes(memInfo.availMem))
}

private fun formatBytes(bytes: Long): String {
  val gb = bytes / (1024.0 * 1024.0 * 1024.0)
  return String.format("%.1f GB", gb)
}

private fun formatTimestamp(timestamp: Long): String {
  if (timestamp == 0L) return "Unknown"
  val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
  return sdf.format(java.util.Date(timestamp))
}

private fun formatInstallSource(source: String): String {
  return when {
    source.contains("vending") -> "Play Store"
    source.contains("adb") -> "ADB"
    source.contains("packageinstaller") -> "Manual"
    source == "Unknown" -> "Unknown"
    else -> source
  }
}

private fun copyAllToClipboard(context: Context, appInfo: AppInfo) {
  val text = buildString {
    appendLine("=== App Info ===")
    appendLine()
    appendLine("-- Build --")
    appendLine("Version: ${appInfo.versionName} (${appInfo.versionCode})")
    appendLine("Package: ${appInfo.packageName}")
    appendLine("Install Source: ${appInfo.installSource}")
    appendLine("First Install: ${appInfo.firstInstallTime}")
    appendLine("Last Update: ${appInfo.lastUpdateTime}")
    appendLine()
    appendLine("-- Device --")
    appendLine("Model: ${appInfo.deviceModel}")
    appendLine("Manufacturer: ${appInfo.manufacturer}")
    appendLine("Brand: ${appInfo.brand}")
    appendLine("Android: ${appInfo.androidVersion} (API ${appInfo.apiLevel})")
    appendLine("RAM: ${appInfo.availableRam} / ${appInfo.totalRam}")
    appendLine()
    appendLine("-- System --")
    appendLine("Board: ${Build.BOARD}")
    appendLine("Device: ${Build.DEVICE}")
    appendLine("Hardware: ${Build.HARDWARE}")
  }

  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  clipboard.setPrimaryClip(ClipData.newPlainText("App Info", text))
  Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
