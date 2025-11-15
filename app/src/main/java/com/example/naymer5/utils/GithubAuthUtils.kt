package com.example.naymer5.utils

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Github
import kotlinx.coroutines.launch

suspend fun signInWithGithub() {
    SupabaseClientInstance.client.auth.signInWith(Github)
}

@Composable
fun GithubSignInButton() {
    val coroutineScope = rememberCoroutineScope()

    Button(onClick = {
        coroutineScope.launch {
            signInWithGithub()
        }
    }) {
        Text("Войти через GitHub")
    }
}