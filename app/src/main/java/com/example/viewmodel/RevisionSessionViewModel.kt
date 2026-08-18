package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.engine.LocalAnswerEvaluator
import com.example.engine.LexiRevisionEngine
import com.example.engine.SessionBufferManager
import com.example.model.VocabularyWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RevisionSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository
    private val preferences = app.container.preferences

    data class RevisionSessionState(
        val isLoading: Boolean = true,
        val isFinished: Boolean = false,
        val isEmpty: Boolean = false,
        val wordsTotal: Int = 0,
        val wordsRevised: Int = 0,
        val correctCount: Int = 0,
        val partialCount: Int = 0,
        val incorrectCount: Int = 0,
        val rescheduledCount: Int = 0,
        val currentWord: VocabularyWord? = null,
        val isModeA: Boolean = true,
        val distractors: List<String> = emptyList(),
        val isRevealed: Boolean = false,
        val evaluationResult: LocalAnswerEvaluator.EvaluationLevel? = null,
        val showBonusPrompt: Boolean = false
    )

    private val _uiState = MutableStateFlow(RevisionSessionState())
    val uiState: StateFlow<RevisionSessionState> = _uiState.asStateFlow()

    private val bufferManager = SessionBufferManager()
    private val processedWords = mutableSetOf<Int>()

    // Separate from UI state — prevents double-click without ever freezing the UI.
    private var isProcessing = false
    private var isSessionStarted = false

    fun startSession(onlySavedWords: Boolean) {
        if (isSessionStarted) return
        isSessionStarted = true
        loadSessionWords(onlySavedWords)
    }

    private fun loadSessionWords(onlySavedWords: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val todayTime = System.currentTimeMillis()
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = todayTime }
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                val startOfDay = calendar.timeInMillis
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                val endOfDay = calendar.timeInMillis

                val dailyGoal = preferences.dailyRevisionGoal.value
                val revisedToday = repository.getWordsRevisedTodayCount(startOfDay, endOfDay)
                
                // Calculate remaining words for the goal. If already met, default to a 10-word practice session.
                val targetSize = if (revisedToday >= dailyGoal) 10 else (dailyGoal - revisedToday)

                val sessionWords = if (onlySavedWords) {
                    val savedWords = repository.favoriteWords.first()
                    savedWords.shuffled().take(targetSize)
                } else {
                    repository.generateRevisionSession(todayTime, targetSize, startOfDay)
                }

                if (sessionWords.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isEmpty = true)
                    return@launch
                }

                bufferManager.loadSession(sessionWords, targetSize)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    wordsTotal = targetSize
                )
                advanceToNextWord()
            } catch (e: Exception) {
                android.util.Log.e("RevisionSessionVM", "Crash during loadSessionWords", e)
                _uiState.value = _uiState.value.copy(isLoading = false, isFinished = true)
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val word = _uiState.value.currentWord ?: return@launch
            val newState = repository.toggleFavorite(word.id)
            _uiState.value = _uiState.value.copy(
                currentWord = word.copy(isFavorite = newState)
            )
        }
    }

    /**
     * Pulls the next word from the buffer and updates the UI state.
     * This is the ONLY function that transitions between words.
     * isProcessing is ALWAYS reset in this function, in every code path.
     */
    private fun recordSessionCompletion() {
        viewModelScope.launch {
            val settingsRepo = app.container.settingsRepository
            val calendar = java.util.Calendar.getInstance()
            val timeOfDayMs = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 * 60 * 1000L +
                              calendar.get(java.util.Calendar.MINUTE) * 60 * 1000L +
                              calendar.get(java.util.Calendar.SECOND) * 1000L
            settingsRepo.recordRevisionSessionCompleted(timeOfDayMs)
            
            val historyStr = settingsRepo.lastRevisionTimesFlow.first()
            val times = historyStr.split(",").mapNotNull { it.toLongOrNull() }
            val averageMs = if (times.isNotEmpty()) times.sum() / times.size else timeOfDayMs
            com.example.notifications.NotificationHelper.scheduleNextRevisionNotification(app, averageMs)
        }
    }

    private fun advanceToNextWord() {
        val nextWord = bufferManager.getNextWord()
        if (nextWord == null) {
            // Session complete
            isProcessing = false
            if (bufferManager.hasMissedWords()) {
                _uiState.value = _uiState.value.copy(showBonusPrompt = true)
            } else {
                _uiState.value = _uiState.value.copy(isFinished = true)
                recordSessionCompletion()
            }
            return
        }

        val isModeA = Math.random() < 0.7
        val questionsAnswered = bufferManager.getQuestionsAsked()

        viewModelScope.launch {
            try {
                val distractors = if (!isModeA) {
                    repository.getDistractorMeanings(nextWord.id)
                } else {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    currentWord = nextWord,
                    isModeA = isModeA,
                    distractors = distractors.shuffled(),
                    isRevealed = false,
                    evaluationResult = null,
                    showBonusPrompt = false,
                    wordsRevised = questionsAnswered
                )
            } catch (e: Exception) {
                android.util.Log.e("RevisionSessionVM", "Error loading distractors", e)
                // Fallback: show the word in Mode A without distractors
                _uiState.value = _uiState.value.copy(
                    currentWord = nextWord,
                    isModeA = true,
                    distractors = emptyList(),
                    isRevealed = false,
                    evaluationResult = null,
                    showBonusPrompt = false,
                    wordsRevised = questionsAnswered
                )
            } finally {
                // ALWAYS unlock, even if an exception occurred
                isProcessing = false
            }
        }
    }

    fun submitAnswer(userAnswer: String) {
        val word = _uiState.value.currentWord ?: return
        val result = LocalAnswerEvaluator.evaluate(
            userAnswer = userAnswer,
            officialMeaning = word.meaning,
            acceptedKeywords = word.acceptedKeywords
        )

        _uiState.value = _uiState.value.copy(
            isRevealed = true,
            evaluationResult = result
        )
    }

    fun submitModeBAnswer(selectedMeaning: String) {
        val word = _uiState.value.currentWord ?: return
        val isCorrect = selectedMeaning == word.meaning
        val result = if (isCorrect) {
            LocalAnswerEvaluator.EvaluationLevel.CORRECT
        } else {
            LocalAnswerEvaluator.EvaluationLevel.INCORRECT
        }

        _uiState.value = _uiState.value.copy(
            isRevealed = true,
            evaluationResult = result
        )
    }

    fun evaluateSelf(feedback: LexiRevisionEngine.Feedback) {
        // Guard: prevent double-clicks. NOT tied to UI rendering.
        if (isProcessing) return
        isProcessing = true

        val word = _uiState.value.currentWord
        if (word == null) {
            isProcessing = false
            return
        }

        // NOTE: We do NOT set isLoading = true here.
        // isLoading is ONLY used for the initial session load spinner.
        // Setting it here was causing the entire UI to freeze.

        val updatedWord = LexiRevisionEngine.processReview(word, feedback)

        // Fire-and-forget DB update
        viewModelScope.launch {
            try {
                repository.updateWordMetadata(
                    wordId = updatedWord.id,
                    stability = updatedWord.stability,
                    lastRevisedTimestamp = updatedWord.lastRevisedTimestamp,
                    difficultyFactor = updatedWord.difficultyFactor,
                    learningStatus = updatedWord.learningStatus,
                    consecutiveFailures = updatedWord.consecutiveFailures
                )
            } catch (e: Exception) {
                android.util.Log.e("RevisionSessionVM", "Error saving word metadata", e)
            }
        }

        // Update stats counters (only once per unique word)
        var updatedState = _uiState.value
        if (!processedWords.contains(word.id)) {
            processedWords.add(word.id)

            when (updatedState.evaluationResult) {
                LocalAnswerEvaluator.EvaluationLevel.CORRECT -> updatedState = updatedState.copy(correctCount = updatedState.correctCount + 1)
                LocalAnswerEvaluator.EvaluationLevel.PARTIALLY_CORRECT -> updatedState = updatedState.copy(partialCount = updatedState.partialCount + 1)
                LocalAnswerEvaluator.EvaluationLevel.INCORRECT -> updatedState = updatedState.copy(incorrectCount = updatedState.incorrectCount + 1)
                null -> {}
            }
        }

        // Process the word in the buffer
        if (feedback == LexiRevisionEngine.Feedback.NEED_REVISION || feedback == LexiRevisionEngine.Feedback.I_DONT_KNOW || feedback == LexiRevisionEngine.Feedback.ALMOST) {
            val displacement = if (feedback == LexiRevisionEngine.Feedback.ALMOST) 6 else 4
            bufferManager.pushWordForward(updatedWord, displacement)
            updatedState = updatedState.copy(
                rescheduledCount = updatedState.rescheduledCount + 1
            )
        } else {
            bufferManager.completeWord()
        }

        _uiState.value = updatedState

        // Advance to next word (this will also reset isProcessing)
        advanceToNextWord()
    }

    fun startBonusRound() {
        bufferManager.startBonusSession()
        processedWords.clear()
        _uiState.value = _uiState.value.copy(
            showBonusPrompt = false,
            isFinished = false,
            wordsTotal = bufferManager.getTargetSessionSize(),
            wordsRevised = 0,
            correctCount = 0,
            partialCount = 0,
            incorrectCount = 0,
            rescheduledCount = 0
        )
        advanceToNextWord()
    }

    fun declineBonusRound() {
        _uiState.value = _uiState.value.copy(
            showBonusPrompt = false,
            isFinished = true
        )
        recordSessionCompletion()
    }

    fun forceEndSession() {
        _uiState.value = _uiState.value.copy(
            showBonusPrompt = false,
            isFinished = true
        )
        recordSessionCompletion()
    }
}
