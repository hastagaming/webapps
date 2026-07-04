package com.web.apps.core.webview

import android.graphics.Bitmap
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.web.apps.core.permission.ContainerPermissionManager
import com.web.apps.core.permission.PermissionDecision
import com.web.apps.core.permission.WebAppPermissionType

class ContainerWebChromeClient(
    private val containerId: Long,
    private val permissionManager: ContainerPermissionManager,
    private val onFaviconReceived: (Bitmap) -> Unit,
    private val onProgressChanged: (Int) -> Unit,
    private val onShowCustomView: (View) -> Unit,
    private val onHideCustomView: () -> Unit
) : WebChromeClient() {

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        super.onReceivedIcon(view, icon)
        onFaviconReceived(icon)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        super.onShowCustomView(view, callback)
        onShowCustomView(view)
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        onHideCustomView()
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        val decision = permissionManager.getDecision(containerId, WebAppPermissionType.LOCATION)
        when (decision) {
            PermissionDecision.ALLOWED -> callback.invoke(origin, true, false)
            PermissionDecision.DENIED -> callback.invoke(origin, false, false)
            PermissionDecision.ASK_EVERY_TIME -> callback.invoke(origin, false, false)
        }
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        val requestedResources = request.resources
        val decisions = requestedResources.mapNotNull { resource ->
            val type = when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> WebAppPermissionType.CAMERA
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> WebAppPermissionType.MICROPHONE
                else -> null
            }
            type?.let { it to permissionManager.getDecision(containerId, it) }
        }

        val allAllowed = decisions.isNotEmpty() && decisions.all { it.second == PermissionDecision.ALLOWED }

        if (allAllowed) {
            request.grant(requestedResources)
        } else {
            request.deny()
        }
    }
}