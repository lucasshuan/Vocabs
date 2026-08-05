package io.github.lucasshuan.vocabu.ui.settings

import io.github.lucasshuan.vocabu.R
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lucasshuan.vocabu.shared.AppContainer
import io.github.lucasshuan.vocabu.shared.ThemePreference
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = AppContainer.preferences(app)
    private val repository = AppContainer.repository(app)

    val theme: StateFlow<ThemePreference> = preferences.observeTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), preferences.theme)

    /**
     * The native language, observed rather than read once.
     *
     * Changing it opens the picker over this screen and returns here: without the
     * flow the row would keep showing the old language until Settings was
     * recreated — and that row is the only proof the change took.
     */
    val native: StateFlow<String> = preferences.observeNativeLanguage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), preferences.native)

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    fun chooseTheme(value: ThemePreference) {
        preferences.theme = value
    }

    fun export(onReady: (File) -> Unit, onError: (String) -> Unit) {
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            runCatching {
                VocabuExporter.create(getApplication(), repository.exportData())
            }.onSuccess(onReady).onFailure { onError(it.message ?: getApplication<Application>().getString(R.string.export_failed)) }
            _exporting.value = false
        }
    }
}
