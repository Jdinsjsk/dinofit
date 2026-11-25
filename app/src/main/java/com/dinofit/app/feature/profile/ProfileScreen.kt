package com.dinofit.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.PageHeader
import com.dinofit.app.core.designsystem.Panel
import com.dinofit.app.core.model.Sex
import com.dinofit.app.core.model.UserProfile
import kotlin.math.roundToInt

@Composable
fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(uiState, viewModel::update)
}

@Composable
internal fun ProfileScreen(uiState: ProfileUiState, onUpdate: (UserProfile) -> Unit) {
    val profile = (uiState as? ProfileUiState.Success)?.profile ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader("Sert à calculer distance et calories", "Profil", Modifier.padding(bottom = 0.dp))
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileField("Prénom", profile.name, KeyboardType.Text) { onUpdate(profile.copy(name = it)) }
                Text("Sexe", style = DinoType.Small, color = DinoColors.InkDim)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DinoColors.Bg)
                        .border(1.dp, DinoColors.Line, RoundedCornerShape(10.dp)).padding(3.dp),
                ) {
                    listOf(Sex.MALE to "Homme", Sex.FEMALE to "Femme").forEach { (sex, label) ->
                        val selected = profile.sex == sex
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) DinoColors.Ink else DinoColors.Bg)
                                .clickable { onUpdate(profile.copy(sex = sex)) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(label, style = DinoType.BodyBold, color = if (selected) DinoColors.Bg else DinoColors.InkDim) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileField("Âge", profile.age.toString(), KeyboardType.Number, Modifier.weight(1f)) { v ->
                        v.toIntOrNull()?.takeIf { it in 10..100 }?.let { onUpdate(profile.copy(age = it)) }
                    }
                    ProfileField("Poids (kg)", Formats.weight(profile.weightKg), KeyboardType.Decimal, Modifier.weight(1f)) { v ->
                        Formats.parseDecimal(v).takeIf { it in 30.0..250.0 }?.let { onUpdate(profile.copy(weightKg = it)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileField("Taille (cm)", profile.heightCm.toString(), KeyboardType.Number, Modifier.weight(1f)) { v ->
                        v.toIntOrNull()?.takeIf { it in 120..230 }?.let { onUpdate(profile.copy(heightCm = it)) }
                    }
                    ProfileField("Objectif de pas", profile.stepGoal.toString(), KeyboardType.Number, Modifier.weight(1f)) { v ->
                        v.toIntOrNull()?.takeIf { it >= 1000 }?.let { onUpdate(profile.copy(stepGoal = it)) }
                    }
                }
                Text(
                    "Longueur de pas estimée : ${(FitnessMath.strideMeters(profile) * 100).roundToInt()} cm. " +
                        "Environ ${FitnessMath.stepsKcal(1000, profile).roundToInt()} kcal pour 1 000 pas.",
                    style = DinoType.Small, color = DinoColors.InkFaint,
                )
            }
        }
        Panel(title = "Tes données") {
            Text(
                "Tes séances sont enregistrées sur ton téléphone à chaque modification. Le podomètre compte même app fermée ; DinoFit relève le compteur toutes les 15 minutes. " +
                    "Faits sur les dinosaures : dinofacts.fyi, d'après Wikipédia.",
                style = DinoType.Small.copy(fontSize = DinoType.Body.fontSize.times(0.93f)), color = DinoColors.InkDim,
            )
        }
    }
}

/** Champ éditable : la saisie reste locale et n'est enregistrée que si la valeur est valide. */
@Composable
private fun ProfileField(
    label: String,
    initial: String,
    type: KeyboardType,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValid: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onValid(it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        textStyle = DinoType.BodyBold.copy(color = DinoColors.Ink),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DinoColors.Amber, unfocusedBorderColor = DinoColors.Line,
            focusedLabelColor = DinoColors.Amber, unfocusedLabelColor = DinoColors.InkDim, cursorColor = DinoColors.Amber,
            focusedContainerColor = DinoColors.Bg, unfocusedContainerColor = DinoColors.Bg,
        ),
        modifier = modifier,
    )
}
