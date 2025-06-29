// LocationUtil.kt
package com.dvhamham.xposed.utils

import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.dvhamham.data.DEFAULT_ACCURACY
import com.dvhamham.data.DEFAULT_ALTITUDE
import com.dvhamham.data.DEFAULT_MEAN_SEA_LEVEL
import com.dvhamham.data.DEFAULT_MEAN_SEA_LEVEL_ACCURACY
import com.dvhamham.data.DEFAULT_RANDOMIZE_RADIUS
import com.dvhamham.data.DEFAULT_SPEED
import com.dvhamham.data.DEFAULT_SPEED_ACCURACY
import com.dvhamham.data.DEFAULT_VERTICAL_ACCURACY
import com.dvhamham.data.PI
import com.dvhamham.data.RADIUS_EARTH
import de.robv.android.xposed.XposedBridge
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.Random
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtil {
    private const val TAG = "[GPSRider-LocationUtil]"

    private const val DEBUG: Boolean = false // Disabled for better stealth

    private val random: Random = Random()

    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var accuracy: Float = 0F
    var altitude: Double = 0.0
    var verticalAccuracy: Float = 0F
    var meanSeaLevel: Double = 0.0
    var meanSeaLevelAccuracy: Float = 0F
    var speed: Float = 0F
    var speedAccuracy: Float = 0F

    @Synchronized
    fun createFakeLocation(originalLocation: Location? = null, provider: String = LocationManager.GPS_PROVIDER): Location {
        val fakeLocation = if (originalLocation == null) {
            Location(provider).apply {
                time = System.currentTimeMillis() - (random.nextInt(9901) + 100).toLong() // Randomized timestamp
            }
        } else {
            Location(originalLocation.provider).apply {
                time = originalLocation.time
                accuracy = originalLocation.accuracy
                bearing = originalLocation.bearing
                bearingAccuracyDegrees = originalLocation.bearingAccuracyDegrees
                elapsedRealtimeNanos = originalLocation.elapsedRealtimeNanos
                verticalAccuracyMeters = originalLocation.verticalAccuracyMeters
            }
        }

        fakeLocation.latitude = latitude
        fakeLocation.longitude = longitude

        if (accuracy != 0F) {
            fakeLocation.accuracy = accuracy
        }

        if (altitude != 0.0) {
            fakeLocation.altitude = altitude
        }

        if (verticalAccuracy != 0F) {
            fakeLocation.verticalAccuracyMeters = verticalAccuracy
        }

        if (speed != 0F) {
            fakeLocation.speed = speed
        }

        if (speedAccuracy != 0F) {
            fakeLocation.speedAccuracyMetersPerSecond = speedAccuracy
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (meanSeaLevel != 0.0) {
                fakeLocation.mslAltitudeMeters = meanSeaLevel
            }

            if (meanSeaLevelAccuracy != 0F) {
                fakeLocation.mslAltitudeAccuracyMeters = meanSeaLevelAccuracy
            }
        }

        // Enhanced mock provider hiding
        attemptHideMockProvider(fakeLocation)

        return fakeLocation
    }

    private fun attemptHideMockProvider(fakeLocation: Location) {
        try {
            // Primary method: HiddenApiBypass
            HiddenApiBypass.invoke(fakeLocation.javaClass, fakeLocation, "setIsFromMockProvider", false)
            
            // Secondary method: Reflection fallback
            try {
                val method = fakeLocation.javaClass.getDeclaredMethod("setIsFromMockProvider", Boolean::class.java)
                method.isAccessible = true
                method.invoke(fakeLocation, false)
            } catch (e: Exception) {
                // Ignore reflection errors
            }
            
        } catch (e: Exception) {
            // Ignore errors for speed
        }
    }

    @Synchronized
    fun updateLocation() {
        try {
            PreferencesUtil.getLastClickedLocation()?.let {
                if (PreferencesUtil.getUseRandomize() == true) {
                    val randomizationRadius = PreferencesUtil.getRandomizeRadius() ?: DEFAULT_RANDOMIZE_RADIUS
                    val randomLocation = getRandomLocation(it.latitude, it.longitude, randomizationRadius)
                    latitude = randomLocation.first
                    longitude = randomLocation.second
                } else {
                    latitude = it.latitude
                    longitude = it.longitude
                }

                if (PreferencesUtil.getUseAccuracy() == true) {
                    accuracy = (PreferencesUtil.getAccuracy() ?: DEFAULT_ACCURACY).toFloat()
                }

                 if (PreferencesUtil.getUseAltitude() == true) {
                     altitude = PreferencesUtil.getAltitude() ?: DEFAULT_ALTITUDE
                }

                if (PreferencesUtil.getUseVerticalAccuracy() == true) {
                    verticalAccuracy = PreferencesUtil.getVerticalAccuracy()?.toFloat() ?: DEFAULT_VERTICAL_ACCURACY
                }

                if (PreferencesUtil.getUseMeanSeaLevel() == true) {
                    meanSeaLevel = PreferencesUtil.getMeanSeaLevel() ?: DEFAULT_MEAN_SEA_LEVEL
                }

                if (PreferencesUtil.getUseMeanSeaLevelAccuracy() == true) {
                    meanSeaLevelAccuracy = PreferencesUtil.getMeanSeaLevelAccuracy()?.toFloat() ?: DEFAULT_MEAN_SEA_LEVEL_ACCURACY
                }

                if (PreferencesUtil.getUseSpeed() == true) {
                    speed = PreferencesUtil.getSpeed()?.toFloat() ?: DEFAULT_SPEED
                }

                if (PreferencesUtil.getUseSpeedAccuracy() == true) {
                    speedAccuracy = PreferencesUtil.getSpeedAccuracy()?.toFloat() ?: DEFAULT_SPEED_ACCURACY
                }

            } ?: run {
                // Default coordinates if no location is set
                if (latitude == 0.0 && longitude == 0.0) {
                    latitude = 40.7128 // NYC latitude
                    longitude = -74.0060 // NYC longitude
                    accuracy = 5.0f
                    altitude = 10.0
                    speed = 0.0f
                }
            }
        } catch (e: Exception) {
            // Ignore errors for speed
        }
    }

    // Enhanced random location calculation with better distribution
    private fun getRandomLocation(lat: Double, lon: Double, radiusInMeters: Double): Pair<Double, Double> {
        val radiusInRadians = radiusInMeters / RADIUS_EARTH

        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)

        // Generate two random numbers with better distribution
        val rand1 = random.nextDouble()
        val rand2 = random.nextDouble()

        // Random distance and bearing
        val distance = radiusInRadians * sqrt(rand1)
        val bearing = 2 * PI * rand2

        val sinDistance = sin(distance)
        val cosDistance = cos(distance)

        val newLatRad = asin(sinLat * cosDistance + cosLat * sinDistance * cos(bearing))
        val newLonRad = lonRad + atan2(
            sin(bearing) * sinDistance * cosLat,
            cosDistance - sinLat * sin(newLatRad)
        )

        // Convert back to degrees
        val newLat = Math.toDegrees(newLatRad)
        var newLon = Math.toDegrees(newLonRad)

        // Normalize longitude to be between -180 and 180 degrees
        newLon = ((newLon + 180) % 360 + 360) % 360 - 180

        // Clamp latitude to -90 to 90 degrees
        val clampedLat = newLat.coerceIn(-90.0, 90.0)

        return Pair(clampedLat, newLon)
    }

    // NEW: Enhanced location validation for better stealth
    fun validateLocation(location: Location): Boolean {
        return try {
            // Check if location has valid coordinates
            val lat = location.latitude
            val lon = location.longitude
            
            // Validate latitude range
            if (lat < -90.0 || lat > 90.0) return false
            
            // Validate longitude range
            if (lon < -180.0 || lon > 180.0) return false
            
            // Check for NaN values
            if (lat.isNaN() || lon.isNaN()) return false
            
            // Check for infinite values
            if (lat.isInfinite() || lon.isInfinite()) return false
            
            true
        } catch (e: Exception) {
            false
        }
    }

    // NEW: Generate realistic location data
    fun generateRealisticLocation(): Location {
        val location = Location(LocationManager.GPS_PROVIDER)
        
        // Generate realistic timestamp
        location.time = System.currentTimeMillis() - (random.nextInt(9901) + 100).toLong()
        
        // Set coordinates
        location.latitude = latitude
        location.longitude = longitude
        
        // Set realistic accuracy (3-15 meters)
        if (accuracy == 0f) {
            location.accuracy = (random.nextInt(13) + 3).toFloat()
        } else {
            location.accuracy = accuracy
        }
        
        // Set realistic altitude
        if (altitude == 0.0) {
            location.altitude = (random.nextInt(200) - 100).toDouble() // -100 to +100 meters
        } else {
            location.altitude = altitude
        }
        
        // Set realistic speed (0-30 m/s)
        if (speed == 0f) {
            location.speed = random.nextFloat() * 30f
        } else {
            location.speed = speed
        }
        
        // Set realistic bearing (0-360 degrees)
        location.bearing = random.nextFloat() * 360f
        
        // Set realistic speed accuracy
        location.speedAccuracyMetersPerSecond = random.nextFloat() * 5f
        
        // Set realistic bearing accuracy
        location.bearingAccuracyDegrees = random.nextFloat() * 10f
        
        // Set realistic vertical accuracy
        location.verticalAccuracyMeters = random.nextFloat() * 10f
        
        return location
    }
}