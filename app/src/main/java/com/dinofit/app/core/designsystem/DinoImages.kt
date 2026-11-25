package com.dinofit.app.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.dinofit.app.R
import com.dinofit.app.core.model.DinoRank

@DrawableRes
fun dinoDrawable(id: String): Int = when (id) {
    "fossile" -> R.drawable.dino_fossile
    "archeopteryx" -> R.drawable.dino_archeopteryx
    "dimorphodon" -> R.drawable.dino_dimorphodon
    "velociraptor" -> R.drawable.dino_velociraptor
    "dilophosaure" -> R.drawable.dino_dilophosaure
    "corythosaure" -> R.drawable.dino_corythosaure
    "parasaurolophus" -> R.drawable.dino_parasaurolophus
    "pteranodon" -> R.drawable.dino_pteranodon
    "stegosaure" -> R.drawable.dino_stegosaure
    "ankylosaure" -> R.drawable.dino_ankylosaure
    "utahraptor" -> R.drawable.dino_utahraptor
    "triceratops" -> R.drawable.dino_triceratops
    "carnotaure" -> R.drawable.dino_carnotaure
    "allosaure" -> R.drawable.dino_allosaure
    "elasmosaure" -> R.drawable.dino_elasmosaure
    "brachiosaure" -> R.drawable.dino_brachiosaure
    "mosasaure" -> R.drawable.dino_mosasaure
    "spinosaure" -> R.drawable.dino_spinosaure
    else -> R.drawable.dino_tyrannosaure
}

/** Image du dinosaure ; [locked] l'affiche en silhouette grise (rang pas encore débloqué). */
@Composable
fun DinoImage(rank: DinoRank, modifier: Modifier = Modifier, locked: Boolean = false, contentDescription: String? = null) {
    Image(
        painter = painterResource(dinoDrawable(rank.id)),
        contentDescription = contentDescription,
        colorFilter = if (locked) ColorFilter.tint(Color.Black.copy(alpha = 0.16f), BlendMode.SrcIn) else null,
        modifier = modifier.aspectRatio(1f),
    )
}
