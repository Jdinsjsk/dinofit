package com.dinofit.app.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinofit.app.R

/** Palette claire : mêmes valeurs que la version PC. */
object DinoColors {
    val Bg = Color(0xFFFFFFFF)
    val Card = Color(0xFFF6F4EF)
    val Card2 = Color(0xFFECE8DF)
    val Line = Color(0xFFE4DFD3)
    val Ink = Color(0xFF1F2421)
    val InkDim = Color(0xFF5D635B)
    val InkFaint = Color(0xFF8E9389)
    val Amber = Color(0xFFF2A01E)
    val AmberInk = Color(0xFFA35F00)
    val AmberSoft = Color(0xFFFFF3DD)
    val OnAmber = Color(0xFF2A1C05)
    val Lava = Color(0xFFD9472F)
    val Green = Color(0xFF3F8F3A)
    val GreenMid = Color(0xFF8CC46F)
    val OnGreen = Color(0xFFFFFFFF)
    val Sky1 = Color(0xFFFFF9EE)
    val Sky2 = Color(0xFFFCEBD0)
    val Strata1 = Color(0xFFE8CFA4)
    val Strata2 = Color(0xFFDDBF8E)
    val Strata3 = Color(0xFFD1B07B)
    val StrataTop = Color(0xFFB88F55)
    val SoilInk = Color(0xFF4A3518)
    val Glow1 = Color(0xFFFFF4DC)
    val Glow2 = Color(0xFFFFE6B8)
    val Glow3 = Color(0xFFFBD58E)
}

val ArchivoBlack = FontFamily(Font(R.font.archivo_black, FontWeight.Normal))
val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
)

object DinoType {
    val Display = TextStyle(fontFamily = ArchivoBlack, fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.4).sp)
    val RankName = TextStyle(fontFamily = ArchivoBlack, fontSize = 32.sp, lineHeight = 34.sp, letterSpacing = (-0.6).sp)
    val Title = TextStyle(fontFamily = ArchivoBlack, fontSize = 18.sp, lineHeight = 22.sp)
    val Number = TextStyle(fontFamily = ArchivoBlack, fontSize = 22.sp, lineHeight = 26.sp)
    val Body = TextStyle(fontFamily = DmSans, fontSize = 15.sp, lineHeight = 21.sp)
    val BodyBold = Body.copy(fontWeight = FontWeight.Bold)
    val Small = TextStyle(fontFamily = DmSans, fontSize = 13.sp, lineHeight = 18.sp)
    val Label = TextStyle(fontFamily = DmSans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
}

private val scheme = lightColorScheme(
    primary = DinoColors.Amber,
    onPrimary = DinoColors.OnAmber,
    secondary = DinoColors.Green,
    onSecondary = DinoColors.OnGreen,
    background = DinoColors.Bg,
    onBackground = DinoColors.Ink,
    surface = DinoColors.Bg,
    onSurface = DinoColors.Ink,
    surfaceVariant = DinoColors.Card,
    onSurfaceVariant = DinoColors.InkDim,
    surfaceContainer = DinoColors.Bg,
    surfaceContainerLow = DinoColors.Bg,
    surfaceContainerHigh = DinoColors.Bg,
    surfaceContainerHighest = DinoColors.Card,
    error = DinoColors.Lava,
    outline = DinoColors.Line,
)

@Composable
fun DinoFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            bodyLarge = DinoType.Body,
            bodyMedium = DinoType.Body,
            bodySmall = DinoType.Small,
            labelLarge = DinoType.BodyBold.copy(fontSize = 14.sp),
            labelMedium = DinoType.Label,
            titleLarge = DinoType.Title,
            headlineSmall = DinoType.Title.copy(fontSize = 20.sp),
        ),
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(22.dp),
        ),
        content = content,
    )
}
