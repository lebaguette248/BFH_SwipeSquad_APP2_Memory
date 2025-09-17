package com.swipesquad.memory

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
internal class QRCodeAnalyzer (
    private val onSuccess: ((Barcode) -> Unit) = {},
    private val onFailure: ((Exception) -> Unit) = {},
    private val onPassCompleted: ((Boolean) -> Unit) = {}
) : ImageAnalysis.Analyzer {
    private val barcodeScanner by lazy {
        val optionsBuilder = BarcodeScannerOptions.Builder().setBarcodeFormats(BARCODE_FORMAT)
        try {
            BarcodeScanning.getClient(optionsBuilder.build())
        } catch (e: Exception) { // for some reason MlKitContext has not been initialized
            onFailure(e)
            null
        }
    }
    @Volatile
    private var failureOccurred = false
    private var failureTimestamp = 0L
    private var firstCall = true

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (imageProxy.image == null) return

        // throttle analysis if error occurred in previous pass
        if (failureOccurred && System.currentTimeMillis() - failureTimestamp < 1000L) {
            imageProxy.close()
            return
        }

        failureOccurred = false
        barcodeScanner?.let { scanner ->
            val inputImage = imageProxy.toInputImage()
            scanner.process(inputImage)
                .addOnSuccessListener { codes ->
                    codes.firstNotNullOfOrNull { it }?.let {
                        if (firstCall) {
                            firstCall = false
                            onSuccess(it)
                        }
                    }
                }
                .addOnFailureListener {
                    failureOccurred = true
                    failureTimestamp = System.currentTimeMillis()
                    onFailure(it)
                }
                .addOnCompleteListener {
                    onPassCompleted(failureOccurred)
                    imageProxy.close()
                }
        }
    }

    @ExperimentalGetImage
    @Suppress("UnsafeCallOnNullableType")
    private fun ImageProxy.toInputImage() =
        InputImage.fromMediaImage(image!!, imageInfo.rotationDegrees)
    companion object {
        const val BARCODE_FORMAT = Barcode.FORMAT_QR_CODE
    }
}