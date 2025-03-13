package com.my.counter

import android.content.Context
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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.KeyboardType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.my.counter.ui.theme.CounterTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import kotlin.math.roundToInt

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

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        loadSavedData()
        checkForNewDay()
        setupStepSensor()
        startMidnightResetChecker()
        calculateCalories()

        setContent {
            CounterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showGoalDialog by remember { mutableStateOf(false) }
                    var showSettingsDialog by remember { mutableStateOf(false) }
                    var goalInputValue by remember { mutableStateOf(dailyGoal.toString()) }
                    var weightInputValue by remember { mutableStateOf(userWeight.toString()) }
                    var stepLengthInputValue by remember { mutableStateOf(stepLength.toString()) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            rememberPermissionState(
                                permission = android.Manifest.permission.ACTIVITY_RECOGNITION
                            )
                        } else {
                            TODO("VERSION.SDK_INT < Q")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        }

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
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .height(80.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "CALORIES",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${caloriesBurned.roundToInt()}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .height(80.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "DISTANCE",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val distanceKm = (counter * stepLength / 1000f)
                                    Text(
                                        text = "%.2f km".format(distanceKm),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val progress = (counter.toFloat() / dailyGoal).coerceIn(0f, 1f)
                        Text(
                            text = "Daily Goal: $counter / $dailyGoal",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                                .height(12.dp),
                            color = when {
                                progress >= 1f -> Color.Green
                                progress >= 0.7f -> Color(0xFF8BC34A)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                                    caloriesBurned = 0f
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

                        Button(
                            onClick = { showGoalDialog = true },
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .width(200.dp)
                        ) {
                            Text("Set Daily Goal")
                        }

                        if (showGoalDialog) {
                            AlertDialog(
                                onDismissRequest = { showGoalDialog = false },
                                title = { Text("Set Daily Goal") },
                                text = {
                                    OutlinedTextField(
                                        value = goalInputValue,
                                        onValueChange = { goalInputValue = it },
                                        label = { Text("Steps") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        )
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            goalInputValue.toIntOrNull()?.let { newGoal ->
                                                if (newGoal > 0) {
                                                    dailyGoal = newGoal
                                                    saveCounterState()
                                                    showGoalDialog = false
                                                } else {
                                                    Toast.makeText(applicationContext, "Please enter a valid goal", Toast.LENGTH_SHORT).show()
                                                }
                                            } ?: run {
                                                Toast.makeText(applicationContext, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("Save")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showGoalDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        if (showSettingsDialog) {
                            AlertDialog(
                                onDismissRequest = { showSettingsDialog = false },
                                title = { Text("Settings") },
                                text = {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text("Personal Information", fontWeight = FontWeight.Bold)

                                        OutlinedTextField(
                                            value = weightInputValue,
                                            onValueChange = { weightInputValue = it },
                                            label = { Text("Weight (kg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                        )

                                        OutlinedTextField(
                                            value = stepLengthInputValue,
                                            onValueChange = { stepLengthInputValue = it },
                                            label = { Text("Step Length (meters)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val newWeight = weightInputValue.toFloatOrNull()
                                            val newStepLength = stepLengthInputValue.toFloatOrNull()

                                            if (newWeight != null && newWeight > 0 &&
                                                newStepLength != null && newStepLength > 0) {
                                                userWeight = newWeight
                                                stepLength = newStepLength
                                                calculateCalories()
                                                saveCounterState()
                                                showSettingsDialog = false
                                            } else {
                                                Toast.makeText(applicationContext, "Please enter valid values", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("Save")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showSettingsDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()
        stopCounter()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val currentSteps = event.values[0].toInt()

                if (initialSteps == null) {
                    initialSteps = currentSteps - totalSteps
                }

                if (currentSteps < lastStepCount) {
                    initialSteps = currentSteps
                    counter = 0
                    totalSteps = 0
                    caloriesBurned = 0f
                } else if (currentSteps - lastStepCount > 50) {
                } else {
                    counter = currentSteps - (initialSteps ?: 0)
                    totalSteps = counter
                    calculateCalories()
                }

                if (currentSteps == 0) {
                    consecutiveZeros++
                    if (consecutiveZeros > 5) {
                        Toast.makeText(this, "Step sensor may be malfunctioning", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    consecutiveZeros = 0
                }

                lastStepCount = currentSteps
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Toast.makeText(this, "Step sensor accuracy is unreliable", Toast.LENGTH_SHORT).show()
        }
    }
}