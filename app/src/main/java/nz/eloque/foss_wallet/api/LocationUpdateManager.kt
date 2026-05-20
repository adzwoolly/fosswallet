package nz.eloque.foss_wallet.api

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject

class LocationUpdateManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun enable() {
            context.startForegroundService(Intent(context, LocationForegroundService::class.java))
        }

        fun disable() {
            context.stopService(Intent(context, LocationForegroundService::class.java))
        }
    }
