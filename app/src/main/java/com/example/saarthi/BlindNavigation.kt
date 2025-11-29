package com.example.saarthi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BlindNavigationActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val TAG = "BlindNavigationActivity"
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private var tts: TextToSpeech? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ListenableFuture<ProcessCameraProvider>? = null

    private val CAMERA_PERMISSION = Manifest.permission.CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blind_navigation)
        supportActionBar?.title = "Blind Navigation"

        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.statusText)

        tts = TextToSpeech(this, this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check camera permission
        if (hasCameraPermission()) {
            initializeCamera()
        } else {
            requestCameraPermission()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(0.95f)
            speakWelcome()
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    private fun speakWelcome() {
        val welcomeText = "Blind Navigation mode activated. Camera is now streaming. Point your device towards objects to detect them. The system will continuously analyze your surroundings."
        tts?.speak(welcomeText, TextToSpeech.QUEUE_FLUSH, null, "WELCOME_NAV")
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted")
                initializeCamera()
            } else {
                Log.e(TAG, "Camera permission denied")
                statusText.text = "Camera permission denied. Cannot start Blind Navigation."
                tts?.speak("Camera permission is required for blind navigation", TextToSpeech.QUEUE_FLUSH, null, "PERMISSION_DENIED")
            }
        }
    }

    private fun initializeCamera() {
        try {
            cameraProvider = ProcessCameraProvider.getInstance(this)
            cameraProvider?.addListener(
                {
                    try {
                        val processCameraProvider = cameraProvider?.get()
                        processCameraProvider?.unbindAll()

                        // Create a preview use case
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        // Create image capture use case
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        // Select back camera
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        // Bind use cases to lifecycle
                        processCameraProvider?.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageCapture
                        )

                        statusText.text = "Camera initialized. Ready for navigation."
                        Log.d(TAG, "Camera initialized successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error binding camera use cases", e)
                        statusText.text = "Error initializing camera"
                        tts?.speak("Error initializing camera: ${e.message}", TextToSpeech.QUEUE_FLUSH, null, "CAMERA_ERROR")
                    }
                },
                cameraExecutor
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up camera provider", e)
            statusText.text = "Error setting up camera"
            tts?.speak("Error setting up camera: ${e.message}", TextToSpeech.QUEUE_FLUSH, null, "CAMERA_SETUP_ERROR")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        cameraExecutor.shutdown()
    }

    companion object {
        const val CAMERA_PERMISSION_CODE = 101
    }
}
