package com.example.naymer5.screens.newad

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Category
import com.example.naymer5.models.GeoPoint
import com.example.naymer5.models.PriceItem
import com.example.naymer5.models.WorkingHours
import com.example.naymer5.utils.SupabaseClientInstance
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHotAdScreen(
    onAdCreated: () -> Unit,
    initialAnnouncementId: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var price by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf(listOf<Category>()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var durationInMinutes by remember { mutableStateOf(1440f) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var initialAnnouncement by remember { mutableStateOf<Announcement?>(null) }

    LaunchedEffect(initialAnnouncementId) {
        if (initialAnnouncementId != null) {
            try {
                val client = SupabaseClientInstance.client
                val announcement = client.from("announcements")
                    .select { filter { eq("announcement_id", initialAnnouncementId) } }
                    .decodeSingle<Announcement>()
                initialAnnouncement = announcement
            } catch (e: Exception) {
                errorMessage = "Ошибка загрузки объявления: ${e.message}"
            }
        }
    }

    LaunchedEffect(initialAnnouncement) {
        initialAnnouncement?.let { ann ->
            title = ann.title
            description = ann.description ?: ""
            price = ann.price_list.firstOrNull()?.price?.replace(" ₽", "") ?: ""
            selectedCategory = categories.find { it.category_id == ann.category_id }
            selectedLocation = ann.geo_position?.let { LatLng(it.coordinates[1], it.coordinates[0]) }
            selectedAddress = selectedLocation?.let {
                val geocoder = Geocoder(context, Locale.getDefault())
                geocoder.getFromLocation(it.latitude, it.longitude, 1)?.firstOrNull()?.locality
            }
            selectedImageUri = ann.image_url?.let { Uri.parse(it) }
//            val expiresAt = ann.expires_at?.let { Instant.parse(it) }
//            val createdAt = Instant.parse(ann.created_at)
            //durationInMinutes = expiresAt?.let { Duration.between(createdAt, it).toMinutes().toFloat() } ?: 1440f
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }

    fun Uri.toByteArray(context: Context): ByteArray {
        return context.contentResolver.openInputStream(this)?.use { it.readBytes() } ?: byteArrayOf()
    }

    LaunchedEffect(Unit) {
        try {
            val response = SupabaseClientInstance.client.postgrest["categories"]
                .select()
                .decodeList<Category>()
            categories = response
            if (initialAnnouncement == null) selectedCategory = response.firstOrNull()
        } catch (e: Exception) {
            errorMessage = "Ошибка загрузки категорий: ${e.message}"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = if (initialAnnouncement == null) "Создание горячего объявления" else "Редактирование горячего объявления",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название объявления") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedImageUri != null) "Фото прикреплено" else "Прикрепить фото")
            }
            Spacer(Modifier.height(8.dp))
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedCategory?.name ?: "Загрузка...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория объявления") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Местоположение",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            if (selectedAddress != null) {
                Text(
                    text = "Выбрано: $selectedAddress",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = { showMap = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedLocation != null) "Изменить местоположение" else "Выбрать на карте")
            }
            Spacer(Modifier.height(8.dp))
            TextField(
                value = price,
                onValueChange = { price = it.filter { char -> char.isDigit() } },
                label = { Text("Цена") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("Введите стоимость") }
            )
            Spacer(Modifier.height(16.dp))
            val totalMinutes = durationInMinutes.toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            Text(
                text = "Время жизни объявления: ${hours} ч ${minutes} мин",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Slider(
                value = durationInMinutes,
                onValueChange = { durationInMinutes = it },
                valueRange = 1f..1440f,
                steps = 1439,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Button(
                onClick = {
                    when {
                        title.isBlank() -> errorMessage = "Название объявления не может быть пустым"
                        price.isBlank() -> errorMessage = "Цена не может быть пустой"
                        selectedLocation == null -> errorMessage = "Выберите местоположение на карте"
                        selectedCategory == null -> errorMessage = "Выберите категорию"
                        else -> {
                            errorMessage = null
                            val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
                            if (userId == null) {
                                errorMessage = "Пользователь не аутентифицирован"
                                return@Button
                            }
                            coroutineScope.launch {
                                try {
                                    val priceList = listOf(PriceItem(price = "$price ₽"))
                                    val duration = durationInMinutes.toInt().minutes
                                    val geoPosition = selectedLocation?.let {
                                        GeoPoint(
                                            type = "Point",
                                            coordinates = listOf(it.longitude, it.latitude)
                                        )
                                    }
                                    val announcementId = initialAnnouncement?.announcement_id ?: UUID.randomUUID().toString()
                                    var imageUrl: String? = initialAnnouncement?.image_url

                                    if (selectedImageUri != null && selectedImageUri.toString() != initialAnnouncement?.image_url) {
                                        val byteArray = selectedImageUri!!.toByteArray(context)
                                        val key = "$announcementId.jpg"
                                        SupabaseClientInstance.client.storage
                                            .from("announcementimages")
                                            .upload(key, byteArray) {
                                                upsert = true
                                            }
                                        imageUrl = SupabaseClientInstance.client.storage
                                            .from("announcementimages")
                                            .publicUrl(key)
                                    }

                                    val announcement = Announcement(
                                        announcement_id = announcementId,
                                        user_id = userId,
                                        title = title,
                                        description = description,
                                        category_id = selectedCategory!!.category_id,
                                        price_list = priceList,
                                        geo_position = geoPosition,
                                        working_hours = null,
                                        status = "check",
                                        type = "hot",
                                        created_at = initialAnnouncement?.created_at ?: Clock.System.now().toString(),
                                        updated_at = Clock.System.now().toString(),
                                        expires_at = Clock.System.now().plus(duration).toString(),
                                        image_url = imageUrl
                                    )
                                    if (initialAnnouncement != null) {
                                        SupabaseClientInstance.client.postgrest["announcements"]
                                            .update(announcement) { filter { eq("announcement_id", announcementId) } }
                                    } else {
                                        SupabaseClientInstance.client.postgrest["announcements"].insert(announcement)
                                    }
                                    onAdCreated()
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка: ${e.message}"
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (initialAnnouncement == null) "Разместить объявление" else "Сохранить изменения")
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showMap) {
        MapPickerScreen(
            onLocationSelected = { latLng, address ->
                selectedLocation = latLng
                selectedAddress = address
                showMap = false
            }
        )
    }
}