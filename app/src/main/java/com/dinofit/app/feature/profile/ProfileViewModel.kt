package com.dinofit.app.feature.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinofit.app.core.data.BackupRepository
import com.dinofit.app.core.data.ProfileRepository
import com.dinofit.app.core.model.UserProfile
import com.dinofit.app.core.network.AppUpdate
import com.dinofit.app.core.network.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: UserProfile) : ProfileUiState
}

sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data class Available(val update: AppUpdate) : UpdateUi
    data object Failed : UpdateUi
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val backup: BackupRepository,
    private val updates: UpdateChecker,
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = profileRepository.profile
        .map<UserProfile, ProfileUiState> { ProfileUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    /** Résultat de la dernière sauvegarde / restauration, affiché sous les boutons. */
    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    private val _update = MutableStateFlow<UpdateUi>(UpdateUi.Idle)
    val update: StateFlow<UpdateUi> = _update.asStateFlow()

    val installedVersion: String get() = updates.installedName

    fun update(profile: UserProfile) {
        viewModelScope.launch { profileRepository.update(profile) }
    }

    fun exportTo(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            _backupMessage.value = try {
                val json = backup.exportJson()
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) } ?: error("fichier")
                }
                "Sauvegarde enregistrée. Garde ce fichier en lieu sûr (Google Drive, PC…)."
            } catch (e: Exception) {
                "La sauvegarde n'a pas pu être écrite. Réessaie à un autre endroit."
            }
        }
    }

    fun importFrom(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            _backupMessage.value = try {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: error("fichier")
                }
                val r = backup.importJson(text)
                if (r.sessions + r.stepDays + r.meals + r.walks + r.templates == 0) {
                    "Rien de nouveau dans ce fichier : tout y était déjà."
                } else {
                    "Ajouté : ${r.sessions} séance(s), ${r.stepDays} jour(s) de pas, ${r.meals} repas, " +
                        "${r.walks} marche(s), ${r.templates} modèle(s). Profil mis à jour."
                }
            } catch (e: Exception) {
                "Ce fichier n'est pas une sauvegarde DinoFit lisible."
            }
        }
    }

    fun checkUpdate() {
        viewModelScope.launch {
            _update.value = UpdateUi.Checking
            _update.value = when (val r = updates.check()) {
                is UpdateChecker.Result.Available -> UpdateUi.Available(r.update)
                UpdateChecker.Result.UpToDate -> UpdateUi.UpToDate
                UpdateChecker.Result.Failed -> UpdateUi.Failed
            }
        }
    }
}
