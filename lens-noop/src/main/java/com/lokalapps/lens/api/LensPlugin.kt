package com.lokalapps.lens.api

import android.content.Context

/** No-op LensPlugin base interface stub for release builds. Exists only for API compatibility. */
interface LensPlugin {
  val id: String
  val name: String
  val icon: Int
  val description: String
  val priority: Int
    get() = 0

  fun onInitialize(context: Context) {}

  fun onEnabled() {}

  fun onDisabled() {}

  fun onDestroy() {}
}
