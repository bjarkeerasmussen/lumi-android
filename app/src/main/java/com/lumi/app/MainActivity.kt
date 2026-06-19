package com.lumi.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lumi.app.data.LumiStore
import com.lumi.app.data.Prefs
import com.lumi.app.ui.nav.Routes
import com.lumi.app.ui.nav.Tab
import com.lumi.app.ui.screens.LearnScreen
import com.lumi.app.ui.screens.ProgressScreen
import com.lumi.app.ui.screens.RoutineScreen
import com.lumi.app.ui.screens.SettingsScreen
import com.lumi.app.ui.screens.SkinCheckScreen
import com.lumi.app.ui.screens.TodayScreen
import com.lumi.app.ui.theme.LumiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LumiStore.get(this)
        val prefs = Prefs(this)
        setContent { LumiTheme { LumiApp(store, prefs) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LumiApp(store: LumiStore, prefs: Prefs) {
    val nav = rememberNavController()
    val context = LocalContext.current

    // Ask for notification permission once (Android 13+), used by the reminder.
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored; reminder simply won't post if denied */ }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val tabs = Tab.values().toList()
    val onTab = currentRoute in tabs.map { it.route }

    Scaffold(
        topBar = {
            val title = when (currentRoute) {
                Routes.SKIN_CHECK -> "Skin Check"
                Routes.SETTINGS -> "Settings"
                else -> tabs.firstOrNull { it.route == currentRoute }?.let { "Lumi · ${it.label}" } ?: "Lumi"
            }
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(),
                actions = {
                    if (onTab) {
                        IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Tab.Today.route)
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Tab.Today.route,
            modifier = Modifier.padding(inner)
        ) {
            composable(Tab.Today.route) {
                TodayScreen(
                    store = store,
                    onOpenSkinCheck = { nav.navigate(Routes.SKIN_CHECK) },
                    requestNotifPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
            composable(Tab.Progress.route) { ProgressScreen(store) }
            composable(Tab.Routine.route) { RoutineScreen(store) }
            composable(Tab.Learn.route) { LearnScreen() }
            composable(Routes.SKIN_CHECK) {
                SkinCheckScreen(store = store, onDone = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(store = store, prefs = prefs, onBack = { nav.popBackStack() })
            }
        }
    }
}
