package com.web.apps.core.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.web.apps.core.inspector.InspectedResource
import com.web.apps.core.security.SafeBrowsingChecker

class WebAppsWebViewClient(
    private val safeBrowsingChecker: SafeBrowsingChecker,
    private val allowHttp: Boolean,
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String) -> Unit,
    private val onDangerousSiteDetected: (String) -> Unit,
    private val onHttpBlocked: (String) -> Unit,
    private val onRenderProcessGone: () -> Boolean,
    private val onResourceLoaded: (InspectedResource) -> Unit
) : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        onPageFinished(url)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        val scheme = request.url.scheme ?: return false

        if (scheme == "http" && !allowHttp) {
            onHttpBlocked(url)
            return true
        }

        if (safeBrowsingChecker.isKnownDangerous(url)) {
            onDangerousSiteDetected(url)
            return true
        }

        return false
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()

        if (safeBrowsingChecker.isKnownDangerous(url)) {
            onDangerousSiteDetected(url)
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                "Blocked by Dangerous Website Protection".byteInputStream()
            )
        }

        onResourceLoaded(
            InspectedResource(
                url = url,
                method = request.method,
                mimeType = guessMimeTypeFromUrl(url)
            )
        )

        return super.shouldInterceptRequest(view, request)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        onDangerousSiteDetected(view.url ?: "unknown")
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        return onRenderProcessGone()
    }

    private fun guessMimeTypeFromUrl(url: String): String {
        return when {
            url.endsWith(".js") -> "application/javascript"
            url.endsWith(".css") -> "text/css"
            url.endsWith(".png") -> "image/png"
            url.endsWith(".jpg") || url.endsWith(".jpeg") -> "image/jpeg"
            url.endsWith(".svg") -> "image/svg+xml"
            url.endsWith(".json") -> "application/json"
            url.endsWith(".woff") || url.endsWith(".woff2") -> "font/woff"
            else -> "unknown"
        }
    }
}