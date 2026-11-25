package com.dinofit.app.feature.meals

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.KcalGauge
import com.dinofit.app.core.designsystem.PageHeader
import com.dinofit.app.core.designsystem.Panel
import com.dinofit.app.core.designsystem.PillButton
import com.dinofit.app.core.designsystem.PillStyle
import com.dinofit.app.core.designsystem.ProgressBar
import com.dinofit.app.core.designsystem.pressClickable
import com.dinofit.app.core.designsystem.strokeIcon
import com.dinofit.app.core.model.Meal
import com.dinofit.app.core.model.MealSource
import com.dinofit.app.core.model.Nutrients
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val GearIcon = strokeIcon(
    "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
    "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
)

@Composable
fun MealsRoute(viewModel: MealsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scan by viewModel.scan.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // L'appareil photo du téléphone enregistre dans un fichier de l'app (aucune autorisation caméra nécessaire).
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMode by rememberSaveable { mutableStateOf("label") }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri?.let(Uri::parse)
        if (ok && uri != null) {
            if (pendingMode == "label") viewModel.onLabelPhoto(uri) else viewModel.onMealPhoto(uri)
        }
    }
    fun takePhoto(mode: String) {
        val file = viewModel.newPhotoFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingUri = uri.toString()
        pendingMode = mode
        runCatching { camera.launch(uri) }.onFailure { viewModel.showError("Aucune application appareil photo trouvée.") }
    }
    fun scanBarcode() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { viewModel.onBarcode(it.rawValue) }
            .addOnFailureListener { viewModel.showError("Le scanner de codes-barres n'a pas pu s'ouvrir. Vérifie que Google Play est à jour.") }
    }

    MealsScreen(
        uiState = uiState,
        scan = scan,
        onBarcode = ::scanBarcode,
        onLabel = { takePhoto("label") },
        onMealPhoto = { takePhoto("meal") },
        onManual = viewModel::manual,
        onSave = viewModel::save,
        onDismissScan = viewModel::dismissScan,
        onDelete = viewModel::delete,
        onSetGoal = viewModel::setGoal,
        onSetApiKey = viewModel::setApiKey,
    )
}

@Composable
internal fun MealsScreen(
    uiState: MealsUiState,
    scan: ScanState,
    onBarcode: () -> Unit,
    onLabel: () -> Unit,
    onMealPhoto: () -> Unit,
    onManual: () -> Unit,
    onSave: (String, Nutrients, MealSource) -> Unit,
    onDismissScan: () -> Unit,
    onDelete: (Long) -> Unit,
    onSetGoal: (Int) -> Unit,
    onSetApiKey: (String) -> Unit,
) {
    val state = uiState as? MealsUiState.Success ?: return
    var showSettings by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Meal?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            PageHeader("La jauge se remet à zéro à minuit", "Repas", Modifier.weight(1f).padding(bottom = 0.dp))
            IconButton(onClick = { showSettings = true }) {
                Icon(GearIcon, contentDescription = "Réglages des repas", tint = DinoColors.InkDim)
            }
        }

        Panel {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                KcalGauge(state.eaten.kcal, state.goal)
                val left = state.goal - state.eaten.kcal.roundToInt()
                Text(
                    if (left >= 0) "Encore ${Formats.int(left)} kcal pour aujourd'hui" else "${Formats.int(-left)} kcal au-dessus de ton objectif",
                    style = DinoType.BodyBold, color = if (left >= 0) DinoColors.Ink else DinoColors.Lava,
                    modifier = Modifier.padding(top = 12.dp, bottom = 14.dp),
                )
            }
            MacroRow("Protéines", state.eaten.protein, state.targets.protein, DinoColors.Green)
            MacroRow("Glucides", state.eaten.carbs, state.targets.carbs, DinoColors.Amber)
            MacroRow("Lipides", state.eaten.fat, state.targets.fat, Color(0xFF6C8EBF))
        }

        Panel(title = "Ajouter ce que tu manges") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AddTile("Code-barres", "Gratuit, produits emballés", Modifier.weight(1f), onBarcode)
                AddTile("Étiquette", "Photo du tableau nutritionnel", Modifier.weight(1f), onLabel)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AddTile(
                    "Photo du plat", if (state.apiKey.isBlank()) "Avec Claude : ajoute ta clé" else "Analysé par Claude",
                    Modifier.weight(1f), onMealPhoto,
                )
                AddTile("À la main", "Saisir les valeurs", Modifier.weight(1f), onManual)
            }
        }

        Panel(title = "Aujourd'hui") {
            if (state.meals.isEmpty()) {
                Text("Rien pour l'instant. Scanne ton premier repas de la journée.", style = DinoType.Body, color = DinoColors.InkDim)
            }
            val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
            state.meals.reversed().forEachIndexed { i, m ->
                if (i > 0) HorizontalDivider(color = DinoColors.Line)
                Row(
                    Modifier.fillMaxWidth().pressClickable(onClickLabel = "Supprimer ce repas") { toDelete = m }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, style = DinoType.BodyBold, color = DinoColors.Ink, maxLines = 1)
                        Text(
                            "${Instant.ofEpochMilli(m.time).atZone(ZoneId.systemDefault()).format(timeFmt)}, " +
                                "P ${m.nutrients.protein.roundToInt()} g, G ${m.nutrients.carbs.roundToInt()} g, L ${m.nutrients.fat.roundToInt()} g",
                            style = DinoType.Small, color = DinoColors.InkDim,
                        )
                    }
                    Text("${Formats.int(m.nutrients.kcal.roundToInt())} kcal", style = DinoType.Title.copy(fontSize = 15.sp), color = DinoColors.AmberInk)
                }
            }
        }
    }

    when (scan) {
        ScanState.Idle -> Unit
        is ScanState.Working -> Dialogs.Working(scan.message)
        is ScanState.Error -> AlertDialog(
            onDismissRequest = onDismissScan,
            containerColor = DinoColors.Bg,
            title = { Text("Oups", style = DinoType.Title) },
            text = { Text(scan.message, style = DinoType.Body) },
            confirmButton = { PillButton("OK", onClick = onDismissScan, style = PillStyle.Primary) },
        )
        is ScanState.Review -> ReviewSheet(scan.draft, onSave, onDismissScan)
    }
    if (showSettings) {
        SettingsSheet(state, onSetGoal, onSetApiKey, onDismiss = { showSettings = false })
    }
    toDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = DinoColors.Bg,
            title = { Text(m.name, style = DinoType.Title) },
            text = { Text("${Formats.int(m.nutrients.kcal.roundToInt())} kcal, ajouté par ${m.source.label.lowercase()}.", style = DinoType.Body) },
            confirmButton = { PillButton("Garder", onClick = { toDelete = null }, style = PillStyle.Primary) },
            dismissButton = { TextButton(onClick = { onDelete(m.id); toDelete = null }) { Text("Supprimer", color = DinoColors.Lava) } },
        )
    }
}

@Composable
private fun MacroRow(label: String, value: Double, target: Double, color: Color) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = DinoType.Small, color = DinoColors.InkDim)
            Text("${value.roundToInt()} / ${target.roundToInt()} g", style = DinoType.Small, color = DinoColors.Ink)
        }
        Spacer(Modifier.height(4.dp))
        ProgressBar(if (target > 0) (value / target).toFloat() else 0f, color = color, track = DinoColors.Card2)
    }
}

@Composable
private fun AddTile(title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .pressClickable(shape = shape, onClick = onClick)
            .background(DinoColors.Bg, shape)
            .padding(14.dp),
    ) {
        Text(title, style = DinoType.BodyBold, color = DinoColors.Ink)
        Text(sub, style = DinoType.Label, color = DinoColors.InkDim)
    }
}

private object Dialogs {
    @Composable
    fun Working(message: String) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = DinoColors.Bg,
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = DinoColors.Amber, modifier = Modifier.size(28.dp))
                    Text(message, style = DinoType.Body, color = DinoColors.Ink)
                }
            },
        )
    }
}

/** Fiche de vérification avant d'ajouter au journal : tout est modifiable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSheet(draft: MealDraft, onSave: (String, Nutrients, MealSource) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(draft.name) }
    var grams by remember { mutableStateOf(if (draft.grams > 0) fmt(draft.grams) else "") }
    val base = draft.per100g ?: draft.total
    var kcal by remember { mutableStateOf(fmt(base.kcal)) }
    var protein by remember { mutableStateOf(fmt(base.protein)) }
    var carbs by remember { mutableStateOf(fmt(base.carbs)) }
    var fat by remember { mutableStateOf(fmt(base.fat)) }

    val entered = Nutrients(Formats.parseDecimal(kcal), Formats.parseDecimal(protein), Formats.parseDecimal(carbs), Formats.parseDecimal(fat))
    val total = if (draft.per100g != null) entered * (Formats.parseDecimal(grams) / 100) else entered

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DinoColors.Bg,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(draft.source.label, style = DinoType.Small, color = DinoColors.AmberInk)
            Text("Vérifier et ajouter", style = DinoType.Title.copy(fontSize = 20.sp), color = DinoColors.Ink)
            draft.note?.let { Text(it, style = DinoType.Small, color = DinoColors.InkDim) }
            draft.items.forEach { Text("• $it", style = DinoType.Small, color = DinoColors.Ink) }
            Field("Nom", name, KeyboardType.Text, Modifier.fillMaxWidth()) { name = it }
            if (draft.per100g != null) {
                Field("Quantité mangée (g)", grams, KeyboardType.Decimal, Modifier.fillMaxWidth()) { grams = it }
                Text("Valeurs pour 100 g", style = DinoType.Small, color = DinoColors.InkDim)
            } else {
                Text("Valeurs pour la portion", style = DinoType.Small, color = DinoColors.InkDim)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Calories", kcal, KeyboardType.Decimal, Modifier.weight(1f)) { kcal = it }
                Field("Protéines g", protein, KeyboardType.Decimal, Modifier.weight(1f)) { protein = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Glucides g", carbs, KeyboardType.Decimal, Modifier.weight(1f)) { carbs = it }
                Field("Lipides g", fat, KeyboardType.Decimal, Modifier.weight(1f)) { fat = it }
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DinoColors.AmberSoft).padding(14.dp)) {
                Text(
                    "Total : ${Formats.int(total.kcal.roundToInt())} kcal, P ${total.protein.roundToInt()} g, " +
                        "G ${total.carbs.roundToInt()} g, L ${total.fat.roundToInt()} g",
                    style = DinoType.BodyBold, color = DinoColors.Ink,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    "Ajouter au journal",
                    onClick = { onSave(name.ifBlank { draft.source.label }, total, draft.source) },
                    style = PillStyle.Primary, enabled = total.kcal > 0,
                )
                PillButton("Annuler", onClick = onDismiss, style = PillStyle.Ghost)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(state: MealsUiState.Success, onSetGoal: (Int) -> Unit, onSetApiKey: (String) -> Unit, onDismiss: () -> Unit) {
    var goal by remember { mutableStateOf(state.goal.toString()) }
    var key by remember { mutableStateOf(state.apiKey) }
    val uri = LocalUriHandler.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DinoColors.Bg,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Réglages des repas", style = DinoType.Title.copy(fontSize = 20.sp), color = DinoColors.Ink)
            Field("Objectif de calories par jour", goal, KeyboardType.Number, Modifier.fillMaxWidth()) { goal = it.filter(Char::isDigit).take(4) }
            Text(
                "Proposé d'après ton profil : ${Formats.int(state.suggestedGoal)} kcal (dépense estimée, activité modérée).",
                style = DinoType.Small, color = DinoColors.InkDim,
            )
            HorizontalDivider(color = DinoColors.Line, modifier = Modifier.padding(vertical = 6.dp))
            Text("Clé API Claude (facultative)", style = DinoType.BodyBold, color = DinoColors.Ink)
            Text(
                "Sert uniquement à analyser les photos de plats. Elle reste sur ton téléphone. " +
                    "Chaque analyse est facturée sur ton compte Claude (de l'ordre d'un centime par photo).",
                style = DinoType.Small, color = DinoColors.InkDim,
            )
            OutlinedTextField(
                value = key, onValueChange = { key = it.trim() },
                label = { Text("sk-ant-…") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DinoColors.Amber, cursorColor = DinoColors.Amber),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Créer une clé sur console.anthropic.com",
                style = DinoType.Small.copy(textDecoration = TextDecoration.Underline), color = DinoColors.AmberInk,
                modifier = Modifier.pressClickable { uri.openUri("https://console.anthropic.com/settings/keys") }.padding(vertical = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    "Enregistrer",
                    onClick = {
                        goal.toIntOrNull()?.let(onSetGoal)
                        onSetApiKey(key)
                        onDismiss()
                    },
                    style = PillStyle.Primary,
                )
                PillButton("Annuler", onClick = onDismiss, style = PillStyle.Ghost)
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, type: KeyboardType, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        textStyle = DinoType.BodyBold.copy(color = DinoColors.Ink),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DinoColors.Amber, unfocusedBorderColor = DinoColors.Line,
            focusedLabelColor = DinoColors.AmberInk, cursorColor = DinoColors.Amber,
        ),
        modifier = modifier,
    )
}

/** Valeur modifiable : sans séparateur de milliers, virgule décimale. */
private fun fmt(v: Double): String = when {
    v == 0.0 -> ""
    v % 1.0 == 0.0 -> v.toLong().toString()
    else -> String.format(java.util.Locale.FRANCE, "%.1f", v)
}
