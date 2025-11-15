package com.example.naymer5.screens.moderation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Category
import com.example.naymer5.models.Profile
import com.example.naymer5.models.Review
import com.example.naymer5.screens.aditems.AnnouncementItem
import com.example.naymer5.utils.SupabaseClientInstance
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Composable
fun ModerationScreen(navController: NavController) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val client = SupabaseClientInstance.client

            val announcementResponse = withContext(Dispatchers.IO) {
                client.from("announcements")
                    .select { filter { eq("status", "check") } }
                    .decodeList<Announcement>()
            }
            announcements = announcementResponse

            val reviewsResponse = withContext(Dispatchers.IO) {
                client.from("reviews")
                    .select { filter { eq("status", "reported") } }
                    .decodeList<Review>()
            }
            reviews = reviewsResponse.map { review ->
                val userResponse = withContext(Dispatchers.IO) {
                    client.from("profiles")
                        .select { filter { eq("user_id", review.user_id) } }
                        .decodeSingle<Profile>()
                }
                review.copy(userName = userResponse.name ?: "Неизвестный")
            }

            val categoryResponse = withContext(Dispatchers.IO) {
                client.postgrest["categories"]
                    .select()
                    .decodeList<Category>()
            }
            categories = categoryResponse

        } catch (e: Exception) {
            errorMessage = "Ошибка загрузки: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = errorMessage ?: "Неизвестная ошибка")
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "Модерация объявлений",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(announcements) { announcement ->
                        AnnouncementItem(
                            announcement = announcement,
                            categories = categories,
                            navController = navController
                        )
                    }
                }


                val context = LocalContext.current
                Text(
                    "Модерация отзывов",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(reviews) { review ->
                        ReviewModerationItem(
                            review = review,
                            onApprove = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        SupabaseClientInstance.client.from("reviews")
                                            .update(mapOf("status" to "active", "is_verified" to true)) {
                                                filter { eq("review_id", review.review_id) }
                                            }
                                        withContext(Dispatchers.Main) {
                                            reviews = reviews.filter { it.review_id != review.review_id }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ModerationScreen", "Ошибка при одобрении: ${e.message}")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onDelete = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        SupabaseClientInstance.client.from("reviews")
                                            .delete { filter { eq("review_id", review.review_id) } }
                                        reviews = reviews.filter { it.review_id != review.review_id }
                                    } catch (e: Exception) {
                                        // Ошибка
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewModerationItem(
    review: Review,
    onApprove: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Отзыв от пользователя ${review.userName ?: "Неизвестный"}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Рейтинг: ${review.rating}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Текст: ${review.text ?: "Нет текста"}", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                Log.d("ModerationScreen", "Approving review ID: ${review.review_id}")
                                SupabaseClientInstance.client.from("reviews")
                                    .update(ReviewUpdate("active", true)) {
                                        filter { eq("review_id", review.review_id) }
                                    }
                                withContext(Dispatchers.Main) {
                                    onApprove()
                                    Toast.makeText(context, "Отзыв одобрен", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                // Error
                            }
                        }
                    }
                ) {
                    Text("Одобрить")
                }
                TextButton(onClick = onDelete) {
                    Text("Удалить")
                }
            }
        }
    }
}

@Serializable
data class ReviewUpdate(
    val status: String,
    val is_verified: Boolean
)