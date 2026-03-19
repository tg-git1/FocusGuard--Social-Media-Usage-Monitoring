package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GoalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal)

        val etName = findViewById<EditText>(R.id.etGoalName)
        val etTime = findViewById<EditText>(R.id.etTimeLimit)
        val etMsg = findViewById<EditText>(R.id.etMessage) // NEW FIELD

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val name = etName.text.toString()
            val time = etTime.text.toString().toIntOrNull() ?: 0
            val msg = etMsg.text.toString()

            if (name.isNotEmpty() && time > 0 && msg.isNotEmpty()) {
                saveGoal(Goal(name, time, msg))
            } else {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveGoal(goal: Goal) {
        val prefs = getSharedPreferences("FocusGuardData", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("GOAL_LIST", "[]")
        val type = object : TypeToken<MutableList<Goal>>() {}.type
        val list: MutableList<Goal> = gson.fromJson(json, type) ?: mutableListOf()

        list.add(goal)
        prefs.edit().putString("GOAL_LIST", gson.toJson(list)).apply()
        Toast.makeText(this, "Goal Added!", Toast.LENGTH_SHORT).show()
        finish()
    }
}