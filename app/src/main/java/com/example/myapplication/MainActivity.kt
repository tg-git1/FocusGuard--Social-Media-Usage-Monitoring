package com.example.myapplication

import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private var isDeepWorkActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Force Permissions on Start
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Usage Access REQUIRED for App Detection", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay Permission REQUIRED for Timer", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 123)
        }

        // 2. Start Service Immediately
        val serviceIntent = Intent(this, FocusService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    // --- ROBUST PERMISSION CHECKER ---
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnSetGoal).setOnClickListener { startActivity(Intent(this, GoalActivity::class.java)) }
        findViewById<Button>(R.id.btnManageApps).setOnClickListener { startActivity(Intent(this, AppSelectionActivity::class.java)) }
        findViewById<Button>(R.id.btnViewStats).setOnClickListener { startActivity(Intent(this, StatsActivity::class.java)) }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            getSharedPreferences("FocusGuardData", Context.MODE_PRIVATE).edit().clear().apply()
            recreate()
        }

        val btnDeepWork = findViewById<Button>(R.id.btnDeepWork)
        btnDeepWork.setOnClickListener {
            if (isDeepWorkActive) {
                isDeepWorkActive = false
                btnDeepWork.text = "Start Deep Work Mode 🍅"
                btnDeepWork.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_light)
                val intent = Intent(this, FocusService::class.java)
                intent.action = "STOP_DEEP_WORK"
                ContextCompat.startForegroundService(this, intent)
            } else {
                showDurationDialog(btnDeepWork)
            }
        }
    }

    private fun showDurationDialog(btn: Button) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Minutes (e.g. 25)"
        AlertDialog.Builder(this)
            .setTitle("Start Focus Session")
            .setView(input)
            .setPositiveButton("Start") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    val minutes = text.toLong()
                    isDeepWorkActive = true
                    btn.text = "STOP Deep Work ($minutes m) 🛑"
                    btn.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
                    val intent = Intent(this, FocusService::class.java)
                    intent.action = "START_DEEP_WORK"
                    intent.putExtra("DURATION_MINUTES", minutes)
                    ContextCompat.startForegroundService(this, intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDashboard() {
        val prefs = getSharedPreferences("FocusGuardData", Context.MODE_PRIVATE)
        val points = prefs.getInt("USER_POINTS", 0)
        findViewById<TextView>(R.id.tvTotalPoints).text = "$points Points"

        val bronze = findViewById<View>(R.id.imgBronze)
        val silver = findViewById<View>(R.id.imgSilver)
        val gold = findViewById<View>(R.id.imgGold)

        bronze.alpha = 0.3f; silver.alpha = 0.3f; gold.alpha = 0.3f
        if (points >= 0) bronze.alpha = 1.0f
        if (points >= 500) silver.alpha = 1.0f
        if (points >= 1000) gold.alpha = 1.0f

        val gson = Gson()
        val json = prefs.getString("GOAL_LIST", "[]")
        val type = object : TypeToken<MutableList<Goal>>() {}.type
        val goals: MutableList<Goal> = gson.fromJson(json, type) ?: mutableListOf()
        val container = findViewById<LinearLayout>(R.id.llGoalContainer)
        container.removeAllViews()

        for (goal in goals) {
            val tv = TextView(this)
            tv.textSize = 16f
            tv.setPadding(0, 8, 0, 8)
            if (goal.isCompleted) {
                tv.text = "✅ ${goal.name}"
                tv.setTextColor(Color.parseColor("#93B88A"))
            } else {
                // Should now update dynamically!
                tv.text = "⏳ ${goal.name} [${goal.progressMinutes}/${goal.requiredMinutes}m]"
                tv.setTextColor(Color.parseColor("#A89685"))
            }
            container.addView(tv)
        }
    }
}