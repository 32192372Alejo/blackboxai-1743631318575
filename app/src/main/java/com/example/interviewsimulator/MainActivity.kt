package com.example.interviewsimulator

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startInterviewButton: Button = findViewById(R.id.startInterviewButton)
        val textResponseButton: Button = findViewById(R.id.textResponseButton)

        startInterviewButton.setOnClickListener {
            val intent = Intent(this, InterviewActivity::class.java)
            intent.putExtra("responseType", "camera")
            startActivity(intent)
        }

        textResponseButton.setOnClickListener {
            val intent = Intent(this, InterviewActivity::class.java)
            intent.putExtra("responseType", "text")
            startActivity(intent)
        }
    }
}