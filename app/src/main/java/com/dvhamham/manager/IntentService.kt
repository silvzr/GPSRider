package com.dvhamham.manager

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dvhamham.data.repository.PreferencesRepository
import com.dvhamham.data.model.FavoriteLocation
import com.dvhamham.data.model.LastClickedLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IntentService : Service() {
    
    companion object {
        private const val TAG = "IntentService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "gps_rider_service"
        
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_ACCURACY = "accuracy"
        const val EXTRA_ALTITUDE = "altitude"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_FAVORITE_NAME = "favorite_name"
        const val EXTRA_RANDOMIZE_RADIUS = "randomize_radius"
        const val EXTRA_RESULT_RECEIVER = "result_receiver"
        const val EXTRA_FAVORITE_DESCRIPTION = "favorite_description"
        const val EXTRA_FAVORITE_CATEGORY = "favorite_category"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_INTERVAL = "interval"
        const val EXTRA_PATH_FILE = "path_file"
        const val EXTRA_HEADING = "heading"
        const val EXTRA_BEARING = "bearing"
        
        const val RESULT_SUCCESS = 1
        const val RESULT_ERROR = 0
        const val RESULT_INVALID_PARAMS = -1
        
        // Intent Actions
        const val ACTION_START_FAKE_LOCATION = "com.dvhamham.START_FAKE_LOCATION"
        const val ACTION_STOP_FAKE_LOCATION = "com.dvhamham.STOP_FAKE_LOCATION"
        const val ACTION_TOGGLE_FAKE_LOCATION = "com.dvhamham.TOGGLE_FAKE_LOCATION"
        const val ACTION_SET_CUSTOM_LOCATION = "com.dvhamham.SET_CUSTOM_LOCATION"
        const val ACTION_GET_STATUS = "com.dvhamham.GET_STATUS"
        const val ACTION_GET_CURRENT_LOCATION = "com.dvhamham.GET_CURRENT_LOCATION"
        const val ACTION_SET_ACCURACY = "com.dvhamham.SET_ACCURACY"
        const val ACTION_RANDOMIZE_LOCATION = "com.dvhamham.RANDOMIZE_LOCATION"
    }
    
    private lateinit var preferencesRepository: PreferencesRepository
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isForeground = false
    
    override fun onCreate() {
        super.onCreate()
        try {
            preferencesRepository = PreferencesRepository(this)
            createNotificationChannel()
            Log.d(TAG, "Service created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Service: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            Log.d(TAG, "onStartCommand called with intent: ${intent?.action}")
            
            // Start foreground immediately
            if (!isForeground) {
                startForeground(NOTIFICATION_ID, createNotification("GPS Rider Service", "Processing intent..."))
                isForeground = true
            }
            
            if (intent == null) {
                Log.w(TAG, "Received null intent")
                return START_NOT_STICKY
            }
            
            handleIntent(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onStartCommand: ${e.message}")
            e.printStackTrace()
        }
        // We want the service to be killed if explicitly stopped, not restarted automatically
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Rider Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "GPS Rider background service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    
    private fun updateNotification(content: String) {
        val notification = createNotification("GPS Rider Service", content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val resultReceiver = intent.getParcelableExtra<ResultReceiver>(EXTRA_RESULT_RECEIVER)
        
        Log.d(TAG, "Handling intent action: $action")
        
        try {
            when (action) {
                ACTION_START_FAKE_LOCATION -> {
                    updateNotification("Starting fake location...")
                    startFakeLocation(resultReceiver)
                }
                ACTION_STOP_FAKE_LOCATION -> {
                    updateNotification("Stopping fake location...")
                    stopFakeLocation(resultReceiver)
                }
                ACTION_TOGGLE_FAKE_LOCATION -> {
                    updateNotification("Toggling fake location...")
                    toggleFakeLocation(resultReceiver)
                }
                ACTION_SET_CUSTOM_LOCATION -> {
                    // Log all extras for debugging
                    Log.d(TAG, "All extras: ${intent.extras?.keySet()}")
                    intent.extras?.keySet()?.forEach { key ->
                        Log.d(TAG, "Extra $key: ${intent.extras?.get(key)} (type: ${intent.extras?.get(key)?.javaClass?.simpleName})")
                    }
                    
                    // Try different ways to read latitude and longitude
                    var latitude = Double.NaN
                    var longitude = Double.NaN
                    
                    // Try getDoubleExtra first
                    if (intent.hasExtra(EXTRA_LATITUDE)) {
                        latitude = intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN)
                        Log.d(TAG, "getDoubleExtra latitude: $latitude")
                    }
                    if (intent.hasExtra(EXTRA_LONGITUDE)) {
                        longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN)
                        Log.d(TAG, "getDoubleExtra longitude: $longitude")
                    }
                    
                    // If still NaN, try getFloatExtra
                    if (latitude.isNaN() && intent.hasExtra(EXTRA_LATITUDE)) {
                        latitude = intent.getFloatExtra(EXTRA_LATITUDE, Float.NaN).toDouble()
                        Log.d(TAG, "getFloatExtra latitude: $latitude")
                    }
                    if (longitude.isNaN() && intent.hasExtra(EXTRA_LONGITUDE)) {
                        longitude = intent.getFloatExtra(EXTRA_LONGITUDE, Float.NaN).toDouble()
                        Log.d(TAG, "getFloatExtra longitude: $longitude")
                    }
                    
                    // If still NaN, try getIntExtra
                    if (latitude.isNaN() && intent.hasExtra(EXTRA_LATITUDE)) {
                        val latInt = intent.getIntExtra(EXTRA_LATITUDE, Int.MIN_VALUE)
                        Log.d(TAG, "getIntExtra latitude: $latInt")
                        if (latInt != Int.MIN_VALUE) {
                            latitude = latInt.toDouble()
                        }
                    }
                    if (longitude.isNaN() && intent.hasExtra(EXTRA_LONGITUDE)) {
                        val lngInt = intent.getIntExtra(EXTRA_LONGITUDE, Int.MIN_VALUE)
                        Log.d(TAG, "getIntExtra longitude: $lngInt")
                        if (lngInt != Int.MIN_VALUE) {
                            longitude = lngInt.toDouble()
                        }
                    }
                    
                    // If still NaN, try getStringExtra and parse
                    if (latitude.isNaN() && intent.hasExtra(EXTRA_LATITUDE)) {
                        try {
                            val latString = intent.getStringExtra(EXTRA_LATITUDE)
                            Log.d(TAG, "getStringExtra latitude string: $latString")
                            latitude = latString?.toDouble() ?: Double.NaN
                            Log.d(TAG, "Parsed latitude: $latitude")
                        } catch (e: NumberFormatException) {
                            Log.e(TAG, "Failed to parse latitude string: ${e.message}")
                            latitude = Double.NaN
                        }
                    }
                    if (longitude.isNaN() && intent.hasExtra(EXTRA_LONGITUDE)) {
                        try {
                            val lngString = intent.getStringExtra(EXTRA_LONGITUDE)
                            Log.d(TAG, "getStringExtra longitude string: $lngString")
                            longitude = lngString?.toDouble() ?: Double.NaN
                            Log.d(TAG, "Parsed longitude: $longitude")
                        } catch (e: NumberFormatException) {
                            Log.e(TAG, "Failed to parse longitude string: ${e.message}")
                            longitude = Double.NaN
                        }
                    }
                    
                    // Log what we received
                    Log.d(TAG, "Final received latitude: $latitude, longitude: $longitude")
                    
                    updateNotification("Setting custom location...")
                    setCustomLocation(latitude, longitude, resultReceiver)
                }
                ACTION_GET_STATUS -> {
                    updateNotification("Getting status...")
                    getStatus(resultReceiver)
                }
                ACTION_GET_CURRENT_LOCATION -> {
                    updateNotification("Getting current location...")
                    getCurrentLocation(resultReceiver)
                }
                ACTION_SET_ACCURACY -> {
                    val accuracy = intent.getFloatExtra(EXTRA_ACCURACY, Float.NaN)
                    updateNotification("Setting accuracy...")
                    setAccuracy(accuracy, resultReceiver)
                }
                ACTION_RANDOMIZE_LOCATION -> {
                    val radius = intent.getDoubleExtra(EXTRA_RANDOMIZE_RADIUS, 100.0)
                    updateNotification("Randomizing location...")
                    randomizeLocation(radius, resultReceiver)
                }
                else -> {
                    Log.w(TAG, "Unknown action: $action")
                    updateNotification("Unknown action: $action")
                    sendResult(resultReceiver, RESULT_ERROR, "Unknown action: $action")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling intent: ${e.message}")
            e.printStackTrace()
            updateNotification("Error: ${e.message}")
            sendResult(resultReceiver, RESULT_ERROR, "Error: ${e.message}")
        }
    }
    
    private fun startFakeLocation(resultReceiver: ResultReceiver?) {
        Log.d(TAG, "Starting fake location...")
        scope.launch {
            try {
                val currentLocation = preferencesRepository.getLastClickedLocation()
                Log.d(TAG, "Current location: $currentLocation")
                
                if (currentLocation == null) {
                    preferencesRepository.saveLastClickedLocation(40.7128, -74.0060)
                    Log.d(TAG, "No location set, using default location (New York)")
                }
                
                preferencesRepository.saveIsPlaying(true)
                Log.d(TAG, "Saved isPlaying = true")
                
                val broadcastIntent = Intent("com.dvhamham.FAKE_LOCATION_STARTED")
                sendBroadcast(broadcastIntent)
                Log.d(TAG, "Sent broadcast: FAKE_LOCATION_STARTED")
                
                val location = preferencesRepository.getLastClickedLocation()
                val message = if (location != null) {
                    "Fake location started at: ${location.latitude}, ${location.longitude}"
                } else {
                    "Fake location started"
                }
                
                updateNotification(message)
                sendResult(resultReceiver, RESULT_SUCCESS, message)
                Log.d(TAG, "Fake location started successfully: $message")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start fake location: ${e.message}")
                e.printStackTrace()
                updateNotification("Failed to start fake location")
                sendResult(resultReceiver, RESULT_ERROR, "Failed to start fake location: ${e.message}")
            }
        }
    }
    
    private fun stopFakeLocation(resultReceiver: ResultReceiver?) {
        Log.d(TAG, "Stopping fake location...")
        scope.launch {
            try {
                preferencesRepository.saveIsPlaying(false)
                
                val broadcastIntent = Intent("com.dvhamham.FAKE_LOCATION_STOPPED")
                sendBroadcast(broadcastIntent)
                
                updateNotification("Fake location stopped")
                sendResult(resultReceiver, RESULT_SUCCESS, "Fake location stopped")
                Log.d(TAG, "Fake location stopped successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop fake location: ${e.message}")
                e.printStackTrace()
                updateNotification("Failed to stop fake location")
                sendResult(resultReceiver, RESULT_ERROR, "Failed to stop fake location: ${e.message}")
            }
        }
    }
    
    private fun toggleFakeLocation(resultReceiver: ResultReceiver?) {
        Log.d(TAG, "Toggling fake location...")
        scope.launch {
            try {
                val isCurrentlyPlaying = preferencesRepository.getIsPlaying()
                preferencesRepository.saveIsPlaying(!isCurrentlyPlaying)
                val status = if (!isCurrentlyPlaying) "started" else "stopped"
                updateNotification("Fake location $status")
                sendResult(resultReceiver, RESULT_SUCCESS, "Fake location $status")
                Log.d(TAG, "Fake location toggled successfully: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle fake location: ${e.message}")
                e.printStackTrace()
                updateNotification("Failed to toggle fake location")
                sendResult(resultReceiver, RESULT_ERROR, "Failed to toggle fake location: ${e.message}")
            }
        }
    }
    
    private fun setCustomLocation(latitude: Double, longitude: Double, resultReceiver: ResultReceiver?) {
        Log.d(TAG, "Setting custom location: $latitude, $longitude")
        
        if (latitude.isNaN() || longitude.isNaN()) {
            Log.w(TAG, "Invalid latitude or longitude")
            updateNotification("Invalid coordinates")
            sendResult(resultReceiver, RESULT_INVALID_PARAMS, "Invalid latitude or longitude")
            return
        }
        
        scope.launch {
            try {
                preferencesRepository.saveLastClickedLocation(latitude, longitude)
                preferencesRepository.saveIsPlaying(true)
                
                // Add to location history
                preferencesRepository.addToLocationHistory("Custom: $latitude, $longitude")
                
                val broadcastIntent = Intent("com.dvhamham.FAKE_LOCATION_STARTED")
                sendBroadcast(broadcastIntent)
                val message = "Location set to: $latitude, $longitude and fake location started"
                updateNotification(message)
                sendResult(resultReceiver, RESULT_SUCCESS, message)
                Log.d(TAG, "Custom location set and fake location started: $latitude, $longitude")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set custom location: ${e.message}")
                e.printStackTrace()
                updateNotification("Failed to set location")
                sendResult(resultReceiver, RESULT_ERROR, "Failed to set custom location: ${e.message}")
            }
        }
    }
    
    private fun getStatus(resultReceiver: ResultReceiver?) {
        Log.d(TAG, "Getting status...")
        scope.launch {
            try {
                val isPlaying = preferencesRepository.getIsPlaying()
                val status = if (isPlaying) "active" else "inactive"
                updateNotification("Status: $status")
                sendResult(resultReceiver, RESULT_SUCCESS, "Status: $status")
                Log.d(TAG, "Status retrieved: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get status: ${e.message}")
                e.printStackTrace()
                updateNotification("Failed to get status")
                sendResult(resultReceiver, RESULT_ERROR, "Failed to get status: ${e.message}")
            }
        }
    }
    
    private fun getCurrentLocation(resultReceiver: ResultReceiver?) {
        Log.d(TAG, "Getting current location...")
        scope.launch {
            try {
                val location = preferencesRepository.getLastClickedLocation()
                if (location != null) {
                    val response = "Current location: ${location.latitude}, ${location.longitude}"
                    updateNotification(response)
                    sendResult(resultReceiver, RESULT_SUCCESS, response)
                    Log.d(TAG, "Current location retrieved: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.w(TAG, "No location set")
                    updateNotification("No location set")
                    sendResult(resultReceiver, RESULT_ERROR, "No location set")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get current location: ${e.message}")
                e.printStackTrace()
                updateNotification("Failed to get location")
                sendResult(resultReceiver, RESULT_ERROR, "Failed to get current location: ${e.message}")
            }
        }
    }
    
    private fun setAccuracy(accuracy: Float, resultReceiver: ResultReceiver?) {
        Log.d(TAG, "Setting accuracy: $accuracy")
        
        if (accuracy.isNaN()) {
            Log.w(TAG, "Invalid accuracy value")
            updateNotification("Invalid accuracy")
            sendResult(resultReceiver, RESULT_INVALID_PARAMS, "Invalid accuracy value")
            return
        }
        
        scope.launch {
            try {
                preferencesRepository.saveUseAccuracy(true)
                preferencesRepository.saveAccuracy(accuracy.toDouble())
                updateNotification("Accuracy set to: $accuracy")
                sendResult(resultReceiver, RESULT_SUCCESS, "Accuracy set to: $accuracy")
                Log.d(TAG, "Accuracy set successfully: $accuracy")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set accuracy: ${e.message}")
                e.printStackTrace()
                updateNotification("Failed to set accuracy")
                sendResult(resultReceiver, RESULT_ERROR, "Failed to set accuracy: ${e.message}")
            }
        }
    }
    
    private fun randomizeLocation(radius: Double, resultReceiver: ResultReceiver?) {
        Log.d(TAG, "Randomizing location with radius: $radius")
        scope.launch {
            try {
                preferencesRepository.saveUseRandomize(true)
                preferencesRepository.saveRandomizeRadius(radius)
                updateNotification("Randomization enabled with radius: $radius")
                sendResult(resultReceiver, RESULT_SUCCESS, "Randomization enabled with radius: $radius")
                Log.d(TAG, "Randomization enabled successfully: $radius")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable randomization: ${e.message}")
                e.printStackTrace()
                updateNotification("Failed to enable randomization")
                sendResult(resultReceiver, RESULT_ERROR, "Failed to enable randomization: ${e.message}")
            }
        }
    }
    
    private fun sendResult(resultReceiver: ResultReceiver?, resultCode: Int, message: String) {
        try {
            resultReceiver?.let { receiver ->
                val bundle = Bundle().apply {
                    putString("message", message)
                }
                receiver.send(resultCode, bundle)
                Log.d(TAG, "Result sent: $resultCode - $message")
            } ?: run {
                Log.d(TAG, "No result receiver, result: $resultCode - $message")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending result: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isForeground = false
    }
} 