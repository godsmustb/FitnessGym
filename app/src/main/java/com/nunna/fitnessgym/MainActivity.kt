package com.nunna.fitnessgym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nunna.fitnessgym.data.ContentRepo
import com.nunna.fitnessgym.ui.screens.ExerciseScreen
import com.nunna.fitnessgym.ui.screens.LibraryScreen
import com.nunna.fitnessgym.ui.screens.MusclesScreen
import com.nunna.fitnessgym.ui.screens.StatusScreen
import com.nunna.fitnessgym.ui.theme.FitnessGymTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BuildConfigInfo.versionName = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "dev"
        setContent { FitnessGymTheme { AppRoot() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("library", "Exercises", Icons.Filled.FitnessCenter),
    Tab("muscles", "Muscles", Icons.Filled.Accessibility),
    Tab("status", "Status", Icons.Filled.Info),
)

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val repo = remember { ContentRepo.get(ctx) }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Scaffold(
        bottomBar = {
            if (route in TABS.map { it.route }) NavigationBar {
                for (t in TABS) NavigationBarItem(
                    selected = route == t.route,
                    onClick = {
                        nav.navigate(t.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(t.icon, contentDescription = t.label) },
                    label = { Text(t.label) },
                )
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "library", modifier = Modifier.padding(pad)) {
            composable("library") { LibraryScreen(repo) { nav.navigate("exercise/$it") } }
            composable("muscles") { MusclesScreen(repo) { nav.navigate("exercise/$it") } }
            composable("status") { StatusScreen(repo) }
            composable("exercise/{id}") { e ->
                ExerciseScreen(repo, e.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
        }
    }
}
