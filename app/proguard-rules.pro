# Add project specific ProGuard rules here.

# Keep Xposed modules
-keep class com.dvhamham.xposed.** { *; }
-keep class com.dvhamham.data.** { *; }
-keep class com.dvhamham.manager.** { *; }

# Keep Gson classes
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Location classes
-keep class android.location.** { *; }

# Keep Xposed classes
-keep class de.robv.android.xposed.** { *; }
-keep class org.lsposed.** { *; }

# Remove debug logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Optimize performance
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Remove unused code
-dontwarn **
-ignorewarnings

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep OSM classes
-keep class org.osmdroid.** { *; }
-keep class com.utsman.osmandcompose.** { *; }

# Keep DataStore classes
-keep class androidx.datastore.** { *; }

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# Keep Compose UI classes
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }

# Keep Navigation classes
-keep class androidx.navigation.** { *; }

# Keep our app classes
-keep class com.dvhamham.** { *; }