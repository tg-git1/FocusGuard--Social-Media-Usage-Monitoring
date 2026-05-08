package com.example.myapplication

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class FocusService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    // STATE VARIABLES
    private var isDeepWorkMode = false
    private var deepWorkEndTime: Long = 0
    private var lastDistractionTime: Long = 0
    private var previousPackageName = ""

    // UI VARIABLES
    private var windowManager: WindowManager? = null
    private var timerView: View? = null
    private var frictionView: View? = null
    private var secondsWasted = 0
    private var secondsProductive = 0
    private var isOverlayShowing = false

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        checkUsage()

        // Start the AI Notification Loop
        runAIPrediction()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_DEEP_WORK") {
            val minutes = intent.getLongExtra("DURATION_MINUTES", 25)
            isDeepWorkMode = true
            deepWorkEndTime = System.currentTimeMillis() + (minutes * 60 * 1000)
            updateNotification("🍅 Deep Work: $minutes min left")

        } else if (intent?.action == "STOP_DEEP_WORK") {
            isDeepWorkMode = false
            updateNotification("🛡️ FocusGuard Active (Normal Mode)")
            removeOverlays()
            isOverlayShowing = false
        }
        return START_STICKY
    }

    private fun checkUsage() {
        isRunning = true
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isRunning) return

                // 1. Check Deep Work Expiry
                if (isDeepWorkMode && System.currentTimeMillis() > deepWorkEndTime) {
                    isDeepWorkMode = false
                    updateNotification("🎉 Session Done! Back to Normal Mode.")
                }

                // 2. Identify Current App
                val currentApp = getForegroundApp()
                val isDistraction = isDistractingApp(currentApp)

                // 3. Compulsion Tracker: Record time when user LEAVES a distraction
                if (!isDistractingApp(currentApp) && isDistractingApp(previousPackageName)) {
                    lastDistractionTime = System.currentTimeMillis()
                }
                previousPackageName = currentApp

                if (isDistraction) {
                    // --- CASE 1: COMPULSION LOOP ---
                    // User returned to distraction in < 5 seconds
                    if (!isDeepWorkMode && (System.currentTimeMillis() - lastDistractionTime < 5000)) {
                        if (isOverlayShowing) { removeOverlays(); isOverlayShowing = false }

                        val blockIntent = Intent(applicationContext, BlockActivity::class.java)
                        blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        blockIntent.putExtra("BLOCK_TYPE", "COMPULSION")
                        startActivity(blockIntent)
                    }
                    // --- CASE 2: DEEP WORK (Strict Block) ---
                    else if (isDeepWorkMode) {
                        if (isOverlayShowing) { removeOverlays(); isOverlayShowing = false }
                        val blockIntent = Intent(applicationContext, BlockActivity::class.java)
                        blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        blockIntent.putExtra("BLOCK_TYPE", "DEEP_WORK")
                        startActivity(blockIntent)
                    }
                    // --- CASE 3: DOOM SCROLL (Normal Mode) ---
                    else {
                        if (!isOverlayShowing) {
                            showOverlays()
                            isOverlayShowing = true
                            secondsWasted = 0
                        }
                        secondsWasted++
                        updateTimerUI(secondsWasted)

                        val maxAlpha = 0.95f
                        val currentAlpha = (secondsWasted / 60f) * maxAlpha
                        frictionView?.alpha = if (currentAlpha > maxAlpha) maxAlpha else currentAlpha
                    }
                } else {
                    // --- SAFE APP ---
                    if (isOverlayShowing) {
                        removeOverlays()
                        isOverlayShowing = false
                    }
                    // Track Goals if active app is not Launcher
                    if (currentApp.isNotEmpty() && !isLauncher(currentApp)) {
                        secondsProductive++
                        if (secondsProductive % 60 == 0) updateGoalProgress()
                    }
                }

                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun updateGoalProgress() {
        val prefs = getSharedPreferences("FocusGuardData", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("GOAL_LIST", "[]")
        val type = object : TypeToken<MutableList<Goal>>() {}.type
        val goals: MutableList<Goal> = gson.fromJson(json, type) ?: mutableListOf()

        var goalCompletedNow = false
        var completedGoalName = ""

        for (goal in goals) {
            if (!goal.isCompleted) {
                goal.progressMinutes += 1
                if (goal.progressMinutes >= goal.requiredMinutes) {
                    goal.isCompleted = true
                    goalCompletedNow = true
                    completedGoalName = goal.name

                    val currentPoints = prefs.getInt("USER_POINTS", 0)
                    prefs.edit().putInt("USER_POINTS", currentPoints + 100).apply()
                }
                break
            }
        }
        prefs.edit().putString("GOAL_LIST", gson.toJson(goals)).apply()

        if (goalCompletedNow) {
            updateNotification("🏆 Goal Completed: $completedGoalName (+100 pts)!")
        }
    }

    // --- AI SMART NOTIFICATION (Updated for Midnight Deadline) ---
    private fun runAIPrediction() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isRunning) return

                val prefs = getSharedPreferences("FocusGuardData", Context.MODE_PRIVATE)
                val gson = Gson()
                val json = prefs.getString("GOAL_LIST", "[]")
                val type = object : TypeToken<MutableList<Goal>>() {}.type
                val goals: MutableList<Goal> = gson.fromJson(json, type) ?: mutableListOf()

                // 1. Calculate Total Work Remaining (in Minutes)
                var minutesOfWorkLeft = 0
                for (goal in goals) {
                    if (!goal.isCompleted) {
                        minutesOfWorkLeft += (goal.requiredMinutes - goal.progressMinutes)
                    }
                }

                // 2. Calculate Time Left (Target: MIDNIGHT / 24:00)
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)

                // Target is 24:00 (1440 minutes)
                val targetMinuteOfDay = 24 * 60
                val currentMinuteOfDay = (currentHour * 60) + currentMinute
                val minutesUntilBedtime = targetMinuteOfDay - currentMinuteOfDay

                // 3. Logic: Triggers even if you are working late (e.g. 10:15 PM)
                if (minutesOfWorkLeft > 0) {

                    // CASE A: CRISIS / OVERLOAD
                    // If you have more work than time left OR time is negative (past midnight)
                    if (minutesOfWorkLeft > minutesUntilBedtime) {
                        val hoursWork = minutesOfWorkLeft / 60
                        val hoursLeft = if (minutesUntilBedtime > 0) minutesUntilBedtime / 60 else 0

                        updateNotification("⚠️ CRITICAL OVERLOAD: ${hoursWork}h work remaining vs ${hoursLeft}h time left!")
                    }
                    // CASE B: TIGHT SCHEDULE (Work > 80% of remaining time)
                    else if (minutesUntilBedtime > 0 && minutesOfWorkLeft > (minutesUntilBedtime * 0.8)) {
                        updateNotification("⚠️ Tight Schedule: ${minutesOfWorkLeft}m goals remaining. Don't stop!")
                    }
                    // CASE C: EVENING REMINDER (After 7 PM)
                    else if (currentHour >= 19 && !isDeepWorkMode) {
                        updateNotification("🌙 Evening Check: You still have ${minutesOfWorkLeft}m of goals.")
                    }
                }

                handler.postDelayed(this, 60 * 1000) // Run every minute
            }
        }, 60 * 1000)
    }

    // --- OVERLAY HELPERS ---
    private fun showOverlays() {
        try {
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            // Friction
            val frictionParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            frictionView = inflater.inflate(R.layout.overlay_friction, null)
            windowManager?.addView(frictionView, frictionParams)

            // Timer
            val timerParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            timerParams.gravity = Gravity.TOP or Gravity.END
            timerParams.y = 150
            timerView = inflater.inflate(R.layout.overlay_timer, null)
            windowManager?.addView(timerView, timerParams)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun removeOverlays() {
        try {
            if (frictionView != null) { windowManager?.removeView(frictionView); frictionView = null }
            if (timerView != null) { windowManager?.removeView(timerView); timerView = null }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateTimerUI(seconds: Int) {
        val tv = timerView?.findViewById<TextView>(R.id.tvDoomTimer)
        val min = seconds / 60
        val sec = seconds % 60
        tv?.text = String.format("WASTED: %02d:%02d", min, sec)
    }

    // --- APP DETECTION ---
    private fun getForegroundApp(): String {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        // 2 minute lookback for better detection
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 120, time)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName ?: ""
    }

    private fun isLauncher(packageName: String): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        val defaultLauncher = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return packageName == defaultLauncher || packageName == "com.android.systemui"
    }

    private fun isDistractingApp(packageName: String): Boolean {
        if (packageName.isEmpty()) return false
        if (packageName == applicationContext.packageName) return false
        if (isLauncher(packageName)) return false
        if (packageName.contains("launcher") || packageName.contains("home")) return false

        val prefs = getSharedPreferences("FocusGuardData", Context.MODE_PRIVATE)
        val json = prefs.getString("PRODUCTIVE_APPS", "[]")
        val productiveApps = Gson().fromJson(json, Array<String>::class.java)?.toSet() ?: emptySet()
        if (productiveApps.contains(packageName)) return false
        return true
    }

    private fun createNotification(): Notification {
        val channelId = "FocusGuardChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Focus Guard", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("FocusGuard Active")
            .setContentText("Monitoring habits...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
    }

    private fun updateNotification(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        }
        val notification = NotificationCompat.Builder(this, "FocusGuardChannel")
            .setContentTitle("FocusGuard")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOnlyAlertOnce(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}