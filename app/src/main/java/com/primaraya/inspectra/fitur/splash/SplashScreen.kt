package com.primaraya.inspectra.fitur.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.masterdata.data.SupabaseMasterDataRepository
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onHealthCheckPassed: () -> Unit) {
    var statusText by remember { mutableStateOf("Booting InSpectra Core Modules...") }
    var progress by remember { mutableStateOf(0.1f) }
    var errorOccurred by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    val repository = remember { SupabaseMasterDataRepository() }

    LaunchedEffect(retryTrigger) {
        errorOccurred = null
        progress = 0.1f
        statusText = "Initializing secure network engine..."
        delay(500)
        
        progress = 0.4f
        statusText = "Validating Supabase connection..."
        
        when (val result = repository.healthCheck()) {
            is NetworkResult.Success -> {
                progress = 1.0f
                statusText = "All systems nominal. Sync active."
                delay(600)
                onHealthCheckPassed()
            }
            is NetworkResult.Error -> {
                errorOccurred = result.message
                statusText = "Connection failed."
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("INSPECTRA", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF60A5FA), fontWeight = FontWeight.Black)
            Text("PT. Primaraya Graha Nusantara", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (errorOccurred != null) {
                Text(
                    text = "Gagal terhubung ke server:\n$errorOccurred", 
                    color = Color(0xFFF87171), 
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = { retryTrigger++ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Coba Lagi")
                }
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFFD97706),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text(statusText, color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
