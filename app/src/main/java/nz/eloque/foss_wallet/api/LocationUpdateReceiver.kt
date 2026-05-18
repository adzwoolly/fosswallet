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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocationUpdateReceiver : BroadcastReceiver() {

    @Inject lateinit var nearbyPassEvaluator: NearbyPassEvaluator

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
                nearbyPassEvaluator.evaluate(location)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
