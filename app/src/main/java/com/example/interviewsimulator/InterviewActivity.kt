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
        "¿Qué te motivó a estudiar ingeniería de sistemas?",
        "¿Cuál es tu lenguaje de programación favorito y por qué?",
        "Describe un proyecto técnico del que estés orgulloso",
        "¿Cómo manejas los plazos ajustados en proyectos de desarrollo?",
        "¿Qué metodologías de desarrollo de software conoces?",
        "Explica cómo resolverías un problema complejo de programación",
        "¿Cómo te mantienes actualizado con las nuevas tecnologías?",
        "Describe tu experiencia trabajando en equipo en proyectos técnicos",
        "¿Qué área de la ingeniería de sistemas te apasiona más?",
        "¿Tienes experiencia con desarrollo de aplicaciones móviles?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview)

        responseType = intent.getStringExtra("responseType")
        viewFinder = findViewById(R.id.viewFinder)
        responseEditText = findViewById(R.id.responseEditText)
        progressBar = findViewById(R.id.progressBar)
        continueButton = findViewById(R.id.continueButton)

        if (responseType == "camera") {
            viewFinder.visibility = View.VISIBLE
            responseEditText.visibility = View.GONE
            checkCameraPermission()
            continueButton.text = "Iniciar Grabación"
        } else {
            viewFinder.visibility = View.GONE
            responseEditText.visibility = View.VISIBLE
        }

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

    private fun updateQuestion() {
        val questionTextView: TextView = findViewById(R.id.questionTextView)
        questionTextView.text = questions[currentQuestionIndex]
        progressBar.max = questions.size - 1
        progressBar.progress = currentQuestionIndex
    }

    private fun checkCameraPermission() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
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
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            } catch(exc: Exception) {
                Toast.makeText(this, "Error al iniciar la cámara", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCamera()
            } else {
                Toast.makeText(this, "Se requieren permisos de cámara y audio", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
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
                    is VideoRecordEvent.Start -> Toast.makeText(this, "Grabación iniciada", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}