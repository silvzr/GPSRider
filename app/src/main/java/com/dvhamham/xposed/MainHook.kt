// MainHook.kt
package com.dvhamham.xposed

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.dvhamham.data.MANAGER_APP_PACKAGE_NAME
import com.dvhamham.xposed.hooks.LocationApiHooks
import com.dvhamham.xposed.hooks.SystemServicesHooks
import com.dvhamham.xposed.utils.PreferencesUtil
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MainHook : IXposedHookLoadPackage {
    private val tag = "[GPSRider-MainHook]"
    private val DEBUG = false // Disabled for better stealth

    lateinit var context: Context

    private var locationApiHooks: LocationApiHooks? = null
    private var systemServicesHooks: SystemServicesHooks? = null

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Avoid hooking own app to prevent recursion
        if (lpparam.packageName == MANAGER_APP_PACKAGE_NAME) {
            return
        }

        // Check if fake location is enabled
        val isPlaying = PreferencesUtil.getIsPlaying()
        
        // If not playing or null, do not proceed with hooking
        if (isPlaying != true) {
            return
        }

        // IMMEDIATE hooks for maximum priority and speed
        initImmediateHooks(lpparam)

        // Enhanced system services hooking with HIGHEST priority
        if (lpparam.packageName == "android" && PreferencesUtil.getHookSystemLocation()) {
            systemServicesHooks = SystemServicesHooks(lpparam).also { it.initHooks() }
        }

        // Initialize enhanced hooking logic
        initEnhancedHookingLogic(lpparam)
    }

    // NEW: Immediate hooks for maximum priority and speed
    private fun initImmediateHooks(lpparam: LoadPackageParam) {
        // Hook Location class methods IMMEDIATELY for speed
        try {
            val locationClass = XposedHelpers.findClass("android.location.Location", lpparam.classLoader)
            
            // Hook getLatitude with MAXIMUM priority
            XposedHelpers.findAndHookMethod(
                locationClass,
                "getLatitude",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            // Update location before getting coordinates
                            com.dvhamham.xposed.utils.LocationUtil.updateLocation()
                            param.result = com.dvhamham.xposed.utils.LocationUtil.latitude
                        }
                    }
                })

            // Hook getLongitude with MAXIMUM priority
            XposedHelpers.findAndHookMethod(
                locationClass,
                "getLongitude",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PreferencesUtil.isFakeLocationEnabled()) {
                            // Update location before getting coordinates
                            com.dvhamham.xposed.utils.LocationUtil.updateLocation()
                            param.result = com.dvhamham.xposed.utils.LocationUtil.longitude
                        }
                    }
                })

            // Hook isFromMockProvider with MAXIMUM priority
            XposedHelpers.findAndHookMethod(
                locationClass,
                "isFromMockProvider",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                })

        } catch (e: Exception) {
            // Ignore errors for speed
        }
    }

    private fun initEnhancedHookingLogic(lpparam: LoadPackageParam) {
        // Hook Application.onCreate for better stealth
        XposedHelpers.findAndHookMethod(
            "android.app.Instrumentation",
            lpparam.classLoader,
            "callApplicationOnCreate",
            Application::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        context = (param.args[0] as Application).applicationContext
                        
                        // Initialize location hooks with enhanced stealth
                        locationApiHooks = LocationApiHooks(lpparam).also { it.initHooks() }
                        
                        // Show toast to confirm GPS Rider is active
                        Toast.makeText(context, "GPS Rider Active!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // Ignore errors for speed
                    }
                }
            }
        )

        // Additional hook for Activity.onCreate for better coverage
        XposedHelpers.findAndHookMethod(
            "android.app.Activity",
            lpparam.classLoader,
            "onCreate",
            "android.os.Bundle",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // Ensure location hooks are active
                    if (locationApiHooks == null && PreferencesUtil.isFakeLocationEnabled()) {
                        locationApiHooks = LocationApiHooks(lpparam).also { it.initHooks() }
                    }
                }
            }
        )
    }
}