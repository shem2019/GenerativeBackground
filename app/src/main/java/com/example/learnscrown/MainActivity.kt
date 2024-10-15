package com.example.cameraapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.learnscrown.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var capturedImageView: ImageView

    private val CAMERA_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        val launchCameraButton: Button = findViewById(R.id.launchCameraButton)
        val captureButton: Button = findViewById(R.id.captureButton)
        val cancelButton: Button = findViewById(R.id.cancelButton)
        previewView = PreviewView(this)
        capturedImageView = ImageView(this)

        // Add PreviewView dynamically to the FrameLayout (cameraframe)
        val frameLayout: FrameLayout = findViewById(R.id.cameraframe)
        frameLayout.addView(previewView)

        // Camera Executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request camera permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }

        // Launch Camera button logic
        launchCameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
                captureButton.visibility = Button.VISIBLE
                cancelButton.visibility = Button.GONE
                launchCameraButton.visibility = Button.GONE
            }
        }

        // Capture button logic
        captureButton.setOnClickListener {
            takePicture { bitmap ->
                capturedImageView.setImageBitmap(bitmap)
                frameLayout.removeView(previewView)  // Remove camera preview
                frameLayout.addView(capturedImageView)  // Show the captured image
                captureButton.visibility = Button.GONE
                cancelButton.visibility = Button.VISIBLE
            }
        }

        // Cancel button logic (retake photo)
        cancelButton.setOnClickListener {
            frameLayout.removeView(capturedImageView)  // Remove the captured image
            frameLayout.addView(previewView)  // Show the camera preview again
            captureButton.visibility = Button.VISIBLE
            cancelButton.visibility = Button.GONE
        }
    }

    // Start CameraX preview
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                // Handle exceptions if necessary
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Capture the image and return it as a Bitmap
    private fun takePicture(onImageCaptured: (Bitmap) -> Unit) {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                    val bitmap = imageProxy.toBitmap()
                    onImageCaptured(bitmap)
                    imageProxy.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    // Handle capture error
                }
            }
        )
    }

    // Convert ImageProxy to Bitmap
    private fun androidx.camera.core.ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
