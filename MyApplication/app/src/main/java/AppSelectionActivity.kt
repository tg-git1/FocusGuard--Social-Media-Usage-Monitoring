package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

class AppSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        val container = findViewById<LinearLayout>(R.id.llAppList)
        val btnSave = findViewById<Button>(R.id.btnSaveApps)

        // 1. Load existing selection
        val prefs = getSharedPreferences("FocusGuardData", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("PRODUCTIVE_APPS", "[]")
        val savedApps: Set<String> = gson.fromJson(json, Array<String>::class.java)?.toSet() ?: emptySet()

        // 2. Get Installed Apps (Clean List - Apps with Icons)
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = pm.queryIntentActivities(intent, 0)

        // Sort Alphabetically
        val sortedApps = resolveInfos.sortedBy { it.loadLabel(pm).toString().lowercase() }

        val checkBoxes = mutableListOf<CheckBox>()

        for (resolveInfo in sortedApps) {
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(pm).toString()

            // UPDATED: We no longer skip our own package name here.
            // This allows FocusGuard to appear in the list.

            val checkBox = CheckBox(this)
            checkBox.text = appName
            checkBox.tag = packageName
            checkBox.isChecked = savedApps.contains(packageName)

            // Visual Styling
            checkBox.setTextColor(Color.parseColor("#4A4A4A"))
            checkBox.textSize = 16f
            checkBox.setPadding(10, 24, 10, 24)

            container.addView(checkBox)
            checkBoxes.add(checkBox)
        }

        // 3. Save Logic
        btnSave.setOnClickListener {
            val selectedApps = mutableListOf<String>()
            for (box in checkBoxes) {
                if (box.isChecked) {
                    selectedApps.add(box.tag.toString())
                }
            }

            val newJson = gson.toJson(selectedApps)
            prefs.edit().putString("PRODUCTIVE_APPS", newJson).apply()

            Toast.makeText(this, "Focus List Updated!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}