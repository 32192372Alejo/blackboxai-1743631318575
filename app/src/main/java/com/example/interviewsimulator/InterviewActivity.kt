package com.example.interviewsimulator

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InterviewActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var responseEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var continueButton: Button
    private var currentQuestionIndex = 0
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var responseType: String? = null
    
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

        responseType = intent.getStringExtra("responseType")
        viewFinder = findViewById(R.id.viewFinder)
        responseEditText = findViewById(R.id.responseEditText)
        progressBar = findViewById(R.id.progressBar)
        continueButton = findViewById(R.id.continueButton)

        // Set visibility based on response type
        if (responseType == "camera") {
            viewFinder.visibility = View.VISIBLE
            responseEditText.visibility = View.GONE
            showRecordingConfirmationDialog()
            continueButton.text = "Iniciar Grabación"
        } else {
            viewFinder.visibility = View.GONE
            responseEditText.visibility = View.VISIBLE
        }

        // Initialize first question and progress
        updateQuestion()

        continueButton.setOnClickListener {
            if (responseType == "camera") {
                if (recording == null) {
                    startRecording()
                } else {
                    stopRecording()
                    if (currentQuestionIndex < questions.size - 1) {
                        currentQuestionIndex++
                        updateQuestion()
                        continueButton.text = "Iniciar Grabación"
                    } else {
                        finish()
                    }
                }
            } else {
                if (responseEditText.text.toString().trim().isEmpty()) {
                    Toast.makeText(this, "Por favor, escribe tu respuesta", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    updateQuestion()
                    responseEditText.text.clear()
                } else {
                    Toast.makeText(this, "¡Entrevista completada!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun showRecordingConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirmación")
            .setMessage("¿Estás seguro de que deseas comenzar la entrevista con grabación de video?")
            .setPositiveButton("Sí") { _, _ ->
                checkCameraPermission()
            }
            .setNegativeButton("No") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun startRecording() {
        val videoCapture = this.videoCapture ?: return
        continueButton.text = "Detener Grabación"

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply { withAudioEnabled() }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Toast.makeText(this, "Grabación iniciada", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Toast.makeText(this, "Grabación guardada", Toast.LENGTH_SHORT).show()
                        } else {
                            recording?.close()
                            recording = null
                            Toast.makeText(this, "Error en la grabación", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    private fun updateQuestion() {
        val questionTextView: TextView = findViewById(R.id.questionTextView)
        questionTextView.text = questions[currentQuestionIndex]
        
        progressBar.max = questions.size - 1
        progressBar.progress = currentQuestionIndex
    }

    private fun checkCameraPermission() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch(exc: Exception) {
                Toast.makeText(this, "Error al iniciar la cámara: ${exc.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCamera()
            } else {
                Toast.makeText(this, "Permisos requeridos no concedidos", Toast.LENGTH_SHORT).show()
                if (responseType == "camera") {
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}