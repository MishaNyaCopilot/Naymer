package com.example.naymer5.screens.adscreens

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Interaction
import com.example.naymer5.models.Profile
import com.example.naymer5.models.Review
import com.example.naymer5.models.ReviewInsert
import com.example.naymer5.utils.SupabaseClientInstance
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotAdScreen(
    announcementId: String,
    onBack: () -> Unit,
    onEdit: (Announcement) -> Unit
) {
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    var category by remember { mutableStateOf<com.example.naymer5.models.Category?>(null) }
    var authorProfile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var canLeaveReview by remember { mutableStateOf(false) }
    var isAuthor by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = SupabaseClientInstance.client

                val announcementResponse = client.from("announcements")
                    .select { filter { eq("announcement_id", announcementId) } }
                    .decodeSingle<Announcement>()
                announcement = announcementResponse

                val authorProfileResponse = client.from("profiles")
                    .select { filter { eq("user_id", announcementResponse.user_id) } }
                    .decodeSingle<Profile>()
                authorProfile = authorProfileResponse

                val categoryResponse = client.from("categories")
                    .select { filter { eq("category_id", announcementResponse.category_id) } }
                    .decodeSingle<com.example.naymer5.models.Category>()
                category = categoryResponse

                val reviewsResponse = client.from("reviews")
                    .select { filter { eq("announcement_id", announcementId); eq("status", "active") } }
                    .decodeList<Review>()
                    .map { review ->
                        val userResponse = client.from("profiles")
                            .select { filter { eq("user_id", review.user_id) } }
                            .decodeSingle<Profile>()
                        review.copy(userName = userResponse.name)
                    }
                reviews = reviewsResponse

                val userId = client.auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    val interactionResponse = client.from("interactions")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("announcement_id", announcementId)
                            }
                        }
                        .decodeList<Interaction>()
                    canLeaveReview = interactionResponse.isNotEmpty()
                    isAuthor = (userId == announcementResponse.user_id)
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка загрузки: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(announcementId) { loadData() }

    val onDeleteReview: (String) -> Unit = { reviewId ->
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseClientInstance.client.from("reviews")
                    .delete { filter { eq("review_id", reviewId) } }
                loadData()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Отзыв удалён", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val onReportReview: (String) -> Unit = { reviewId ->
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseClientInstance.client.from("reviews")
                    .update(mapOf("status" to "reported")) { filter { eq("review_id", reviewId) } }
                loadData()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Жалоба отправлена", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка при отправке жалобы", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Горячее объявление") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isAuthor) {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Опции")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Редактировать") },
                                onClick = {
                                    announcement?.let { onEdit(it) }
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Архивировать") },
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            SupabaseClientInstance.client.from("announcements")
                                                .update(mapOf("status" to "inactive")) {
                                                    filter { eq("announcement_id", announcementId) }
                                                }
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Объявление архивировано", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Неизвестная ошибка",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            announcement != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                            if (announcement!!.status == "inactive") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Объявление архивировано",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (authorProfile?.avatar_url != null) {
                                        AsyncImage(
                                            model = authorProfile!!.avatar_url,
                                            contentDescription = "Аватар автора",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Аватар автора",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Gray)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = authorProfile?.name ?: "Какой то рандом чел",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Репутация: ${authorProfile?.reputation_score ?: 0}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

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
                                        addresses?.get(0)?.locality ?: addresses?.get(0)?.adminArea ?: "Неизвестный адрес"
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                announcement!!.geo_position?.let { geo ->
                                    val latLng = LatLng(geo.coordinates[1], geo.coordinates[0])
                                    LaunchedEffect(latLng) {
                                        withContext(Dispatchers.IO) {
                                            address = getAddressFromLatLng(latLng)
                                        }
                                    }
                                    Text(
                                        text = "Геолокация: ${address ?: geo.coordinates.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                } ?: Text(
                                    text = "Геолокация: Не указано",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                announcement!!.expires_at?.let { expiresAt ->
                                    val formattedDate = formatDate(expiresAt)
                                    Text(
                                        text = "Истекает: $formattedDate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                if (!isAuthor && announcement!!.status == "active") {
                                    Button(
                                        onClick = {
                                            val userId =
                                                SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
                                            if (userId != null) {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    try {
                                                        val client = SupabaseClientInstance.client
                                                        client.from("interactions").insert(
                                                            mapOf(
                                                                "user_id" to userId,
                                                                "announcement_id" to announcement!!.announcement_id
                                                            )
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            canLeaveReview = true
                                                            val phoneNumber =
                                                                authorProfile?.phone_number
                                                            if (phoneNumber != null) {
                                                                val intent =
                                                                    Intent(Intent.ACTION_DIAL).apply {
                                                                        data =
                                                                            Uri.parse("tel:$phoneNumber")
                                                                    }
                                                                context.startActivity(intent)
                                                            } else {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Номер телефона не указан",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                context,
                                                                "Ошибка взаимодействия",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Авторизуйтесь для связи",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.padding(top = 16.dp)
                                    ) {
                                        Text("Связаться")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Отзывы", style = MaterialTheme.typography.titleMedium)
                        if (reviews.isEmpty()) {
                            Text("Пока нет отзывов", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            reviews.forEach { review ->
                                ReviewItem(review = review, onDelete = onDeleteReview, onReport = onReportReview)
                            }
                        }
                    }

                    if (canLeaveReview && announcement!!.status == "active") {
                        item {
                            var rating by remember { mutableStateOf(0) }
                            var reviewText by remember { mutableStateOf("") }
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text("Оставить отзыв", style = MaterialTheme.typography.titleMedium)
                                Row {
                                    (1..5).forEach { star ->
                                        IconButton(onClick = { rating = star }) {
                                            Icon(
                                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                                contentDescription = "Рейтинг $star",
                                                tint = if (star <= rating) Color.Yellow else Color.Gray
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = reviewText,
                                    onValueChange = { reviewText = it },
                                    label = { Text("Ваш отзыв") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
                                        if (userId != null && rating > 0) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                try {
                                                    val client = SupabaseClientInstance.client
                                                    val existingReviews = client.from("reviews")
                                                        .select {
                                                            filter {
                                                                eq("user_id", userId)
                                                                eq("announcement_id", announcementId)
                                                            }
                                                        }
                                                        .decodeList<Review>()
                                                    if (existingReviews.isNotEmpty()) {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Вы уже оставили отзыв на это объявление!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        client.from("reviews").insert(
                                                            ReviewInsert(
                                                                announcement_id = announcementId,
                                                                user_id = userId,
                                                                rating = rating,
                                                                text = reviewText
                                                            )
                                                        )

                                                        val updatedReviews = client.from("reviews")
                                                            .select { filter { eq("announcement_id", announcementId) } }
                                                            .decodeList<Review>()
                                                        withContext(Dispatchers.Main) {
                                                            reviews = updatedReviews.map { review ->
                                                                val userResponse = client.from("profiles")
                                                                    .select { filter { eq("user_id", review.user_id) } }
                                                                    .decodeSingle<Profile>()
                                                                review.copy(userName = userResponse.name)
                                                            }
                                                            reviewText = ""
                                                            rating = 0
                                                            Toast.makeText(context, "Отзыв отправлен", Toast.LENGTH_SHORT).show()
                                                        }

                                                        val authorUserId = announcement!!.user_id
                                                        val allAnnouncements = client.from("announcements")
                                                            .select { filter { eq("user_id", authorUserId) } }
                                                            .decodeList<Announcement>()
                                                        val announcementIds = allAnnouncements.map { it.announcement_id }
                                                        val allReviews = client.from("reviews")
                                                            .select { filter { announcementIds.contains("announcement_id") } }
                                                            .decodeList<Review>()
                                                        val averageRating = if (allReviews.isNotEmpty()) {
                                                            allReviews.map { it.rating }.average().toInt()
                                                        } else {
                                                            0
                                                        }
                                                        client.from("profiles")
                                                            .update(mapOf("reputation_score" to averageRating)) {
                                                                filter { eq("user_id", authorUserId) }
                                                            }
                                                        withContext(Dispatchers.Main) {
                                                            authorProfile = authorProfile?.copy(reputation_score = averageRating)
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Ошибка при отправке отзыва", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Пожалуйста, выберите рейтинг", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Отправить отзыв")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}