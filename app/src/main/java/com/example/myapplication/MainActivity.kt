package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MonitorScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MonitorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isChecked by remember { mutableStateOf(true) }
    var lastCheck by remember { mutableStateOf<Date?>(null) }
    var isUp by remember { mutableStateOf(true) }
    var downTime by remember { mutableStateOf<Date?>(null) }

    LaunchedEffect(Unit) {
        if (isChecked) {
            val intent = Intent(context, MonitorService::class.java)
            context.startService(intent)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isUp = intent?.getBooleanExtra("isUp", true) ?: true
                lastCheck = intent?.getLongExtra("lastCheck", 0L)?.let { Date(it) }
                downTime = intent?.getLongExtra("downTime", 0L)?.let { Date(it) }
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter("monitor-status"))
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    if (isChecked) {
                        val intent = Intent(context, MonitorService::class.java)
                        context.startService(intent)
                    } else {
                        val intent = Intent(context, MonitorService::class.java)
                        context.stopService(intent)
                    }
                }
            )
            Text("Enable URL Monitor")
        }

        lastCheck?.let {
            Text("Last check: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it)}")
        }

        if (!isUp) {
            downTime?.let {
                val diff = Date().time - it.time
                val seconds = diff / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                Text("Downtime: ${hours}h ${minutes % 60}m ${seconds % 60}s")
            }
        }

        Text(
            text = "Manual Check",
            modifier = Modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://isr.freeddns.org/")
                    context.startActivity(intent)
                }
                .padding(top = 16.dp),
            textDecoration = TextDecoration.Underline,
            color = Color.Blue
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MonitorScreenPreview() {
    MyApplicationTheme {
        MonitorScreen()
    }
}
