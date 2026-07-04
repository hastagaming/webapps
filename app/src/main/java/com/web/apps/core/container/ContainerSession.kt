package com.web.apps.core.container

import android.webkit.WebView

data class ContainerSession(
    val containerId: Long,
    val webView: WebView,
    var isAlive: Boolean = true,
    var currentUrl: String = "",
    var loadProgress: Int = 0,
    var faviconBitmap: android.graphics.Bitmap? = null,
    var lastActiveTimestamp: Long = System.currentTimeMillis()
)