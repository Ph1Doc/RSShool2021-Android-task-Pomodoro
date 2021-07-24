package com.example.pomodoro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pomodoro.databinding.ActivityMainBinding
import com.example.pomodoro.stopwatch.Stopwatch
import com.example.pomodoro.stopwatch.StopwatchAdapter
import com.example.pomodoro.stopwatch.StopwatchListener
import foregroundservice.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), StopwatchListener, LifecycleObserver {

    private lateinit var binding: ActivityMainBinding

    private val stopwatchAdapter = StopwatchAdapter(this)
    private val stopwatches = mutableListOf<Stopwatch>()
    private var nextId = 0
    private var startTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stopwatchAdapter
        }

        startTime = System.currentTimeMillis()

        lifecycleScope.launch(Dispatchers.Main) {
            while (true) {
                binding.timerView.text = (System.currentTimeMillis() - startTime).displayTime()
                delay(INTERVAL)
            }
        }

        binding.addNewStopwatchButton.setOnClickListener {
            if (binding.minutes.text.toString().toIntOrNull() != null ) {
                stopwatches.add(Stopwatch(nextId++, binding.minutes.text.toString().toLong() * 1000 * 60, false, timeSpend = 0L, newTimer = true, System.currentTimeMillis(), isFinish = false))
                stopwatchAdapter.submitList(stopwatches.toList())
            } else {
                Toast.makeText(this, "Enter the time", Toast.LENGTH_SHORT).show()
            }
        }

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        val startIntent = Intent(this, ForegroundService::class.java)
        startIntent.putExtra(COMMAND_ID, COMMAND_START)
        stopwatches.forEach {
            if (it.isStarted && it.timeLeft.toInt() != 0) {
                startIntent.putExtra(STARTED_TIMER_TIME_MS,  it.timeLeft )
            }
        }

        startService(startIntent)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        val stopIntent = Intent(this, ForegroundService::class.java)
        stopIntent.putExtra(COMMAND_ID, COMMAND_STOP)
        startService(stopIntent)
    }

    override fun start(id: Int, currentMs: Long) {

        changeStopwatch(id, currentMs, true)
    }

    override fun stop(id: Int, currentMs: Long) {
        changeStopwatch(id, null, false)
    }

    override fun reset(id: Int) {
        changeStopwatch(id, 0L, false)
    }

    override fun delete(id: Int) {
        stopwatches.remove(stopwatches.find { it.id == id })
        stopwatchAdapter.submitList(stopwatches.toList())
    }

    private fun changeStopwatch(id: Int, currentMs: Long?, isStarted: Boolean) {
        val newTimers = mutableListOf<Stopwatch>()
        stopwatches.forEach {
            if (it.id == id) {
                newTimers.add(Stopwatch(it.id, currentMs ?: it.timeLeft , isStarted, it.timeSpend, false, System.currentTimeMillis(), it.isFinish))

            } else {
                if (it.isStarted) {
                    newTimers.add(Stopwatch(it.id, it.timeLeft - (System.currentTimeMillis() - it.startTime), false, it.timeSpend, false, it.startTime, it.isFinish))
                } else {
                    newTimers.add(Stopwatch(it.id, it.timeLeft, false, it.timeSpend, false, it.startTime, it.isFinish))

                }

                Log.d(
                    "TAG",
                    "!!!! getCountDownTimer \n stopwatch.timeSpend " + stopwatches[0].timeSpend
                            + " \n stopwatch.timeLeft " + stopwatches[0].timeLeft
                            + "\n stopwatch.startTime " + stopwatches[0].timeSpend
                )

            }
        }
        stopwatchAdapter.submitList(newTimers)
        stopwatches.clear()
        stopwatches.addAll(newTimers)
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

        private const val INTERVAL = 10L
        private const val START_TIME = "00:00:00"
    }
}