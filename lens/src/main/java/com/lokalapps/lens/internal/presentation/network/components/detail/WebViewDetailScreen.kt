package com.lokalapps.lens.internal.presentation.network.components.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.internal.data.model.WebViewLogEntry
import com.lokalapps.lens.internal.presentation.common.DetailRow
import com.lokalapps.lens.internal.presentation.common.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewDetailScreen(entry: WebViewLogEntry, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
        })

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
      SectionTitle("Request Info")
      DetailRow("URL", entry.url)
      DetailRow("Method", entry.method)
      DetailRow("Main Frame", if (entry.isMainFrame) "Yes" else "No")

      Spacer(modifier = Modifier.height(16.dp))

      SectionTitle("Response")
      entry.statusCode?.let { DetailRow("Status Code", it.toString()) }
      entry.durationString?.let { DetailRow("Duration", it) }
      entry.mimeType?.let { DetailRow("MIME Type", it) }
      entry.errorMessage?.let {
        DetailRow("Error", it, valueColor = MaterialTheme.colorScheme.error)
      }

      if (entry.requestHeaders.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle("Request Headers (${entry.requestHeaders.size})")
        entry.requestHeaders.forEach { (key, value) -> DetailRow(key, value) }
      }
    }
  }
}
