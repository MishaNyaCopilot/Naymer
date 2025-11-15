package com.example.naymer5.screens.aditems

import android.location.Geocoder
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.naymer5.Routes
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Category
import com.example.naymer5.models.Favorite
import com.example.naymer5.screens.adscreens.calculateRemainingTime
import com.example.naymer5.utils.SupabaseClientInstance
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun AnnouncementItem(
    announcement: Announcement,
    categories: List<Category>,
    navController: NavController,
    modifier: Modifier = Modifier,
    showFavoriteButton: Boolean = true
) {
    val context = LocalContext.current
    val categoryName = categories.find { it.category_id == announcement.category_id }?.name ?: "Не указано"
    var isFavorite by remember { mutableStateOf(false) }
    val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id

    // Состояние для адреса
    var address by remember { mutableStateOf<String?>(null) }

    fun getAddressFromLatLng(latLng: LatLng): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                addr.locality ?: addr.adminArea ?: "Неизвестный адрес"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    announcement.geo_position?.let { geo ->
        val latLng = LatLng(geo.coordinates[1], geo.coordinates[0])
        LaunchedEffect(latLng) {
            withContext(Dispatchers.IO) {
                val addr = getAddressFromLatLng(latLng)
                withContext(Dispatchers.Main) {
                    address = addr
                }
            }
        }
    }

    LaunchedEffect(announcement.announcement_id) {
        if (userId != null) {
            val client = SupabaseClientInstance.client
            val response = client.from("favorite_announcements")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("announcement_id", announcement.announcement_id)
                    }
                }
                .decodeList<Favorite>()
            isFavorite = response.isNotEmpty()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                val route = if (announcement.status == "check") {
                    "moderationAd/${announcement.announcement_id}"
                } else {
                    "ad/${announcement.announcement_id}"
                }
                navController.navigate(route)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    if (announcement.image_url != null) {
                        AsyncImage(
                            model = announcement.image_url,
                            contentDescription = "Изображение объявления",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("Фото", color = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    val minPrice = announcement.price_list
                        .mapNotNull { it.price.replace(" ₽", "").toIntOrNull() }
                        .minOrNull()?.toString() ?: "Не указана"
                    Text(
                        text = "Цена: $minPrice ₽",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Категория: $categoryName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (address != null) "Геолокация: $address" else "Геолокация: Не указано",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Время работы: с ${announcement.working_hours?.start} до ${announcement.working_hours?.end}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (showFavoriteButton) {
                IconButton(
                    onClick = {
                        if (userId == null) {
                            Toast.makeText(context, "Авторизуйтесь для добавления в избранное", Toast.LENGTH_SHORT).show()
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                val client = SupabaseClientInstance.client
                                if (isFavorite) {
                                    client.from("favorite_announcements")
                                        .delete {
                                            filter {
                                                eq("user_id", userId)
                                                eq("announcement_id", announcement.announcement_id)
                                            }
                                        }
                                } else {
                                    client.from("favorite_announcements")
                                        .insert(
                                            Favorite(
                                                user_id = userId,
                                                announcement_id = announcement.announcement_id
                                            )
                                        )
                                }
                                isFavorite = !isFavorite
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun HotAnnouncementItem(
    announcement: Announcement,
    categories: List<Category>,
    navController: NavController,
    modifier: Modifier = Modifier,
    showFavoriteButton: Boolean = true
) {
    val context = LocalContext.current
    val categoryName = categories.find { it.category_id == announcement.category_id }?.name ?: "Не указано"
    val remainingTime = calculateRemainingTime(announcement.expires_at)
    var isFavorite by remember { mutableStateOf(false) }
    val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id

    var address by remember { mutableStateOf<String?>(null) }

    fun getAddressFromLatLng(latLng: LatLng): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                addr.locality ?: addr.adminArea ?: "Неизвестный адрес"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    announcement.geo_position?.let { geo ->
        val latLng = LatLng(geo.coordinates[1], geo.coordinates[0])
        LaunchedEffect(latLng) {
            withContext(Dispatchers.IO) {
                val addr = getAddressFromLatLng(latLng)
                withContext(Dispatchers.Main) {
                    address = addr
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (userId != null) {
            val client = SupabaseClientInstance.client
            val response = client.from("favorite_announcements")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("announcement_id", announcement.announcement_id)
                    }
                }
                .decodeList<Favorite>()
            isFavorite = response.isNotEmpty()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navController.navigate("${Routes.HOT_AD}/${announcement.announcement_id}")
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    if (announcement.image_url != null) {
                        AsyncImage(
                            model = announcement.image_url,
                            contentDescription = "Изображение объявления",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("Фото", color = Color.White)
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Категория: $categoryName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (address != null) "Адрес: $address" else "Адрес: Не указано",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    announcement.price_list.firstOrNull()?.price?.let { price ->
                        Text(
                            text = "Цена: $price",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Осталось: $remainingTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red
                    )
                }
            }

            if (showFavoriteButton) {
                IconButton(
                    onClick = {
                        if (userId == null) {
                            Toast.makeText(context, "Авторизуйтесь для добавления в избранное", Toast.LENGTH_SHORT).show()
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                val client = SupabaseClientInstance.client
                                if (isFavorite) {
                                    client.from("favorite_announcements")
                                        .delete {
                                            filter {
                                                eq("user_id", userId)
                                                eq("announcement_id", announcement.announcement_id)
                                            }
                                        }
                                } else {
                                    client.from("favorite_announcements")
                                        .insert(
                                            Favorite(
                                                user_id = userId,
                                                announcement_id = announcement.announcement_id
                                            )
                                        )
                                }
                                isFavorite = !isFavorite
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }
        }
    }
}