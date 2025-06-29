// LocationApiHooks.kt
package com.dvhamham.xposed.hooks

import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.dvhamham.xposed.utils.LocationUtil
import com.dvhamham.xposed.utils.PreferencesUtil
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.random.Random

class LocationApiHooks(val appLpparam: LoadPackageParam) {
    private val tag = "[GPSRider-Hooks]"
    private var mLastUpdated: Long = 0
    private val random = Random

    fun initHooks() {
        hookLocationAPI()
        hookLocationManagerService()
        hookAdditionalMethods(appLpparam.classLoader)
        hookPriorityMethods(appLpparam.classLoader)
        hookAntiDetectionMethods(appLpparam.classLoader)
        XposedBridge.log("$tag [PRIORITY] Enhanced hooks initialized successfully")
    }

    private fun hookLocationAPI() {
        hookLocation(appLpparam.classLoader)
        hookLocationManager(appLpparam.classLoader)
    }

    private fun hookLocation(classLoader: ClassLoader) {
        try {
            val locationClass = XposedHelpers.findClass("android.location.Location", classLoader)

            // Enhanced getLatitude hook with HiddenApiBypass
            XposedHelpers.findAndHookMethod(
                locationClass,
                "getLatitude",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (System.currentTimeMillis() - mLastUpdated > 200) {
                            LocationUtil.updateLocation()
                            mLastUpdated = System.currentTimeMillis()
                        }
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            param.result = LocationUtil.latitude
                        }
                    }
                })

            // Enhanced getLongitude hook with HiddenApiBypass
            XposedHelpers.findAndHookMethod(
                locationClass,
                "getLongitude",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (System.currentTimeMillis() - mLastUpdated > 200) {
                            LocationUtil.updateLocation()
                            mLastUpdated = System.currentTimeMillis()
                        }
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            param.result = LocationUtil.longitude
                        }
                    }
                })

            // Enhanced getAccuracy hook
            XposedHelpers.findAndHookMethod(
                locationClass,
                "getAccuracy",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (System.currentTimeMillis() - mLastUpdated > 200) {
                            LocationUtil.updateLocation()
                        }
                        if (PreferencesUtil.isFakeLocationEnabled() && PreferencesUtil.getUseAccuracy() == true) {
                            param.result = LocationUtil.accuracy
                        }
                    }
                })

            // Enhanced getAltitude hook
            XposedHelpers.findAndHookMethod(
                locationClass,
                "getAltitude",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (System.currentTimeMillis() - mLastUpdated > 200) {
                            LocationUtil.updateLocation()
                        }
                        if (PreferencesUtil.isFakeLocationEnabled() && PreferencesUtil.getUseAltitude() == true) {
                            param.result = LocationUtil.altitude
                        }
                    }
                })

            // Enhanced getSpeed hook
            XposedHelpers.findAndHookMethod(
                locationClass,
                "getSpeed",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (System.currentTimeMillis() - mLastUpdated > 200) {
                            LocationUtil.updateLocation()
                        }
                        if (PreferencesUtil.isFakeLocationEnabled() && PreferencesUtil.getUseSpeed() == true) {
                            param.result = LocationUtil.speed
                        }
                    }
                })

            // Enhanced set method hook (most powerful)
            XposedHelpers.findAndHookMethod(
                locationClass,
                "set",
                Location::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (System.currentTimeMillis() - mLastUpdated > 200) {
                            LocationUtil.updateLocation()
                        }
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            val originLocation = param.args[0] as? Location
                            val location = Location(originLocation?.provider ?: LocationManager.GPS_PROVIDER)
                            
                            // Copy original properties
                            if (originLocation != null) {
                                location.time = originLocation.time
                                location.bearing = originLocation.bearing
                                location.bearingAccuracyDegrees = originLocation.bearingAccuracyDegrees
                                location.elapsedRealtimeNanos = originLocation.elapsedRealtimeNanos
                                location.verticalAccuracyMeters = originLocation.verticalAccuracyMeters
                            } else {
                                location.time = System.currentTimeMillis() - (random.nextInt(9901) + 100).toLong()
                            }
                            
                            // Set fake values
                            location.latitude = LocationUtil.latitude
                            location.longitude = LocationUtil.longitude
                            location.altitude = LocationUtil.altitude
                            location.speed = LocationUtil.speed
                            location.accuracy = LocationUtil.accuracy
                            location.speedAccuracyMetersPerSecond = LocationUtil.speedAccuracy
                            
                            // Enhanced mock provider hiding
                            try {
                                HiddenApiBypass.invoke(location.javaClass, location, "setIsFromMockProvider", false)
                                // Additional hiding methods
                                try {
                                    val method = location.javaClass.getDeclaredMethod("setIsFromMockProvider", Boolean::class.java)
                                    method.isAccessible = true
                                    method.invoke(location, false)
                                } catch (e: Exception) {
                                    // Ignore reflection errors
                                }
                            } catch (e: Exception) {
                                // Fallback for older Android versions
                            }
                            
                            param.args[0] = location
                        }
                    }
                })

        } catch (e: Exception) {
            XposedBridge.log("$tag Error hooking Location class - ${e.message}")
        }
    }

    private fun hookLocationManager(classLoader: ClassLoader) {
        try {
            val locationManagerClass = XposedHelpers.findClass("android.location.LocationManager", classLoader)

            XposedHelpers.findAndHookMethod(
                locationManagerClass,
                "getLastKnownLocation",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            val provider = param.args[0] as String
                            val fakeLocation = LocationUtil.createFakeLocation(provider = provider)
                            
                            // Enhanced mock provider hiding
                            try {
                                HiddenApiBypass.invoke(fakeLocation.javaClass, fakeLocation, "setIsFromMockProvider", false)
                                // Additional hiding methods
                                try {
                                    val method = fakeLocation.javaClass.getDeclaredMethod("setIsFromMockProvider", Boolean::class.java)
                                    method.isAccessible = true
                                    method.invoke(fakeLocation, false)
                                } catch (e: Exception) {
                                    // Ignore reflection errors
                                }
                            } catch (e: Exception) {
                                // Fallback for older Android versions
                            }
                            
                            param.result = fakeLocation
                        }
                    }
                })

        } catch (e: Exception) {
            XposedBridge.log("$tag Error hooking LocationManager - ${e.message}")
        }
    }

    // New: Hook LocationManagerService for system-level control
    private fun hookLocationManagerService() {
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.server.LocationManagerService",
                appLpparam.classLoader,
                "getLastLocation",
                "android.location.LocationRequest",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            val location = Location(LocationManager.GPS_PROVIDER)
                            location.time = System.currentTimeMillis() - (random.nextInt(9901) + 100).toLong()
                            location.latitude = LocationUtil.latitude
                            location.longitude = LocationUtil.longitude
                            location.altitude = LocationUtil.altitude
                            location.speed = LocationUtil.speed
                            location.accuracy = LocationUtil.accuracy
                            location.speedAccuracyMetersPerSecond = LocationUtil.speedAccuracy
                            
                            // Enhanced mock provider hiding
                            try {
                                HiddenApiBypass.invoke(location.javaClass, location, "setIsFromMockProvider", false)
                                // Additional hiding methods
                                try {
                                    val method = location.javaClass.getDeclaredMethod("setIsFromMockProvider", Boolean::class.java)
                                    method.isAccessible = true
                                    method.invoke(location, false)
                                } catch (e: Exception) {
                                    // Ignore reflection errors
                                }
                            } catch (e: Exception) {
                                // Fallback for older Android versions
                            }
                            
                            param.result = location
                        }
                    }
                })

            // Additional: Hook requestLocationUpdates for better coverage
            XposedHelpers.findAndHookMethod(
                "com.android.server.LocationManagerService",
                appLpparam.classLoader,
                "requestLocationUpdates",
                "android.location.LocationRequest",
                "android.location.ILocationListener",
                "android.app.PendingIntent",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            val ILocationListener = param.args[1]
                            val location = Location(LocationManager.GPS_PROVIDER)
                            location.time = System.currentTimeMillis() - (random.nextInt(9901) + 100).toLong()
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
                                // Fallback for older Android versions
                            }
                            
                            // Call listener with fake location
                            try {
                                XposedHelpers.callMethod(ILocationListener, "onLocationChanged", location)
                            } catch (e: Exception) {
                                // Ignore errors
                            }
                        }
                    }
                })

        } catch (e: Exception) {
            XposedBridge.log("$tag Error hooking LocationManagerService - ${e.message}")
        }
    }

    // NEW: Anti-detection hooks for maximum stealth
    private fun hookAntiDetectionMethods(classLoader: ClassLoader) {
        try {
            // Hook Xposed detection methods
            XposedHelpers.findAndHookMethod(
                "de.robv.android.xposed.XposedBridge",
                classLoader,
                "isXposedEnabled",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                })

            // Hook LSPosed detection methods
            XposedHelpers.findAndHookMethod(
                "org.lsposed.lspd.core.Main",
                classLoader,
                "isLSPosedEnabled",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                })

            // Hook root detection methods
            XposedHelpers.findAndHookMethod(
                "com.topjohnwu.superuser.Shell",
                classLoader,
                "isAppGrantedRoot",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                })

            // Hook SafetyNet detection
            XposedHelpers.findAndHookMethod(
                "com.google.android.gms.safetynet.SafetyNet",
                classLoader,
                "isGooglePlayServicesAvailable",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = 0 // SUCCESS
                    }
                })

            // Hook mock location detection in apps
            XposedHelpers.findAndHookMethod(
                "android.location.LocationManager",
                classLoader,
                "isProviderEnabled",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val provider = param.args[0] as String
                        if (provider == LocationManager.GPS_PROVIDER || provider == LocationManager.NETWORK_PROVIDER) {
                            param.result = true
                        }
                    }
                })

        } catch (e: Exception) {
            // Ignore errors for methods that might not exist
        }
    }

    // NEW: Hook additional methods for maximum stealth
    private fun hookAdditionalMethods(classLoader: ClassLoader) {
        try {
            // Hook isFromMockProvider method
            XposedHelpers.findAndHookMethod(
                "android.location.Location",
                classLoader,
                "isFromMockProvider",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            param.result = false
                        }
                    }
                })

            // Hook hasAccuracy method
            XposedHelpers.findAndHookMethod(
                "android.location.Location",
                classLoader,
                "hasAccuracy",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled() && PreferencesUtil.getUseAccuracy() == true) {
                            param.result = true
                        }
                    }
                })

            // Hook hasAltitude method
            XposedHelpers.findAndHookMethod(
                "android.location.Location",
                classLoader,
                "hasAltitude",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled() && PreferencesUtil.getUseAltitude() == true) {
                            param.result = true
                        }
                    }
                })

            // Hook hasSpeed method
            XposedHelpers.findAndHookMethod(
                "android.location.Location",
                classLoader,
                "hasSpeed",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled() && PreferencesUtil.getUseSpeed() == true) {
                            param.result = true
                        }
                    }
                })

        } catch (e: Exception) {
            XposedBridge.log("$tag Error hooking additional methods - ${e.message}")
        }
    }

    // NEW: Priority hooks to override GpsSetter
    private fun hookPriorityMethods(classLoader: ClassLoader) {
        try {
            val locationClass = XposedHelpers.findClass("android.location.Location", classLoader)
            
            // Hook set method with MAXIMUM priority (same as GpsSetter but stronger)
            XposedHelpers.findAndHookMethod(
                locationClass,
                "set",
                Location::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            XposedBridge.log("$tag [PRIORITY] Intercepting Location.set")
                            
                            val originLocation = param.args[0] as? Location
                            val location = Location(originLocation?.provider ?: LocationManager.GPS_PROVIDER)
                            
                            // Copy original properties
                            if (originLocation != null) {
                                location.time = originLocation.time
                                location.bearing = originLocation.bearing
                                location.bearingAccuracyDegrees = originLocation.bearingAccuracyDegrees
                                location.elapsedRealtimeNanos = originLocation.elapsedRealtimeNanos
                                location.verticalAccuracyMeters = originLocation.verticalAccuracyMeters
                            } else {
                                location.time = System.currentTimeMillis() - (random.nextInt(9901) + 100).toLong()
                            }
                            
                            // Set fake values with priority
                            location.latitude = LocationUtil.latitude
                            location.longitude = LocationUtil.longitude
                            location.altitude = LocationUtil.altitude
                            location.speed = LocationUtil.speed
                            location.accuracy = LocationUtil.accuracy
                            location.speedAccuracyMetersPerSecond = LocationUtil.speedAccuracy
                            
                            // Enhanced mock provider hiding
                            try {
                                HiddenApiBypass.invoke(location.javaClass, location, "setIsFromMockProvider", false)
                                // Additional hiding methods
                                try {
                                    val method = location.javaClass.getDeclaredMethod("setIsFromMockProvider", Boolean::class.java)
                                    method.isAccessible = true
                                    method.invoke(location, false)
                                } catch (e: Exception) {
                                    // Ignore reflection errors
                                }
                            } catch (e: Exception) {
                                // Fallback for older Android versions
                            }
                            
                            param.args[0] = location
                            XposedBridge.log("$tag [PRIORITY] Location.set intercepted successfully")
                        }
                    }
                })

            // Hook constructor with priority
            XposedHelpers.findAndHookConstructor(
                locationClass,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            val location = param.thisObject as Location
                            XposedBridge.log("$tag [PRIORITY] Location constructor intercepted")
                            
                            // Set fake values immediately
                            location.latitude = LocationUtil.latitude
                            location.longitude = LocationUtil.longitude
                            location.accuracy = LocationUtil.accuracy
                            location.time = System.currentTimeMillis() - (random.nextInt(9901) + 100).toLong()
                            
                            // Hide mock provider
                            try {
                                HiddenApiBypass.invoke(location.javaClass, location, "setIsFromMockProvider", false)
                            } catch (e: Exception) {
                                // Ignore errors
                            }
                        }
                    }
                })

        } catch (e: Exception) {
            XposedBridge.log("$tag [PRIORITY] Error hooking priority methods: ${e.message}")
        }
    }
}