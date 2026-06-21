package com.primaraya.inspectra.fitur.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.core.network.StatusKoneksi
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi

@Composable
fun SplashScreen(
    onSelesai: (StatusKoneksi) -> Unit,
    viewModel: SplashViewModel = viewModel(
        factory = pabrikViewModelAplikasi(
            application = LocalContext.current.applicationContext as Application,
            pembuat = { SplashViewModel(it) }
        )
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.periksaKoneksi()
    }

    LaunchedEffect(state.status) {
        if (state.status == StatusKoneksi.ONLINE) onSelesai(StatusKoneksi.ONLINE)
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
            
            if (state.status == StatusKoneksi.OFFLINE) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFFBBF24))
                Text(
                    text = "Koneksi belum tersedia\nPerangkat belum bisa terhubung ke server. Data tetap dapat disiapkan sebagai draft.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { onSelesai(StatusKoneksi.OFFLINE) }) {
                        Text("Mode Offline")
                    }
                    Button(onClick = viewModel::periksaKoneksi, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Coba Lagi")
                    }
                }
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFFD97706),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text(state.pesan, color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
