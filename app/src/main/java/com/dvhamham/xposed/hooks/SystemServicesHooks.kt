// SystemServicesHooks.kt
package com.dvhamham.xposed.hooks

import android.location.Location
import android.location.LocationRequest
import android.os.Build
import com.dvhamham.xposed.utils.LocationUtil
import com.dvhamham.xposed.utils.PreferencesUtil
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.random.Random

class SystemServicesHooks(val appLpparam: LoadPackageParam) {
    private val tag = "[GPSRider-SystemHooks]"
    private val DEBUG = false // Disabled for better stealth

    fun initHooks() {
        hookSystemServices(appLpparam.classLoader)
    }

    private fun hookSystemServices(classLoader: ClassLoader) {
        try {
            val locationManagerServiceClass = XposedHelpers.findClass("com.android.server.LocationManagerService", classLoader)

            // Enhanced getLastLocation hook for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.findAndHookMethod(
                    locationManagerServiceClass,
                    "getLastLocation",
                    LocationRequest::class.java,
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (PreferencesUtil.isFakeLocationEnabled()) {
                                val location = Location(android.location.LocationManager.GPS_PROVIDER)
                                location.time = System.currentTimeMillis() - Random.nextInt(100, 10001).toLong()
                                location.latitude = LocationUtil.latitude
                                location.longitude = LocationUtil.longitude
                                location.altitude = LocationUtil.altitude
                                location.speed = LocationUtil.speed
                                location.accuracy = LocationUtil.accuracy
                                location.speedAccuracyMetersPerSecond = LocationUtil.speedAccuracy
                                
                                // Enhanced mock provider hiding
                                try {
                                    HiddenApiBypass.invoke(location.javaClass, location, "setIsFromMockProvider", false)
                                } catch (e: Exception) {
                                    // Ignore errors for speed
                                }
                                
                                param.result = location
                            }
                        }
                    })
            }

            // Enhanced getLastLocation hook for older Android versions
            XposedHelpers.findAndHookMethod(
                locationManagerServiceClass,
                "getLastLocation",
                String::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            val provider = param.args[0] as String
                            val fakeLocation = LocationUtil.createFakeLocation(provider = provider)
                            param.result = fakeLocation
                        }
                    }
                })

            // Enhanced requestLocationUpdates hook
            XposedHelpers.findAndHookMethod(
                locationManagerServiceClass,
                "requestLocationUpdates",
                LocationRequest::class.java,
                "android.location.ILocationListener",
                "android.app.PendingIntent",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            val ILocationListener = param.args[1]
                            val fakeLocation = LocationUtil.createFakeLocation()
                            
                            // Call the listener with fake location
                            try {
                                XposedHelpers.callMethod(ILocationListener, "onLocationChanged", fakeLocation)
                            } catch (e: Exception) {
                                // Ignore errors for speed
                            }
                        }
                    }
                })

            // Disable GNSS methods for better stealth
            val methodsToReplace = arrayOf(
                "addGnssBatchingCallback",
                "addGnssMeasurementsListener",
                "addGnssNavigationMessageListener",
                "registerGnssBatchingCallback",
                "unregisterGnssBatchingCallback"
            )

            for (methodName in methodsToReplace) {
                try {
                    XposedHelpers.findAndHookMethod(
                        locationManagerServiceClass,
                        methodName,
                        XC_MethodReplacement.returnConstant(false)
                    )
                } catch (e: Exception) {
                    // Method might not exist on this Android version
                }
            }

            // Enhanced callLocationChangedLocked hook
            XposedHelpers.findAndHookMethod(
                XposedHelpers.findClass("com.android.server.LocationManagerService\$Receiver", classLoader),
                "callLocationChangedLocked",
                Location::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            val fakeLocation = LocationUtil.createFakeLocation(param.args[0] as? Location)
                            param.args[0] = fakeLocation
                        }
                    }
                })

            // Additional hook for location updates
            XposedHelpers.findAndHookMethod(
                locationManagerServiceClass,
                "requestLocationUpdates",
                String::class.java,
                Long::class.java,
                Float::class.java,
                "android.location.ILocationListener",
                "android.app.PendingIntent",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            val ILocationListener = param.args[3]
                            val fakeLocation = LocationUtil.createFakeLocation()
                            
                            try {
                                XposedHelpers.callMethod(ILocationListener, "onLocationChanged", fakeLocation)
                            } catch (e: Exception) {
                                // Ignore errors for speed
                            }
                        }
                    }
                })

        } catch (e: Exception) {
            // Ignore errors for speed
        }
    }
}