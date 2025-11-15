package com.example.naymer5.screens.profile

import com.example.naymer5.models.Announcement
import com.example.naymer5.models.Review
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun updateUserReputation(userId: String, client: SupabaseClient) {
    withContext(Dispatchers.IO) {
        val announcements = client.postgrest["announcements"]
            .select { filter { eq("user_id", userId) } }
            .decodeList<Announcement>()

        val reviews = announcements.flatMap { announcement ->
            client.postgrest["reviews"]
                .select { filter { eq("announcement_id", announcement.announcement_id) } }
                .decodeList<Review>()
        }

        val averageRating = if (reviews.isNotEmpty()) {
            reviews.map { it.rating }.average()
        } else {
            0.0
        }

        val finalRating = averageRating.coerceIn(0.0, 5.0).toInt()

        client.postgrest["profiles"]
            .update(mapOf("reputation_score" to finalRating)) {
                filter { eq("user_id", userId) }
            }
    }
}