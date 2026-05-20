package nz.eloque.foss_wallet.api

import android.content.Context
import android.location.Location
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import nz.eloque.foss_wallet.notifications.NotificationService
import nz.eloque.foss_wallet.persistence.NearbyPassesStore
import nz.eloque.foss_wallet.persistence.PassStore
import nz.eloque.foss_wallet.widget.NearbyPassesWidget

@Singleton
class NearbyPassEvaluator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val passStore: PassStore,
        private val nearbyPassesStore: NearbyPassesStore,
        private val notificationService: NotificationService,
    ) {
        suspend fun evaluate(location: Location) {
            val allPasses = passStore.allPasses().first().filter { !it.metadata.archived }
            val nearby =
                allPasses.mapNotNull { localizedPass ->
                    val threshold = (localizedPass.pass.maxDistance ?: 100.0).toFloat()
                    val matchedLoc = localizedPass.pass.locations.firstOrNull { loc -> location.distanceTo(loc) <= threshold }
                    matchedLoc?.let { localizedPass.pass to it }
                }
            nearbyPassesStore.setNearbyPassIds(nearby.map { it.first.id }.toSet())
            notificationService.createNearbyNotificationChannel()
            notificationService.updateNearbyNotifications(nearby)
            updateWidget()
        }

        private suspend fun updateWidget() {
            val widget = NearbyPassesWidget()
            GlanceAppWidgetManager(context)
                .getGlanceIds(NearbyPassesWidget::class.java)
                .forEach { widget.update(context, it) }
        }
    }
