package com.behtar.lens.sample

import com.behtar.lens.api.DeepLink
import com.behtar.lens.api.DeepLinkProvider

/**
 * Sample DeepLinkProvider demonstrating how to supply app-specific quick links to the Lens Deep
 * Link Tester.
 */
class SampleDeepLinkProvider : DeepLinkProvider {

  override fun getQuickLinks(): List<DeepLink> =
      listOf(
          DeepLink(label = "Home", path = "/home"),
          DeepLink(label = "Profile", path = "/profile"),
          DeepLink(label = "Settings", path = "/settings"),
      )
}
