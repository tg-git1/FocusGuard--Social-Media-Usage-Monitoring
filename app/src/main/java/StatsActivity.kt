package com.example.myapplication

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.util.Calendar

class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        loadGraphData()
    }

    private fun loadGraphData() {
        val prefs = getSharedPreferences("FocusGuardData", Context.MODE_PRIVATE)
        val prodAppsJson = prefs.getString("PRODUCTIVE_APPS", "[]")
        val gson = Gson()
        val productivePackages: Set<String> = gson.fromJson(prodAppsJson, Array<String>::class.java)?.toSet() ?: emptySet()

        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()

        val distractionData = FloatArray(24)
        val productiveData = FloatArray(24)
        val now = System.currentTimeMillis()

        // Loop through 24 hours of the day
        for (i in 0..23) {
            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, i)
            calendar.set(Calendar.MINUTE, 0)
            val start = calendar.timeInMillis
            val end = start + 3600000

            if (start > now) break // Don't graph future hours

            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

            if (stats != null) {
                for (usage in stats) {
                    val pkg = usage.packageName
                    if (pkg.contains("android") || pkg.contains("launcher")) continue

                    // Approximate usage for this "bucket"
                    if (usage.lastTimeUsed in start..end) {
                        val minutes = (usage.totalTimeInForeground / 1000 / 60).toFloat()
                        if (productivePackages.contains(pkg)) {
                            productiveData[i] += minutes
                        } else {
                            distractionData[i] += minutes
                        }
                    }
                }
            }
        }

        // Update Graphs
        findViewById<SimpleLineGraph>(R.id.graphDistraction).dataPoints = distractionData.toList()
        findViewById<SimpleLineGraph>(R.id.graphDistraction).invalidate()

        findViewById<SimpleLineGraph>(R.id.graphProductive).dataPoints = productiveData.toList()
        findViewById<SimpleLineGraph>(R.id.graphProductive).invalidate()
    }
}