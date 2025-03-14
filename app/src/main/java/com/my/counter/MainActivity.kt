package com.my.counter

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.my.counter.ui.CounterScreen
import com.my.counter.ui.theme.CounterTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity(), SensorEventListener {
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var sensor: Sensor? = null
    private var counter by mutableIntStateOf(0)
    private var isCounting by mutableStateOf(false)
    private var initialSteps: Int? = null
    private var totalSteps: Int = 0
    private var dailyGoal by mutableIntStateOf(10000)
    @RequiresApi(Build.VERSION_CODES.O)
    private var lastSavedDate: LocalDate = LocalDate.now()
    private var lastStepCount = 0
    private var consecutiveZeros = 0
    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private var userWeight by mutableFloatStateOf(70.0f)
    private var stepLength by mutableFloatStateOf(0.7f)
    private var caloriesBurned by mutableFloatStateOf(0f)

    @SuppressLint("SourceLockedOrientationActivity")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        loadSavedData()
        checkForNewDay()
        setupStepSensor()
        startMidnightResetChecker()
        calculateCalories()

        setContent {
            CounterTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    CounterScreen(
                        counter = counter,
                        isCounting = isCounting,
                        dailyGoal = dailyGoal,
                        caloriesBurned = caloriesBurned,
                        stepLength = stepLength,
                        userWeight = userWeight,
                        onStartStopClick = { handleStartStop() },
                        onResetClick = { resetCounter() },
                        onDailyGoalChanged = { newGoal -> updateDailyGoal(newGoal) },
                        onUserWeightChanged = { newWeight -> updateUserWeight(newWeight) },
                        onStepLengthChanged = { newStepLength -> updateStepLength(newStepLength) }
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleStartStop() {
        if (!isCounting && sensor != null) {
            startCounter()
        } else if (isCounting) {
            stopCounter()
        } else {
            Toast.makeText(applicationContext, "Step counter sensor not available", Toast.LENGTH_SHORT).show()
        }
        if (sensor != null) {
            isCounting = !isCounting
            saveCounterState()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun resetCounter() {
        counter = 0
        totalSteps = 0
        initialSteps = null
        caloriesBurned = 0f
        saveCounterState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateDailyGoal(newGoal: Int) {
        if (newGoal > 0) {
            dailyGoal = newGoal
            saveCounterState()
        } else {
            Toast.makeText(applicationContext, "Please enter a valid goal", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateUserWeight(newWeight: Float) {
        if (newWeight > 0) {
            userWeight = newWeight
            calculateCalories()
            saveCounterState()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateStepLength(newStepLength: Float) {
        if (newStepLength > 0) {
            stepLength = newStepLength
            calculateCalories()
            saveCounterState()
        }
    }

    private fun calculateCalories() {
        val distanceKm = counter * stepLength / 1000f
        val met = 3.5f
        val durationHours = distanceKm / 5f
        caloriesBurned = met * userWeight * durationHours
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadSavedData() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        counter = sharedPref.getInt("counter", 0)
        totalSteps = sharedPref.getInt("totalSteps", 0)
        isCounting = sharedPref.getBoolean("isCounting", false)
        dailyGoal = sharedPref.getInt("dailyGoal", 10000)
        userWeight = sharedPref.getFloat("userWeight", 70.0f)
        stepLength = sharedPref.getFloat("stepLength", 0.7f)
        caloriesBurned = sharedPref.getFloat("caloriesBurned", 0f)

        val savedDateStr = sharedPref.getString("lastSavedDate", LocalDate.now().toString())
        lastSavedDate = LocalDate.parse(savedDateStr, dateFormatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkForNewDay() {
        val today = LocalDate.now()
        if (lastSavedDate.isBefore(today)) {
            counter = 0
            caloriesBurned = 0f
            lastSavedDate = today
            initialSteps = null
            saveCounterState()
        }
    }

    private fun setupStepSensor() {
        if (sensor == null) {
            Toast.makeText(this, "Step counter sensor not available", Toast.LENGTH_LONG).show()
        } else if (isCounting) {
            startCounter()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMidnightResetChecker() {
        lifecycleScope.launch {
            while (isActive) {
                val now = LocalDate.now()
                if (lastSavedDate.isBefore(now)) {
                    counter = 0
                    caloriesBurned = 0f
                    lastSavedDate = now
                    initialSteps = null
                    saveCounterState()
                }
                delay(60000)
            }
        }
    }

    private fun startCounter() {
        sensor?.let { stepSensor ->
            try {
                val wasReset = getPreferences(Context.MODE_PRIVATE).getBoolean("wasReset", false)
                if (wasReset) {
                    initialSteps = null
                    totalSteps = 0
                    counter = 0
                    caloriesBurned = 0f
                }
                sensorManager.registerListener(
                    this,
                    stepSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
                isCounting = false
            }
        } ?: run {
            Toast.makeText(this, "Step counter not available", Toast.LENGTH_SHORT).show()
            isCounting = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun stopCounter() {
        sensorManager.unregisterListener(this)
        saveCounterState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveCounterState() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("counter", counter)
            putInt("totalSteps", totalSteps)
            putBoolean("wasReset", counter == 0)
            putBoolean("isCounting", isCounting)
            putInt("dailyGoal", dailyGoal)
            putFloat("userWeight", userWeight)
            putFloat("stepLength", stepLength)
            putFloat("caloriesBurned", caloriesBurned)
            putString("lastSavedDate", lastSavedDate.format(dateFormatter))
            apply()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadSavedData()
        checkForNewDay()
        if (isCounting) {
            startCounter()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPause() {
        super.onPause()
        saveCounterState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        stopCounter()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val currentSteps = event.values[0].toInt()

                if (initialSteps == null) {
                    initialSteps = currentSteps
                    counter = 0
                    totalSteps = 0
                }

                if (currentSteps < lastStepCount) {
                    initialSteps = currentSteps
                    counter = 0
                    totalSteps = 0
                    caloriesBurned = 0f
                } else {

                    val stepDelta = currentSteps - lastStepCount

                    if (stepDelta <= 50) {
                        counter = currentSteps - (initialSteps ?: 0)
                        totalSteps = counter
                        calculateCalories()
                    }
                }
                lastStepCount = currentSteps

                if (currentSteps == 0) {
                    consecutiveZeros++
                    if (consecutiveZeros > 5) {
                        Toast.makeText(this, "Step sensor may be malfunctioning", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    consecutiveZeros = 0
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Toast.makeText(this, "Step sensor accuracy is unreliable", Toast.LENGTH_SHORT).show()
        }
    }
}