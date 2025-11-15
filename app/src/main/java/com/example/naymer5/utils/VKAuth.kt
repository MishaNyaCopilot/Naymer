package com.example.naymer5.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.naymer5.models.Profile
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthUiParams
import com.vk.id.onetap.common.OneTapStyle
import com.vk.id.onetap.compose.onetap.OneTap
import com.vk.id.onetap.compose.onetap.OneTapTitleScenario
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

//@Composable
//fun ScreenWithVKIDButton() {
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//
//    OneTap(
//        onAuth = { oAuth, token ->
//            scope.launch {
//                try {
//                    val client = HttpClient()
//                    val response: HttpResponse = client.get("https://api.vk.com/method/users.get") {
//                        parameter("access_token", token.token)
//                        parameter("v", "5.131")
//                        parameter("fields", "first_name,last_name") // Убрали email
//                    }
//
//                    val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
//                    val userArray = json["response"]?.jsonArray
//                    if (userArray == null || userArray.isEmpty()) {
//                        println("Ошибка: данные пользователя не получены")
//                        return@launch
//                    }
//
//                    val user = userArray[0].jsonObject
//                    val firstName = user["first_name"]?.jsonPrimitive?.content
//                    val lastName = user["last_name"]?.jsonPrimitive?.content
//
//                    if (firstName == null || lastName == null) {
//                        println("Ошибка: обязательные поля (имя или фамилия) отсутствуют")
//                        return@launch
//                    }
//
//                    val userId = UUID.randomUUID().toString()
//
//                    val profile = Profile(
//                        user_id = userId,
//                        name = "$firstName $lastName",
//                        email = null,
//                        phone_number = null,
//                        address = null,
//                        reputation_score = 0,
//                        role = "user"
//                    )
//
//                    SupabaseClientInstance.client.from("profiles").insert(profile)
//
//                    println("Пользователь успешно создан в базе данных")
//                } catch (e: Exception) {
//                    println("Ошибка при создании пользователя: ${e.message}")
//                }
//            }
//        },
//        onFail = { oAuth, fail ->
//            when (fail) {
//                is VKIDAuthFail.Canceled -> println("Пользователь отменил авторизацию")
//                is VKIDAuthFail.FailedApiCall -> println("Ошибка API: ${fail.description}")
//                is VKIDAuthFail.FailedOAuthState -> println("Ошибка OAuth")
//                is VKIDAuthFail.FailedRedirectActivity -> println("Ошибка редиректа")
//                is VKIDAuthFail.NoBrowserAvailable -> println("Браузер недоступен")
//                is VKIDAuthFail.FailedOAuth -> println("Ошибка OAuth: ${fail.description}")
//            }
//        },
//        scenario = OneTapTitleScenario.SignIn,
//        signInAnotherAccountButtonEnabled = true,
//        style = OneTapStyle.Light(),
//        fastAuthEnabled = true
//    )
//}