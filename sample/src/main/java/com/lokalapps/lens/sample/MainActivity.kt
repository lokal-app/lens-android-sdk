package com.lokalapps.lens.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.api.Lens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Request

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val scope = rememberCoroutineScope()
          Column(
              modifier = Modifier.padding(32.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
              horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
                text = "Lens SDK Sample",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Tap 5 times anywhere to open the Lens dashboard",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                  scope.launch(Dispatchers.IO) {
                    // Make a sample network request to see it in the Network Inspector
                    val client = (application as SampleApp).okHttpClient
                    val request = Request.Builder().url("https://httpbin.org/get").build()
                    try {
                      client.newCall(request).execute().close()
                    } catch (_: Exception) {
                      // Ignore network errors in sample
                    }
                  }
                }) {
                  Text("Make Sample Request")
                }
            Button(onClick = { Lens.open() }) { Text("Open Lens Dashboard") }
          }
        }
      }
    }
  }
}
