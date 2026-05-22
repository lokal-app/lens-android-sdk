package com.lokalapps.lens.plugins.preferences.ui

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.delay

/**
 * SharedPreferences viewer and editor screen.
 *
 * Features:
 * - View all SharedPreferences files
 * - View key-value pairs with type badges
 * - Edit values (String, Int, Long, Float, Boolean, Set<String>)
 * - Delete entries
 * - Add new entries
 * - Copy values to clipboard
 *
 * Similar to Flipper's SharedPreferences plugin.
 */
@Composable
fun PreferencesScreen() {
  val context = LocalContext.current
  var selectedFile by remember { mutableStateOf<String?>(null) }
  var prefFiles by remember { mutableStateOf<List<String>>(emptyList()) }

  LaunchedEffect(Unit) { prefFiles = getSharedPreferencesFiles(context) }

  if (selectedFile != null) {
    PreferencesDetailScreen(
        context = context, fileName = selectedFile!!, onBack = { selectedFile = null })
  } else {
    PreferencesListScreen(files = prefFiles, onFileClick = { selectedFile = it })
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesListScreen(files: List<String>, onFileClick: (String) -> Unit) {
  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(title = { Text("SharedPreferences (${files.size})") })

    if (files.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = null,
              modifier = Modifier.size(64.dp),
              tint = MaterialTheme.colorScheme.outline)
          Spacer(modifier = Modifier.height(16.dp))
          Text(
              text = "No SharedPreferences files found",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.outline)
        }
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files) { fileName ->
          PrefFileCard(fileName = fileName, onClick = { onFileClick(fileName) })
        }
      }
    }
  }
}

@Composable
private fun PrefFileCard(fileName: String, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = Icons.Default.Folder,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                  text = fileName.removeSuffix(".xml"),
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium)
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesDetailScreen(context: Context, fileName: String, onBack: () -> Unit) {
  var entries by remember { mutableStateOf<List<Pair<String, Any?>>>(emptyList()) }
  var refreshTrigger by remember { mutableIntStateOf(0) }
  var showAddDialog by remember { mutableStateOf(false) }
  var editingEntry by remember { mutableStateOf<Pair<String, Any?>?>(null) }

  val prefName = fileName.removeSuffix(".xml")

  // Load entries
  LaunchedEffect(fileName, refreshTrigger) {
    val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    entries = prefs.all.entries.map { it.key to it.value }.sortedBy { it.first }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
        title = { Text(prefName) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { showAddDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = "Add entry")
          }
        })

    if (entries.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "No entries in this file",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.outline)
          Spacer(modifier = Modifier.height(16.dp))
          OutlinedButton(onClick = { showAddDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Entry")
          }
        }
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(entries, key = { it.first }) { (key, value) ->
          PrefEntryCard(key = key, value = value, onEdit = { editingEntry = key to value })
        }
      }
    }
  }

  // Edit dialog
  editingEntry?.let { (key, value) ->
    EditPreferenceDialog(
        key = key,
        value = value,
        isNewEntry = false,
        onDismiss = { editingEntry = null },
        onSave = { newValue ->
          savePreference(context, prefName, key, newValue)
          editingEntry = null
          refreshTrigger++
          Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
        },
        onDelete = {
          deletePreference(context, prefName, key)
          editingEntry = null
          refreshTrigger++
          Toast.makeText(context, "Deleted!", Toast.LENGTH_SHORT).show()
        })
  }

  // Add new entry dialog
  if (showAddDialog) {
    AddPreferenceDialog(
        onDismiss = { showAddDialog = false },
        onSave = { key, value ->
          savePreference(context, prefName, key, value)
          showAddDialog = false
          refreshTrigger++
          Toast.makeText(context, "Added!", Toast.LENGTH_SHORT).show()
        })
  }
}

@Composable
private fun PrefEntryCard(key: String, value: Any?, onEdit: () -> Unit) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  var showCopyFeedback by remember { mutableStateOf(false) }
  val formattedValue = formatValue(value)

  val backgroundColor by
      animateColorAsState(
          targetValue =
              if (showCopyFeedback) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Transparent,
          label = "copyFeedback")

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .clickable(onClick = onEdit),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.background(backgroundColor).padding(12.dp).fillMaxWidth()) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                // Key name
                SelectionContainer(modifier = Modifier.weight(1f)) {
                  Text(
                      text = key,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Type badge
                Text(
                    text = getTypeLabel(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = getTypeColor(value),
                    modifier =
                        Modifier.background(
                                getTypeColor(value).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp))

                Spacer(modifier = Modifier.width(8.dp))

                // Edit icon
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))

                Spacer(modifier = Modifier.width(8.dp))

                // Copy icon
                IconButton(
                    onClick = {
                      clipboardManager.setText(AnnotatedString("$key: $formattedValue"))
                      Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                      showCopyFeedback = true
                    },
                    modifier = Modifier.size(24.dp)) {
                      Icon(
                          imageVector =
                              if (showCopyFeedback) Icons.Default.Check
                              else Icons.Default.ContentCopy,
                          contentDescription = "Copy",
                          modifier = Modifier.size(16.dp),
                          tint =
                              if (showCopyFeedback) Color(0xFF4CAF50)
                              else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
              }

          Spacer(modifier = Modifier.height(8.dp))

          // Value box
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .background(
                          MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                      .padding(10.dp)) {
                SelectionContainer {
                  Text(
                      text = formattedValue,
                      style = MaterialTheme.typography.bodySmall,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      lineHeight = 18.sp)
                }
              }
        }
      }

  // Reset feedback after delay
  LaunchedEffect(showCopyFeedback) {
    if (showCopyFeedback) {
      delay(800)
      showCopyFeedback = false
    }
  }
}

/** Dialog for editing an existing preference value. */
@Composable
private fun EditPreferenceDialog(
    key: String,
    value: Any?,
    isNewEntry: Boolean,
    onDismiss: () -> Unit,
    onSave: (Any?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
  var editedValue by remember { mutableStateOf(valueToEditString(value)) }
  var isValid by remember { mutableStateOf(true) }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Column {
          Text(if (isNewEntry) "Add Entry" else "Edit Entry")
          Spacer(modifier = Modifier.height(4.dp))
          Text(
              text = key,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.primary,
              fontFamily = FontFamily.Monospace)
        }
      },
      text = {
        Column {
          // Type indicator
          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "Type: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
                Text(
                    text = getTypeLabel(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = getTypeColor(value),
                    modifier =
                        Modifier.background(
                                getTypeColor(value).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp))
              }

          // Type-specific editor
          when (value) {
            is Boolean -> {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Text("Value")
                    Switch(
                        checked = editedValue.toBoolean(),
                        onCheckedChange = { editedValue = it.toString() })
                  }
            }
            is Set<*> -> {
              OutlinedTextField(
                  value = editedValue,
                  onValueChange = { editedValue = it },
                  label = { Text("Values (one per line)") },
                  modifier = Modifier.fillMaxWidth().height(150.dp),
                  minLines = 5)
            }
            is Int,
            is Long -> {
              OutlinedTextField(
                  value = editedValue,
                  onValueChange = {
                    editedValue = it
                    isValid = it.toLongOrNull() != null || it.isEmpty()
                  },
                  label = { Text("Value") },
                  modifier = Modifier.fillMaxWidth(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  isError = !isValid,
                  supportingText =
                      if (!isValid) {
                        { Text("Enter a valid number") }
                      } else null)
            }
            is Float -> {
              OutlinedTextField(
                  value = editedValue,
                  onValueChange = {
                    editedValue = it
                    isValid = it.toFloatOrNull() != null || it.isEmpty()
                  },
                  label = { Text("Value") },
                  modifier = Modifier.fillMaxWidth(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  isError = !isValid,
                  supportingText =
                      if (!isValid) {
                        { Text("Enter a valid decimal number") }
                      } else null)
            }
            else -> {
              // String or unknown - use text field
              OutlinedTextField(
                  value = editedValue,
                  onValueChange = { editedValue = it },
                  label = { Text("Value") },
                  modifier = Modifier.fillMaxWidth(),
                  minLines = 2)
            }
          }
        }
      },
      confirmButton = {
        Button(
            onClick = {
              val newValue = parseEditedValue(value, editedValue)
              onSave(newValue)
            },
            enabled = isValid) {
              Text("Save")
            }
      },
      dismissButton = {
        Row {
          // Delete button (only for existing entries)
          if (onDelete != null && !isNewEntry) {
            TextButton(
                onClick = onDelete,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error)) {
                  Icon(
                      Icons.Default.Delete,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Delete")
                }
            Spacer(modifier = Modifier.width(8.dp))
          }
          TextButton(onClick = onDismiss) { Text("Cancel") }
        }
      })
}

/** Dialog for adding a new preference entry. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPreferenceDialog(onDismiss: () -> Unit, onSave: (String, Any?) -> Unit) {
  var key by remember { mutableStateOf("") }
  var value by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf(PrefType.STRING) }
  var typeExpanded by remember { mutableStateOf(false) }
  var isValid by remember { mutableStateOf(true) }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Add New Entry") },
      text = {
        Column {
          // Key input
          OutlinedTextField(
              value = key,
              onValueChange = { key = it },
              label = { Text("Key") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true)

          Spacer(modifier = Modifier.height(12.dp))

          // Type selector
          ExposedDropdownMenuBox(
              expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = {
                      ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                    },
                    modifier =
                        Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                ExposedDropdownMenu(
                    expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                      PrefType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                              selectedType = type
                              typeExpanded = false
                              value = "" // Reset value on type change
                            })
                      }
                    }
              }

          Spacer(modifier = Modifier.height(12.dp))

          // Value input based on type
          when (selectedType) {
            PrefType.BOOLEAN -> {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Text("Value")
                    Switch(checked = value.toBoolean(), onCheckedChange = { value = it.toString() })
                  }
            }
            PrefType.STRING_SET -> {
              OutlinedTextField(
                  value = value,
                  onValueChange = { value = it },
                  label = { Text("Values (one per line)") },
                  modifier = Modifier.fillMaxWidth().height(120.dp),
                  minLines = 4)
            }
            PrefType.INT,
            PrefType.LONG -> {
              OutlinedTextField(
                  value = value,
                  onValueChange = {
                    value = it
                    isValid = it.toLongOrNull() != null || it.isEmpty()
                  },
                  label = { Text("Value") },
                  modifier = Modifier.fillMaxWidth(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  isError = !isValid,
                  supportingText =
                      if (!isValid) {
                        { Text("Enter a valid number") }
                      } else null)
            }
            PrefType.FLOAT -> {
              OutlinedTextField(
                  value = value,
                  onValueChange = {
                    value = it
                    isValid = it.toFloatOrNull() != null || it.isEmpty()
                  },
                  label = { Text("Value") },
                  modifier = Modifier.fillMaxWidth(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  isError = !isValid,
                  supportingText =
                      if (!isValid) {
                        { Text("Enter a valid decimal") }
                      } else null)
            }
            PrefType.STRING -> {
              OutlinedTextField(
                  value = value,
                  onValueChange = { value = it },
                  label = { Text("Value") },
                  modifier = Modifier.fillMaxWidth(),
                  minLines = 2)
            }
          }
        }
      },
      confirmButton = {
        Button(
            onClick = {
              val parsedValue = parseNewValue(selectedType, value)
              onSave(key, parsedValue)
            },
            enabled = key.isNotBlank() && isValid) {
              Text("Add")
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ============================================================================
// Helper Functions
// ============================================================================

private enum class PrefType(val label: String) {
  STRING("String"),
  INT("Int"),
  LONG("Long"),
  FLOAT("Float"),
  BOOLEAN("Boolean"),
  STRING_SET("Set<String>")
}

private fun getSharedPreferencesFiles(context: Context): List<String> {
  val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
  return if (prefsDir.exists() && prefsDir.isDirectory) {
    prefsDir.listFiles()?.filter { it.extension == "xml" }?.map { it.name }?.sorted() ?: emptyList()
  } else {
    emptyList()
  }
}

private fun getTypeLabel(value: Any?): String =
    when (value) {
      is String -> "String"
      is Int -> "Int"
      is Long -> "Long"
      is Float -> "Float"
      is Boolean -> "Bool"
      is Set<*> -> "Set"
      null -> "Null"
      else -> value::class.simpleName ?: "?"
    }

private fun getTypeColor(value: Any?): Color =
    when (value) {
      is String -> Color(0xFF4CAF50)
      is Int,
      is Long -> Color(0xFF2196F3)
      is Float -> Color(0xFF9C27B0)
      is Boolean -> Color(0xFFFF9800)
      is Set<*> -> Color(0xFF00BCD4)
      else -> Color(0xFF607D8B)
    }

private fun formatValue(value: Any?): String =
    when (value) {
      is String -> "\"$value\""
      is Set<*> -> value.joinToString("\n") { "• $it" }
      null -> "null"
      else -> value.toString()
    }

/** Converts a value to its editable string representation. */
private fun valueToEditString(value: Any?): String =
    when (value) {
      is String -> value
      is Set<*> -> value.filterIsInstance<String>().joinToString("\n")
      is Boolean -> value.toString()
      null -> ""
      else -> value.toString()
    }

/** Parses the edited string back to the original type. */
private fun parseEditedValue(originalValue: Any?, editedString: String): Any? =
    when (originalValue) {
      is String -> editedString
      is Int -> editedString.toIntOrNull() ?: 0
      is Long -> editedString.toLongOrNull() ?: 0L
      is Float -> editedString.toFloatOrNull() ?: 0f
      is Boolean -> editedString.toBoolean()
      is Set<*> -> editedString.lines().filter { it.isNotBlank() }.toSet()
      else -> editedString
    }

/** Parses a new value based on the selected type. */
private fun parseNewValue(type: PrefType, valueString: String): Any? =
    when (type) {
      PrefType.STRING -> valueString
      PrefType.INT -> valueString.toIntOrNull() ?: 0
      PrefType.LONG -> valueString.toLongOrNull() ?: 0L
      PrefType.FLOAT -> valueString.toFloatOrNull() ?: 0f
      PrefType.BOOLEAN -> valueString.toBoolean()
      PrefType.STRING_SET -> valueString.lines().filter { it.isNotBlank() }.toSet()
    }

/** Saves a preference value to SharedPreferences. */
private fun savePreference(context: Context, prefName: String, key: String, value: Any?) {
  val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
  prefs.edit().apply {
    when (value) {
      is String -> putString(key, value)
      is Int -> putInt(key, value)
      is Long -> putLong(key, value)
      is Float -> putFloat(key, value)
      is Boolean -> putBoolean(key, value)
      is Set<*> -> {
        @Suppress("UNCHECKED_CAST") putStringSet(key, value as Set<String>)
      }
      null -> remove(key)
    }
    apply()
  }
}

/** Deletes a preference entry from SharedPreferences. */
private fun deletePreference(context: Context, prefName: String, key: String) {
  val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
  prefs.edit().remove(key).apply()
}
