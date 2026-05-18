package nz.eloque.foss_wallet.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nz.eloque.foss_wallet.notifications.NotificationService
import nz.eloque.foss_wallet.persistence.NearbyPassesStore
import nz.eloque.foss_wallet.persistence.PassStore

@AndroidEntryPoint
class LocationUpdateReceiver : BroadcastReceiver() {

    @Inject lateinit var passStore: PassStore
    @Inject lateinit var nearbyPassesStore: NearbyPassesStore
    @Inject lateinit var notificationService: NotificationService

    override fun onReceive(context: Context, intent: Intent) {
        val location: Location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED, Location::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED)
        } ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val allPasses = passStore.allPasses().first().filter { !it.metadata.archived }
                val nearby = allPasses.mapNotNull { localizedPass ->
                    val matchedLoc = localizedPass.pass.locations.firstOrNull { loc -> location.distanceTo(loc) <= 50f }
                    matchedLoc?.let { localizedPass.pass to it }
                }
                nearbyPassesStore.setNearbyPassIds(nearby.map { it.first.id }.toSet())
                notificationService.createNearbyNotificationChannel()
                notificationService.updateNearbyNotifications(nearby)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
