package com.primaraya.inspectra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.primaraya.inspectra.core.ui.theme.InSpectraTheme
import com.primaraya.inspectra.core.sync.SyncManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Phase 2: Schedule background sync
        SyncManager.scheduleInitialSync(this)

        setContent {
            val navController = androidx.navigation.compose.rememberNavController()

            InSpectraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    com.primaraya.inspectra.core.ui.navigation.AdaptiveNavigationShell(navController = navController) {
                        com.primaraya.inspectra.core.ui.navigation.AppNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
