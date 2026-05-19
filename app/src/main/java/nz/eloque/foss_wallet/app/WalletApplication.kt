package nz.eloque.foss_wallet.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import nz.eloque.foss_wallet.api.LocationUpdateManager
import nz.eloque.foss_wallet.notifications.NotificationService
import nz.eloque.foss_wallet.persistence.SettingsStore

@HiltAndroidApp
class WalletApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationService: NotificationService
    @Inject lateinit var locationUpdateManager: LocationUpdateManager
    @Inject lateinit var settingsStore: SettingsStore

    override fun onCreate() {
        super.onCreate()
        notificationService.createNearbyNotificationChannel()
        notificationService.createLocationTrackingChannel()
        if (settingsStore.isLocationEnabled()) {
            locationUpdateManager.enable()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
