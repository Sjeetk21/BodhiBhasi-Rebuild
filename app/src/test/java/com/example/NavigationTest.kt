package com.example

import com.example.navigation.ChapterWordsRoute
import com.example.navigation.WordDetailRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTest {

    @Test
    fun testWordDetailRouteSerialization() {
        val route = WordDetailRoute(wordId = 42)
        assertEquals(42, route.wordId)
    }

    @Test
    fun testChapterWordsRouteSerialization() {
        val route = ChapterWordsRoute(chapterName = "Polity")
        assertEquals("Polity", route.chapterName)
    }

    @Test
    fun testDeepLinkPatterns() {
        val wordDeepLink = WordDetailRoute.deepLinks[0].uriPattern
        org.junit.Assert.assertTrue(wordDeepLink?.startsWith("lexiupsc://word") == true || wordDeepLink?.startsWith("bodhibhasi://word") == true)
        org.junit.Assert.assertTrue(wordDeepLink?.contains("wordId") == true)

        val chapterDeepLink = ChapterWordsRoute.deepLinks[0].uriPattern
        org.junit.Assert.assertTrue(chapterDeepLink?.startsWith("bodhibhasi://chapter") == true || chapterDeepLink?.startsWith("lexiupsc://chapter") == true)
        org.junit.Assert.assertTrue(chapterDeepLink?.contains("chapterName") == true)
    }
}
