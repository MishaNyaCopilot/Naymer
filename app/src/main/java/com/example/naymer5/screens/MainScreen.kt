package com.example.naymer5.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Category
import com.example.naymer5.models.FilterCriteria
import com.example.naymer5.screens.aditems.AnnouncementItem
import com.example.naymer5.utils.SupabaseClientInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.Objects.isNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    var loadedAnnouncements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterCriteria by remember { mutableStateOf(FilterCriteria()) }
    var selectedAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var isMapFullScreen by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }
    var hasMoreAnnouncements by remember { mutableStateOf(true) }
    val pageSize = 10

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun isHotAnnouncementActive(announcement: Announcement): Boolean {
        if (announcement.type != "hot") return true
        val expiresAt = announcement.expires_at?.let { Instant.parse(it) } ?: return false
        return expiresAt > Clock.System.now()
    }

    val mapAnnouncements = loadedAnnouncements.filter { announcement ->
        announcement.status == "active" && (announcement.type == "regular" || isHotAnnouncementActive(announcement))
    }

    val regularAnnouncements = loadedAnnouncements.filter { it.type == "regular" }

    val filteredAnnouncements = regularAnnouncements.filter { announcement ->
        announcement.title.contains(searchText, ignoreCase = true) &&
                (filterCriteria.categories.isNullOrEmpty() || filterCriteria.categories!!.contains(
                    categories.find { it.category_id == announcement.category_id }?.name
                )) &&
                (filterCriteria.priceFrom == null || extractPrice(announcement) >= filterCriteria.priceFrom!!) &&
                (filterCriteria.priceTo == null || extractPrice(announcement) <= filterCriteria.priceTo!!)
    }

    val sortedAnnouncements = when (filterCriteria.sortOrder) {
        SortOrder.CHEAPER -> filteredAnnouncements.sortedBy { extractPrice(it) }
        SortOrder.EXPENSIVE -> filteredAnnouncements.sortedByDescending { extractPrice(it) }
        else -> filteredAnnouncements
    }

    fun loadNextPage() {
        scope.launch {
            try {
                val client = SupabaseClientInstance.client
                val newAnnouncements = withContext(Dispatchers.IO) {
                    client.from("announcements")
                        .select {
                            filter { eq("status", "active") }
                            range((currentPage * pageSize).toLong(), ((currentPage + 1) * pageSize - 1).toLong())
                        }
                        .decodeList<Announcement>()
                }
                if (newAnnouncements.size < pageSize) {
                    hasMoreAnnouncements = false
                }
                loadedAnnouncements = loadedAnnouncements + newAnnouncements
                currentPage++
                val categoryResponse = withContext(Dispatchers.IO) {
                    client.postgrest["categories"]
                        .select()
                        .decodeList<Category>()
                }
                categories = categoryResponse
            } catch (e: Exception) {
                errorMessage = "Ошибка загрузки: ${e.message}"
                hasMoreAnnouncements = false
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadNextPage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && loadedAnnouncements.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage ?: "Неизвестная ошибка")
                }
            }
            else -> {
                if (isMapFullScreen) {
                    AnnouncementsMap(
                        announcements = mapAnnouncements,
                        onMarkerClick = { announcement -> selectedAnnouncement = announcement },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            label = { Text("Поиск по названию") },
                            leadingIcon = {
                                IconButton(onClick = { showFilterSheet = true }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Фильтр")
                                }
                            },
                            trailingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Поиск")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        if (loadedAnnouncements.isEmpty() && !isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Нет объявлений")
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(sortedAnnouncements) { announcement ->
                                    AnnouncementItem(
                                        announcement = announcement,
                                        categories = categories,
                                        navController = navController
                                    )
                                }
                                if (sortedAnnouncements.isEmpty() && !isLoading) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            Text("Нет объявлений, соответствующих фильтрам")
                                        }
                                    }
                                }
                                if (hasMoreAnnouncements) {
                                    item {
                                        Button(
                                            onClick = {
                                                isLoading = true
                                                loadNextPage()
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                                        ) {
                                            Text("Загрузить еще")
                                        }
                                    }
                                }
                                if (isLoading) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { isMapFullScreen = !isMapFullScreen },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Icon(
                imageVector = if (isMapFullScreen) Icons.Default.List else Icons.Default.Map,
                contentDescription = if (isMapFullScreen) "Список" else "Карта"
            )
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            MainScreenFilterMenu(
                currentFilters = filterCriteria,
                categories = categories,
                onDismiss = { showFilterSheet = false },
                onApplyFilters = { criteria ->
                    filterCriteria = criteria
                    showFilterSheet = false
                },
                onResetFilters = {
                    filterCriteria = FilterCriteria()
                    showFilterSheet = false
                }
            )
        }
    }

    if (selectedAnnouncement != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedAnnouncement = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AnnouncementItem(
                announcement = selectedAnnouncement!!,
                categories = categories,
                navController = navController
            )
        }
    }
}

@Composable
fun AnnouncementsMap(
    announcements: List<Announcement>,
    onMarkerClick: (Announcement) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val announcementsState = rememberUpdatedState(announcements)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLastKnownLocation(context, fusedLocationClient) { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            }
        } else {
            Toast.makeText(context, "Нет разрешений - нет карты", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.apply {
                    onCreate(null)
                    onResume()
                    getMapAsync { map ->
                        googleMap = map
                        updateMap(map, announcementsState.value, onMarkerClick)
                    }
                }
            },
            update = { view ->
                googleMap?.let { map ->
                    updateMap(map, announcementsState.value, onMarkerClick)
                }
            },
            modifier = Modifier
                .border(1.dp, Color.Gray)
        )

        FloatingActionButton(
            onClick = {
                if (hasLocationPermission(context)) {
                    getLastKnownLocation(context, fusedLocationClient) { location ->
                        location?.let {
                            val userLatLng = LatLng(it.latitude, it.longitude)
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                        }
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Мое местоположение")
        }
    }
}

private fun updateMap(
    map: GoogleMap,
    announcements: List<Announcement>,
    onMarkerClick: (Announcement) -> Unit
) {
    map.clear()
    announcements.forEach { announcement ->
        announcement.geo_position?.let { geoPoint ->
            val latLng = LatLng(geoPoint.coordinates[1], geoPoint.coordinates[0])
            val marker = map.addMarker(MarkerOptions().position(latLng).title(announcement.title))
            marker?.tag = announcement
        }
    }
    map.setOnMarkerClickListener { marker ->
        val announcement = marker.tag as? Announcement
        announcement?.let {
            onMarkerClick(it)
        }
        true
    }
    if (announcements.isNotEmpty()) {
        val boundsBuilder = LatLngBounds.Builder()
        announcements.forEach { announcement ->
            announcement.geo_position?.let { geoPoint ->
                val latLng = LatLng(geoPoint.coordinates[1], geoPoint.coordinates[0])
                boundsBuilder.include(latLng)
            }
        }
        try {
            val bounds = boundsBuilder.build()
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (e: Exception) {
            // Исключение
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun getLastKnownLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Location?) -> Unit
) {
    if (!hasLocationPermission(context)) {
        onLocationReceived(null)
        return
    }
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        onLocationReceived(null)
        return
    }
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            onLocationReceived(location)
        }
        .addOnFailureListener {
            onLocationReceived(null)
        }
}