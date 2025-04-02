package com.example.interviewsimulator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InterviewActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var responseEditText: EditText
    private lateinit var progressBar: ProgressBar
    private var currentQuestionIndex = 0
    
    private val questions = listOf(
        "¿Por qué quieres trabajar aquí?",
        "¿Cuál es tu experiencia previa en este campo?",
        "¿Cuáles son tus fortalezas y debilidades?",
        "¿Dónde te ves en 5 años?",
        "¿Por qué deberíamos contratarte?",
        "¿Cómo manejas el trabajo bajo presión?",
        "¿Cuál ha sido tu mayor logro profesional?",
        "¿Qué sabes sobre nuestra empresa?",
        "¿Qué te motiva en tu trabajo?",
        "¿Tienes alguna pregunta para nosotros?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview)

        val responseType = intent.getStringExtra("responseType")
        viewFinder = findViewById(R.id.viewFinder)
        responseEditText = findViewById(R.id.responseEditText)
        progressBar = findViewById(R.id.progressBar)

        val questionTextView: TextView = findViewById(R.id.questionTextView)
        val continueButton: Button = findViewById(R.id.continueButton)

        // Set visibility based on response type
        if (responseType == "camera") {
            viewFinder.visibility = View.VISIBLE
            responseEditText.visibility = View.GONE
            checkCameraPermission()
        } else {
            viewFinder.visibility = View.GONE
            responseEditText.visibility = View.VISIBLE
        }

        // Initialize first question and progress
        updateQuestion()

        continueButton.setOnClickListener {
            if (responseType != "camera" && responseEditText.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Por favor, escribe tu respuesta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                updateQuestion()
                responseEditText.text.clear()
            } else {
                // Interview finished
                Toast.makeText(this, "¡Entrevista completada!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun updateQuestion() {
        val questionTextView: TextView = findViewById(R.id.questionTextView)
        questionTextView.text = questions[currentQuestionIndex]
        
        // Update progress bar
        progressBar.max = questions.size - 1
        progressBar.progress = currentQuestionIndex
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)
            } catch(exc: Exception) {
                Toast.makeText(this, "Error al iniciar la cámara: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}