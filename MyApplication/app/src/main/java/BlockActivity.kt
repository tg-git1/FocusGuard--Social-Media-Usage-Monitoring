package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class BlockActivity : AppCompatActivity() {

    // Timer variable to prevent memory leaks
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        val imgIcon = findViewById<ImageView>(R.id.imgBlockIcon)
        val tvTitle = findViewById<TextView>(R.id.tvBlockTitle)
        val tvMessage = findViewById<TextView>(R.id.tvBlockMessage)
        val btnHome = findViewById<Button>(R.id.btnGoHome)

        val type = intent.getStringExtra("BLOCK_TYPE")

        if (type == "COMPULSION") {
            // --- COMPULSION MODE (With Countdown) ---
            imgIcon.setColorFilter(Color.parseColor("#FF9800")) // Orange
            tvTitle.text = "⚠️ Compulsion Detected"

            // Hide the Home button initially (Force them to wait)
            btnHome.visibility = View.INVISIBLE

            // Start 5 Second Countdown
            timer = object : CountDownTimer(5000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsLeft = millisUntilFinished / 1000
                    tvMessage.text = "You closed this app and opened it too fast.\n\nTake a deep breath...\n\n$secondsLeft"
                    tvMessage.textSize = 24f
                }

                override fun onFinish() {
                    tvMessage.text = "Okay, you can go back now."
                    btnHome.visibility = View.VISIBLE // Show button after timer
                }
            }.start()

        } else {
            // --- DEEP WORK MODE (Static Block) ---
            tvTitle.text = "⛔ FocusGuard Active"
            tvMessage.text = "You are in Deep Work mode.\nThis app is blocked until the session ends."
        }

        btnHome.setOnClickListener {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
            finish()
        }

        // Disable Back Button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel() // Stop timer if app closes
    }
}