package com.example.naymer5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.naymer5.navbar.AuthOptionsSheet
import com.example.naymer5.navbar.BottomNavBar
import com.example.naymer5.navbar.NavBarItem
import com.example.naymer5.screens.*
import com.example.naymer5.screens.adscreens.AdScreen
import com.example.naymer5.screens.adscreens.HotAdScreen
import com.example.naymer5.screens.adscreens.HotAdsScreen
import com.example.naymer5.screens.moderation.ModerationRegularAdCheck
import com.example.naymer5.screens.moderation.ModerationScreen
import com.example.naymer5.screens.profile.ProfileScreen
import com.example.naymer5.screens.newad.CreateHotAdScreen
import com.example.naymer5.screens.newad.CreateRegularAdScreen
import com.example.naymer5.screens.router.NewAdTypeSheet
import com.example.naymer5.utils.AppTheme
import com.example.naymer5.utils.SupabaseClientInstance
import com.example.naymer5.utils.getUserRole
import com.vk.id.VKID
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import java.util.Locale

object Routes {
    const val MAIN = "main"
    const val CREATE_REGULAR_AD = "createRegularAd"
    const val CREATE_HOT_AD = "createHotAd"
    const val HOT = "hot"
    const val BOOKMARKS = "bookmarks"
    const val PROFILE = "profile"
    const val AD = "ad"
    const val MODERATION = "moderation"
    const val HOT_AD = "hotAd"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VKID.init(this)
        VKID.instance.setLocale(Locale("ru"))
        setContent {
            AppTheme {
                MyApp()
            }
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    var selectedItem by remember { mutableStateOf(NavBarItem.Home) }
    var showAuthOptions by remember { mutableStateOf(false) }
    var authSession by remember { mutableStateOf(SupabaseClientInstance.client.auth.currentSessionOrNull()) }
    var showNewAdTypeSheet by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val currentSession = SupabaseClientInstance.client.auth.currentSessionOrNull()
            if (currentSession != authSession) {
                authSession = currentSession
                if (currentSession != null) {
                    showAuthOptions = false
                    selectedItem = NavBarItem.Profile
                    userRole = getUserRole()
                } else {
                    userRole = null
                }
            }
            delay(1000)
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route)
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    selectedItem = item
                    when (item) {
                        NavBarItem.Home -> navigateTo(Routes.MAIN)
                        NavBarItem.HotAds -> navigateTo(Routes.HOT)
                        NavBarItem.NewAd -> {
                            if (userRole == "moderator") {
                                navigateTo(Routes.MODERATION)
                            } else {
                                showNewAdTypeSheet = true
                            }
                        }
                        NavBarItem.Bookmarks -> navigateTo(Routes.BOOKMARKS)
                        NavBarItem.Profile -> {
                            if (SupabaseClientInstance.client.auth.currentSessionOrNull() == null) {
                                showAuthOptions = true
                            } else {
                                navigateTo(Routes.PROFILE)
                            }
                        }
                    }
                },
                onShowAuthOptions = { showAuthOptions = true },
                userRole = userRole
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.MAIN) { MainScreen(navController = navController) }
            composable(Routes.CREATE_REGULAR_AD) {
                CreateRegularAdScreen(
                    onAdCreated = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.CREATE_HOT_AD) {
                CreateHotAdScreen(
                    onAdCreated = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOT) { HotAdsScreen(navController = navController) }
            composable(Routes.BOOKMARKS) { BookmarksScreen(navController = navController) }
            composable(Routes.PROFILE) { ProfileScreen(navController = navController) }

            composable("${Routes.AD}/{announcementId}") { backStackEntry ->
                val announcementId = backStackEntry.arguments?.getString("announcementId") ?: ""
                AdScreen(
                    announcementId = announcementId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("editAd/$announcementId") }
                )
            }

            composable("moderationAd/{announcementId}") { backStackEntry ->
                val announcementId = backStackEntry.arguments?.getString("announcementId") ?: ""
                ModerationRegularAdCheck(
                    announcementId = announcementId,
                    navController = navController
                )
            }
            composable(Routes.MODERATION) { ModerationScreen(navController = navController) }

            composable("${Routes.HOT_AD}/{announcementId}") { backStackEntry ->
                val announcementId = backStackEntry.arguments?.getString("announcementId") ?: ""
                HotAdScreen(
                    announcementId = announcementId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("editHotAd/$announcementId") }
                )
            }

            composable("editAd/{announcementId}") { backStackEntry ->
                val announcementId = backStackEntry.arguments?.getString("announcementId") ?: ""
                CreateRegularAdScreen(
                    onAdCreated = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    },
                    initialAnnouncementId = announcementId
                )
            }

            composable("editHotAd/{announcementId}") { backStackEntry ->
                val announcementId = backStackEntry.arguments?.getString("announcementId") ?: ""
                CreateHotAdScreen(
                    onAdCreated = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    },
                    initialAnnouncementId = announcementId
                )
            }
        }

        if (showAuthOptions) {
            AuthOptionsSheet(onDismiss = { showAuthOptions = false })
        }
        if (showNewAdTypeSheet) {
            NewAdTypeSheet(
                onDismiss = { showNewAdTypeSheet = false },
                onRegularAd = {
                    showNewAdTypeSheet = false
                    navigateTo(Routes.CREATE_REGULAR_AD)
                },
                onHotAd = {
                    showNewAdTypeSheet = false
                    navigateTo(Routes.CREATE_HOT_AD)
                }
            )
        }
    }
}