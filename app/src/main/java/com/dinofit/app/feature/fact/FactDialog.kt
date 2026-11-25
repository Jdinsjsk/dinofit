package com.dinofit.app.feature.fact

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoImage
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.PillButton
import com.dinofit.app.core.designsystem.PillStyle

@Composable
fun FactDialog(state: FactUiState, onDismiss: () -> Unit) {
    if (state == FactUiState.Dismissed) return
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DinoColors.Bg)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is FactUiState.Shown -> {
                    DinoImage(state.rank, Modifier.size(150.dp))
                    Text("Le fait dino du jour", style = DinoType.BodyBold.copy(fontSize = 14.sp), color = DinoColors.AmberInk)
                    Text(state.fact.dino, style = DinoType.Title.copy(fontSize = 20.sp), color = DinoColors.Ink, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                    Text(state.fact.fact, style = DinoType.Body.copy(fontSize = 17.sp, lineHeight = 25.sp), color = DinoColors.Ink, textAlign = TextAlign.Center)
                    val url = state.fact.sourceUrl
                    if (url != null) {
                        Text(
                            "Source : Wikipédia via dinofacts.fyi (en anglais)",
                            style = DinoType.Small.copy(textDecoration = TextDecoration.Underline),
                            color = DinoColors.InkFaint, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp).clickable { uriHandler.openUri(url) },
                        )
                    } else {
                        Text("Fait hors connexion", style = DinoType.Small, color = DinoColors.InkFaint, modifier = Modifier.padding(top = 10.dp))
                    }
                }
                else -> {
                    Text("Le fait dino du jour", style = DinoType.BodyBold.copy(fontSize = 14.sp), color = DinoColors.AmberInk)
                    Text("Chargement…", style = DinoType.Title.copy(fontSize = 20.sp), color = DinoColors.Ink, modifier = Modifier.padding(vertical = 10.dp))
                    Box(Modifier.width(220.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(DinoColors.Card2))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(DinoColors.Card2))
                }
            }
            Spacer(Modifier.height(16.dp))
            PillButton("C'est parti", onClick = onDismiss, style = PillStyle.Primary, big = true, modifier = Modifier.fillMaxWidth())
        }
    }
}
