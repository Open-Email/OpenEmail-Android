package com.mercata.pingworks.sign_in.qr_code_scanner_screen

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.QR_SCANNER_RESULT
import com.mercata.pingworks.R
import com.mercata.pingworks.common.Logo
import com.mercata.pingworks.common.LogoSize
import java.util.concurrent.Executors

@Composable
fun QRCodeScannerScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = modifier.fillMaxSize(),
            update = { previewView ->
                cameraProvider ?: return@AndroidView
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            processImage(imageProxy, onScanResult = { scannedCode ->
                                if (vib.hasVibrator()) {
                                    vib.cancel()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                                    } else {
                                        vib.vibrate(100)
                                    }
                                }
                                navController.previousBackStackEntry?.savedStateHandle?.set(
                                    QR_SCANNER_RESULT,
                                    scannedCode
                                )
                                navController.popBackStack()
                            })
                            imageProxy.close()
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider!!.unbindAll()
                    cameraProvider!!.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QRCodeScanner", "Camera binding failed", e)
                }
            }
        )
        TranslucentOverlay(navController)
    }
}

@Composable
fun TranslucentOverlay(navController: NavController) {
    val bgColor = Color.Black.copy(alpha = 0.6f)
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(bgColor)
                    .fillMaxWidth()
                    .padding(
                        bottom = MARGIN_DEFAULT,
                        top = MARGIN_DEFAULT + padding.calculateTopPadding()
                    )
            ) {
                Logo(
                    modifier = Modifier.padding(horizontal = MARGIN_DEFAULT),
                    size = LogoSize.Small,
                    lightFont = true
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                drawRect(color = bgColor)

                val squareSize = 300.dp.toPx()
                val cornerRadius = DEFAULT_CORNER_RADIUS.toPx()
                val squareLeft = (canvasWidth - squareSize) / 2
                val squareTop = (canvasHeight - squareSize) / 2

                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        color = android.graphics.Color.TRANSPARENT
                        xfermode =
                            PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        squareLeft,
                        squareTop,
                        squareLeft + squareSize,
                        squareTop + squareSize,
                        cornerRadius,
                        cornerRadius,
                        paint
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(bgColor)
                    .fillMaxWidth()
                    .padding(
                        top = MARGIN_DEFAULT,
                        bottom = MARGIN_DEFAULT + padding.calculateBottomPadding()
                    )
            ) {
                Button(onClick = {
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.enter_private_keys_manually))
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImage(imageProxy: ImageProxy, onScanResult: (String) -> Unit) {
    val mediaImage = imageProxy.image ?: return
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    val scanner: BarcodeScanner = BarcodeScanning.getClient()
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                barcode.rawValue?.let { qrCode ->
                    onScanResult(qrCode)
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("QRCodeScanner", "Barcode scanning failed", e)
        }
}