package com.example.naymer5.screens.profile

import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Category
import com.example.naymer5.models.GeoPoint
import com.example.naymer5.models.Profile
import com.example.naymer5.screens.aditems.AnnouncementItem
import com.example.naymer5.screens.aditems.HotAnnouncementItem
import com.example.naymer5.screens.newad.MapPickerScreen
import com.example.naymer5.utils.SupabaseClientInstance
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<Profile?>(null) }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var signOutMessage by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }

    var currentCity by remember { mutableStateOf<String?>(null) }
    var showMap by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            coroutineScope.launch {
                try {
                    val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
                    if (userId != null) {
                        val timestamp = System.currentTimeMillis()
                        val fileName = "avatar_${userId}_$timestamp.jpg"

                        val currentAvatarUrl = profile?.avatar_url
                        if (currentAvatarUrl != null) {
                            try {
                                val oldFileName = currentAvatarUrl.substringAfterLast("/")
                                SupabaseClientInstance.client.storage.from("avatars").delete(oldFileName)
                            } catch (e: Exception) {
                                errorMessage = "Не удалось удалить старый аватар: ${e.message}"
                            }
                        }

                        val byteArray = inputStream?.readBytes() ?: byteArrayOf()
                        SupabaseClientInstance.client.storage.from("avatars").upload(fileName, byteArray)
                        val avatarUrl = SupabaseClientInstance.client.storage.from("avatars").publicUrl(fileName)
                        SupabaseClientInstance.client.postgrest["profiles"]
                            .update({ set("avatar_url", avatarUrl) }) { filter { eq("user_id", userId) } }
                        profile = profile?.copy(avatar_url = avatarUrl)
                    }
                } catch (e: Exception) {
                    errorMessage = "Ошибка при загрузке аватарки: ${e.message}"
                } finally {
                    inputStream?.close()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    val profileResponse = SupabaseClientInstance.client.postgrest["profiles"]
                        .select { filter { eq("user_id", userId) } }
                        .decodeSingle<Profile>()
                    profile = profileResponse
                    phoneNumber = profileResponse.phone_number ?: ""
                    val categoryResponse = SupabaseClientInstance.client.postgrest["categories"]
                        .select()
                        .decodeList<Category>()
                    categories = categoryResponse
                    val announcementsResponse = SupabaseClientInstance.client.from("announcements")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<Announcement>()
                    announcements = announcementsResponse
                    profileResponse.geo_position?.let { geoPoint ->
                        val latLng = LatLng(geoPoint.coordinates[1], geoPoint.coordinates[0])
                        val geocoder = Geocoder(context, Locale.getDefault())
                        try {
                            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                            if (addresses?.isNotEmpty() == true) {
                                val address = addresses[0]
                                currentCity = address.locality ?: address.subLocality ?: address.adminArea ?: "Неизвестное место"
                            } else {
                                currentCity = "Неизвестное место"
                            }
                        } catch (e: Exception) {
                            currentCity = "Ошибка получения города"
                        }
                    }
                } else {
                    errorMessage = "Пользователь не аутентифицирован"
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка при получении данных: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteAnnouncement(announcement: Announcement) {
        coroutineScope.launch {
            try {
                val client = SupabaseClientInstance.client
                client.from("favorite_announcements")
                    .delete { filter { eq("announcement_id", announcement.announcement_id) } }
                client.from("announcements")
                    .delete { filter { eq("announcement_id", announcement.announcement_id) } }
                announcements = announcements.filter { it.announcement_id != announcement.announcement_id }
            } catch (e: Exception) {
                errorMessage = "Ошибка при удалении: ${e.message}"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Загрузка...")
                    }
                }
                errorMessage.isNotEmpty() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Ошибка",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    profile?.let { prof ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (prof.avatar_url != null) {
                                        AsyncImage(
                                            model = prof.avatar_url,
                                            contentDescription = "Аватар",
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(CircleShape)
                                                .clickable { launcher.launch("image/*") },
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Аватар",
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clickable { launcher.launch("image/*") },
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Редактировать аватар",
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(24.dp)
                                            .background(MaterialTheme.colorScheme.background, CircleShape)
                                            .padding(4.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = prof.name ?: "Не указано", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = phoneNumber,
                                            onValueChange = { phoneNumber = it },
                                            label = { Text("Номер телефона") },
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    coroutineScope.launch {
                                                        try {
                                                            val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
                                                            if (userId != null) {
                                                                SupabaseClientInstance.client.postgrest["profiles"]
                                                                    .update({ set("phone_number", phoneNumber) }) { filter { eq("user_id", userId) } }
                                                                profile = profile?.copy(phone_number = phoneNumber)
                                                                isEditing = false
                                                            }
                                                        } catch (e: Exception) {
                                                            errorMessage = "Ошибка при сохранении: ${e.message}"
                                                        }
                                                    }
                                                }) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Сохранить номер")
                                                }
                                            }
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "Номер телефона: ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(text = prof.phone_number ?: "Не указан", style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(onClick = { isEditing = true }) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Редактировать номер телефона")
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ProfileItem("Email", prof.email ?: "Не указан")
                                    ProfileItem("Репутация", prof.reputation_score.toString())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ProfileItem("Местоположение", currentCity ?: "Не указан")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showMap = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (currentCity != null) "Изменить город" else "Выбрать город")
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Мои объявления",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (announcements.isEmpty()) {
                            Text(
                                text = "У вас пока нет объявлений",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(announcements) { announcement ->
                                    Box {
                                        if (announcement.type == "hot") {
                                            HotAnnouncementItem(
                                                announcement = announcement,
                                                categories = categories,
                                                navController = navController,
                                                modifier = Modifier.alpha(if (announcement.status == "inactive") 0.5f else 1f),
                                                showFavoriteButton = false
                                            )
                                        } else {
                                            AnnouncementItem(
                                                announcement = announcement,
                                                categories = categories,
                                                navController = navController,
                                                modifier = Modifier.alpha(if (announcement.status == "inactive") 0.5f else 1f),
                                                showFavoriteButton = false
                                            )
                                        }
                                        if (announcement.status == "inactive") {
                                            Text(
                                                text = "Неактивно",
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(8.dp)
                                                    .background(Color.White.copy(alpha = 0.7f), shape = RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                announcementToDelete = announcement
                                                showDeleteDialog = true
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Удалить объявление",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            SupabaseClientInstance.client.auth.signOut()
                            Toast.makeText(context, "Вы успешно вышли", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ошибка выхода: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выйти")
            }
        }
        DeleteConfirmationDialog(
            show = showDeleteDialog,
            onConfirm = {
                announcementToDelete?.let { deleteAnnouncement(it) }
                showDeleteDialog = false
                announcementToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                announcementToDelete = null
            }
        )
        if (showMap) {
            MapPickerScreen(
                onLocationSelected = { latLng, address ->
                    coroutineScope.launch {
                        try {
                            val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
                            if (userId != null) {
                                val geoPosition = GeoPoint(
                                    type = "Point",
                                    coordinates = listOf(latLng.longitude, latLng.latitude)
                                )
                                SupabaseClientInstance.client.postgrest["profiles"]
                                    .update({ set("geo_position", geoPosition) }) { filter { eq("user_id", userId) } }
                                profile = profile?.copy(geo_position = geoPosition)
                                currentCity = address ?: "Неизвестное место"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка при сохранении местоположения: ${e.message}"
                        } finally {
                            showMap = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}