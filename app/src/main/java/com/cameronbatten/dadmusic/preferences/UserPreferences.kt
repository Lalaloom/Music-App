package com.cameronbatten.dadmusic.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Font-size preset for the Zen performance view's current line. */
enum class FontSizePreset(val currentLineSp: Int, val neighbourLineSp: Int) {
    SMALL(48, 20),
    MEDIUM(64, 24),
    LARGE(80, 28),
}

/** What the player does when the current song finishes playing to the end. */
enum class EndBehaviour { PAUSE, AUTO_ADVANCE, REPEAT }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * DataStore-backed user preferences. All values have sensible defaults so the
 * first launch has no empty-key dance.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.dataStore

    val lyricOffsetMs: Flow<Long> = store.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { it[LYRIC_OFFSET_KEY] ?: 0L }

    suspend fun setLyricOffsetMs(ms: Long) {
        store.edit { it[LYRIC_OFFSET_KEY] = ms }
    }

    val fontSizePreset: Flow<FontSizePreset> = store.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map {
            val name = it[FONT_SIZE_KEY] ?: FontSizePreset.MEDIUM.name
            runCatching { FontSizePreset.valueOf(name) }.getOrDefault(FontSizePreset.MEDIUM)
        }

    suspend fun setFontSizePreset(preset: FontSizePreset) {
        store.edit { it[FONT_SIZE_KEY] = preset.name }
    }

    val onboardingCompleted: Flow<Boolean> = store.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { it[ONBOARDING_COMPLETED_KEY] ?: false }

    suspend fun markOnboardingComplete() {
        store.edit { it[ONBOARDING_COMPLETED_KEY] = true }
    }

    val onSongEnded: Flow<EndBehaviour> = store.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map {
            val name = it[END_BEHAVIOUR_KEY] ?: EndBehaviour.PAUSE.name
            runCatching { EndBehaviour.valueOf(name) }.getOrDefault(EndBehaviour.PAUSE)
        }

    suspend fun setOnSongEnded(behaviour: EndBehaviour) {
        store.edit { it[END_BEHAVIOUR_KEY] = behaviour.name }
    }

    /**
     * Encoded pedal bindings (semicolon-separated `keyCode:ACTION` — see
     * [com.cameronbatten.dadmusic.pedal.PedalBindings]). Empty string = no
     * bindings configured.
     */
    val pedalBindingsText: Flow<String> = store.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { it[PEDAL_BINDINGS_KEY] ?: "" }

    suspend fun setPedalBindingsText(text: String) {
        store.edit { it[PEDAL_BINDINGS_KEY] = text }
    }

    /**
     * Admin-only entitlement override. `null` means "no override — use the
     * entitlement source's own value". Non-null forces `proEnabled` to that
     * boolean. Lets the author QA Free-tier behaviour on the sideload build.
     */
    val adminProOverride: Flow<Boolean?> = store.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { prefs ->
            when (prefs[ADMIN_PRO_OVERRIDE_KEY]) {
                "pro" -> true
                "free" -> false
                else -> null
            }
        }

    suspend fun setAdminProOverride(value: Boolean?) {
        store.edit {
            if (value == null) it.remove(ADMIN_PRO_OVERRIDE_KEY)
            else it[ADMIN_PRO_OVERRIDE_KEY] = if (value) "pro" else "free"
        }
    }

    /**
     * Admin → Reset device state: clear every preference except the SAF folder
     * registry (`FolderRegistry` manages that separately in SharedPreferences,
     * so it isn't touched here) and reset `onboarding_completed` so the next
     * launch lands on the Welcome screen.
     */
    suspend fun resetAllExceptFolders() {
        store.edit { it.clear() }
    }

    private companion object {
        val LYRIC_OFFSET_KEY = longPreferencesKey("lyric_offset_ms")
        val FONT_SIZE_KEY = stringPreferencesKey("font_size_preset")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val END_BEHAVIOUR_KEY = stringPreferencesKey("end_behaviour")
        val ADMIN_PRO_OVERRIDE_KEY = stringPreferencesKey("admin_pro_override")
        val PEDAL_BINDINGS_KEY = stringPreferencesKey("pedal_bindings")
    }
}
