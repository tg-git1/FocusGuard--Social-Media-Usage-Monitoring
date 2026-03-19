package com.example.myapplication

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class DelayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delay)

        val btnProceed = findViewById<Button>(R.id.btnProceed)
        val tvTimer = findViewById<TextView>(R.id.tvTimer)

        // 1. Disable the button initially
        btnProceed.isEnabled = false
        btnProceed.alpha = 0.5f

        // 2. Start the 10-second timer
        object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                tvTimer.text = "0"
                btnProceed.isEnabled = true
                btnProceed.alpha = 1.0f
                btnProceed.text = "I have checked my intention."
            }
        }.start()

        // 3. Handle Button Click
        btnProceed.setOnClickListener {
            finish() // Close the blocker
        }

        // 4. NEW WAY TO BLOCK BACK BUTTON 🛑
        // This replaces the red 'onBackPressed' error
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing (This keeps the user stuck on this screen)
            }
        })
    }
}