package com.behtar.lens.internal.presentation.network.components.detail

import android.util.LruCache
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.behtar.lens.internal.data.model.NetworkLogEntry
import com.behtar.lens.internal.presentation.common.CodeBlock
import com.behtar.lens.internal.presentation.common.DetailRow
import com.behtar.lens.internal.presentation.common.SectionTitle
import com.behtar.lens.internal.presentation.common.StatusBadge
import com.behtar.lens.internal.presentation.network.HttpDetailTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpDetailScreen(
    entry: NetworkLogEntry,
    selectedTab: HttpDetailTab,
    onTabSelected: (HttpDetailTab) -> Unit,
    onBack: () -> Unit,
    formattedBodyCache: LruCache<String, String>,
    highlightedBodyCache: LruCache<String, AnnotatedString>,
    modifier: Modifier = Modifier
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  Column(modifier = modifier.fillMaxSize()) {
    TopAppBar(
        title = {
          Column {
            Text(
                text = entry.path,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                text = entry.host,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(entry.toCurlCommand()))
                Toast.makeText(context, "cURL copied to clipboard", Toast.LENGTH_SHORT).show()
              }) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy cURL")
              }
        })

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface) {
          HttpDetailTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                  Text(
                      text = tab.title,
                      fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                })
          }
        }

    when (selectedTab) {
      HttpDetailTab.OVERVIEW -> HttpOverviewTab(entry)
      HttpDetailTab.HEADERS -> HttpHeadersTab(entry)
      HttpDetailTab.REQUEST -> HttpRequestBodyTab(entry)
      HttpDetailTab.RESPONSE -> HttpResponseBodyTab(entry, formattedBodyCache, highlightedBodyCache)
      HttpDetailTab.CURL -> HttpCurlTab(entry)
    }
  }
}

@Composable
private fun HttpOverviewTab(entry: NetworkLogEntry) {
  Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center) {
          StatusBadge(
              statusCode = entry.responseCode,
              statusMessage = entry.responseMessage,
              isInProgress = entry.isInProgress)
        }
    SectionTitle("Request Info")
    DetailRow("URL", entry.url)
    DetailRow("Method", entry.method)
    entry.protocol?.let { DetailRow("Protocol", it) }
    Spacer(modifier = Modifier.height(16.dp))
    SectionTitle("Timing")
    DetailRow("Duration", entry.durationString)
    DetailRow("Response Size", entry.responseSizeString)
    entry.errorMessage?.let {
      Spacer(modifier = Modifier.height(16.dp))
      SectionTitle("Error")
      DetailRow("Message", it, valueColor = MaterialTheme.colorScheme.error)
    }
  }
}

@Composable
private fun HttpHeadersTab(entry: NetworkLogEntry) {
  Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
    SectionTitle("Request Headers (${entry.requestHeaders.size})")
    if (entry.requestHeaders.isEmpty()) {
      Text(
          text = "No request headers",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.outline,
          modifier = Modifier.padding(vertical = 8.dp))
    } else {
      entry.requestHeaders.forEach { (key, value) -> DetailRow(key, value) }
    }
    Spacer(modifier = Modifier.height(24.dp))
    SectionTitle("Response Headers (${entry.responseHeaders.size})")
    if (entry.responseHeaders.isEmpty()) {
      Text(
          text = "No response headers",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.outline,
          modifier = Modifier.padding(vertical = 8.dp))
    } else {
      entry.responseHeaders.forEach { (key, value) -> DetailRow(key, value) }
    }
  }
}

@Composable
private fun HttpRequestBodyTab(entry: NetworkLogEntry) {
  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    entry.requestBody?.let { body ->
      SectionTitle("Request Body")
      Spacer(modifier = Modifier.height(8.dp))
      CodeBlock(body)
    } ?: EmptyBodyPlaceholder(emoji = "📭", message = "No Request Body")
  }
}

@Composable
private fun HttpResponseBodyTab(
    entry: NetworkLogEntry,
    formattedBodyCache: LruCache<String, String>,
    highlightedBodyCache: LruCache<String, AnnotatedString>,
) {
  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    entry.responseBody?.let { body ->
      SectionTitle("Response Body")
      Spacer(modifier = Modifier.height(8.dp))
      CodeBlock(
          content = body,
          cachedFormatted = formattedBodyCache[entry.id],
          onFormatted = { formatted -> formattedBodyCache.put(entry.id, formatted) },
          cachedHighlighted = highlightedBodyCache[entry.id],
          onHighlighted = { highlighted -> highlightedBodyCache.put(entry.id, highlighted) },
      )
    }
        ?: EmptyBodyPlaceholder(
            emoji = if (entry.isInProgress) "⏳" else "📭",
            message = if (entry.isInProgress) "Loading..." else "No Response Body")
  }
}

@Composable
private fun HttpCurlTab(entry: NetworkLogEntry) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val curlCommand = entry.toCurlCommand()

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          SectionTitle("cURL Command")
          IconButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(curlCommand))
                Toast.makeText(context, "cURL copied to clipboard", Toast.LENGTH_SHORT).show()
              }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy cURL",
                    tint = MaterialTheme.colorScheme.primary)
              }
        }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Tap the copy icon or use the button in the toolbar to copy this command.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline)

    Spacer(modifier = Modifier.height(16.dp))

    CodeBlock(curlCommand)
  }
}

@Composable
private fun EmptyBodyPlaceholder(emoji: String, message: String, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = emoji, fontSize = 48.sp)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text = message,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.outline)
    }
  }
}
