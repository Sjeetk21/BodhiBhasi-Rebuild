package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.School
import com.example.ui.screens.revision.*
import com.example.viewmodel.RevisionDashboardViewModel
import com.example.viewmodel.RevisionSessionViewModel
import com.example.viewmodel.AnalyticsViewModel
import com.example.ui.screens.LearningAnalyticsScreen

import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelProvider
import android.app.Application
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.navigation.*
import com.example.preferences.UserPreferences
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SearchViewModel
import com.example.viewmodel.WordViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: WordViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            
            // Request notification permission for Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val permissionState = androidx.core.content.ContextCompat.checkSelfPermission(
                    LocalContext.current,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                if (permissionState != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) {}
                    LaunchedEffect(Unit) {
                        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            
            val isDark = when (themeMode) {
                UserPreferences.ThemeMode.LIGHT -> false
                UserPreferences.ThemeMode.DARK -> true
                UserPreferences.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                
                // Active bottom navigation items list
                val bottomNavItems = listOf(
                    BottomNavItem(
                        route = HomeRoute,
                        label = "Home",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home
                    ),
                    BottomNavItem(
                        route = SavedRoute,
                        label = "Saved",
                        selectedIcon = Icons.Filled.Bookmark,
                        unselectedIcon = Icons.Outlined.BookmarkBorder
                    ),
                    BottomNavItem(
                        route = RevisionDashboardRoute,
                        label = "Revise",
                        selectedIcon = Icons.Filled.School,
                        unselectedIcon = Icons.Outlined.School
                    ),
                    BottomNavItem(
                        route = HistoryRoute,
                        label = "History",
                        selectedIcon = Icons.Filled.History,
                        unselectedIcon = Icons.Outlined.History
                    ),
                    BottomNavItem(
                        route = LibraryRoute,
                        label = "Library",
                        selectedIcon = Icons.Filled.LibraryBooks,
                        unselectedIcon = Icons.Outlined.LibraryBooks
                    )
                )

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Determine if bottom bar should be visible based on current screen route
                val showBottomBar = currentDestination != null && bottomNavItems.any { item ->
                    currentDestination.hierarchy.any { it.hasRoute(item.route::class) }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                bottomNavItems.forEach { item ->
                                    val isSelected = currentDestination?.hierarchy?.any {
                                        it.hasRoute(item.route::class)
                                    } == true
                                    
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = SplashRoute,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { fadeIn() + scaleIn(initialScale = 0.95f) },
                        exitTransition = { fadeOut() + scaleOut(targetScale = 0.95f) }
                    ) {
                        composable<SplashRoute> {
                            SplashScreen(
                                viewModel = viewModel,
                                onNavigateToHome = {
                                    navController.navigate(HomeRoute) {
                                        popUpTo(SplashRoute) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable<HomeRoute> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToSearch = {
                                    navController.navigate(SearchRoute)
                                },
                                onNavigateToWord = { wordId ->
                                    navController.navigate(WordDetailRoute(wordId, "All"))
                                },
                                onNavigateToChapter = { chapter ->
                                    navController.navigate(ChapterWordsRoute(chapter))
                                },
                                onNavigateToLibrary = {
                                    navController.navigate(LibraryRoute)
                                },
                                onNavigateToChapterList = {
                                    navController.navigate(ChapterListRoute)
                                },
                                onNavigateToAlphabetical = {
                                    navController.navigate(AlphabeticalIndexRoute)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(SettingsRoute)
                                }
                            )
                        }

                        composable<SearchRoute> {
                            val searchViewModel: SearchViewModel = viewModel()
                            SearchScreen(
                                viewModel = searchViewModel,
                                onNavigateToWord = { wordId ->
                                    navController.navigate(WordDetailRoute(wordId, "All"))
                                },
                                onNavigateToTopicWords = { topic ->
                                    navController.navigate(TopicWordsRoute(topic))
                                },
                                onNavigateToChapterWords = { chapter ->
                                    navController.navigate(ChapterWordsRoute(chapter))
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<SavedRoute> {
                            SavedScreen(
                                viewModel = viewModel,
                                onNavigateToWord = { wordId ->
                                    navController.navigate(WordDetailRoute(wordId, "Saved"))
                                },
                                onStartSavedRevision = {
                                    navController.navigate(RevisionSessionRoute(onlySavedWords = true))
                                }
                            )
                        }

                        composable<HistoryRoute> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToWord = { wordId ->
                                    navController.navigate(WordDetailRoute(wordId, "History"))
                                }
                            )
                        }

                        composable<LibraryRoute> {
                            LibraryScreen(
                                viewModel = viewModel,
                                onNavigateToWord = { wordId ->
                                    navController.navigate(WordDetailRoute(wordId, "All"))
                                },
                                onNavigateToChapter = { chapter ->
                                    navController.navigate(ChapterWordsRoute(chapter))
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToCategories = {
                                    navController.navigate(WordCategoryRoute)
                                },
                                onStartSavedRevision = {
                                    navController.navigate(RevisionSessionRoute(onlySavedWords = true))
                                }
                            )
                        }

                        composable<WordCategoryRoute> {
                            val context = LocalContext.current
                            val categoryViewModel: com.example.viewmodel.WordCategoryViewModel = viewModel(
                                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
                            )
                            val uiState by categoryViewModel.uiState.collectAsState()
                            com.example.ui.screens.WordCategoryScreen(
                                viewModel = categoryViewModel,
                                onNavigateToWord = { wordId ->
                                    navController.navigate(WordDetailRoute(wordId, "Category:${uiState.selectedCategory}"))
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable<ChapterListRoute> {
                            ChapterListScreen(
                                viewModel = viewModel,
                                onNavigateToChapter = { chapter ->
                                    navController.navigate(ChapterWordsRoute(chapter))
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<AlphabeticalIndexRoute> {
                            AlphabeticalIndexScreen(
                                viewModel = viewModel,
                                onNavigateToWord = { wordId ->
                                    navController.navigate(WordDetailRoute(wordId, "Alphabetical"))
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(viewModel = viewModel)
                        }

                        composable<LearningAnalyticsRoute> {
                            val context = LocalContext.current
                            val analyticsViewModel: AnalyticsViewModel = viewModel(
                                factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
                            )
                            LearningAnalyticsScreen(
                                viewModel = analyticsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<WordDetailRoute> { backStackEntry ->
                            val route: WordDetailRoute = backStackEntry.toRoute()
                            WordDetailScreen(
                                wordId = route.wordId,
                                listContext = route.contextFilter,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<ChapterWordsRoute> { backStackEntry ->
                            val route: ChapterWordsRoute = backStackEntry.toRoute()
                            ChapterWordsScreen(
                                chapterName = route.chapterName,
                                viewModel = viewModel,
                                onNavigateToWord = { wordId ->
                                    navController.navigate(WordDetailRoute(wordId, "Chapter:${route.chapterName}"))
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<TopicWordsRoute> { backStackEntry ->
                            val route: TopicWordsRoute = backStackEntry.toRoute()
                            TopicWordsScreen(
                                topicName = route.topicName,
                                viewModel = viewModel,
                                onNavigateToWord = { wordId ->
                                    navController.navigate(WordDetailRoute(wordId))
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<RevisionDashboardRoute> {
                            val context = LocalContext.current
                            val dashboardViewModel: RevisionDashboardViewModel = viewModel(
                                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
                            )
                            RevisionDashboardScreen(
                                viewModel = dashboardViewModel,
                                onStartSession = { durationMinutes ->
                                    navController.navigate(RevisionSessionRoute(durationMinutes = durationMinutes))
                                },
                                onNavigateToAnalytics = {
                                    navController.navigate(LearningAnalyticsRoute)
                                }
                            )
                        }

                        composable<RevisionSessionRoute> { backStackEntry ->
                            val route = backStackEntry.toRoute<RevisionSessionRoute>()
                            val context = LocalContext.current
                            val sessionViewModel: RevisionSessionViewModel = viewModel(
                                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
                            )
                            RevisionSessionScreen(
                                viewModel = sessionViewModel,
                                onlySavedWords = route.onlySavedWords,
                                durationMinutes = route.durationMinutes,
                                onSessionFinished = {
                                    navController.navigate(RevisionSummaryRoute)
                                }
                            )
                        }

                        composable<RevisionSummaryRoute> { backStackEntry ->
                            // Retrieve the ViewModel scoped to the session route
                            val sessionEntry = remember(backStackEntry) {
                                try {
                                    navController.getBackStackEntry<RevisionSessionRoute>()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            if (sessionEntry != null) {
                                val context = LocalContext.current
                                val sessionViewModel: RevisionSessionViewModel = viewModel(
                                    sessionEntry,
                                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
                                )
                                RevisionSummaryScreen(
                                    viewModel = sessionViewModel,
                                    onFinish = {
                                        navController.popBackStack(RevisionDashboardRoute, inclusive = false)
                                    }
                                )
                            } else {
                                // Fallback if session entry not found
                                LaunchedEffect(Unit) {
                                    navController.popBackStack<RevisionDashboardRoute>(inclusive = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: Any,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

