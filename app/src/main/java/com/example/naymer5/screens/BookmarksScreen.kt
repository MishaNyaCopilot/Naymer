package com.example.naymer5.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Category
import com.example.naymer5.models.Favorite
import com.example.naymer5.screens.aditems.AnnouncementItem
import com.example.naymer5.screens.aditems.HotAnnouncementItem
import com.example.naymer5.utils.SupabaseClientInstance
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.Clock
import java.util.Objects.isNull

@Composable
fun BookmarksScreen(navController: NavController) {
    val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
    if (userId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Пожалуйста, войдите, чтобы просмотреть закладки")
        }
        return
    }

    var favoriteAnnouncements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val currentTime = Clock.System.now().toString()
            val categoriesResponse = SupabaseClientInstance.client.from("categories")
                .select()
                .decodeList<Category>()
            categories = categoriesResponse

            val favorites = SupabaseClientInstance.client.from("favorite_announcements")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Favorite>()

            val favoriteAnnouncementIds = favorites.filter { it.announcement_id != null }.map { it.announcement_id!! }

            if (favoriteAnnouncementIds.isNotEmpty()) {
                val announcementsResponse = SupabaseClientInstance.client.from("announcements")
                    .select {
                        filter {
                            isIn("announcement_id", favoriteAnnouncementIds)
                        }
                    }
                    .decodeList<Announcement>()
                favoriteAnnouncements = announcementsResponse
            }

        } catch (e: Exception) {
            errorMessage = "Ошибка загрузки закладок: ${e.message}"
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
                Text(errorMessage ?: "Неизвестная ошибка")
            }
        }
        favoriteAnnouncements.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("У вас пока нет закладок")
            }
        }
        else -> {
            LazyColumn {
                if (favoriteAnnouncements.isNotEmpty()) {
                    item {
                        Text(
                            "Избранные объявления",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    items(favoriteAnnouncements) { announcement ->
                        if (announcement.type == "hot") {
                            HotAnnouncementItem(announcement = announcement, categories = categories, navController = navController)
                        } else {
                            AnnouncementItem(announcement = announcement, categories = categories, navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}