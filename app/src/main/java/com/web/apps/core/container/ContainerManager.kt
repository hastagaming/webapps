package com.web.apps.core.container

import android.content.Context
import com.web.apps.core.webview.ContainerWebViewFactory
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.repository.ContainerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerManager @Inject constructor(
    private val webViewFactory: ContainerWebViewFactory,
    private val containerRepository: ContainerRepository
) {

    private val activeSessions = mutableMapOf<Long, ContainerSession>()
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun getOrCreateSession(
        context: Context,
        container: ContainerEntity,
        onFaviconReceived: (Long, android.graphics.Bitmap) -> Unit,
        onProgressChanged: (Long, Int) -> Unit,
        onPageFinished: (Long, String) -> Unit,
        onDangerousSiteDetected: (Long, String) -> Unit,
        onHttpBlocked: (Long, String) -> Unit,
        onDownloadRequested: (Long, String, String, String, Long) -> Unit,
        onShowCustomView: (android.view.View) -> Unit,
        onHideCustomView: () -> Unit
    ): ContainerSession {
        val existing = activeSessions[container.containerId]
        if (existing != null && existing.isAlive) {
            existing.lastActiveTimestamp = System.currentTimeMillis()
            return existing
        }

        val webView = webViewFactory.createWebView(
            context = context,
            container = container,
            onPageStarted = { url ->
                activeSessions[container.containerId]?.currentUrl = url
            },
            onPageFinished = { url ->
                activeSessions[container.containerId]?.currentUrl = url
                onPageFinished(container.containerId, url)
                managerScope.launch {
                    containerRepository.markAccessed(container.containerId)
                    containerRepository.incrementOpenCount(container.containerId)
                }
            },
            onDangerousSiteDetected = { url ->
                onDangerousSiteDetected(container.containerId, url)
            },
            onHttpBlocked = { url ->
                onHttpBlocked(container.containerId, url)
            },
            onFaviconReceived = { bitmap ->
                activeSessions[container.containerId]?.faviconBitmap = bitmap
                onFaviconReceived(container.containerId, bitmap)
            },
            onProgressChanged = { progress ->
                activeSessions[container.containerId]?.loadProgress = progress
                onProgressChanged(container.containerId, progress)
            },
            onShowCustomView = onShowCustomView,
            onHideCustomView = onHideCustomView,
            onDownloadRequested = { url, fileName, mimeType, contentLength ->
                onDownloadRequested(container.containerId, url, fileName, mimeType, contentLength)
            }
        )

        webView.loadUrl(container.url)

        val session = ContainerSession(
            containerId = container.containerId,
            webView = webView,
            currentUrl = container.url
        )
        activeSessions[container.containerId] = session
        return session
    }

    fun getSession(containerId: Long): ContainerSession? = activeSessions[containerId]

    fun refreshContainer(containerId: Long) {
        activeSessions[containerId]?.webView?.reload()
    }

    fun refreshAll() {
        activeSessions.values.forEach { it.webView.reload() }
    }

    fun stopContainer(containerId: Long, keepAliveOverride: Boolean = false) {
        val session = activeSessions[containerId] ?: return
        if (keepAliveOverride) return

        session.webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            destroy()
        }
        session.isAlive = false
        activeSessions.remove(containerId)
    }

    fun stopAll(keepAliveContainerIds: Set<Long> = emptySet()) {
        val idsToStop = activeSessions.keys.filter { it !in keepAliveContainerIds }
        idsToStop.forEach { stopContainer(it) }
    }

    fun evictInactiveSessions(maxIdleMillis: Long, keepAliveContainerIds: Set<Long>) {
        val now = System.currentTimeMillis()
        val toEvict = activeSessions.values.filter { session ->
            session.containerId !in keepAliveContainerIds &&
                (now - session.lastActiveTimestamp) > maxIdleMillis
        }
        toEvict.forEach { stopContainer(it.containerId) }
    }

    fun goBack(containerId: Long): Boolean {
        val webView = activeSessions[containerId]?.webView ?: return false
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    fun goForward(containerId: Long) {
        activeSessions[containerId]?.webView?.let { webView ->
            if (webView.canGoForward()) webView.goForward()
        }
    }

    fun getActiveSessionCount(): Int = activeSessions.count { it.value.isAlive }

    fun getAllActiveContainerIds(): Set<Long> = activeSessions.keys.toSet()

    suspend fun getKeepAliveContainerIds(): Set<Long> {
        return containerRepository.getKeepAliveContainers().map { it.containerId }.toSet()
    }

    fun performHardReset(context: Context, containerId: Long) {
        stopContainer(containerId)
        webViewFactory.clearContainerData(context, containerId)
    }

    fun destroyAllSessions() {
        activeSessions.values.forEach { session ->
            session.webView.apply {
                stopLoading()
                destroy()
            }
        }
        activeSessions.clear()
    }
}