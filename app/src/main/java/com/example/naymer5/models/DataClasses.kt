package com.example.naymer5.models

import com.example.naymer5.screens.SortOrder
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val review_id: String,
    val announcement_id: String,
    val user_id: String,
    val rating: Int,
    val text: String?,
    val is_verified: Boolean,
    val status: String, // Добавлено
    val created_at: String?,
    val updated_at: String?,
    val userName: String? = null
)

@Serializable
data class Interaction(
    val interaction_id: String,
    val user_id: String,
    val announcement_id: String,
    val created_at: String
)

@Serializable
data class ReviewInsert(
    val announcement_id: String,
    val user_id: String,
    val rating: Int,
    val text: String
)

@Serializable
data class Favorite(
    val favorite_id: String? = null,
    val user_id: String,
    val announcement_id: String? = null,
    val category_id: String? = null
)

@Serializable
data class FilterCriteria(
    val categories: List<String>? = null,
    val city: String? = null,  // Поле для города
    val priceFrom: Int? = null,
    val priceTo: Int? = null,
    val sortOrder: SortOrder = SortOrder.DEFAULT
)

@Serializable
data class GeoPoint(
    val type: String,
    val coordinates: List<Double>
)

@Serializable
data class Category(
    val category_id: String,
    val name: String
)

@Serializable
data class PriceItem(
    val name: String? = null,
    val price: String
)

@Serializable
data class WorkingHours(
    val days: List<String>? = null,
    val start: String,
    val end: String
)

@Serializable
data class Announcement(
    val announcement_id: String,
    val user_id: String,
    val title: String,
    val description: String,
    val category_id: String,
    val price_list: List<PriceItem>,
    val geo_position: GeoPoint?,
    val working_hours: WorkingHours?,
    val status: String,
    val type: String,
    val created_at: String,
    val updated_at: String,
    val expires_at: String? = null,
    val image_url: String? = null
)

@Serializable
data class Profile(
    val user_id: String,
    val name: String?,
    val email: String?,
    val phone_number: String?,
    val address: String?,
    val geo_position: GeoPoint?,
    val reputation_score: Int,
    val role: String,
    val avatar_url: String?
)