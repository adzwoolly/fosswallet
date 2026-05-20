package nz.eloque.foss_wallet.persistence

import android.content.SharedPreferences
import androidx.core.content.edit
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class NearbyPassesStore
    @Inject
    constructor(
        private val prefs: SharedPreferences,
    ) {
        private val _nearbyPassIds = MutableStateFlow(load())
        val nearbyPassIds: StateFlow<Set<String>> = _nearbyPassIds.asStateFlow()

        fun setNearbyPassIds(ids: Set<String>) {
            prefs.edit { putStringSet(KEY, ids) }
            _nearbyPassIds.value = ids
        }

        private fun load(): Set<String> = prefs.getStringSet(KEY, emptySet()) ?: emptySet()

        companion object {
            private const val KEY = "nearbyPassIds"
        }
    }
