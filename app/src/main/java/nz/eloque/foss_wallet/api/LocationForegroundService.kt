package nz.eloque.foss_wallet.api

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import nz.eloque.foss_wallet.notifications.NotificationService

private const val FOREGROUND_NOTIFICATION_ID = 2

@AndroidEntryPoint
class LocationForegroundService :
    Service(),
    LocationListener {
    @Inject lateinit var nearbyPassEvaluator: NearbyPassEvaluator

    @Inject lateinit var notificationService: NotificationService

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var locationManager: LocationManager

    @SuppressLint("MissingPermission")
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        notificationService.createLocationTrackingChannel()
        startForeground(FOREGROUND_NOTIFICATION_ID, notificationService.buildLocationTrackingNotification())

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60_000L, 20f, this)
        }

        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        scope.launch {
            nearbyPassEvaluator.evaluate(location)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
