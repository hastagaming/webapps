package com.web.apps.ui.qr

import android.Manifest
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.web.apps.core.qr.QrImportResult
import kotlinx.coroutines.launch

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: QrScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var scannedValue by remember { mutableStateOf<String?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val scanner = BarcodeScanning.getClient()
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && scannedValue == null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            val value = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue
                                            if (value != null && scannedValue == null) {
                                                scannedValue = value
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                            } catch (e: Exception) {
                                // ignore
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Camera permission is required to scan QR codes.", modifier = Modifier.padding(16.dp))
            }
        }
    }

    LaunchedEffect(scannedValue) {
        val value = scannedValue
        if (value != null && !isProcessing) {
            isProcessing = true
            coroutineScope.launch {
                when (val result = viewModel.importFromQr(value)) {
                    is QrImportResult.Success -> {
                        resultMessage = "Imported ${result.containersImported} containers and ${result.groupsImported} groups."
                    }
                    is QrImportResult.Failure -> {
                        resultMessage = result.message
                        scannedValue = null
                        isProcessing = false
                    }
                }
            }
        }
    }

    if (resultMessage != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Import Result") },
            text = { Text(resultMessage!!) },
            confirmButton = {
                TextButton(onClick = onNavigateBack) {
                    Text("OK")
                }
            }
        )
    }
}