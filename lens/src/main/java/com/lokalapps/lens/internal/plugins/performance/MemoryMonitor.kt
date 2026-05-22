package com.lokalapps.lens.internal.plugins.performance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors JVM heap memory usage via [Runtime] at a configurable interval.
 *
 * Reports used heap, total heap, max heap, and native heap (via
 * `Debug.getNativeHeapAllocatedSize()`). All values are in bytes; UI is responsible for formatting.
 */
internal class MemoryMonitor(private val scope: CoroutineScope) {

  companion object {
    /** Polling interval in milliseconds. */
    private const val POLL_INTERVAL_MS = 1_000L

    /** Maximum number of historical memory samples to retain. */
    private const val MAX_HISTORY = 60
  }

  /** Snapshot of memory state at a point in time. */
  data class MemorySnapshot(
      val usedHeapBytes: Long,
      val totalHeapBytes: Long,
      val maxHeapBytes: Long,
      val nativeHeapBytes: Long,
  )

  private val _snapshot = MutableStateFlow(MemorySnapshot(0L, 0L, 0L, 0L))
  /** Current memory snapshot (updated every [POLL_INTERVAL_MS]). */
  val snapshot: StateFlow<MemorySnapshot> = _snapshot.asStateFlow()

  private val _usedHeapHistory = MutableStateFlow<List<Long>>(emptyList())
  /** Rolling history of used heap bytes (last [MAX_HISTORY] samples). */
  val usedHeapHistory: StateFlow<List<Long>> = _usedHeapHistory.asStateFlow()

  private var job: Job? = null

  /** Start polling memory stats. */
  fun start() {
    if (job?.isActive == true) return
    _usedHeapHistory.value = emptyList()
    job =
        scope.launch {
          while (isActive) {
            val runtime = Runtime.getRuntime()
            val totalHeap = runtime.totalMemory()
            val freeHeap = runtime.freeMemory()
            val maxHeap = runtime.maxMemory()
            val usedHeap = totalHeap - freeHeap
            val nativeHeap = android.os.Debug.getNativeHeapAllocatedSize()

            _snapshot.value =
                MemorySnapshot(
                    usedHeapBytes = usedHeap,
                    totalHeapBytes = totalHeap,
                    maxHeapBytes = maxHeap,
                    nativeHeapBytes = nativeHeap,
                )

            val history = _usedHeapHistory.value.toMutableList()
            history.add(usedHeap)
            if (history.size > MAX_HISTORY) {
              history.removeAt(0)
            }
            _usedHeapHistory.value = history

            delay(POLL_INTERVAL_MS)
          }
        }
  }

  /** Stop polling. */
  fun stop() {
    job?.cancel()
    job = null
  }
}
