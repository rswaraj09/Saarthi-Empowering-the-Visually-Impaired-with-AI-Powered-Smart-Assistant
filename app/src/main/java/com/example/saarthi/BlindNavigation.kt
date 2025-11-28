package com.example.saarthi


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class BlindNavigationActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_feature_placeholder)
        supportActionBar?.title = "Blind Navigation"
    }
}