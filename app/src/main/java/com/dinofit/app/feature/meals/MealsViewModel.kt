package com.dinofit.app.feature.meals

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.common.NutritionLabelParser
import com.dinofit.app.core.data.NutritionRepository
import com.dinofit.app.core.data.ProfileRepository
import com.dinofit.app.core.model.Meal
import com.dinofit.app.core.model.MealSource
import com.dinofit.app.core.model.Nutrients
import com.dinofit.app.core.network.ClaudeFoodApi
import com.dinofit.app.core.network.OpenFoodFactsApi
import com.dinofit.app.core.scan.FoodImages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

/** Ce que l'on propose d'ajouter, à vérifier avant d'enregistrer. */
data class MealDraft(
    val name: String,
    /** Valeurs pour 100 g (code-barres, étiquette) : le total suit alors le poids saisi. */
    val per100g: Nutrients?,
    val grams: Double,
    val total: Nutrients,
    val source: MealSource,
    val note: String? = null,
    /** Détail par aliment pour une photo analysée par Claude. */
    val items: List<String> = emptyList(),
)

sealed interface ScanState {
    data object Idle : ScanState
    data class Working(val message: String) : ScanState
    data class Review(val draft: MealDraft) : ScanState
    data class Error(val message: String) : ScanState
}

data class MacroTargets(val protein: Double, val carbs: Double, val fat: Double)

sealed interface MealsUiState {
    data object Loading : MealsUiState
    data class Success(
        val date: LocalDate,
        val goal: Int,
        val suggestedGoal: Int,
        val eaten: Nutrients,
        val targets: MacroTargets,
        val meals: List<Meal>,
        val apiKey: String,
    ) : MealsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealsViewModel @Inject constructor(
    private val nutrition: NutritionRepository,
    private val images: FoodImages,
    private val off: OpenFoodFactsApi,
    private val claude: ClaudeFoodApi,
    profileRepository: ProfileRepository,
) : ViewModel() {

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan: StateFlow<ScanState> = _scan.asStateFlow()

    // La jauge suit la date du jour : à minuit, la liste du jour change et la jauge repart de zéro.
    private val todayMeals = nutrition.today.flatMapLatest { day -> nutrition.mealsOn(day).map { day to it } }

    val uiState: StateFlow<MealsUiState> = combine(
        todayMeals,
        nutrition.kcalGoal,
        nutrition.claudeApiKey,
        profileRepository.profile,
    ) { (day, meals), goal, key, profile ->
        val protein = profile.weightKg * 1.6
        val fat = goal * 0.28 / 9
        MealsUiState.Success(
            date = day,
            goal = goal,
            suggestedGoal = FitnessMath.suggestedKcalGoal(profile),
            eaten = meals.fold(Nutrients.ZERO) { acc, m -> acc + m.nutrients },
            targets = MacroTargets(protein, ((goal - protein * 4 - fat * 9) / 4).coerceAtLeast(0.0), fat),
            meals = meals,
            apiKey = key,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealsUiState.Loading)

    fun newPhotoFile(): File = images.newPhotoFile()

    fun onBarcode(code: String?) {
        if (code.isNullOrBlank()) return
        viewModelScope.launch {
            _scan.value = ScanState.Working("Recherche du produit…")
            _scan.value = when (val r = off.product(code)) {
                is OpenFoodFactsApi.Result.Found -> {
                    val grams = r.product.servingGrams ?: 100.0
                    ScanState.Review(
                        MealDraft(
                            name = r.product.name, per100g = r.product.per100g, grams = grams,
                            total = r.product.per100g * (grams / 100), source = MealSource.BARCODE,
                            note = "Indique la quantité que tu as mangée.",
                        ),
                    )
                }
                OpenFoodFactsApi.Result.NotFound -> ScanState.Error("Produit inconnu dans Open Food Facts. Prends plutôt l'étiquette en photo.")
                OpenFoodFactsApi.Result.Offline -> ScanState.Error("Pas de connexion internet pour chercher ce produit.")
            }
        }
    }

    fun onLabelPhoto(uri: Uri) {
        viewModelScope.launch {
            _scan.value = ScanState.Working("Lecture de l'étiquette…")
            val reading = runCatching { NutritionLabelParser.parse(images.readText(uri)) }.getOrNull()
            if (reading != null && reading.isUseful) {
                val per100 = Nutrients(reading.kcal ?: 0.0, reading.protein ?: 0.0, reading.carbs ?: 0.0, reading.fat ?: 0.0)
                val grams = reading.servingGrams ?: 100.0
                val missing = listOfNotNull(
                    "calories".takeIf { reading.kcal == null }, "protéines".takeIf { reading.protein == null },
                    "glucides".takeIf { reading.carbs == null }, "lipides".takeIf { reading.fat == null },
                )
                _scan.value = ScanState.Review(
                    MealDraft(
                        name = "", per100g = per100, grams = grams, total = per100 * (grams / 100), source = MealSource.LABEL,
                        note = if (missing.isEmpty()) "Vérifie les valeurs lues (pour 100 g) et la quantité mangée."
                        else "Non lu : ${missing.joinToString(", ")}. Complète si besoin.",
                    ),
                )
            } else if (nutrition.claudeApiKey.first().isNotBlank()) {
                analyzeWithClaude(uri)
            } else {
                _scan.value = ScanState.Error("Je n'arrive pas à lire l'étiquette. Prends-la bien de face, nette et éclairée.")
            }
        }
    }

    fun onMealPhoto(uri: Uri) {
        viewModelScope.launch {
            if (nutrition.claudeApiKey.first().isBlank()) {
                _scan.value = ScanState.Error("L'analyse d'un plat utilise Claude : ajoute ta clé API dans les réglages (roue dentée).")
                return@launch
            }
            analyzeWithClaude(uri)
        }
    }

    private suspend fun analyzeWithClaude(uri: Uri) {
        _scan.value = ScanState.Working("Claude analyse ta photo…")
        val jpeg = runCatching { images.compressedJpeg(uri) }.getOrElse {
            _scan.value = ScanState.Error("Photo illisible, réessaie.")
            return
        }
        _scan.value = when (val r = claude.analyze(jpeg, nutrition.claudeApiKey.first())) {
            is ClaudeFoodApi.Result.Success -> {
                val e = r.estimate
                ScanState.Review(
                    MealDraft(
                        name = e.name, per100g = null, grams = e.items.sumOf { it.grams }, total = e.total, source = MealSource.PHOTO_AI,
                        note = when (e.confidence) {
                            "high" -> "Estimation de Claude. Ajuste si besoin."
                            "low" -> "Estimation incertaine : vérifie bien les quantités."
                            else -> "Estimation de Claude : vérifie les quantités."
                        },
                        items = e.items.map { "${it.name} (${Math.round(it.grams)} g) : ${Math.round(it.nutrients.kcal)} kcal" },
                    ),
                )
            }
            is ClaudeFoodApi.Result.Failure -> ScanState.Error(r.message)
        }
    }

    fun manual() {
        _scan.value = ScanState.Review(MealDraft("", null, 0.0, Nutrients.ZERO, MealSource.MANUAL))
    }

    fun save(name: String, total: Nutrients, source: MealSource) {
        viewModelScope.launch {
            nutrition.add(name, total, source)
            _scan.value = ScanState.Idle
        }
    }

    fun dismissScan() {
        _scan.value = ScanState.Idle
    }

    fun showError(message: String) {
        _scan.value = ScanState.Error(message)
    }

    fun delete(id: Long) {
        viewModelScope.launch { nutrition.delete(id) }
    }

    fun setGoal(value: Int) {
        viewModelScope.launch { nutrition.setKcalGoal(value) }
    }

    fun setApiKey(value: String) {
        viewModelScope.launch { nutrition.setClaudeApiKey(value) }
    }
}
