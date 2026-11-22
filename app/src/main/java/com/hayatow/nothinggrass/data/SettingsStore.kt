package com.hayatow.nothinggrass.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ColorMode { MONO, GREEN, WALLPAPER }

data class WidgetState(
    val login: String = "",
    val token: String = "",
    val colorMode: ColorMode = ColorMode.MONO,
    val calendar: ContributionCalendar? = null,
    val updatedAt: Long = 0,
    val error: String? = null,
)

private val Context.dataStore by preferencesDataStore("grass")

class SettingsStore(private val context: Context) {
    private object Keys {
        val login = stringPreferencesKey("login")
        val token = stringPreferencesKey("token")
        val color = stringPreferencesKey("color")
        val cache = stringPreferencesKey("cache")
        val updatedAt = longPreferencesKey("updated_at")
        val error = stringPreferencesKey("error")
    }

    val state: Flow<WidgetState> = context.dataStore.data.map { p ->
        WidgetState(
            login = p[Keys.login].orEmpty(),
            token = p[Keys.token].orEmpty(),
            colorMode = p[Keys.color]?.let { runCatching { ColorMode.valueOf(it) }.getOrNull() } ?: ColorMode.MONO,
            calendar = p[Keys.cache]?.let { runCatching { CalendarCodec.decode(it) }.getOrNull() },
            updatedAt = p[Keys.updatedAt] ?: 0,
            error = p[Keys.error],
        )
    }

    suspend fun current() = state.first()

    suspend fun saveSettings(login: String, token: String, colorMode: ColorMode) {
        context.dataStore.edit { p ->
            if (p[Keys.login] != login) { p.remove(Keys.cache); p.remove(Keys.updatedAt) }
            p[Keys.login] = login
            p[Keys.token] = token
            p[Keys.color] = colorMode.name
            p.remove(Keys.error)
        }
    }

    /** Ignored when the user switched login while [login] was being fetched. */
    suspend fun saveResult(login: String, result: Result<ContributionCalendar>) {
        context.dataStore.edit { p ->
            if (p[Keys.login] != login) return@edit
            result.onSuccess {
                p[Keys.cache] = CalendarCodec.encode(it)
                p[Keys.updatedAt] = System.currentTimeMillis()
                p.remove(Keys.error)
            }.onFailure { p[Keys.error] = it.message ?: it.javaClass.simpleName }
        }
    }
}
