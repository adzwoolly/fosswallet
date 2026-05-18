package nz.eloque.foss_wallet.api

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject

class LocationUpdateManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun enable() {
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            60_000L,
            20f,
            buildPendingIntent(),
        )
    }

    fun disable() {
        locationManager.removeUpdates(buildPendingIntent())
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, LocationUpdateReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
}
