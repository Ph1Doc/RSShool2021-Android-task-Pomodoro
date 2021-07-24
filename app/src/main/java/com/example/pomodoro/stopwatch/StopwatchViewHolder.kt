package com.example.pomodoro.stopwatch

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.CountDownTimer
import android.util.Log
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.example.pomodoro.R
import com.example.pomodoro.databinding.StopwatchItemBinding

class StopwatchViewHolder(
    private val binding: StopwatchItemBinding,
    private val listener: StopwatchListener,
    private val resources: Resources
) : RecyclerView.ViewHolder(binding.root) {

    private var timer: CountDownTimer? = null
    private var timerNotAdd = true

    fun bind(stopwatch: Stopwatch) {
        binding.stopwatchTimer.text = stopwatch.timeLeft.displayTime()

        if (stopwatch.isFinish) {
            binding.root.setBackgroundResource(R.color.brown)
        }
        if (stopwatch.isStarted) {

            val intervalPeriod = System.currentTimeMillis() - stopwatch.startTime
            stopwatch.timeLeft = stopwatch.timeLeft - intervalPeriod
            stopwatch.timeSpend = stopwatch.timeSpend + intervalPeriod
            startTimer(stopwatch)
        } else {
            stopTimer(stopwatch)
        }

        initButtonsListeners(stopwatch)
    }

    private fun initButtonsListeners(stopwatch: Stopwatch) {
        binding.startPauseButton.setOnClickListener {
            if (stopwatch.isStarted) {

                val intervalPeriod = System.currentTimeMillis() - stopwatch.startTime
                stopwatch.timeLeft = stopwatch.timeLeft - intervalPeriod
                stopwatch.timeSpend = stopwatch.timeSpend + intervalPeriod

                listener.stop(stopwatch.id, stopwatch.timeLeft)
            } else {

                listener.start(stopwatch.id, stopwatch.timeLeft)
            }
        }

        if (stopwatch.newTimer) {
            binding.customView.setPeriod(stopwatch.timeLeft)
            binding.customView.setCurrent(0L)

            binding.root.setBackgroundResource(R.color.cardview_light_background)
            timerNotAdd = false
            stopwatch.isStarted = false
        }

        binding.deleteButton.setOnClickListener { listener.delete(stopwatch.id) }
    }

    private fun startTimer(stopwatch: Stopwatch) {
        stopwatch.isFinish = false

        binding.startPauseButton.text = "STOP"
        binding.root.setBackgroundResource(R.color.cardview_light_background)
        timer?.cancel()

        stopwatch.startTime = System.currentTimeMillis()
        timer = getCountDownTimer(stopwatch)
        timer?.start()

        binding.blinkingIndicator.isInvisible = false
        (binding.blinkingIndicator.background as? AnimationDrawable)?.start()
    }

    private fun stopTimer(stopwatch: Stopwatch) {
        binding.startPauseButton.text = "START"
        timer?.cancel()

        binding.blinkingIndicator.isInvisible = true
        (binding.blinkingIndicator.background as? AnimationDrawable)?.stop()

        if (stopwatch.isFinish) {
            stopwatch.timeLeft = stopwatch.timeSpend + stopwatch.timeLeft
            binding.stopwatchTimer.text = stopwatch.timeLeft.displayTime()
            stopwatch.timeSpend = 0
        }
    }

    private fun getCountDownTimer(stopwatch: Stopwatch): CountDownTimer {
        return object : CountDownTimer(PERIOD, UNIT_TEN_MS) {

            override fun onTick(millisUntilFinished: Long) {
                if (stopwatch.timeLeft > 0) {

                    val intervalPeriod = System.currentTimeMillis() - stopwatch.startTime

                        stopwatch.timeLeft = stopwatch.timeLeft - intervalPeriod
                        stopwatch.timeSpend = stopwatch.timeSpend + intervalPeriod

                        binding.stopwatchTimer.text = stopwatch.timeLeft.displayTime()
                        binding.customView.setCurrent(stopwatch.timeSpend)
                    stopwatch.startTime = System.currentTimeMillis()

                } else {
                    stopTimer(stopwatch)
                    onFinish()
                }

            }

            override fun onFinish() {
                binding.stopwatchTimer.text = stopwatch.timeSpend.displayTime()
                stopwatch.timeLeft = stopwatch.timeSpend + stopwatch.timeLeft
                stopwatch.timeSpend = 0

                binding.root.setBackgroundResource(R.color.brown)
                stopwatch.isFinish = true
                stopwatch.isStarted = false
            }
        }
    }

    private fun Long.displayTime(): String {
        if (this <= 0L) {
            return START_TIME
        }
        val h = this / 1000 / 3600
        val m = this / 1000 % 3600 / 60
        val s = this / 1000 % 60

        return "${displaySlot(h)}:${displaySlot(m)}:${displaySlot(s)}"
    }

    private fun displaySlot(count: Long): String {
        return if (count / 10L > 0) {
            "$count"
        } else {
            "0$count"
        }
    }

    private companion object {

        private const val START_TIME = "00:00:00"
        private const val UNIT_TEN_MS = 10L
        private const val PERIOD  = 1000L * 60L * 60L * 24L // Day
    }
}