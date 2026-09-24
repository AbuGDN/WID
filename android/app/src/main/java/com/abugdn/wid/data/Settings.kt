package com.abugdn.wid.data

import android.content.SharedPreferences
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val notifyUrgent: Boolean = true,
    val notifyTop: Boolean = true,
    val dailyDigest: Boolean = true,
    val digestHour: Int = 8,
    /** Regiões (tags) que geram notificação. Vazio = todas. */
    val regions: Set<String> = emptySet(),
    val quietHours: Boolean = true,
    val quietStart: Int = 22,
    val quietEnd: Int = 7,
    val theme: ThemeMode = ThemeMode.SYSTEM,
) {
    fun matchesRegion(tags: List<String>) = regions.isEmpty() || tags.any { it in regions }

    fun isQuiet(now: LocalTime = LocalTime.now()): Boolean {
        if (!quietHours) return false
        val h = now.hour
        return if (quietStart > quietEnd) h >= quietStart || h < quietEnd else h in quietStart until quietEnd
    }
}

class SettingsStore(private val prefs: SharedPreferences) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state.asStateFlow()
    val value: Settings get() = _state.value

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_state.value)
        prefs.edit()
            .putBoolean("s_notify_urgent", next.notifyUrgent)
            .putBoolean("s_notify_top", next.notifyTop)
            .putBoolean("s_daily_digest", next.dailyDigest)
            .putInt("s_digest_hour", next.digestHour)
            .putStringSet("s_regions", next.regions)
            .putBoolean("s_quiet", next.quietHours)
            .putInt("s_quiet_start", next.quietStart)
            .putInt("s_quiet_end", next.quietEnd)
            .putString("s_theme", next.theme.name)
            .apply()
        _state.value = next
    }

    private fun load() = Settings(
        notifyUrgent = prefs.getBoolean("s_notify_urgent", true),
        notifyTop = prefs.getBoolean("s_notify_top", true),
        dailyDigest = prefs.getBoolean("s_daily_digest", true),
        digestHour = prefs.getInt("s_digest_hour", 8),
        regions = prefs.getStringSet("s_regions", emptySet())!!.toSet(),
        quietHours = prefs.getBoolean("s_quiet", true),
        quietStart = prefs.getInt("s_quiet_start", 22),
        quietEnd = prefs.getInt("s_quiet_end", 7),
        theme = runCatching { ThemeMode.valueOf(prefs.getString("s_theme", null)!!) }.getOrDefault(ThemeMode.SYSTEM),
    )
}
