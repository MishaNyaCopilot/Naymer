package com.example.naymer5.screens.newad

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
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
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRegularAdScreen(
    onAdCreated: () -> Unit,
    initialAnnouncementId: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf(listOf<Category>()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val services = remember { mutableStateListOf<Pair<String, String>>() }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var newServicePrice by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var initialAnnouncement by remember { mutableStateOf<Announcement?>(null) }

    val startTimeState = rememberTimePickerState(
        initialHour = initialAnnouncement?.working_hours?.start?.split(":")?.get(0)?.toInt() ?: 0,
        initialMinute = initialAnnouncement?.working_hours?.start?.split(":")?.get(1)?.toInt() ?: 0,
        is24Hour = true
    )

    val endTimeState = rememberTimePickerState(
        initialHour = initialAnnouncement?.working_hours?.end?.split(":")?.get(0)?.toInt() ?: 0,
        initialMinute = initialAnnouncement?.working_hours?.end?.split(":")?.get(1)?.toInt() ?: 0,
        is24Hour = true
    )

    LaunchedEffect(Unit) {
        try {
            val response = SupabaseClientInstance.client.postgrest["categories"]
                .select()
                .decodeList<Category>()
            categories = response
            selectedCategory = response.firstOrNull()
        } catch (e: Exception) {
            errorMessage = "Ошибка загрузки категорий: ${e.message}"
        }
    }

    LaunchedEffect(initialAnnouncementId) {
        if (initialAnnouncementId != null) {
            val client = SupabaseClientInstance.client
            val announcement = client.from("announcements")
                .select { filter { eq("announcement_id", initialAnnouncementId) } }
                .decodeSingle<Announcement>()
            initialAnnouncement = announcement
        }
    }

    LaunchedEffect(initialAnnouncement) {
        initialAnnouncement?.let { ann ->
            title = ann.title
            description = ann.description ?: ""
            selectedCategory = categories.find { it.category_id == ann.category_id }
            selectedDays = ann.working_hours?.days?.toSet() ?: emptySet()
            services.clear()
            services.addAll(ann.price_list.mapNotNull { priceItem ->
                priceItem.name?.let { name -> name to priceItem.price.replace(" ₽", "") }
            })
            selectedImageUri = ann.image_url?.let { Uri.parse(it) }
            selectedLocation = ann.geo_position?.let { LatLng(it.coordinates[1], it.coordinates[0]) }
            selectedAddress = selectedLocation?.let {
                val geocoder = Geocoder(context, Locale.getDefault())
                geocoder.getFromLocation(it.latitude, it.longitude, 1)?.firstOrNull()?.locality
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }

    fun Uri.toByteArray(context: Context): ByteArray {
        return context.contentResolver.openInputStream(this)?.use { it.readBytes() } ?: byteArrayOf()
    }

    fun formatTime(hours: Int, minutes: Int): String = String.format("%02d:%02d", hours, minutes)
    fun filterDigitsOnly(text: String): String = text.filter { it.isDigit() }

    if (showAddServiceDialog) {
        AlertDialog(
            onDismissRequest = { showAddServiceDialog = false },
            title = { Text("Добавить услугу") },
            text = {
                Column {
                    TextField(
                        value = newServiceName,
                        onValueChange = { newServiceName = it },
                        label = { Text("Название услуги") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = newServicePrice,
                        onValueChange = { newServicePrice = filterDigitsOnly(it) },
                        label = { Text("Стоимость") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newServiceName.isNotBlank() && newServicePrice.isNotBlank()) {
                            services.add(newServiceName to newServicePrice)
                            newServiceName = ""
                            newServicePrice = ""
                            showAddServiceDialog = false
                        }
                    }
                ) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { showAddServiceDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onCancel = { showStartTimePicker = false },
            onConfirm = { showStartTimePicker = false }
        ) { TimePicker(state = startTimeState) }
    }
    if (showEndTimePicker) {
        TimePickerDialog(
            onCancel = { showEndTimePicker = false },
            onConfirm = { showEndTimePicker = false }
        ) { TimePicker(state = endTimeState) }
    }

    if (showMapPicker) {
        Dialog(onDismissRequest = { showMapPicker = false }) {
            MapPickerScreen(onLocationSelected = { latLng, address ->
                selectedLocation = latLng
                selectedAddress = address
                showMapPicker = false
            })
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = if (initialAnnouncement == null) "Создание объявления" else "Редактирование объявления",
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
                text = selectedAddress?.let { "Выбранный адрес: $it" } ?: "Адрес не выбран",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showMapPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выбрать адрес на карте")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Список услуг",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showAddServiceDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Добавить услугу") }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(services, key = { it.first + it.second }) { (name, price) ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        shadowElevation = 2.dp
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "$price ₽",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { services.remove(name to price) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Удалить"
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(text = "Дни работы", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    val isSelected = selectedDays.contains(day)
                    Column(
                        modifier = Modifier.padding(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = {
                                selectedDays = if (it) selectedDays + day else selectedDays - day
                            }
                        )
                        Text(text = day)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Время работы:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showStartTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Начало: ${formatTime(startTimeState.hour, startTimeState.minute)}")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showEndTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Окончание: ${formatTime(endTimeState.hour, endTimeState.minute)}")
            }
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
                        selectedCategory == null -> errorMessage = "Выберите категорию"
                        services.isEmpty() -> errorMessage = "Добавьте хотя бы одну услугу"
                        else -> {
                            errorMessage = null
                            val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
                            if (userId == null) {
                                errorMessage = "Пользователь не аутентифицирован"
                                return@Button
                            }
                            val priceList = services.map { PriceItem(name = it.first, price = "${it.second} ₽") }
                            val workingHours = WorkingHours(
                                days = selectedDays.toList(),
                                start = formatTime(startTimeState.hour, startTimeState.minute),
                                end = formatTime(endTimeState.hour, endTimeState.minute)
                            )
                            val geoPosition: GeoPoint? = selectedLocation?.let {
                                GeoPoint(
                                    type = "Point",
                                    coordinates = listOf(it.longitude, it.latitude)
                                )
                            }
                            coroutineScope.launch {
                                try {
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
                                        working_hours = workingHours,
                                        status = "check",
                                        type = "regular",
                                        created_at = initialAnnouncement?.created_at ?: Clock.System.now().toString(),
                                        updated_at = Clock.System.now().toString(),
                                        expires_at = initialAnnouncement?.expires_at,
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
}

@Composable
fun TimePickerDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onCancel) { Text("Отмена") }
                    Button(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}