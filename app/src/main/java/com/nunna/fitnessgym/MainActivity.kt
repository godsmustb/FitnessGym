package com.nunna.fitnessgym

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nunna.fitnessgym.data.ContentRepo
import com.nunna.fitnessgym.data.db.ProfileEntity
import com.nunna.fitnessgym.ui.screens.ExerciseScreen
import com.nunna.fitnessgym.ui.screens.LibraryScreen
import com.nunna.fitnessgym.ui.screens.MeScreen
import com.nunna.fitnessgym.ui.screens.MusclesScreen
import com.nunna.fitnessgym.ui.screens.OnboardingScreen
import com.nunna.fitnessgym.ui.screens.PickerScreen
import com.nunna.fitnessgym.ui.screens.ProgressScreen
import com.nunna.fitnessgym.ui.screens.SessionScreen
import com.nunna.fitnessgym.ui.screens.StatusScreen
import com.nunna.fitnessgym.ui.screens.SummaryHolder
import com.nunna.fitnessgym.ui.screens.SummaryScreen
import com.nunna.fitnessgym.ui.screens.TodayScreen
import com.nunna.fitnessgym.ui.screens.WorkoutScreen
import com.nunna.fitnessgym.ui.theme.FitnessGymTheme
import com.nunna.fitnessgym.ui.theme.Tokens

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
    Tab("today", "Today", Icons.Filled.Home),
    Tab("library", "Exercises", Icons.Filled.FitnessCenter),
    Tab("progress", "Progress", Icons.Filled.CalendarMonth),
    Tab("muscles", "Muscles", Icons.Filled.Accessibility),
    Tab("me", "Me", Icons.Filled.Person),
)

/** Sentinel for "database not read yet" (distinct from "no profile"). */
private val LOADING = ProfileEntity(id = -1, settingsJson = "", onboarded = false, planSeed = 0, createdAt = 0)

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val repo = remember { AppGraph.repo(ctx) }
    val content = remember { ContentRepo.get(ctx) }
    val profile by repo.profileFlow.collectAsState(LOADING)
    if (profile === LOADING) {
        Box(Modifier.fillMaxSize().background(Tokens.Bg).testTag("loading"))
        return
    }
    val onboarded = profile?.onboarded == true
    // A fresh navigation graph whenever onboarding state flips (finished setup, or "Reset everything").
    key(onboarded) { AppNav(repo, content, onboarded) }
}

@Composable
private fun AppNav(repo: com.nunna.fitnessgym.data.GymRepo, content: ContentRepo, onboarded: Boolean) {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    // Ask for notification permission (rest-timer alerts) the first time a workout starts.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val askNotifications = {
        if (AppGraph.animations && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) runCatching { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    Scaffold(
        containerColor = Tokens.Bg,
        bottomBar = {
            if (route in TABS.map { it.route }) NavigationBar {
                for (t in TABS) NavigationBarItem(
                    selected = route == t.route,
                    onClick = { nav.tab(t.route) },
                    icon = { Icon(t.icon, contentDescription = t.label) },
                    label = { Text(t.label) },
                    modifier = Modifier.testTag("tab_${t.route}"),
                )
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = if (onboarded) "today" else "onboarding", modifier = Modifier.padding(pad)) {
            composable("onboarding") {
                // When setup is saved the profile becomes "onboarded" and AppRoot swaps in the main graph.
                OnboardingScreen(repo, null, editing = false, onDone = {})
            }
            composable("edit") {
                val s by repo.settingsFlow.collectAsState(null)
                s?.let { OnboardingScreen(repo, it, editing = true, onDone = { nav.popBackStack() }, onCancel = { nav.popBackStack() }) }
            }
            composable("today") {
                TodayScreen(repo, onWorkout = { nav.navigate("workout") { launchSingleTop = true } }, onExercise = { nav.navigate("exercise/$it") }, onBeforeStart = askNotifications)
            }
            composable("library") { LibraryScreen(content) { nav.navigate("exercise/$it") } }
            composable("progress") { ProgressScreen(repo) { nav.navigate("session/$it") } }
            composable("muscles") { MusclesScreen(content) { nav.navigate("exercise/$it") } }
            composable("me") {
                MeScreen(repo, onEdit = { nav.navigate("edit") }, onStatus = { nav.navigate("status") }, onExercise = { nav.navigate("exercise/$it") },
                    onReset = {})
            }
            composable("status") { StatusScreen(content) }
            composable("workout") {
                WorkoutScreen(
                    repo,
                    onFinished = { sum -> SummaryHolder.value = sum; nav.navigate("summary") { popUpTo("workout") { inclusive = true } } },
                    onPick = { seId -> nav.navigate("pick/$seId") },
                    onExercise = { nav.navigate("exercise/$it") },
                    onLeave = { nav.goToday() },
                )
            }
            composable("pick/{seId}") { e -> PickerScreen(repo, e.arguments?.getString("seId")?.toLongOrNull() ?: 0L) { nav.popBackStack() } }
            composable("summary") { SummaryScreen(SummaryHolder.value) { nav.goToday() } }
            composable("session/{id}") { e -> SessionScreen(repo, e.arguments?.getString("id")?.toLongOrNull() ?: 0L) { nav.popBackStack() } }
            composable("exercise/{id}") { e ->
                ExerciseScreen(content, repo, e.arguments?.getString("id").orEmpty(), back = { nav.popBackStack() }, onAdded = { nav.navigate("workout") { launchSingleTop = true } })
            }
        }
    }
}

private fun NavHostController.tab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Back to the Today tab from a flow screen (workout, summary): pop to it, or rebuild the stack if it's gone. */
private fun NavHostController.goToday() {
    if (!popBackStack("today", inclusive = false)) navigate("today") { popUpTo(graph.id) { inclusive = true } }
}
