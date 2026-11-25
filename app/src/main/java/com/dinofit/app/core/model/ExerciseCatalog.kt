package com.dinofit.app.core.model

import com.dinofit.app.core.model.MuscleGroup.ABS
import com.dinofit.app.core.model.MuscleGroup.ARMS
import com.dinofit.app.core.model.MuscleGroup.BACK
import com.dinofit.app.core.model.MuscleGroup.CHEST
import com.dinofit.app.core.model.MuscleGroup.LEGS
import com.dinofit.app.core.model.MuscleGroup.SHOULDERS

/** Même catalogue que la version PC, mêmes identifiants. */
val ExerciseCatalog: List<Exercise> = listOf(
    Exercise("bench", "Développé couché", CHEST),
    Exercise("incline", "Développé incliné haltères", CHEST),
    Exercise("fly", "Écarté à la poulie", CHEST),
    Exercise("dips", "Dips", CHEST, bodyweight = true),
    Exercise("pushup", "Pompes", CHEST, bodyweight = true),
    Exercise("chestpress", "Chest press machine", CHEST),
    Exercise("deadlift", "Soulevé de terre", BACK),
    Exercise("row", "Rowing barre", BACK),
    Exercise("pullup", "Tractions", BACK, bodyweight = true),
    Exercise("latpull", "Tirage vertical", BACK),
    Exercise("seatedrow", "Tirage horizontal", BACK),
    Exercise("dbrow", "Rowing haltère", BACK),
    Exercise("squat", "Squat", LEGS),
    Exercise("legpress", "Presse à cuisses", LEGS),
    Exercise("lunge", "Fentes", LEGS),
    Exercise("rdl", "Soulevé de terre jambes tendues", LEGS),
    Exercise("hipthrust", "Hip thrust", LEGS),
    Exercise("legext", "Leg extension", LEGS),
    Exercise("legcurl", "Leg curl", LEGS),
    Exercise("calf", "Mollets debout", LEGS),
    Exercise("ohp", "Développé militaire", SHOULDERS),
    Exercise("dbpress", "Développé haltères assis", SHOULDERS),
    Exercise("lateral", "Élévations latérales", SHOULDERS),
    Exercise("facepull", "Face pull", SHOULDERS),
    Exercise("shrug", "Shrugs", SHOULDERS),
    Exercise("curl", "Curl barre", ARMS),
    Exercise("hammer", "Curl marteau", ARMS),
    Exercise("pushdown", "Extension triceps poulie", ARMS),
    Exercise("skull", "Barre au front", ARMS),
    Exercise("closebench", "Développé couché prise serrée", ARMS),
    Exercise("crunch", "Crunch à la poulie", ABS),
    Exercise("legraise", "Relevé de jambes", ABS, bodyweight = true),
    Exercise("plank", "Gainage lesté", ABS, bodyweight = true),
    Exercise("abwheel", "Roue abdominale", ABS, bodyweight = true),
)
