package com.behtar.lens.internal.presentation.common

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.behtar.lens.internal.presentation.theme.NetworkInspectorColors
import kotlinx.coroutines.delay

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
  Row(
      modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier.width(4.dp)
                    .height(22.dp)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary)),
                        shape = RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
      }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  var showCopyFeedback by remember { mutableStateOf(false) }

  val backgroundColor by
      animateColorAsState(
          targetValue =
              if (showCopyFeedback) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.Transparent,
          label = "copyFeedback")

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .background(backgroundColor, RoundedCornerShape(6.dp))
              .clickable {
                clipboardManager.setText(AnnotatedString(value))
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                showCopyFeedback = true
              }
              .padding(vertical = 10.dp, horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.weight(1f))
          Icon(
              imageVector =
                  if (showCopyFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
              contentDescription = "Copy",
              modifier = Modifier.size(14.dp),
              tint =
                  if (showCopyFeedback) Color(0xFF4CAF50)
                  else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.height(6.dp))
        SelectionContainer {
          Text(
              text = value,
              style = MaterialTheme.typography.bodySmall,
              color = valueColor,
              fontFamily = FontFamily.Monospace,
              lineHeight = 18.sp)
        }
      }

  LaunchedEffect(showCopyFeedback) {
    if (showCopyFeedback) {
      delay(800)
      showCopyFeedback = false
    }
  }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(64.dp),
          tint = MaterialTheme.colorScheme.outline)
      Spacer(modifier = Modifier.height(16.dp))
      Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.outline)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline)
    }
  }
}

@Composable
fun ClearHeader(onClear: () -> Unit, modifier: Modifier = Modifier) {
  Row(
      modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.End) {
        IconButton(onClick = onClear) {
          Icon(
              Icons.Default.Delete,
              contentDescription = "Clear",
              tint = MaterialTheme.colorScheme.outline)
        }
      }
}

@Composable
fun StatusBadge(
    statusCode: Int?,
    statusMessage: String?,
    isInProgress: Boolean,
    modifier: Modifier = Modifier
) {
  val statusColor =
      when {
        isInProgress -> Color(0xFF9E9E9E)
        statusCode == null -> Color(0xFF9E9E9E)
        statusCode in 200..299 -> Color(0xFF4CAF50)
        statusCode in 300..399 -> Color(0xFF2196F3)
        statusCode in 400..499 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
      }

  Box(
      modifier =
          modifier
              .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
              .padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text =
                when {
                  isInProgress -> "In Progress..."
                  statusCode != null -> "$statusCode ${statusMessage ?: ""}"
                  else -> "Unknown"
                },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = statusColor)
      }
}

@Composable
fun InfoBox(
    title: String,
    message: String,
    color: Color = NetworkInspectorColors.info,
    modifier: Modifier = Modifier
) {
  Card(
      modifier = modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
      shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
              text = "ℹ️ $title",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = color)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
              text = message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
      }
}
