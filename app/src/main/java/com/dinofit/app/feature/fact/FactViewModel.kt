package com.dinofit.app.feature.fact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinofit.app.core.model.DinoFact
import com.dinofit.app.core.model.DinoRank
import com.dinofit.app.core.model.DinoRanks
import com.dinofit.app.core.model.OfflineDinoFacts
import com.dinofit.app.core.network.DinoFactsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FactUiState {
    data object Loading : FactUiState
    data class Shown(val fact: DinoFact, val rank: DinoRank) : FactUiState
    data object Dismissed : FactUiState
}

/** Un fait au hasard à chaque ouverture de l'app (une fois par lancement). */
@HiltViewModel
class FactViewModel @Inject constructor(
    private val api: DinoFactsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FactUiState>(FactUiState.Loading)
    val uiState: StateFlow<FactUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val fact = api.randomFact() ?: OfflineDinoFacts.random()
            if (_uiState.value != FactUiState.Dismissed) {
                _uiState.value = FactUiState.Shown(fact, rankFor(fact))
            }
        }
    }

    fun dismiss() {
        _uiState.value = FactUiState.Dismissed
    }

    private fun rankFor(fact: DinoFact): DinoRank =
        DinoRanks.firstOrNull { it.latin.equals(fact.dino, ignoreCase = true) } ?: DinoRanks.drop(1).random()
}
