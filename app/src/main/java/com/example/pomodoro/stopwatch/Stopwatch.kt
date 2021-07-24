package com.example.pomodoro.stopwatch

data class Stopwatch(
    val id: Int,
    var timeLeft: Long,
    var isStarted: Boolean,
    var timeSpend: Long,
    var newTimer: Boolean,
    var startTime: Long,
    var isFinish: Boolean
)