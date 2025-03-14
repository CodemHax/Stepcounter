package com.my.counter.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.my.counter.ui.theme.CounterTheme
import com.my.counter.R as MyCounterR

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CounterScreen(
    counter: Int,
    isCounting: Boolean,
    dailyGoal: Int,
    caloriesBurned: Float,
    stepLength: Float,
    userWeight: Float,
    onStartStopClick: () -> Unit,
    onResetClick: () -> Unit,
    onDailyGoalChanged: (Int) -> Unit,
    onUserWeightChanged: (Float) -> Unit,
    onStepLengthChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var showGoalDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var goalInputValue by remember { mutableStateOf(dailyGoal.toString()) }
    var weightInputValue by remember { mutableStateOf(userWeight.toString()) }
    var stepLengthInputValue by remember { mutableStateOf(stepLength.toString()) }


    val isPreview = LocalInspectionMode.current
    println(isPreview)
    val permissionState = if (!isPreview) {
        rememberPermissionState(permission = Manifest.permission.ACTIVITY_RECOGNITION)
    } else {
        null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween ,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FootFlow",
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal,
                )
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = counter.toString(),
                fontSize = 70.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Steps Today",
                fontSize = 15.sp,
                color = Color.Gray,
//                fontWeight = FontWeight.Bold
            )

            val progress = (counter.toFloat() / dailyGoal).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Gray,
                    trackColor = Color(0xFF333333),
                    strokeWidth = 7.dp,
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
            }
            Text(
                text = "Daily Goal: $counter / $dailyGoal",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val minBlack = colorResource(id = MyCounterR.color.minBlack)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .height(110.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = minBlack
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            painter = painterResource(id = MyCounterR.drawable.iconsf),
                            contentDescription = "Calories",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )

                        Text(
                            text = "${caloriesBurned.toInt()}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Calories",
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .height(110.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = minBlack
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Calories",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        val distanceKm = (counter * stepLength / 1000f)
                        Text(
                            text = "%.2f".format(distanceKm),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Km",
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
            Button(
                onClick = {
                    if (isPreview || permissionState?.status?.isGranted == true) {
                        onStartStopClick()
                    } else {
                        permissionState?.launchPermissionRequest()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCounting) Color.Red else minBlack,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
                    .padding(5.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(20),
            ) {

               if(!isCounting){
                   Icon(
                       imageVector = Icons.Default.PlayArrow,
                       contentDescription = "Stop",
                       tint = Color.White,
                       modifier = Modifier.padding(3.dp)
                   )
                }else{
                     Icon(
                          imageVector = Icons.Default.Pause,
                          contentDescription = "Start",
                          tint = Color.White,
                          modifier = Modifier.padding(3.dp)
                     )
               }
                Text(
                    text = if (isCounting) "Stop" else "Start",
                    fontSize = 19.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Button(
                    onClick = { onResetClick() },
                    modifier = Modifier
                        .width(170.dp)
                        .height(60.dp),
                    shape = RoundedCornerShape(20),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = minBlack,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector =  Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White,
                        modifier = Modifier.padding(3.dp)
                    )
                    Text(
                        text = "Reset",
                        fontSize = 19.sp
                    )
                }
                Button(
                    onClick = { showGoalDialog = true },
                    modifier = Modifier
                        .width(170.dp)
                        .height(60.dp),
                    shape = RoundedCornerShape(20),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = minBlack,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector =  Icons.Default.AddCircle,
                        contentDescription = "Reset",
                        tint = Color.White,
                        modifier = Modifier.padding(3.dp)
                    )
                    Text(
                        "Set Goal",
                        fontSize = 19.sp)
                }
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
                                        onDailyGoalChanged(newGoal)
                                        showGoalDialog = false
                                    } else {
                                        Toast.makeText(context, "Please enter a valid goal", Toast.LENGTH_SHORT).show()
                                    }
                                } ?: run {
                                    Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show()
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
                                    onUserWeightChanged(newWeight)
                                    onStepLengthChanged(newStepLength)
                                    showSettingsDialog = false
                                } else {
                                    Toast.makeText(context, "Please enter valid values", Toast.LENGTH_SHORT).show()
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

//@RequiresApi(Build.VERSION_CODES.Q)
//@Preview(showBackground = true)
//@Composable
//fun CounterScreenPreview() {
//    CounterTheme {
//        PreviewCounterScreen()
//    }
//}
//
//
//@RequiresApi(Build.VERSION_CODES.Q)
//@Composable
//fun PreviewCounterScreen() {
//    CounterScreen(
//        counter = 5243,
//        isCounting = false,
//        dailyGoal = 10000,
//        caloriesBurned = 312.5f,
//        stepLength = 0.7f,
//        userWeight = 70.0f,
//        onStartStopClick = {},
//        onResetClick = {},
//        onDailyGoalChanged = {},
//        onUserWeightChanged = {},
//        onStepLengthChanged = {}
//    )
//}