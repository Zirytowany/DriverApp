package eu.mobileApp.DriverApp.mapComponents

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager

class MarkerReceiver:BroadcastReceiver() {
    internal var isGpsEnabled: Boolean = false
    internal var isNetworkEnabled: Boolean = false

    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.let { act ->
            if (act.matches("android.location.PROVIDERS_CHANGED".toRegex())) {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                isNetworkEnabled =
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                //Timber.i("Location Providers changed, is GPS Enabled: " + isGpsEnabled)
                //Start your Activity if location was enabled:
                MarkerSettings.setChangeAndPost(isGpsEnabled) //notify
                //if (isGpsEnabled) {
                //Some action
                //}
            }
        }
    }
}