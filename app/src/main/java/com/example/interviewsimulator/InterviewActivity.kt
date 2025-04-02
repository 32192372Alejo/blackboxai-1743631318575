package com.example.interviewsimulator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class InterviewActivity : AppCompatActivity() {
    private lateinit var camera: Camera
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview)

        val responseType = intent.getStringExtra("responseType")

        val questionTextView: TextView = findViewById(R.id.questionTextView)
        val continueButton: Button = findViewById(R.id.continueButton)

        // Set the question based on your logic
        questionTextView.text = "¿Por qué quieres trabajar aquí?"

        continueButton.setOnClickListener {
            if (responseType == "camera") {
                checkCameraPermission()
            } else {
                // Handle text response
                Toast.makeText(this, "Respondiendo con texto...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        // Implement camera start logic here
        Toast.makeText(this, "Iniciando grabación...", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}