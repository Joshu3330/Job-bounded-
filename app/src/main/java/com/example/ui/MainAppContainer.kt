package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import com.example.ai.AiTailorService
import com.example.data.model.JobApplication
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.JobPosting
import com.example.data.repository.JobSearchRepository
import com.example.ui.auth.performSignOut
import com.example.ui.home.HomeScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.search.JobSearchScreen
import com.example.ui.tailor.TailorScreen
import com.example.ui.tracker.ApplicationDetailScreen
import com.example.ui.tracker.ApplicationsListScreen
import kotlinx.coroutines.CoroutineScope

enum class Screen {
    HOME,
    SEARCH,
    TAILOR,
    TRACKER,
    PROFILE,
    DETAIL
}

@Composable
fun MainAppContainer(
    repository: FirestoreRepository,
    aiService: AiTailorService,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val searchRepository = remember { JobSearchRepository() }

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedApplication by remember { mutableStateOf<JobApplication?>(null) }
    var selectedJobForTailoring by remember { mutableStateOf<JobPosting?>(null) }

    // Handle back button when on sub-screen
    if (currentScreen != Screen.HOME) {
        BackHandler {
            if (currentScreen == Screen.DETAIL) {
                currentScreen = Screen.TRACKER
                selectedApplication = null
            } else {
                currentScreen = Screen.HOME
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.DETAIL) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.HOME,
                        onClick = { currentScreen = Screen.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_item_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.SEARCH,
                        onClick = { currentScreen = Screen.SEARCH },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Find Jobs") },
                        label = { Text("Find Jobs") },
                        modifier = Modifier.testTag("nav_item_search")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.TAILOR,
                        onClick = { currentScreen = Screen.TAILOR },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Tailor") },
                        label = { Text("Tailor") },
                        modifier = Modifier.testTag("nav_item_tailor")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.TRACKER,
                        onClick = { currentScreen = Screen.TRACKER },
                        icon = { Icon(Icons.Default.Work, contentDescription = "Tracker") },
                        label = { Text("Tracker") },
                        modifier = Modifier.testTag("nav_item_tracker")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.PROFILE,
                        onClick = { currentScreen = Screen.PROFILE },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        modifier = Modifier.testTag("nav_item_profile")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    repository = repository,
                    onNavigateToTailor = {
                        selectedJobForTailoring = null
                        currentScreen = Screen.TAILOR
                    },
                    onNavigateToSearch = { currentScreen = Screen.SEARCH },
                    onNavigateToApplications = { currentScreen = Screen.TRACKER },
                    onNavigateToProfile = { currentScreen = Screen.PROFILE },
                    onSelectApplication = { app ->
                        selectedApplication = app
                        currentScreen = Screen.DETAIL
                    },
                    onSignOut = {
                        performSignOut(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onComplete = onSignOutComplete
                        )
                    }
                )
                Screen.SEARCH -> JobSearchScreen(
                    searchRepository = searchRepository,
                    onSelectJobForTailoring = { job ->
                        selectedJobForTailoring = job
                        currentScreen = Screen.TAILOR
                    }
                )
                Screen.TAILOR -> TailorScreen(
                    repository = repository,
                    aiService = aiService,
                    initialJobPosting = selectedJobForTailoring,
                    onApplicationSaved = {
                        selectedJobForTailoring = null
                        currentScreen = Screen.TRACKER
                    }
                )
                Screen.TRACKER -> ApplicationsListScreen(
                    repository = repository,
                    onSelectApplication = { app ->
                        selectedApplication = app
                        currentScreen = Screen.DETAIL
                    },
                    onNewApplicationClick = {
                        selectedJobForTailoring = null
                        currentScreen = Screen.TAILOR
                    }
                )
                Screen.PROFILE -> ProfileScreen(
                    repository = repository,
                    onNavigateBack = { currentScreen = Screen.HOME }
                )
                Screen.DETAIL -> {
                    val app = selectedApplication
                    if (app != null) {
                        ApplicationDetailScreen(
                            initialApp = app,
                            repository = repository,
                            onNavigateBack = {
                                selectedApplication = null
                                currentScreen = Screen.TRACKER
                            }
                        )
                    } else {
                        currentScreen = Screen.TRACKER
                    }
                }
            }
        }
    }
}
