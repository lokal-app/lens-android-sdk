package com.lokalapps.lens.internal.interceptors

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lokalapps.lens.api.Lens
import com.lokalapps.lens.internal.di.LensServiceLocator
import timber.log.Timber

/**
 * A WebViewClient wrapper that intercepts network requests for Lens logging.
 *
 * This class wraps an existing WebViewClient and logs all network requests to
 * [WebViewLogRepository] before delegating to the original client.
 *
 * ## Features:
 * - Logs all resource requests (HTML, CSS, JS, images, XHR, etc.)
 * - Captures request method, headers, and URL
 * - Tracks response status codes and errors
 * - Distinguishes main frame loads from sub-resources
 *
 * ## Usage:
 * ```kotlin
 * val lensClient = Lens.wrapWebViewClient(myClient)
 * webView.webViewClient = lensClient
 * ```
 *
 * @param delegate The original WebViewClient to wrap (can be null)
 */
class LensWebViewClient private constructor(private val delegate: WebViewClient? = null) :
    WebViewClient() {

  private val repository
    get() = LensServiceLocator.webViewLogRepository

  // Track request IDs for updating with response info
  private val pendingRequests = mutableMapOf<String, String>() // url -> entryId

  /**
   * Intercepts all resource requests for logging.
   *
   * This is the main interception point for WebView network traffic. Called for every resource
   * (HTML, CSS, JS, images, XHR, etc.)
   */
  override fun shouldInterceptRequest(
      view: WebView?,
      request: WebResourceRequest?
  ): WebResourceResponse? {
    if (Lens.isEnabled && request != null) {
      try {
        val url = request.url?.toString() ?: return delegate?.shouldInterceptRequest(view, request)
        val method = request.method ?: "GET"
        val headers = request.requestHeaders ?: emptyMap()
        val isMainFrame = request.isForMainFrame

        val entryId =
            repository.logRequest(
                url = url, method = method, headers = headers, isMainFrame = isMainFrame)

        // Store entry ID for later update with response info
        pendingRequests[url] = entryId

        Timber.d("Lens WebView: Logged request: $method $url")
      } catch (e: Exception) {
        Timber.w(e, "Lens WebView: Error logging request")
      }
    }

    return delegate?.shouldInterceptRequest(view, request)
  }

  override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
    delegate?.onPageStarted(view, url, favicon)
  }

  override fun onPageFinished(view: WebView?, url: String?) {
    // Mark the main frame request as completed with 200
    if (Lens.isEnabled && url != null) {
      pendingRequests[url]?.let { entryId ->
        repository.updateWithResponse(entryId, 200, "text/html")
        pendingRequests.remove(url)
      }
    }
    delegate?.onPageFinished(view, url)
  }

  override fun onReceivedError(
      view: WebView?,
      request: WebResourceRequest?,
      error: WebResourceError?
  ) {
    if (Lens.isEnabled && request != null) {
      val url = request.url?.toString()
      val errorDesc = error?.description?.toString() ?: "Unknown error"
      val errorCode = error?.errorCode ?: -1

      url?.let {
        pendingRequests[it]?.let { entryId ->
          repository.updateWithError(entryId, "Error $errorCode: $errorDesc")
          pendingRequests.remove(it)
        }
      }
    }
    delegate?.onReceivedError(view, request, error)
  }

  override fun onReceivedHttpError(
      view: WebView?,
      request: WebResourceRequest?,
      errorResponse: WebResourceResponse?
  ) {
    if (Lens.isEnabled && request != null && errorResponse != null) {
      val url = request.url?.toString()
      val statusCode = errorResponse.statusCode
      val mimeType = errorResponse.mimeType

      url?.let {
        pendingRequests[it]?.let { entryId ->
          repository.updateWithResponse(entryId, statusCode, mimeType)
          pendingRequests.remove(it)
        }
      }
    }
    delegate?.onReceivedHttpError(view, request, errorResponse)
  }

  @Deprecated("Deprecated in API 23")
  override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
    return delegate?.shouldOverrideUrlLoading(view, url) ?: false
  }

  override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
    return delegate?.shouldOverrideUrlLoading(view, request) ?: false
  }

  companion object {
    /**
     * Wraps an existing WebViewClient with Lens logging.
     *
     * @param delegate The original WebViewClient (can be null)
     * @return A LensWebViewClient that wraps the delegate
     */
    fun wrap(delegate: WebViewClient? = null): LensWebViewClient {
      return LensWebViewClient(delegate)
    }

    /**
     * Installs Lens logging on a WebView.
     *
     * @param webView The WebView to install logging on
     * @param existingClient The existing WebViewClient to wrap (optional)
     */
    fun install(webView: WebView, existingClient: WebViewClient? = null) {
      webView.webViewClient = wrap(existingClient)
    }
  }
}
