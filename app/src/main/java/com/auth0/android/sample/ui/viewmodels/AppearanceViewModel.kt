package com.auth0.android.sample.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.auth0.universalcomponents.theme.Auth0Color
import com.auth0.universalcomponents.theme.Auth0ThemeConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThemeOption(
    val label: String,
    val darkTheme: Boolean?,
    val configuration: Auth0ThemeConfiguration = Auth0ThemeConfiguration.Default
)

class AppearanceViewModel : ViewModel() {

    val themeOptions: List<ThemeOption> = listOf(
        ThemeOption(
            label = "Automatic",
            darkTheme = null,
            configuration = Auth0ThemeConfiguration.Default
        ),
        ThemeOption(
            label = "Light",
            darkTheme = false,
            configuration = Auth0ThemeConfiguration(color = Auth0Color.light())
        ),
        ThemeOption(
            label = "Dark",
            darkTheme = true,
            configuration = Auth0ThemeConfiguration(color = Auth0Color.dark())
        ),
    )

    private val _selectedIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    private val _appliedOption: MutableStateFlow<ThemeOption> = MutableStateFlow(themeOptions[0])
    val appliedOption: StateFlow<ThemeOption> = _appliedOption.asStateFlow()

    fun selectTheme(index: Int) {
        _selectedIndex.value = index
    }

    /**
     * Applies the currently selected theme option globally.
     */
    fun applySelectedTheme() {
        val option = themeOptions.getOrNull(_selectedIndex.value) ?: themeOptions[0]
        _appliedOption.value = option
    }
}
