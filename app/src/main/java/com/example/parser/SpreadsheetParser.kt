package com.example.parser

import android.util.Xml
import com.example.model.VocabularyWord
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object SpreadsheetParser {

    /**
     * Parses a CSV string into a list of structured VocabularyWords.
     */
    fun parseCsvToWords(csvContent: String): List<VocabularyWord> {
        val words = mutableListOf<VocabularyWord>()
        val lines = csvContent.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val columns = parseCsvLine(trimmed)
            if (columns.size >= 2 && columns[0].isNotBlank()) {
                val wordVal = columns[0]
                if (wordVal.equals("Word", ignoreCase = true)) continue // Skip header row
                
                val meaningVal = columns[1]
                
                val examplesList = mutableListOf<String>()
                if (columns.size > 2 && columns[2].isNotBlank()) examplesList.add(columns[2])
                if (columns.size > 3 && columns[3].isNotBlank()) examplesList.add(columns[3])
                if (columns.size > 4 && columns[4].isNotBlank()) examplesList.add(columns[4])
                
                val memoryHookVal = if (columns.size > 5 && columns[5].isNotBlank()) columns[5] else null
                val baseFormVal = if (columns.size > 6 && columns[6].isNotBlank()) columns[6] else null
                val topicVal = if (columns.size > 7 && columns[7].isNotBlank()) columns[7] else null
                val chapterVal = if (columns.size > 8 && columns[8].isNotBlank()) columns[8] else null
                val relatedFormsVal = if (columns.size > 9 && columns[9].isNotBlank()) columns[9] else null
                val acceptedKeywordsVal = if (columns.size > 10 && columns[10].isNotBlank()) columns[10] else null
                val antonymsVal = if (columns.size > 11 && columns[11].isNotBlank()) columns[11] else null
                val pronunciationVal = if (columns.size > 12 && columns[12].isNotBlank()) columns[12] else null
                
                words.add(
                    VocabularyWord(
                        word = wordVal,
                        meaning = meaningVal,
                        examples = examplesList,
                        memoryHook = memoryHookVal,
                        baseForm = baseFormVal,
                        topic = topicVal,
                        chapter = chapterVal,
                        relatedForms = relatedFormsVal,
                        acceptedKeywords = acceptedKeywordsVal,
                        antonyms = antonymsVal,
                        pronunciation = pronunciationVal
                    )
                )
            }
        }
        return words
    }

    /**
     * Parses a CSV input stream into a list of VocabularyWords.
     */
    fun parseCsvStreamToWords(inputStream: InputStream): List<VocabularyWord> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val content = reader.readText()
        return parseCsvToWords(content)
    }

    /**
     * Parses an RFC-4180 compliant CSV line into individual fields.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when (c) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(',')
                    } else {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    }
                }
                else -> {
                    current.append(c)
                }
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    /**
     * Extracts and parses vocabulary from an Excel (.xlsx) file stream by reading
     * its worksheets/sheet1.xml and sharedStrings.xml.
     */
    fun parseXlsxToWords(inputStream: InputStream): List<VocabularyWord> {
        val sharedStrings = mutableListOf<String>()
        var sheet1Bytes: ByteArray? = null
        var sharedStringsBytes: ByteArray? = null
        
        val zipInputStream = ZipInputStream(inputStream)
        try {
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> {
                        sharedStringsBytes = zipInputStream.readBytes()
                    }
                    "xl/worksheets/sheet1.xml" -> {
                        sheet1Bytes = zipInputStream.readBytes()
                    }
                }
                entry = zipInputStream.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            zipInputStream.close()
        }

        if (sheet1Bytes == null) return emptyList()

        // 1. Parse sharedStrings.xml if present
        if (sharedStringsBytes != null) {
            try {
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                parser.setInput(sharedStringsBytes.inputStream(), "UTF-8")
                
                var eventType = parser.eventType
                var currentText = StringBuilder()
                var insideSi = false
                
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val name = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (name == "si") {
                                insideSi = true
                                currentText = StringBuilder()
                            } else if (name == "t" && insideSi) {
                                try {
                                    currentText.append(parser.nextText())
                                } catch (e: Exception) {}
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (name == "si") {
                                insideSi = false
                                sharedStrings.add(currentText.toString())
                            }
                        }
                    }
                    eventType = parser.next()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Parse sheet1.xml
        val rows = mutableListOf<MutableMap<Int, String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(sheet1Bytes.inputStream(), "UTF-8")

            var eventType = parser.eventType
            var currentCellCol = -1
            var currentCellType: String? = null
            var insideCell = false
            var currentVal = StringBuilder()
            var currentCellRowMap = mutableMapOf<Int, String>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "row") {
                            currentCellRowMap = mutableMapOf()
                        } else if (name == "c") {
                            insideCell = true
                            val rAttr = parser.getAttributeValue(null, "r") ?: ""
                            val colStr = rAttr.takeWhile { it.isLetter() }
                            currentCellCol = columnToIndex(colStr)
                            currentCellType = parser.getAttributeValue(null, "t")
                            currentVal = StringBuilder()
                        } else if (name == "v" && insideCell) {
                            try {
                                currentVal.append(parser.nextText())
                            } catch (e: Exception) {}
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "row") {
                            if (currentCellRowMap.isNotEmpty()) {
                                rows.add(currentCellRowMap)
                            }
                        } else if (name == "c") {
                            insideCell = false
                            val vStr = currentVal.toString()
                            val finalValue = if (currentCellType == "s") {
                                val idx = vStr.toIntOrNull()
                                if (idx != null && idx >= 0 && idx < sharedStrings.size) {
                                    sharedStrings[idx]
                                } else {
                                    ""
                                }
                            } else {
                                vStr
                            }
                            if (currentCellCol >= 0) {
                                currentCellRowMap[currentCellCol] = finalValue
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Map parsed rows to VocabularyWord objects
        val words = mutableListOf<VocabularyWord>()
        for (rowMap in rows) {
            val wordVal = rowMap[0]?.trim() ?: ""
            val meaningVal = rowMap[1]?.trim() ?: ""
            if (wordVal.isBlank() || wordVal.equals("Word", ignoreCase = true)) continue
            
            val examplesList = mutableListOf<String>()
            rowMap[2]?.trim()?.takeIf { it.isNotEmpty() }?.let { examplesList.add(it) }
            rowMap[3]?.trim()?.takeIf { it.isNotEmpty() }?.let { examplesList.add(it) }
            rowMap[4]?.trim()?.takeIf { it.isNotEmpty() }?.let { examplesList.add(it) }
            
            val memoryHookVal = rowMap[5]?.trim()?.takeIf { it.isNotEmpty() }
            val baseFormVal = rowMap[6]?.trim()?.takeIf { it.isNotEmpty() }
            val topicVal = rowMap[7]?.trim()?.takeIf { it.isNotEmpty() }
            val chapterVal = rowMap[8]?.trim()?.takeIf { it.isNotEmpty() }
            val relatedFormsVal = rowMap[9]?.trim()?.takeIf { it.isNotEmpty() }
            val acceptedKeywordsVal = rowMap[10]?.trim()?.takeIf { it.isNotEmpty() }
            val antonymsVal = rowMap[11]?.trim()?.takeIf { it.isNotEmpty() }

            words.add(
                VocabularyWord(
                    word = wordVal,
                    meaning = meaningVal,
                    examples = examplesList,
                    memoryHook = memoryHookVal,
                    baseForm = baseFormVal,
                    topic = topicVal,
                    chapter = chapterVal,
                    relatedForms = relatedFormsVal,
                    acceptedKeywords = acceptedKeywordsVal,
                    antonyms = antonymsVal
                )
            )
        }
        return words
    }

    private fun columnToIndex(colStr: String): Int {
        var index = 0
        for (i in 0 until colStr.length) {
            index = index * 26 + (colStr[i].uppercaseChar() - 'A' + 1)
        }
        return index - 1
    }
}
