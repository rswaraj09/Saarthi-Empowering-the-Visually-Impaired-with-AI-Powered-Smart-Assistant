package com.example.saarthi

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnBlindNav: Button
    private lateinit var btnCurrency: Button
    private lateinit var btnReading: Button

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isTtsSpeaking = false

    private val TAG = "MainActivity"

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnBlindNav = findViewById(R.id.btnBlindNav)
        btnCurrency = findViewById(R.id.btnCurrency)
        btnReading = findViewById(R.id.btnReading)

        // Set up button click listeners to launch respective activities
        btnBlindNav.setOnClickListener {
            Log.d(TAG, "Blind Navigation button clicked")
            startActivity(Intent(this, BlindNavigationActivity::class.java))
        }

        btnCurrency.setOnClickListener {
            Log.d(TAG, "Currency Detection button clicked")
            startActivity(Intent(this, CurrencyDetectionActivity::class.java))
        }

        btnReading.setOnClickListener {
            Log.d(TAG, "Reading Mode button clicked")
            startActivity(Intent(this, ReadingModeActivity::class.java))
        }

        tts = TextToSpeech(this, this)
        initializeSpeechRecognizer()

        requestAllPermissions()
    }

    // Initialize SpeechRecognizer
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            Log.d(TAG, "SpeechRecognizer initialized successfully")
        } else {
            Log.e(TAG, "Speech recognition is not available on this device")
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show()
        }
    }

    // TTS initialization
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(0.95f)
            Log.d(TAG, "TTS initialized successfully")
            speakWelcome()
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun speakWelcome() {
        val welcomeText = "Welcome to Saarthi. For navigation say Blind Navigation. To read something say Reading Mode. To detect the currency say Currency Detection."
        isTtsSpeaking = true
        tts?.speak(welcomeText, TextToSpeech.QUEUE_FLUSH, null, "WELCOME")
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS completed: $utteranceId")
                isTtsSpeaking = false
                runOnUiThread {
                    checkPermissionsAndStartListening()
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS error: $utteranceId")
                isTtsSpeaking = false
                runOnUiThread {
                    checkPermissionsAndStartListening()
                }
            }
        })
    }

    // Permission request logic
    private fun requestAllPermissions() {
        val notGranted = PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (notGranted.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${notGranted.joinToString()}")
            ActivityCompat.requestPermissions(this, notGranted, 1001)
        } else {
            Log.d(TAG, "All permissions already granted")
            checkPermissionsAndStartListening()
        }
    }

    private fun checkPermissionsAndStartListening() {
        val allGranted = PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            Log.d(TAG, "All permissions granted, starting listening")
            Toast.makeText(this, "All permissions granted. Listening...", Toast.LENGTH_SHORT).show()
            startListening()
        } else {
            Log.e(TAG, "Some permissions are still not granted")
            Toast.makeText(this, "Please grant all permissions to use the app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            Log.d(TAG, "Permission request completed")
            checkPermissionsAndStartListening()
        }
    }

    // Always-on voice recognition
    private fun startListening() {
        // Ensure microphone permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Microphone permission not granted")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
            return
        }

        if (isListening || isTtsSpeaking) {
            Log.d(TAG, "Already listening or TTS is speaking")
            return
        }

        isListening = true
        Log.d(TAG, "Starting speech recognition")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        try {
            // Recreate speech recognizer to ensure fresh state
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Speech recognizer started")
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "SpeechRecognizer not found", e)
            isListening = false
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            isListening = false
            Toast.makeText(this, "Error starting speech recognition: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech detected - user is speaking")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.d(TAG, "RMS changed: $rmsdB")
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d(TAG, "Buffer received")
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech detected")
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found - please try again"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected - please try again"
                    else -> "Unknown error: $error"
                }
                Log.e(TAG, "Speech recognition error: $errorMessage")
                Toast.makeText(this@MainActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                // Always restart listening on error
                restartListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].trim().lowercase(Locale.getDefault())
                    Log.d(TAG, "Recognized text: '$spokenText'")
                    Log.d(TAG, "All matches: $matches")

                    // Show what was recognized
                    Toast.makeText(this@MainActivity, "You said: $spokenText", Toast.LENGTH_LONG).show()

                    // More flexible matching - check for key words in any order
                    val matched = when {
                        // Blind Navigation - check for "blind" OR "navigation"
                        (spokenText.contains("blind") || spokenText.contains("navigation")) -> {
                            Log.d(TAG, "Blind Navigation command recognized")
                            btnBlindNav.performClick()
                            true
                        }
                        // Currency Detection - check for "currency" OR "detection"
                        (spokenText.contains("currency") || spokenText.contains("detection")) -> {
                            Log.d(TAG, "Currency Detection command recognized")
                            btnCurrency.performClick()
                            true
                        }
                        // Reading Mode - check for "reading" OR "read"
                        (spokenText.contains("reading") || spokenText.contains("read") || spokenText.contains("mode")) -> {
                            Log.d(TAG, "Reading Mode command recognized")
                            btnReading.performClick()
                            true
                        }
                        else -> false
                    }

                    if (!matched) {
                        Log.d(TAG, "Command not recognized: $spokenText")
                        speakRetryMessage()
                    }
                } else {
                    Log.d(TAG, "No speech results")
                    speakRetryMessage()
                }
                restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.d(TAG, "Partial results received")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "Event: $eventType")
            }
        }
    }

    private fun speakRetryMessage() {
        isTtsSpeaking = true
        tts?.speak("Please say Blind Navigation, Currency Detection, or Reading Mode.", TextToSpeech.QUEUE_FLUSH, null, "RETRY")
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                isTtsSpeaking = false
                runOnUiThread { restartListening() }
            }
            override fun onError(utteranceId: String?) {
                isTtsSpeaking = false
                runOnUiThread { restartListening() }
            }
        })
    }

    private fun restartListening() {
        isListening = false
        // Add a small delay to avoid rapid restart
        Thread {
            Thread.sleep(500)
            runOnUiThread { startListening() }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        isListening = false
    }
}