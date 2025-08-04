package com.example.batterypercentagenotifier

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
/* import androidx.compose.ui.text.input.KeyboardOptions */
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.batterypercentagenotifier.ui.theme.BatteryPercentageNotifierTheme
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BatteryPercentageNotifierTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var threshold by remember { mutableStateOf(ThresholdPrefs.getThreshold(context)) }
    var inputThreshold by remember { mutableStateOf(threshold.toString()) }
    var showSavedMsg by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Current Threshold: $threshold%", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = inputThreshold,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } && newValue.length <= 3) inputThreshold = newValue
            },
            label = { Text("Set Threshold (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = {
                val value = inputThreshold.toIntOrNull()
                if (value != null && value in 10..100) { // reasonable range
                    ThresholdPrefs.setThreshold(context, value)
                    threshold = value
                    showSavedMsg = true
                }
            }) {
                Text("Save Threshold")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                startBatteryMonitorService(context)
            }) {
                Text("Start Monitoring")
            }
        }
        if (showSavedMsg) {
            Spacer(Modifier.height(8.dp))
            Text("Threshold Saved!", color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(32.dp))
        Text("The service will create a timer alarm when charging reaches threshold.", style = MaterialTheme.typography.bodyMedium)
    }
}

fun startBatteryMonitorService(context: Context) {
    val intent = Intent(context, BatteryMonitorService::class.java)
    context.startForegroundService(intent)
}