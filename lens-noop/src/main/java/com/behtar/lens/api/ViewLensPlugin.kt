package com.behtar.lens.api

import android.content.Context
import android.view.View

/** No-op ViewLensPlugin stub for release builds. Exists only for API compatibility. */
@LensExperimental
interface ViewLensPlugin : LensPlugin {
  fun createView(context: Context): View
}
