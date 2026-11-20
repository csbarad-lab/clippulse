package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.ai.ClipPulseAiService
import com.example.data.repository.ClipPulseRepository
import com.example.data.youtube.YouTubeMetadataService
import com.example.ui.editor.ClipEditorScreen
import com.example.ui.editor.ClipEditorViewModel
import com.example.ui.hooklab.HookLabScreen
import com.example.ui.hooklab.HookLabViewModel
import com.example.ui.library.ClipsLibraryScreen
import com.example.ui.studio.StudioScreen
import com.example.ui.studio.StudioViewModel
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TextMuted
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_STUDIO = "studio"
const val ROUTE_LIBRARY = "library"
const val ROUTE_HOOK_LAB = "hook_lab"
const val ROUTE_CLIP_EDITOR = "clip_editor/{projectId}/{clipId}"

private data class NavTabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val studioTabs = listOf(
    NavTabItem(ROUTE_STUDIO, R.string.tab_studio, Icons.Filled.ContentCut, Icons.Outlined.ContentCut),
    NavTabItem(ROUTE_LIBRARY, R.string.tab_library, Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    NavTabItem(ROUTE_HOOK_LAB, R.string.tab_hook_lab, Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh)
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedStudioShell(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedStudioShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    var libraryProjectFilterId by remember { mutableStateOf<String?>(null) }

    val studioViewModel: StudioViewModel = viewModel(
        key = "studio_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                StudioViewModel(
                    repository = ClipPulseRepository(db),
                    youtubeService = YouTubeMetadataService(),
                    aiService = ClipPulseAiService(),
                    currentUserId = user.uid
                )
            }
        }
    )

    val hookLabViewModel: HookLabViewModel = viewModel(
        key = "hooklab_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                HookLabViewModel(
                    repository = ClipPulseRepository(db),
                    aiService = ClipPulseAiService(),
                    currentUserId = user.uid
                )
            }
        }
    )

    val showChrome = currentRoute == ROUTE_STUDIO || currentRoute == ROUTE_LIBRARY || currentRoute == ROUTE_HOOK_LAB

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = StudioObsidian,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (showChrome) {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { signOutUser(credentialManager, scope) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = stringResource(R.string.sign_out),
                                    tint = TextMuted
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = StudioSlate,
                            titleContentColor = Color.White
                        )
                    )
                }
            },
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(
                        containerColor = StudioSlate,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        studioTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab.route != ROUTE_LIBRARY) libraryProjectFilterId = null
                                    navController.navigate(tab.route) {
                                        popUpTo(ROUTE_STUDIO) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CoralPulse,
                                    selectedTextColor = CoralPulse,
                                    indicatorColor = CoralPulse.copy(alpha = 0.16f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail(containerColor = StudioSlate) {
                        studioTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (tab.route != ROUTE_LIBRARY) libraryProjectFilterId = null
                                    navController.navigate(tab.route) {
                                        popUpTo(ROUTE_STUDIO) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CoralPulse,
                                    selectedTextColor = CoralPulse,
                                    indicatorColor = CoralPulse.copy(alpha = 0.16f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_STUDIO
                    ) {
                        composable(ROUTE_STUDIO) {
                            StudioScreen(
                                viewModel = studioViewModel,
                                onOpenProjectClips = { projectId ->
                                    libraryProjectFilterId = projectId
                                    navController.navigate(ROUTE_LIBRARY) {
                                        popUpTo(ROUTE_STUDIO) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(ROUTE_LIBRARY) {
                            ClipsLibraryScreen(
                                viewModel = studioViewModel,
                                initialProjectFilterId = libraryProjectFilterId,
                                onOpenClipEditor = { projectId, clipId ->
                                    navController.navigate("clip_editor/$projectId/$clipId")
                                }
                            )
                        }

                        composable(ROUTE_HOOK_LAB) {
                            HookLabScreen(viewModel = hookLabViewModel)
                        }

                        composable(
                            route = ROUTE_CLIP_EDITOR,
                            arguments = listOf(
                                navArgument("projectId") { type = NavType.StringType },
                                navArgument("clipId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
                            val clipId = backStackEntry.arguments?.getString("clipId").orEmpty()

                            val editorViewModel: ClipEditorViewModel = viewModel(
                                key = "editor_${user.uid}_${projectId}_$clipId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        val dbId = app.getString(R.string.firestore_database_id)
                                        val db = FirebaseFirestore.getInstance(dbId)
                                        ClipEditorViewModel(
                                            repository = ClipPulseRepository(db),
                                            aiService = ClipPulseAiService(),
                                            currentUserId = user.uid,
                                            projectId = projectId,
                                            clipId = clipId
                                        )
                                    }
                                }
                            )

                            ClipEditorScreen(
                                viewModel = editorViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
