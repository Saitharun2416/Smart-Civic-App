package com.example.smartcivic.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.smartcivic.data.models.Complaint
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberMarkerState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.ui.graphics.Color

@Composable
fun MapView(
    complaints: List<Complaint>,
    isHeatmapMode: Boolean,
    onLocationSelected: ((Double, Double, String) -> Unit)? = null,
    onPinClicked: ((Complaint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Center map around Richmond Circle, Bengaluru
    val defaultLocation = LatLng(12.97159, 77.59456)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = false
        )
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = mapUiSettings,
        properties = mapProperties,
        onMapClick = { latLng ->
            val address = "Location near Lat: %.4f, Lng: %.4f".format(latLng.latitude, latLng.longitude)
            onLocationSelected?.invoke(latLng.latitude, latLng.longitude, address)
        }
    ) {
        // Render Heatmap circles if selected
        if (isHeatmapMode) {
            complaints.forEach { complaint ->
                val center = LatLng(complaint.latitude, complaint.longitude)
                val fillColor = when (complaint.priority) {
                    "High" -> Color(0x66FF3D00)    // High priority: red translucent circle
                    "Medium" -> Color(0x44FFC107)  // Medium priority: orange translucent circle
                    else -> Color(0x2200E676)       // Low priority: green translucent circle
                }
                Circle(
                    center = center,
                    fillColor = fillColor,
                    strokeColor = Color.Transparent,
                    radius = 250.0 // radius in meters
                )
            }
        }

        // Render Complaint pins
        complaints.forEach { complaint ->
            val position = LatLng(complaint.latitude, complaint.longitude)
            val markerState = rememberMarkerState(key = complaint.id, position = position)
            
            val markerHue = when (complaint.category) {
                "Pothole" -> BitmapDescriptorFactory.HUE_ORANGE
                "Garbage Overflow" -> BitmapDescriptorFactory.HUE_RED
                "Water Leakage" -> BitmapDescriptorFactory.HUE_BLUE
                "Drainage Blockage" -> BitmapDescriptorFactory.HUE_GREEN
                "Broken Streetlight" -> BitmapDescriptorFactory.HUE_YELLOW
                "Traffic Problem" -> BitmapDescriptorFactory.HUE_VIOLET
                else -> BitmapDescriptorFactory.HUE_ROSE
            }

            Marker(
                state = markerState,
                title = complaint.title,
                snippet = "${complaint.category} - ${complaint.status}",
                icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                onClick = {
                    onPinClicked?.invoke(complaint)
                    false // Return false so the default click action (showing tooltip/centering) happens
                }
            )
        }
    }
}
