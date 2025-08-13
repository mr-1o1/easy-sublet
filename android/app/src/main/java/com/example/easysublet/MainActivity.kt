package com.example.easysublet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.easysublet.ui.theme.EasySubletTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasySubletTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HealthCheckScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun HealthCheckScreen(modifier: Modifier = Modifier) {
    val statusState = remember { mutableStateOf("Checking...") }

    LaunchedEffect(Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}/health")
            .build()
        val result = withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        "Connected to API"
                    } else {
                        "API error: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                "Connection failed: ${e.message}"
            }
        }
        statusState.value = result
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = statusState.value, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun HealthCheckPreview() {
    EasySubletTheme {
        HealthCheckScreen()
    }
}