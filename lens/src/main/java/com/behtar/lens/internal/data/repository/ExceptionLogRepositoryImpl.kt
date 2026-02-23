package com.behtar.lens.internal.data.repository

import android.content.Context
import com.behtar.lens.internal.data.model.ExceptionLogEntry
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Persistent implementation of [ExceptionLogRepository].
 *
 * Stores exceptions to disk so they survive app crashes and restarts. This is critical for crash
 * logging - we need to capture the exception and write it to disk BEFORE the app process
 * terminates.
 *
 * ## How it works:
 * 1. On initialization, loads previously persisted exceptions from disk
 * 2. On addEntry(), writes to disk SYNCHRONOUSLY before returning
 * 3. Uses Kotlin Flow for reactive UI updates
 *
 * ## File Storage:
 * Exceptions are stored as JSON in the app's internal files directory:
 * `/data/data/{package}/files/lens_exceptions.json`
 *
 * ## Thread Safety:
 * All file operations are synchronized to prevent corruption.
 */
class ExceptionLogRepositoryImpl(private val context: Context) : ExceptionLogRepository {

  private companion object {
    const val MAX_ENTRIES = 100
    const val FILE_NAME = "lens_exceptions.json"
  }

  private val file: File by lazy { File(context.filesDir, FILE_NAME) }

  private val _exceptions = MutableStateFlow<List<ExceptionLogEntry>>(emptyList())

  override val exceptions: StateFlow<List<ExceptionLogEntry>> = _exceptions.asStateFlow()

  override val count: Int
    get() = _exceptions.value.size

  init {
    // Load persisted exceptions on startup
    loadFromDisk()
  }

  /**
   * Adds an exception entry and persists to disk SYNCHRONOUSLY.
   *
   * This is intentionally synchronous because:
   * 1. For crashes, we must write before the process dies
   * 2. File I/O is fast for small JSON files
   * 3. Crash capture is rare, so performance impact is minimal
   */
  override fun addEntry(entry: ExceptionLogEntry) {
    synchronized(this) {
      val current = _exceptions.value.toMutableList()
      current.add(0, entry) // Add to beginning (newest first)

      // Trim if exceeds max
      val trimmed =
          if (current.size > MAX_ENTRIES) {
            current.take(MAX_ENTRIES)
          } else {
            current
          }

      _exceptions.value = trimmed

      // Persist synchronously - critical for crash survival
      saveToDisk(trimmed)
    }
  }

  override fun logException(
      throwable: Throwable,
      isHandled: Boolean,
      additionalInfo: Map<String, String>
  ) {
    val entry =
        ExceptionLogEntry.fromThrowable(
            throwable = throwable, isHandled = isHandled, additionalInfo = additionalInfo)
    addEntry(entry)
  }

  override fun clear() {
    synchronized(this) {
      _exceptions.value = emptyList()
      deleteFile()
    }
  }

  /** Loads exceptions from disk into memory. Called once during initialization. */
  private fun loadFromDisk() {
    try {
      if (!file.exists()) {
        Timber.d("ExceptionLogRepository: No persisted exceptions found")
        return
      }

      val json = file.readText()
      val entries = ExceptionLogEntry.listFromJson(json)
      _exceptions.value = entries

      Timber.d("ExceptionLogRepository: Loaded ${entries.size} persisted exceptions")
    } catch (e: Exception) {
      Timber.e(e, "ExceptionLogRepository: Failed to load persisted exceptions")
      // Don't crash - just start with empty list
      _exceptions.value = emptyList()
    }
  }

  /**
   * Saves exceptions to disk synchronously. Must be synchronous to ensure data is written before
   * crash completes.
   */
  private fun saveToDisk(entries: List<ExceptionLogEntry>) {
    try {
      val json = ExceptionLogEntry.listToJson(entries)
      file.writeText(json)
      Timber.d("ExceptionLogRepository: Persisted ${entries.size} exceptions to disk")
    } catch (e: Exception) {
      Timber.e(e, "ExceptionLogRepository: Failed to persist exceptions")
      // Don't throw - we don't want to cause additional crashes
    }
  }

  /** Deletes the persistence file. */
  private fun deleteFile() {
    try {
      if (file.exists()) {
        file.delete()
        Timber.d("ExceptionLogRepository: Cleared persisted exceptions")
      }
    } catch (e: Exception) {
      Timber.e(e, "ExceptionLogRepository: Failed to delete persistence file")
    }
  }
}
