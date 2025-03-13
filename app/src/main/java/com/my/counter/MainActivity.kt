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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.my.counter.ui.theme.CounterTheme


class MainActivity : ComponentActivity(), SensorEventListener {
    private val sensorManager: SensorManager by lazy{
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var sensor : Sensor? = null
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

        if (sensor == null) {
            Toast.makeText(this, "Step counter sensor not available", Toast.LENGTH_LONG).show()
        }
        setContent {
            CounterTheme {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val permission = rememberPermissionState(permission = android.Manifest.permission.ACTIVITY_RECOGNITION)
                    Spacer(modifier = Modifier
                        .padding(top= 300.dp))
                    Text(
                        text = counter.toString(),
                        modifier = Modifier,
                        fontSize = 100.sp
                    )

                    Button(onClick = {
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
                            }
                        } else {
                            permission.launchPermissionRequest()
                        }
                    },
                        modifier = Modifier.width(109.dp)
                    ) {
                        Text(text = if (isCounting) "Stop" else "Start")
                    }

                    Button(onClick = {
                        counter = 0
                        totalSteps = 0
                        saveCounterState()
                    },
                        modifier = Modifier.width(109.dp)
                    ) {
                        Text(
                            text = "Reset"
                        )
                    }
                }
            }
        }
    }

    private fun startCounter() {
        sensor?.let { stepSensor ->
            try {
                initialSteps = null
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}