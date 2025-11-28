package com.example.saarthi
import android.content.Intent
import android.widget.Button
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val btnBlindNav = findViewById<Button>(R.id.btnBlindNav)
        val btnCurrency = findViewById<Button>(R.id.btnCurrency)
        val btnReading = findViewById<Button>(R.id.btnReading)


        btnBlindNav.setOnClickListener {
// Launch blind navigation activity (implement VIO/SLAM later)
            startActivity(Intent(this, BlindNavigationActivity::class.java))
        }


        btnCurrency.setOnClickListener {
// Launch currency detection activity (implement OCR/Model later)
            startActivity(Intent(this, CurrencyDetectionActivity::class.java))
        }


        btnReading.setOnClickListener {
// Launch reading mode activity (implement TTS/ASR later)
            startActivity(Intent(this, ReadingModeActivity::class.java))
        }
    }
}