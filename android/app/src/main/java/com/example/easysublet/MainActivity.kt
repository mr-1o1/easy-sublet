package com.example.easysublet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import com.example.easysublet.data.TokenStore
import com.example.easysublet.ui.theme.EasySubletTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

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
    var token by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context = context) }

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

        // Load token if exists
        tokenStore.tokenFlow.collect { saved ->
            token = saved
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = statusState.value, style = MaterialTheme.typography.titleMedium)
        if (token == null) {
            Button(onClick = {
                // Trigger demo sign-up/sign-in in background
                // This is a simple MVP flow to validate Step-2 from the app
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    val client = OkHttpClient()
                    // Try sign up
                    val signupJson = "{" +
                        "\"email\":\"test@example.com\"," +
                        "\"password\":\"password\"," +
                        "\"name\":\"Test User\"}";
                    val signupReq = Request.Builder()
                        .url("${BuildConfig.API_BASE_URL}/auth/signup")
                        .post(signupJson.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(signupReq).execute().close()

                    // Login
                    val formBody = FormBody.Builder()
                        .add("username", "test@example.com")
                        .add("password", "password")
                        .build()
                    val loginReq = Request.Builder()
                        .url("${BuildConfig.API_BASE_URL}/auth/login")
                        .post(formBody)
                        .build()
                    client.newCall(loginReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: ""
                            val marker = "\"access_token\":\""
                            val startIdx = body.indexOf(marker)
                            val extracted = if (startIdx != -1) {
                                val from = startIdx + marker.length
                                val end = body.indexOf('"', from)
                                if (end != -1) body.substring(from, end) else null
                            } else null
                            if (extracted != null) {
                                tokenStore.saveToken(extracted)
                            }
                        }
                    }
                }
            }) {
                Text("Sign In (MVP)")
            }
        } else {
            Text(text = "Signed in", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    tokenStore.clearToken()
                }
            }) { Text("Sign out") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HealthCheckPreview() {
    EasySubletTheme {
        HealthCheckScreen()
    }
}