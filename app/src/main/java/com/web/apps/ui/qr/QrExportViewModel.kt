package com.web.apps.ui.qr

import androidx.lifecycle.ViewModel
import com.web.apps.core.qr.QrExportImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class QrExportViewModel @Inject constructor(
    private val qrManager: QrExportImportManager
) : ViewModel() {

    suspend fun generateQrCode(): android.graphics.Bitmap? {
        val encodedData = qrManager.buildExportString()
        return qrManager.generateQrBitmap(encodedData)
    }
}