package io.rownd.android.views.key_transfer

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@ExperimentalGetImage
internal fun setupAnalysisUseCase(
    onSuccess: (value: String) -> Unit,
    onFailure: ((err: Exception) -> Unit)? = null
): ImageAnalysis {

// configure our MLKit BarcodeScanning client
/* passing in our desired barcode formats - MLKit supports additional formats outside of the ones listed here, and you may not need to offer support for all of these. You should only specify the ones you need */
    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
        Barcode.FORMAT_QR_CODE,
    ).build()
// getClient() creates a new instance of the MLKit barcode scanner with the specified options
    val scanner = BarcodeScanning.getClient(options)

// setting up the analysis use case
    val analysisUseCase = ImageAnalysis.Builder()
        .build()

// define the actual functionality of our analysis use case
    analysisUseCase.setAnalyzer(
// newSingleThreadExecutor() will let us perform analysis on a single worker thread
        Executors.newSingleThreadExecutor()
    ) { imageProxy ->
        processImageProxy(
            scanner,
            imageProxy,
            onSuccess,
            onFailure
        )
    }

    return analysisUseCase
}

@ExperimentalGetImage
private fun processImageProxy(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (value: String) -> Unit,
    onFailure: ((err: Exception) -> Unit)? = null
) {
    imageProxy.image?.let { image ->
        val inputImage =
            InputImage.fromMediaImage(
                image,
                imageProxy.imageInfo.rotationDegrees
            )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodeList ->
                val barcode = barcodeList.getOrNull(0)
                // `rawValue` is the decoded value of the barcode
                barcode?.rawValue?.let { value ->
                    // update our textView to show the decoded value
                    onSuccess.invoke(value)
                }
            }
            .addOnFailureListener {
                // This failure will happen if the barcode scanning model
                // fails to download from Google Play Services
                Log.e("Rownd.camera.processor", it.message.orEmpty())
                onFailure?.invoke(it)
            }.addOnCompleteListener {
                // When the image is from CameraX analysis use case, must
                // call image.close() on received images when finished
                // using them. Otherwise, new images may not be received
                // or the camera may stall.
                imageProxy.image?.close()
                imageProxy.close()
            }
    }
}

class CodeScanProcessor