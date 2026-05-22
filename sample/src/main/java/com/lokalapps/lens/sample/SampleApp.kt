package com.lokalapps.lens.sample

import android.app.Application
import com.lokalapps.lens.api.ActivationGesture
import com.lokalapps.lens.api.Lens
import okhttp3.OkHttpClient

class SampleApp : Application() {

  lateinit var okHttpClient: OkHttpClient
    private set

  override fun onCreate() {
    super.onCreate()

    // Install Lens with 5-tap activation
    Lens.install(this) {
      activationGesture = ActivationGesture.FIVE_TAP
      showNotification = true
      environments(SampleEnvironmentProvider(this@SampleApp))
      deepLinks(SampleDeepLinkProvider())
    }

    // Create OkHttpClient with Lens interceptor
    okHttpClient = OkHttpClient.Builder().addInterceptor(Lens.getNetworkInterceptor()).build()
  }
}
