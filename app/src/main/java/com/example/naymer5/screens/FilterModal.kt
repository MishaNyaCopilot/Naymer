package com.example.naymer5.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Category
import com.example.naymer5.models.FilterCriteria
import kotlinx.serialization.Serializable

enum class SortOrder {
    DEFAULT, CHEAPER, EXPENSIVE
}

fun extractPrice(announcement: Announcement): Int {
    return announcement.price_list.firstOrNull()?.price
        ?.replace(Regex("[^\\d]"), "")
        ?.toIntOrNull() ?: 0
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreenFilterMenu(
    currentFilters: FilterCriteria,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onApplyFilters: (FilterCriteria) -> Unit,
    onResetFilters: () -> Unit
) {
    var selectedCategories by remember { mutableStateOf(currentFilters.categories ?: emptyList()) }
    //var locationInput by remember { mutableStateOf(currentFilters.location ?: "") }
    var priceFromInput by remember { mutableStateOf(currentFilters.priceFrom?.toString() ?: "") }
    var priceToInput by remember { mutableStateOf(currentFilters.priceTo?.toString() ?: "") }
    var selectedSortOrder by remember { mutableStateOf(currentFilters.sortOrder) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val availableCategories = categories.map { it.name }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтры",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = onResetFilters,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Сбросить")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Категории:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedCategories.isEmpty()) {
                        Text(
                            text = "Не выбрано",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedCategories.forEach { category ->
                                CategoryChip(
                                    category = category,
                                    onRemove = { selectedCategories = selectedCategories - category }
                                )
                            }
                        }
                    }
                }

                Box {
                    Button(
                        onClick = { categoryMenuExpanded = true },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Добавить")
                    }

                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        offset = DpOffset(0.dp, 0.dp)
                    ) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedCategories = if (selectedCategories.contains(category))
                                        selectedCategories - category
                                    else
                                        selectedCategories + category
                                },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedCategories.contains(category),
                                            onCheckedChange = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = category)
                                    }
                                }
                            )
                        }

                        DropdownMenuItem(
                            onClick = { categoryMenuExpanded = false },
                            text = {
                                Text(
                                    text = "Готово",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = priceFromInput,
                    onValueChange = { priceFromInput = it },
                    label = { Text("Цена от") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )
                TextField(
                    value = priceToInput,
                    onValueChange = { priceToInput = it },
                    label = { Text("Цена до") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
            }

            Text(
                text = "Сортировка",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Column {
                listOf(
                    SortOrder.DEFAULT to "По умолчанию",
                    SortOrder.CHEAPER to "Дешевле",
                    SortOrder.EXPENSIVE to "Дороже",
                ).forEach { (order, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSortOrder = order }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedSortOrder == order,
                            onClick = { selectedSortOrder = order }
                        )
                        Text(text = label)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    onApplyFilters(
                        FilterCriteria(
                            categories = selectedCategories.takeIf { it.isNotEmpty() },
                            //location = locationInput.takeIf { it.isNotBlank() },
                            priceFrom = priceFromInput.toIntOrNull(),
                            priceTo = priceToInput.toIntOrNull(),
                            sortOrder = selectedSortOrder
                        )
                    )
                }) {
                    Text("Применить")
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: String,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}