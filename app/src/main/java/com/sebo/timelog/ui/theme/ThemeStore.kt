package com.sebo.timelog.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemeStore(private val context: Context) {

    private val KEY_THEME = stringPreferencesKey("theme_preference")

    val themePreference: Flow<ThemePreference> = context.themeDataStore.data.map { prefs ->
        val name = prefs[KEY_THEME] ?: ThemePreference.SYSTEM.name
        ThemePreference.entries.firstOrNull { it.name == name } ?: ThemePreference.SYSTEM
    }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_THEME] = preference.name
        }
    }
}

