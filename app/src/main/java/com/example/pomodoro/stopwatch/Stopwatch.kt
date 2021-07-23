package com.example.pomodoro.stopwatch

data class Stopwatch(
    val id: Int,
    var timeLeft: Long,
    var isStarted: Boolean,
    var timeSpend: Long,
    val newTimer: Boolean,
    val startTime: Long,
    var isFinish: Boolean
)