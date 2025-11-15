package com.example.naymer5.screens.adscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Category
import com.example.naymer5.models.Profile
import com.example.naymer5.screens.aditems.HotAnnouncementItem
import com.example.naymer5.utils.SupabaseClientInstance
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Objects.isNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotAdsScreen(navController: NavController) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var appliedRadius by remember { mutableStateOf<Double?>(null) }
    var sliderValue by remember { mutableStateOf(50f) }
    var isUnlimited by remember { mutableStateOf(true) }
    var distanceInput by remember { mutableStateOf("0") }
    var unit by remember { mutableStateOf("км") }
    var isUnitDropdownExpanded by remember { mutableStateOf(false) }
    var userGeoPosition by remember { mutableStateOf<String?>(null) }

    var showFilterSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
        if (userId != null) {
            try {
                val profile = SupabaseClientInstance.client.from("profiles").select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeSingle<Profile>()
                userGeoPosition = profile.geo_position?.let {
                    "POINT(${it.coordinates[0]} ${it.coordinates[1]})"
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка загрузки профиля: ${e.message}"
            }
        }
    }

    LaunchedEffect(searchQuery, appliedRadius, userGeoPosition) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = SupabaseClientInstance.client
                val params = mutableMapOf<String, Any?>().apply {
                    put("search_text", if (searchQuery.isNotEmpty()) searchQuery else null)
                    if (appliedRadius != null && userGeoPosition != null) {
                        put("user_geo", userGeoPosition)
                        put("radius", appliedRadius)
                    } else {
                        put("user_geo", null)
                        put("radius", null)
                    }
                }

                val jsonParams = buildJsonObject {
                    params.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Double -> put(key, value)
                            null -> put(key, null)
                            else -> throw IllegalArgumentException("Неподдерживаемый тип для ключа: $key")
                        }
                    }
                }

                val response = client.postgrest.rpc("get_hot_announcements", jsonParams)
                    .decodeList<Announcement>()
                announcements = response
            } catch (e: Exception) {
                errorMessage = "Ошибка загрузки: ${e.message}"
            } finally {
                isLoading = false
            }
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
                Text(text = errorMessage ?: "Неизвестная ошибка")
            }
        }
        else -> {
            Column {
                Text(
                    text = "Горячие объявления",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
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
                LazyColumn {
                    items(announcements) { announcement ->
                        HotAnnouncementItem(announcement, categories, navController)
                    }
                    if (announcements.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Нет объявлений")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isUnlimited = !isUnlimited }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = isUnlimited,
                        onCheckedChange = { isUnlimited = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = if (isUnlimited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Не ограничено",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (isUnlimited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                if (!isUnlimited) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = distanceInput,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    distanceInput = newValue
                                }
                            },
                            label = { Text("Расстояние") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = isUnitDropdownExpanded,
                            onExpandedChange = { isUnitDropdownExpanded = !isUnitDropdownExpanded },
                            modifier = Modifier.width(120.dp)
                        ) {
                            TextField(
                                value = unit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Единица", style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitDropdownExpanded)
                                },
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .height(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = isUnitDropdownExpanded,
                                onDismissRequest = { isUnitDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("метры", style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        unit = "м"
                                        isUnitDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("километры", style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        unit = "км"
                                        isUnitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isUnlimited) {
                                appliedRadius = null
                            } else {
                                val distance = distanceInput.toDoubleOrNull()
                                if (distance != null) {
                                    appliedRadius = if (unit == "м") distance / 1000 else distance
                                } else {
                                    appliedRadius = null
                                }
                            }
                            showFilterSheet = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = "Применить",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            isUnlimited = true
                            distanceInput = ""
                            unit = "км"
                            appliedRadius = null
                            searchQuery = ""
                            showFilterSheet = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Сбросить",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

fun calculateRemainingTime(expiresAt: String?): String {
    if (expiresAt == null) return "Не указано"
    val expiresInstant = Instant.parse(expiresAt)
    val now = Clock.System.now()
    val duration = expiresInstant - now
    return if (duration.isNegative()) {
        "Истекло"
    } else {
        duration.toComponents { days, hours, minutes, _, _ ->
            "$days д $hours ч $minutes мин"
        }
    }
}