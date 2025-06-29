package com.dvhamham.manager

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.dvhamham.manager.ui.navigation.MainNavGraphWithBottomBarAndPermissions
import com.dvhamham.manager.ui.theme.GPSRiderTheme
import com.dvhamham.manager.ui.theme.LocalThemeManager
import com.dvhamham.manager.ui.theme.rememberThemeManager
import com.dvhamham.manager.ui.theme.StatusBarDark
import com.dvhamham.manager.ui.theme.StatusBarLight
import androidx.core.view.WindowCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log

class MainActivity : ComponentActivity() {
    
    private var broadcastReceiver: GPSRiderBroadcastReceiver? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val themeManager = rememberThemeManager()
            val isDarkMode = themeManager.isDarkMode.collectAsState().value
            
            CompositionLocalProvider(LocalThemeManager provides themeManager) {
                GPSRiderTheme(darkTheme = isDarkMode) {
                    // Update status bar after theme is applied
                    LaunchedEffect(isDarkMode) {
                        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
                        
                        if (isDarkMode) {
                            // Dark theme: use custom dark status bar color
                            window.statusBarColor = StatusBarDark.toArgb()
                            window.navigationBarColor = StatusBarDark.toArgb()
                            windowInsetsController.isAppearanceLightStatusBars = false
                            windowInsetsController.isAppearanceLightNavigationBars = false
                        } else {
                            // Light theme: use custom light status bar color
                            window.statusBarColor = StatusBarLight.toArgb()
                            window.navigationBarColor = StatusBarLight.toArgb()
                            windowInsetsController.isAppearanceLightStatusBars = true
                            windowInsetsController.isAppearanceLightNavigationBars = true
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        MainNavGraphWithBottomBarAndPermissions(
                            navController = navController
                        )
                    }
                }
            }
        }
        
        // Handle incoming intents
        handleIncomingIntent(intent)
        
        // Register broadcast receiver
        registerBroadcastReceiver()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            // Unregister broadcast receiver
            unregisterBroadcastReceiver()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        try {
            handleIncomingIntent(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun registerBroadcastReceiver() {
        try {
            broadcastReceiver = GPSRiderBroadcastReceiver.register(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun unregisterBroadcastReceiver() {
        try {
            broadcastReceiver?.let { receiver ->
                GPSRiderBroadcastReceiver.unregister(this, receiver)
                broadcastReceiver = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun handleIncomingIntent(intent: Intent?) {
        try {
            intent?.let { incomingIntent ->
                val action = incomingIntent.action
                
                if (action != null && action.startsWith("com.dvhamham.")) {
                    // Forward the intent to IntentService
                    val serviceIntent = Intent(this, IntentService::class.java).apply {
                        this.action = action
                        // Copy all extras
                        incomingIntent.extras?.let { extras ->
                            putExtras(extras)
                        }
                    }
                    
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 