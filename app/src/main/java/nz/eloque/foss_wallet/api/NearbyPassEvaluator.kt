package nz.eloque.foss_wallet.api

import android.location.Location
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import nz.eloque.foss_wallet.notifications.NotificationService
import nz.eloque.foss_wallet.persistence.NearbyPassesStore
import nz.eloque.foss_wallet.persistence.PassStore

@Singleton
class NearbyPassEvaluator @Inject constructor(
    private val passStore: PassStore,
    private val nearbyPassesStore: NearbyPassesStore,
    private val notificationService: NotificationService,
) {
    suspend fun evaluate(location: Location) {
        val allPasses = passStore.allPasses().first().filter { !it.metadata.archived }
        val nearby = allPasses.mapNotNull { localizedPass ->
            val threshold = (localizedPass.pass.maxDistance ?: 100.0).toFloat()
            val matchedLoc = localizedPass.pass.locations.firstOrNull { loc -> location.distanceTo(loc) <= threshold }
            matchedLoc?.let { localizedPass.pass to it }
        }
        nearbyPassesStore.setNearbyPassIds(nearby.map { it.first.id }.toSet())
        notificationService.createNearbyNotificationChannel()
        notificationService.updateNearbyNotifications(nearby)
    }
}
