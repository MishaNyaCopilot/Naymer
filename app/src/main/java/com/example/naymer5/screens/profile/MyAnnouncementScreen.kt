package com.example.naymer5.screens.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.naymer5.models.Announcement
import com.example.naymer5.utils.SupabaseClientInstance
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.Objects.isNull

@Composable
fun MyAnnouncementsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
    if (userId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Пожалуйста, войдите, чтобы просмотреть объявления")
        }
        return
    }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }

    LaunchedEffect(userId) {
        try {
            val currentTime = Clock.System.now().toString()
            val response = SupabaseClientInstance.client.from("announcements")
                .select { filter {
                    eq("user_id", userId)
                    or {
                        isNull("expires_at")
                        gt("expires_at", currentTime)
                    }
                } }
                .decodeList<Announcement>()
            announcements = response
        } catch (e: Exception) {
            errorMessage = "Ошибка загрузки объявлений: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Мои объявления", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
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
            announcements.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("У вас пока нет активных объявлений")
                }
            }
            else -> {
                LazyColumn {
                    items(announcements) { announcement ->
                        AnnouncementCard(announcement = announcement, onDeleteClick = {
                            announcementToDelete = announcement
                            showDeleteDialog = true
                        })
                    }
                }
            }
        }
    }
    DeleteConfirmationDialog(
        show = showDeleteDialog,
        onConfirm = {
            coroutineScope.launch {
                try {
                    SupabaseClientInstance.client.from("interactions")
                        .delete { filter { eq("announcementId", announcementToDelete!!.announcement_id) } }

                    SupabaseClientInstance.client.from("reviews")
                        .delete { filter { eq("announcement_id", announcementToDelete!!.announcement_id) } }

                    SupabaseClientInstance.client.from("announcements")
                        .delete { filter { eq("announcement_id", announcementToDelete!!.announcement_id) } }

                    announcements = announcements.filter { it.announcement_id != announcementToDelete!!.announcement_id }

                    Toast.makeText(context, "Объявление удалено", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка при удалении: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            showDeleteDialog = false
            announcementToDelete = null
        },
        onDismiss = {
            showDeleteDialog = false
            announcementToDelete = null
        }
    )
}

@Composable
fun AnnouncementCard(announcement: Announcement, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = announcement.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = announcement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить объявление",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}