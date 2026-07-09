package com.web.apps.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import com.web.apps.core.inspector.SourceInspectorManager
import com.web.apps.core.permission.ContainerPermissionManager
import com.web.apps.core.recovery.RecoveryManager
import com.web.apps.core.security.SafeBrowsingChecker
import com.web.apps.data.local.entity.ContainerEntity
import javax.inject.Inject
import javax.inject.Singleton

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

@Singleton
class ContainerWebViewFactory @Inject constructor(
    private val permissionManager: ContainerPermissionManager,
    private val safeBrowsingChecker: SafeBrowsingChecker,
    private val sourceInspectorManager: SourceInspectorManager,
    private val recoveryManager: RecoveryManager,
    private val notificationHelper: com.web.apps.core.notification.ContainerNotificationHelper,
    private val themePreferenceManager: com.web.apps.core.preferences.ThemePreferenceManager
) {

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(
        context: Context,
        container: ContainerEntity,
        onPageStarted: (String) -> Unit,
        onPageFinished: (String) -> Unit,
        onDangerousSiteDetected: (String) -> Unit,
        onHttpBlocked: (String) -> Unit,
        onFaviconReceived: (android.graphics.Bitmap) -> Unit,
        onProgressChanged: (Int) -> Unit,
        onShowCustomView: (android.view.View) -> Unit,
        onHideCustomView: () -> Unit,
        onDownloadRequested: (String, String, String, Long) -> Unit
    ): WebView {
        val webView = WebView(context)

        configureSettings(webView.settings, container)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        if (container.isNotificationEnabled) {
            webView.addJavascriptInterface(
                object {
                    @android.webkit.JavascriptInterface
                    fun onTitleChanged(newTitle: String) {
                        notificationHelper.showUnreadNotification(
                            context = context,
                            containerId = container.containerId,
                            containerName = container.name,
                            title = newTitle
                        )
                    }
                },
                "AndroidTitleWatcher"
            )
        }

        webView.webViewClient = WebAppsWebViewClient(
            safeBrowsingChecker = safeBrowsingChecker,
            allowHttp = container.isHttpAllowed,
            onPageStarted = { url ->
                recoveryManager.onPageLoadStarted(container.containerId)
                onPageStarted(url)
            },
            onPageFinished = { url ->
                recoveryManager.onPageLoadFinished(container.containerId)
                onPageFinished(url)

                if (container.isNotificationEnabled) {
                    webView.evaluateJavascript(
                        """
                        (function() {
                            if (window.__webappsTitleObserverInstalled) return;
                            window.__webappsTitleObserverInstalled = true;
                            var target = document.querySelector('title');
                            if (!target) return;
                            var observer = new MutationObserver(function() {
                                AndroidTitleWatcher.onTitleChanged(document.title);
                            });
                            observer.observe(target, { childList: true });
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            },
            onDangerousSiteDetected = onDangerousSiteDetected,
            onHttpBlocked = onHttpBlocked,
            onRenderProcessGone = {
                recoveryManager.onRenderProcessGone(container.containerId)
            },
            onResourceLoaded = { resource ->
                sourceInspectorManager.logResource(container.containerId, resource)
            }
        )

        webView.webChromeClient = ContainerWebChromeClient(
            containerId = container.containerId,
            permissionManager = permissionManager,
            onFaviconReceived = onFaviconReceived,
            onProgressChanged = onProgressChanged,
            onShowCustomView = onShowCustomView,
            onHideCustomView = onHideCustomView
        )

        webView.setDownloadListener { url, _, contentDisposition, mimeType, contentLength ->
            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
            onDownloadRequested(url, fileName, mimeType, contentLength)
        }

        return webView
    }

    private fun configureSettings(settings: WebSettings, container: ContainerEntity) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.mediaPlaybackRequiresUserGesture = false
        settings.setGeolocationEnabled(true)
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.textZoom = themePreferenceManager.getFontScalePercentBlocking()

        val userAgent = when {
            container.userAgentOverride != null -> container.userAgentOverride
            container.isDesktopMode -> DESKTOP_USER_AGENT
            else -> settings.userAgentString
        }
        settings.userAgentString = userAgent
    }

    fun clearContainerData(context: Context, containerId: Long) {
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}