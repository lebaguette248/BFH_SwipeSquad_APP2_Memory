package com.swipesquad.memory

import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

private fun defaultOnQRCodeResult(uri: Uri, value: String) {}

@Composable
fun QRCameraView(onQRCodeResult: (Uri, String) -> Unit) {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera preview
    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember {
        ImageCapture.Builder()
            .setTargetResolution(Size(640, 480)).setJpegQuality(80).build()
    }

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing).build()
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720)).build()
    }

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()

        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(context),
            QRCodeAnalyzer(onSuccess = { barcode ->
                val file = File(
                    context.filesDir,
                    "${System.currentTimeMillis()}.jpg"
                )
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                imageCapture.takePicture(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(error: ImageCaptureException) {
                            error.printStackTrace()
                        }

                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = outputFileResults.savedUri ?: Uri.fromFile(file)
                            if (barcode.rawValue != null) {
                                onQRCodeResult(savedUri, barcode.rawValue!!)
                            }
                        }
                    }
                )
            })
        )
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview,
            imageCapture, imageAnalysis
        )

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    AndroidView(factory = {
        previewView
    }, modifier = Modifier.fillMaxSize())
}