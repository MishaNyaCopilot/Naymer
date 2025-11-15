package com.example.naymer5.screens.newad

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

@Composable
fun MapPickerScreen(
    onLocationSelected: (LatLng, String?) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    fun getAddressFromLatLng(latLng: LatLng): String? {
        return try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subLocality ?: address.adminArea ?: "Неизвестное место"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    selectedLocation = latLng
                    selectedAddress = getAddressFromLatLng(latLng)
                    googleMap?.apply {
                        clear()
                        addMarker(MarkerOptions().position(latLng))
                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                    showConfirmationDialog = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            update = { view ->
                view.onCreate(null)
                view.onResume()
                view.getMapAsync { map ->
                    googleMap = map
                    map.setOnMapClickListener { latLng ->
                        selectedLocation = latLng
                        selectedAddress = getAddressFromLatLng(latLng)
                        map.clear()
                        map.addMarker(MarkerOptions().position(latLng))
                        showConfirmationDialog = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val latLng = LatLng(it.latitude, it.longitude)
                            selectedLocation = latLng
                            selectedAddress = getAddressFromLatLng(latLng)
                            googleMap?.apply {
                                clear()
                                addMarker(MarkerOptions().position(latLng))
                                moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                            }
                            showConfirmationDialog = true
                        }
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Мое месторасположение")
        }

        if (showConfirmationDialog && selectedLocation != null) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                title = { Text("Подтверждение местоположения") },
                text = {
                    Text(
                        selectedAddress?.let { "Вы выбрали: $it" } ?: "Местоположение неизвестно"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onLocationSelected(selectedLocation!!, selectedAddress)
                            showConfirmationDialog = false
                        }
                    ) { Text("Подтвердить") }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmationDialog = false }
                    ) { Text("Отмена") }
                }
            )
        }
    }
}