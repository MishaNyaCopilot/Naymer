package com.example.naymer5.navbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.naymer5.utils.SupabaseClientInstance
import io.github.jan.supabase.auth.auth

enum class NavBarItem {
    Home, HotAds, NewAd, Bookmarks, Profile
}

@Composable
fun BottomNavBar(
    selectedItem: NavBarItem,
    onItemSelected: (NavBarItem) -> Unit,
    onShowAuthOptions: () -> Unit,
    userRole: String?
) {
    val isUserAuthenticated = SupabaseClientInstance.client.auth.currentSessionOrNull() != null

    Column {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
        NavigationBar(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBarItem(
                selected = selectedItem == NavBarItem.Home,
                onClick = { onItemSelected(NavBarItem.Home) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Главная"
                    )
                },
                label = {
                    Text(
                        "Главная",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }
            )
            NavigationBarItem(
                selected = selectedItem == NavBarItem.HotAds,
                onClick = { onItemSelected(NavBarItem.HotAds) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Горячие объявления"
                    )
                },
                label = {
                    Text(
                        "Горячие",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }
            )
            NavigationBarItem(
                selected = selectedItem == NavBarItem.NewAd,
                onClick = { onItemSelected(NavBarItem.NewAd) },
                icon = {
                    if (userRole == "moderator") {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Модерация"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Новое объявление"
                        )
                    }
                },
                label = {
                    if (userRole == "moderator") {
                        Text(
                            "Модерация",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(IntrinsicSize.Min)
                        )
                    }
                },
                alwaysShowLabel = userRole == "moderator"
            )
            NavigationBarItem(
                selected = selectedItem == NavBarItem.Bookmarks,
                onClick = { onItemSelected(NavBarItem.Bookmarks) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Закладки"
                    )
                },
                label = {
                    Text(
                        "Закладки",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }
            )
            NavigationBarItem(
                selected = selectedItem == NavBarItem.Profile,
                onClick = {
                    if (!isUserAuthenticated) {
                        onShowAuthOptions()
                    } else {
                        onItemSelected(NavBarItem.Profile)
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Профиль"
                    )
                },
                label = {
                    Text(
                        "Профиль",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }
            )
        }
    }
}