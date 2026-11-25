# DinoFit pour Android

App native Kotlin + Jetpack Compose (architecture MVVM, Hilt, Room, DataStore, WorkManager).

## Obtenir le fichier .apk sans rien installer (GitHub, gratuit)

1. Crée un compte sur github.com puis un nouveau dépôt (bouton « New »), par exemple `dinofit`.
2. Dans le dépôt, clique sur « uploading an existing file » et glisse **tout le contenu** de ce dossier
   (y compris le dossier caché `.github`). Valide avec « Commit changes ».
3. Ouvre l'onglet **Actions** : la construction démarre seule (environ 5 minutes).
4. Quand elle est verte, ouvre l'onglet **Releases** : `DinoFit.apk` y est. Ouvre cette page depuis ton
   téléphone et télécharge le fichier.
5. Sur le téléphone, autorise l'installation depuis ton navigateur quand Android le demande, puis installe.

## Ou avec Android Studio

Ouvre ce dossier dans Android Studio, attends la synchronisation, puis
« Build > Build App Bundle(s) / APK(s) > Build APK(s) ».

## Pas et trajets

- **Pas** : comptés automatiquement toute la journée par le podomètre du téléphone, même app fermée.
  L'autorisation « Activité physique » est demandée à la première ouverture.
- **Trajets** : onglet **Marche**, bouton « Démarrer une marche ». Le GPS trace le parcours sur une carte
  OpenStreetMap (gratuite, sans compte), même écran éteint ; une notification permet de terminer.
  Autorisations demandées au premier lancement d'une marche : position, notifications, activité physique.
  Le fond de carte a besoin d'internet ; le trajet, lui, s'enregistre même sans réseau.

## Structure (couches du guide d'architecture Android)

```
app/src/main/java/com/dinofit/app/
  core/model        modèles purs, rangs dinosaures, catalogue d'exercices
  core/common       FitnessMath (calories, distance, tonnage, rangs) + formats français
  core/database     Room : séances, séries, pas par jour, exercices perso
  core/datastore    profil + dernière valeur du podomètre
  core/sensor       podomètre matériel (Sensor.TYPE_STEP_COUNTER)
  walk              service GPS au premier plan (trajet écran éteint + notification)
  core/data         repositories (source de vérité locale)
  core/designsystem thème, couleurs, polices, composants (dinos, carte de rang, anneau de pas)
  core/ui           composants partagés entre écrans
  feature/home      Aujourd'hui      (Screen + ViewModel + UiState)
  feature/walk      Marche : carte OpenStreetMap, trajets, détail
  feature/workout   Séance + passage de rang animé
  feature/ranks     Rangs et records
  feature/profile   Profil
  work              relevé du podomètre toutes les 15 min
```

Tests : `./gradlew testDebugUnitTest` (formules de calcul et rangs).
