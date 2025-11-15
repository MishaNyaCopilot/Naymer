package com.example.naymer5.screens.moderation

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Category
import com.example.naymer5.utils.SupabaseClientInstance
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ModerationHotAdCheck(announcementId: String, navController: NavController) {
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    var category by remember { mutableStateOf<Category?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(announcementId) {
        try {
            val client = SupabaseClientInstance.client
            val announcementResponse = client.from("announcements")
                .select { filter { eq("announcement_id", announcementId) } }
                .decodeSingle<Announcement>()

            if (announcementResponse.status != "check") {
                errorMessage = "Объявление не находится на модерации"
                isLoading = false
                return@LaunchedEffect
            }
            announcement = announcementResponse

            val categoryResponse = client.from("categories")
                .select { filter { eq("category_id", announcementResponse.category_id) } }
                .decodeSingle<Category>()
            category = categoryResponse
        } catch (e: Exception) {
            errorMessage = "Ошибка загрузки: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        errorMessage != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage ?: "Неизвестная ошибка", color = MaterialTheme.colorScheme.error)
            }
        }
        announcement != null && category != null -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Gray, shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (announcement!!.image_url != null) {
                            AsyncImage(
                                model = announcement!!.image_url,
                                contentDescription = "Изображение объявления",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Фото", color = Color.White)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = announcement!!.title,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Категория: ${category!!.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (announcement!!.price_list.isNotEmpty()) {
                                Text(
                                    text = "Цена: ${announcement!!.price_list.first().price}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Text(
                                text = "Описание: ${announcement!!.description}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )

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

                            announcement!!.geo_position?.let { geo ->
                                val latLng = LatLng(geo.coordinates[1], geo.coordinates[0])
                                LaunchedEffect(latLng) {
                                    withContext(Dispatchers.IO) {
                                        val addr = getAddressFromLatLng(latLng)
                                        withContext(Dispatchers.Main) {
                                            address = addr
                                        }
                                    }
                                }

                                if (address != null) {
                                    Text(
                                        text = "Геолокация: $address",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Геолокация: ${geo.coordinates.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            } ?: Text(
                                text = "Геолокация: Не указано",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Text(
                                text = "Время работы: с ${announcement!!.working_hours?.start} до ${announcement!!.working_hours?.end}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            announcement!!.working_hours?.days?.let { days ->
                                Text(
                                    text = "Дни работы: ${days.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            announcement!!.expires_at?.let {
                                Text(
                                    text = "Истекает: $it",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val client = SupabaseClientInstance.client
                                    client.from("announcements")
                                        .update(mapOf("status" to "active")) {
                                            filter { eq("announcement_id", announcementId) }
                                        }
                                    withContext(Dispatchers.Main) {
                                        navController.popBackStack()
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка публикации: ${e.message}"
                                }
                            }
                        }) {
                            Text("Опубликовать")
                        }
                        Button(onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val client = SupabaseClientInstance.client
                                    client.from("announcements")
                                        .update(mapOf("status" to "rejected")) {
                                            filter { eq("announcement_id", announcementId) }
                                        }
                                    withContext(Dispatchers.Main) {
                                        navController.popBackStack()
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка отклонения: ${e.message}"
                                }
                            }
                        }) {
                            Text("Отклонить")
                        }
                    }
                }
            }
        }
    }
}