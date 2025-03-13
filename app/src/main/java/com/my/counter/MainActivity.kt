package com.my.counter

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.my.counter.ui.theme.CounterTheme

class MainActivity : ComponentActivity(), SensorEventListener {
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var sensor: Sensor? = null
    private var counter by mutableIntStateOf(0)
    private var isCounting by mutableStateOf(false)
    private var initialSteps: Int? = null
    private var totalSteps: Int = 0

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        counter = sharedPref.getInt("counter", 0)
        totalSteps = sharedPref.getInt("totalSteps", 0)
        isCounting = sharedPref.getBoolean("isCounting", false)

        if (sensor == null) {
            Toast.makeText(this, "Step counter sensor not available", Toast.LENGTH_LONG).show()
        }

        if (isCounting) {
            startCounter()
        }

        setContent {
            CounterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val permission = rememberPermissionState(
                            permission = android.Manifest.permission.ACTIVITY_RECOGNITION
                        )

                        Text(
                            text = "Steps",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = counter.toString(),
                            fontSize = 96.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 32.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (permission.status.isGranted) {
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
                                    } else {
                                        permission.launchPermissionRequest()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCounting) Color.Red else MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.width(120.dp)
                            ) {
                                Text(
                                    text = if (isCounting) "Stop" else "Start",
                                    fontSize = 16.sp
                                )
                            }

                            Button(
                                onClick = {
                                    counter = 0
                                    totalSteps = 0
                                    initialSteps = null
                                    saveCounterState()
                                },
                                modifier = Modifier.width(120.dp)
                            ) {
                                Text(
                                    text = "Reset",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
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
                }
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
                isCounting = false
            }
        } ?: run {
            Toast.makeText(this, "Step counter not available", Toast.LENGTH_SHORT).show()
            isCounting = false
        }
    }

    private fun stopCounter() {
        sensorManager.unregisterListener(this)
        initialSteps = null
        saveCounterState()
    }

    private fun saveCounterState() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("counter", counter)
            putInt("totalSteps", totalSteps)
            putBoolean("wasReset", counter == 0)
            putBoolean("isCounting", isCounting)
            apply()
        }
    }

    override fun onPause() {
        super.onPause()
        saveCounterState()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCounter()
        saveCounterState()
    }

    override fun onSensorChanged(sevent: SensorEvent?) {
        sevent?.let { event ->
            if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val currentSteps = event.values[0].toInt()

                if (initialSteps == null) {
                    initialSteps = currentSteps - totalSteps
                }

                counter = currentSteps - (initialSteps ?: 0)
                totalSteps = counter
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}