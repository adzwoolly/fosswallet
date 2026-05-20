package nz.eloque.foss_wallet.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var locationUpdateManager: LocationUpdateManager

    @Inject
    lateinit var settingsStore: SettingsStore

    override fun onCreate() {
        super.onCreate()
        notificationService.createNearbyNotificationChannel()
        notificationService.createLocationTrackingChannel()
        if (settingsStore.isLocationEnabled()) {
            // Defer startForegroundService until the app is actually in the foreground.
            // Android 12+ blocks foreground service starts from background processes (e.g.
            // when the process is created to handle a widget update broadcast).
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        locationUpdateManager.enable()
                        owner.lifecycle.removeObserver(this)
                    }
                },
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
