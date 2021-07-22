package com.example.pomodoro.stopwatch

data class Stopwatch(
    val id: Int,
    var currentMs: Long,
    var isStarted: Boolean,
    var current: Long,
    val newTimer: Boolean
)