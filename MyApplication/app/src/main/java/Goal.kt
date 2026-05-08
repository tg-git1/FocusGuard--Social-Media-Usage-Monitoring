package com.example.myapplication

data class Goal(
    val name: String,
    val requiredMinutes: Int,
    val selfMessage: String, // The motivation message
    var progressMinutes: Int = 0,
    var isCompleted: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis()
)