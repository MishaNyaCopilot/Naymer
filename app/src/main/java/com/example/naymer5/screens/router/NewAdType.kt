package com.example.naymer5.screens.router

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.naymer5.models.Profile
import com.example.naymer5.utils.SupabaseClientInstance
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAdTypeSheet(
    onDismiss: () -> Unit,
    onRegularAd: () -> Unit,
    onHotAd: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var profile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val userId = SupabaseClientInstance.client.auth.currentSessionOrNull()?.user?.id
            if (userId != null) {
                val profileResponse = SupabaseClientInstance.client.postgrest["profiles"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingle<Profile>()
                profile = profileResponse
            } else {
                errorMessage = "Пользователь не аутентифицирован"
            }
        } catch (e: Exception) {
            errorMessage = "Ошибка при получении данных: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    if (!isLoading) {
        if (profile == null || profile!!.phone_number.isNullOrEmpty()) {
            LaunchedEffect(Unit) {
                Toast.makeText(context, "Сначала укажите номер телефона в профиле", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            return
        }
    }

    if (isLoading) {
        Text("Загрузка данных...")
        return
    }
    if (errorMessage != null) {
        Text("Ошибка: $errorMessage")
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Выберите тип объявления", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRegularAd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Обычное объявление")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onHotAd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Горячее объявление")
            }
        }
    }
}