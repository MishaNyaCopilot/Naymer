package com.example.naymer5.utils

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

val SUPABASE_URL = "SUPABASE_URL_HERE"
val SUPABASE_ANON_KEY = "ANON_KEY_HERE"

object SupabaseClientInstance {
    @OptIn(SupabaseInternal::class)
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Storage)
            install(ComposeAuth) {
                googleNativeLogin(serverClientId = "server-client-id-here")
            }
        }
    }
}