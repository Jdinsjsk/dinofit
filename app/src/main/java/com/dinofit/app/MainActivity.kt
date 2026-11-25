package com.dinofit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dinofit.app.core.designsystem.DinoFitTheme
import com.dinofit.app.navigation.DinoFitApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DinoFitTheme {
                DinoFitApp()
            }
        }
    }
}
