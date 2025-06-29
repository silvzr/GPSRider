package com.dvhamham.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Helper class for external apps to interact with GPS Rider
 * Provides easy-to-use methods for controlling fake location functionality
 */
class IntentHelper {
    
    companion object {
        private const val TAG = "IntentHelper"
        
        // Intent Actions
        const val ACTION_START_FAKE_LOCATION = "com.dvhamham.START_FAKE_LOCATION"
        const val ACTION_STOP_FAKE_LOCATION = "com.dvhamham.STOP_FAKE_LOCATION"
        const val ACTION_TOGGLE_FAKE_LOCATION = "com.dvhamham.TOGGLE_FAKE_LOCATION"
        const val ACTION_SET_CUSTOM_LOCATION = "com.dvhamham.SET_CUSTOM_LOCATION"
        const val ACTION_GET_STATUS = "com.dvhamham.GET_STATUS"
        const val ACTION_GET_CURRENT_LOCATION = "com.dvhamham.GET_CURRENT_LOCATION"
        const val ACTION_SET_ACCURACY = "com.dvhamham.SET_ACCURACY"
        const val ACTION_RANDOMIZE_LOCATION = "com.dvhamham.RANDOMIZE_LOCATION"
        
        // Intent Extras
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_ACCURACY = "accuracy"
        const val EXTRA_RANDOMIZE_RADIUS = "randomize_radius"
        const val EXTRA_RESULT_RECEIVER = "result_receiver"
        
        // Result Codes
        const val RESULT_SUCCESS = 1
        const val RESULT_ERROR = 0
        const val RESULT_INVALID_PARAMS = -1
        
        /**
         * Start fake location service
         */
        fun startFakeLocation(context: Context, callback: ((Boolean, String) -> Unit)? = null) {
            sendIntent(context, ACTION_START_FAKE_LOCATION, callback)
        }
        
        /**
         * Stop fake location service
         */
        fun stopFakeLocation(context: Context, callback: ((Boolean, String) -> Unit)? = null) {
            sendIntent(context, ACTION_STOP_FAKE_LOCATION, callback)
        }
        
        /**
         * Toggle fake location service (start if stopped, stop if started)
         */
        fun toggleFakeLocation(context: Context, callback: ((Boolean, String) -> Unit)? = null) {
            sendIntent(context, ACTION_TOGGLE_FAKE_LOCATION, callback)
        }
        
        /**
         * Set custom location with latitude and longitude
         */
        fun setCustomLocation(
            context: Context, 
            latitude: Double, 
            longitude: Double, 
            callback: ((Boolean, String) -> Unit)? = null
        ) {
            val intent = Intent(context, IntentService::class.java).apply {
                action = ACTION_SET_CUSTOM_LOCATION
                putExtra(EXTRA_LATITUDE, latitude)
                putExtra(EXTRA_LONGITUDE, longitude)
            }
            sendIntent(context, intent, callback)
        }
        
        /**
         * Get current fake location status
         */
        fun getStatus(context: Context, callback: ((Boolean, String) -> Unit)? = null) {
            sendIntent(context, ACTION_GET_STATUS, callback)
        }
        
        /**
         * Get current fake location coordinates
         */
        fun getCurrentLocation(context: Context, callback: ((Boolean, String) -> Unit)? = null) {
            sendIntent(context, ACTION_GET_CURRENT_LOCATION, callback)
        }
        
        /**
         * Set location accuracy
         */
        fun setAccuracy(
            context: Context, 
            accuracy: Float, 
            callback: ((Boolean, String) -> Unit)? = null
        ) {
            val intent = Intent(context, IntentService::class.java).apply {
                action = ACTION_SET_ACCURACY
                putExtra(EXTRA_ACCURACY, accuracy)
            }
            sendIntent(context, intent, callback)
        }
        
        /**
         * Enable location randomization with specified radius
         */
        fun randomizeLocation(
            context: Context, 
            radius: Double = 100.0, 
            callback: ((Boolean, String) -> Unit)? = null
        ) {
            val intent = Intent(context, IntentService::class.java).apply {
                action = ACTION_RANDOMIZE_LOCATION
                putExtra(EXTRA_RANDOMIZE_RADIUS, radius)
            }
            sendIntent(context, intent, callback)
        }
        
        /**
         * Send intent to the service
         */
        private fun sendIntent(
            context: Context, 
            action: String, 
            callback: ((Boolean, String) -> Unit)?
        ) {
            val intent = Intent(action)
            sendIntent(context, intent, callback)
        }
        
        /**
         * Send intent to the service with result receiver
         */
        private fun sendIntent(
            context: Context, 
            intent: Intent, 
            callback: ((Boolean, String) -> Unit)?
        ) {
            try {
                intent.setPackage("com.dvhamham")
                
                if (callback != null) {
                    val resultReceiver = object : ResultReceiver(null) {
                        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                            val message = resultData?.getString("message") ?: "Unknown result"
                            val success = resultCode == RESULT_SUCCESS
                            callback(success, message)
                        }
                    }
                    intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver)
                }
                
                context.startService(intent)
                
                if (callback == null) {
                    Log.d(TAG, "Intent sent without callback: ${intent.action}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending intent: ${e.message}")
                callback?.invoke(false, "Error: ${e.message}")
            }
        }
        
        /**
         * Register broadcast receiver
         */
        fun register(context: Context): GPSRiderBroadcastReceiver {
            return GPSRiderBroadcastReceiver.register(context)
        }
        
        /**
         * Unregister broadcast receiver
         */
        fun unregister(context: Context, receiver: GPSRiderBroadcastReceiver) {
            GPSRiderBroadcastReceiver.unregister(context, receiver)
        }
    }
}

/**
 * Example usage for external apps:
 * 
 * // Start fake location
 * IntentHelper.startFakeLocation(context) { success, message ->
 *     if (success) {
 *         Log.d("MyApp", "Fake location started: $message")
 *     } else {
 *         Log.e("MyApp", "Failed to start fake location: $message")
 *     }
 * }
 * 
 * // Set custom location
 * IntentHelper.setCustomLocation(context, 40.7128, -74.0060) { success, message ->
 *     Log.d("MyApp", "Location set result: $message")
 * }
 * 
 * // Get status
 * IntentHelper.getStatus(context) { success, message ->
 *     Log.d("MyApp", "Status: $message")
 * }
 */ 