package com.dvhamham.manager.ui.map.components

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dvhamham.data.DEFAULT_MAP_ZOOM
import com.dvhamham.data.USER_LOCATION_ZOOM
import com.dvhamham.data.WORLD_MAP_ZOOM
import com.dvhamham.data.model.LatLng
import com.dvhamham.manager.ui.map.DialogState
import com.dvhamham.manager.ui.map.LoadingState
import com.dvhamham.manager.ui.map.MapViewModel
import com.dvhamham.manager.ui.theme.LocalThemeManager
import com.utsman.osmandcompose.*
import org.osmdroid.util.GeoPoint

@Composable
fun MapViewContainer(
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current
    val themeManager = LocalThemeManager.current
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme by themeManager.isDarkMode.collectAsState()

    // Extract state from uiState
    val loadingState = uiState.loadingState
    val lastClickedLocation = uiState.lastClickedLocation
    val isPlaying = uiState.isPlaying
    val mapZoom = uiState.mapZoom
    val userLocation = uiState.userLocation

    // Convert LatLng to GeoPoint for OSM
    val lastClickedGeoPoint = lastClickedLocation?.let { 
        GeoPoint(it.latitude, it.longitude) 
    }
    val userGeoPoint = userLocation?.let { 
        GeoPoint(it.latitude, it.longitude) 
    }

    // Handle map events and updates
    var cameraState by remember { 
        mutableStateOf(
            CameraState(
                geoPoint = lastClickedGeoPoint ?: userGeoPoint ?: GeoPoint(0.0, 0.0),
                zoom = (mapZoom ?: DEFAULT_MAP_ZOOM).toFloat()
            )
        )
    }

    HandleCenterMapEvent(userGeoPoint, mapViewModel, cameraState) { newCameraState ->
        cameraState = newCameraState
    }
    HandleGoToPointEvent(mapViewModel, cameraState) { newCameraState ->
        cameraState = newCameraState
    }

    // Display loading spinner or MapView
    if (loadingState == LoadingState.Loading) {
        LoadingSpinner()
    } else {
        DisplayOSMMap(
            lastClickedGeoPoint = lastClickedGeoPoint,
            userGeoPoint = userGeoPoint,
            isDarkTheme = isDarkTheme,
            isPlaying = isPlaying,
            onMapClick = { geoPoint ->
                val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                mapViewModel.updateClickedLocation(latLng)
            },
            mapViewModel = mapViewModel,
            cameraState = cameraState,
            onCameraChange = { newCameraState ->
                cameraState = newCameraState
                mapViewModel.updateMapZoom(newCameraState.zoom.toDouble())
                mapViewModel.updateMapPosition(
                    newCameraState.geoPoint.latitude,
                    newCameraState.geoPoint.longitude
                )
            }
        )
    }
}

@Composable
private fun HandleCenterMapEvent(
    userGeoPoint: GeoPoint?,
    mapViewModel: MapViewModel,
    cameraState: CameraState,
    onCameraChange: (CameraState) -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        mapViewModel.centerMapEvent.collect {
            try {
                val currentUserLocation = mapViewModel.uiState.value.userLocation
                
                if (currentUserLocation != null) {
                    val geoPoint = GeoPoint(currentUserLocation.latitude, currentUserLocation.longitude)
                    val newCameraState = CameraState(
                        geoPoint = geoPoint,
                        zoom = USER_LOCATION_ZOOM.toFloat()
                    )
                    onCameraChange(newCameraState)
                } else {
                    mapViewModel.refreshUserLocation()
                    kotlinx.coroutines.delay(300)
                    
                    val refreshedLocation = mapViewModel.uiState.value.userLocation
                    if (refreshedLocation != null) {
                        val geoPoint = GeoPoint(refreshedLocation.latitude, refreshedLocation.longitude)
                        val newCameraState = CameraState(
                            geoPoint = geoPoint,
                            zoom = USER_LOCATION_ZOOM.toFloat()
                        )
                        onCameraChange(newCameraState)
                    } else {
                        Toast.makeText(context, "User location not available. Please check location permissions and GPS.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error moving to location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun HandleGoToPointEvent(
    mapViewModel: MapViewModel,
    cameraState: CameraState,
    onCameraChange: (CameraState) -> Unit
) {
    LaunchedEffect(Unit) {
        mapViewModel.goToPointEvent.collect { latLng ->
            try {
                val geoPoint = GeoPoint(latLng.latitude, latLng.longitude)
                
                // Update clicked location for marker
                mapViewModel.updateClickedLocation(latLng)
                
                // Move camera to the selected location
                val newCameraState = CameraState(
                    geoPoint = geoPoint,
                    zoom = 15f
                )
                onCameraChange(newCameraState)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}

@Composable
private fun DisplayOSMMap(
    lastClickedGeoPoint: GeoPoint?,
    userGeoPoint: GeoPoint?,
    isDarkTheme: Boolean,
    isPlaying: Boolean,
    onMapClick: (GeoPoint) -> Unit,
    mapViewModel: MapViewModel,
    cameraState: CameraState,
    onCameraChange: (CameraState) -> Unit
) {
    val markerState = rememberMarkerState()
    
    // Update marker position when clicked location changes
    LaunchedEffect(lastClickedGeoPoint) {
        lastClickedGeoPoint?.let { point ->
            markerState.geoPoint = point
        }
    }

    OpenStreetMap(
        modifier = Modifier.fillMaxSize(),
        cameraState = cameraState,
        onMapClick = { geoPoint ->
            onMapClick(geoPoint)
        },
        onMapCameraMove = { newCameraState ->
            onCameraChange(newCameraState)
        },
        properties = MapProperties(
            isMultiTouchEnabled = true,
            isFlingEnabled = true,
            isZoomEnabled = true,
            tileSources = if (isDarkTheme) {
                TileSourceFactory.WIKIMEDIA
            } else {
                TileSourceFactory.MAPNIK
            }
        )
    ) {
        // Show clicked location marker
        lastClickedGeoPoint?.let { location ->
            Marker(
                state = MarkerState(geoPoint = location),
                title = "Selected Location",
                snippet = "Lat: ${location.latitude}, Lng: ${location.longitude}"
            )
        }
    }
}

@Composable
private fun LoadingSpinner() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Updating Map...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}