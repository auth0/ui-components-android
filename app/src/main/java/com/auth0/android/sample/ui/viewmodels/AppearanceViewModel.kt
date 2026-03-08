package com.auth0.android.sample.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThemeOption(
    val label: String,
    val presetIndex: Int
)

class AppearanceViewModel : ViewModel() {

    val themeOptions: List<ThemeOption> = listOf(
        ThemeOption("Automatic", presetIndex = 0),
        ThemeOption("Light", presetIndex = 0),
        ThemeOption("Dark", presetIndex = 1),
        ThemeOption("Custom Theme (Olive)", presetIndex = 6),
        ThemeOption("Custom Theme (Purple)", presetIndex = 7),
    )

    private val _selectedIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    fun selectTheme(index: Int) {
        _selectedIndex.value = index
    }

    /**
     * Returns the ThemePreset index for the currently selected option.
     */
    fun getSelectedPresetIndex(): Int {
        return themeOptions.getOrNull(_selectedIndex.value)?.presetIndex ?: 0
    }
}
