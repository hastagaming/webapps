package com.web.apps.core.inspector

import android.webkit.WebView
import javax.inject.Inject
import javax.inject.Singleton

data class InspectedResource(
    val url: String,
    val method: String,
    val mimeType: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class SourceInspectorManager @Inject constructor() {

    private val resourceLogByContainer = mutableMapOf<Long, MutableList<InspectedResource>>()
    private val lock = Any()

    companion object {
        private const val MAX_LOG_ENTRIES_PER_CONTAINER = 200
    }

    fun logResource(containerId: Long, resource: InspectedResource) {
        synchronized(lock) {
            val log = resourceLogByContainer.getOrPut(containerId) { mutableListOf() }
            log.add(0, resource)
            while (log.size > MAX_LOG_ENTRIES_PER_CONTAINER) {
                log.removeAt(log.lastIndex)
            }
        }
    }

    fun getResourceLog(containerId: Long): List<InspectedResource> {
        synchronized(lock) {
            return resourceLogByContainer[containerId]?.toList() ?: emptyList()
        }
    }

    fun clearLog(containerId: Long) {
        synchronized(lock) {
            resourceLogByContainer.remove(containerId)
        }
    }

    fun fetchPageSource(webView: WebView, onResult: (String) -> Unit) {
        webView.evaluateJavascript(
            "(function() { return document.documentElement.outerHTML; })();"
        ) { rawResult ->
            onResult(unescapeJsString(rawResult))
        }
    }

    private fun unescapeJsString(raw: String): String {
        var result = raw
        if (result.length >= 2 && result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length - 1)
        }
        return result
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}