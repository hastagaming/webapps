package com.web.apps.ui.qr

import androidx.lifecycle.ViewModel
import com.web.apps.core.qr.QrExportImportManager
import com.web.apps.core.qr.QrImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val qrManager: QrExportImportManager
) : ViewModel() {

    suspend fun importFromQr(scannedData: String): QrImportResult {
        return qrManager.importFromScannedString(scannedData)
    }
}