package com.example.vedaahar.dosha

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DoshaAssessmentViewModel(
    private val context: Context,
    answerPreferencesName: String = "dosha_assessment"
) : ViewModel() {
    private val preferences = context.getSharedPreferences(answerPreferencesName, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<DoshaAssessmentUiState> = _uiState.asStateFlow()

    fun selectOption(optionId: String) {
        _uiState.update { state ->
            val updatedAnswers = state.answers + (state.currentQuestion.id to optionId)
            state.copy(answers = updatedAnswers, result = null)
        }
        persistAnswers()
    }

    fun next() {
        _uiState.update { state ->
            if (state.isLastQuestion) {
                val result = DoshaScoringEngine.generateResult(state.questions, state.answers)
                val savedAtMillis = System.currentTimeMillis()
                val previousResult = DoshaResultStore.save(context, result, savedAtMillis)
                state.copy(
                    result = result,
                    savedAtMillis = savedAtMillis,
                    previousSavedResult = previousResult,
                    savedAssessmentCount = DoshaResultStore.historyCount(context)
                )
            } else {
                state.copy(currentQuestionIndex = (state.currentQuestionIndex + 1).coerceAtMost(state.questions.lastIndex))
            }
        }
        persistAnswers()
    }

    fun previous() {
        _uiState.update { state ->
            if (state.result != null) {
                state.copy(result = null, currentQuestionIndex = state.questions.lastIndex)
            } else {
                state.copy(currentQuestionIndex = (state.currentQuestionIndex - 1).coerceAtLeast(0))
            }
        }
        persistAnswers()
    }

    fun selectConstitution(profileName: String) {
        val result = DoshaScoringEngine.resultForProfile(profileName)
        val savedAtMillis = System.currentTimeMillis()
        val previousResult = DoshaResultStore.save(context, result, savedAtMillis)
        _uiState.update { state ->
            state.copy(
                result = result,
                savedAtMillis = savedAtMillis,
                previousSavedResult = previousResult,
                savedAssessmentCount = DoshaResultStore.historyCount(context)
            )
        }
    }

    fun restart() {
        preferences.edit().clear().apply()
        _uiState.value = loadState()
    }

    private fun loadState(): DoshaAssessmentUiState {
        val currentSaved = DoshaResultStore.current(context)
        val initialResult = currentSaved?.let {
            DoshaScoringEngine.resultForProfile(it.profileName)
        } ?: DoshaScoringEngine.resultForProfile("Vata-Pitta")
        return DoshaAssessmentUiState(
            result = initialResult,
            savedAtMillis = currentSaved?.savedAtMillis ?: System.currentTimeMillis(),
            previousSavedResult = currentSaved,
            savedAssessmentCount = DoshaResultStore.historyCount(context)
        )
    }

    private fun persistAnswers() {
        val state = _uiState.value
        preferences.edit().apply {
            putInt("current_index", state.currentQuestionIndex)
            state.answers.forEach { (questionId, optionId) ->
                putString("question_$questionId", optionId)
            }
        }.apply()
    }

    class Factory(
        private val context: Context,
        private val answerPreferencesName: String = "dosha_assessment"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DoshaAssessmentViewModel(context.applicationContext, answerPreferencesName) as T
        }
    }
}
