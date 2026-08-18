package com.example.navigation

import androidx.navigation.navDeepLink
import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object HomeRoute

@Serializable
object SearchRoute

@Serializable
data class WordDetailRoute(val wordId: Int, val contextFilter: String = "All") {
    companion object {
        val deepLinks = listOf(
            navDeepLink<WordDetailRoute>(
                basePath = "lexiupsc://word"
            )
        )
    }
}

@Serializable
object LibraryRoute

@Serializable
object WordCategoryRoute

@Serializable
object ChapterListRoute

@Serializable
object AlphabeticalIndexRoute

@Serializable
data class ChapterWordsRoute(val chapterName: String) {
    companion object {
        val deepLinks = listOf(
            navDeepLink<ChapterWordsRoute>(
                basePath = "bodhibhasi://chapter"
            )
        )
    }
}

@Serializable
data class TopicWordsRoute(val topicName: String) {
    companion object {
        val deepLinks = listOf(
            navDeepLink<TopicWordsRoute>(
                basePath = "bodhibhasi://topic"
            )
        )
    }
}

@Serializable
object BookmarksRoute

@Serializable
object SavedRoute

@Serializable
object HistoryRoute

@Serializable
object StatisticsRoute

@Serializable
object LearningAnalyticsRoute

@Serializable
object SettingsRoute

@Serializable
object SyncProgressRoute

@Serializable
object AboutRoute

@Serializable
object RevisionDashboardRoute

@Serializable
data class RevisionSessionRoute(val onlySavedWords: Boolean = false, val durationMinutes: Int? = null)

@Serializable
object RevisionSummaryRoute
