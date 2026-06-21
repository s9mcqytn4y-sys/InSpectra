package com.primaraya.inspectra.fitur.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSelesai: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1_200)
        onSelesai()
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
            
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFFD97706),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Text("Menyiapkan workspace...", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}
