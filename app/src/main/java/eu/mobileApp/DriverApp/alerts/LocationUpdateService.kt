package eu.mobileApp.DriverApp.alerts

import eu.mobileApp.DriverApp.R
import android.app.*
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*



class LocationUpdateService : Service() {
    private val EXTRA_STARTED_FROM_NOTIFICATION = "started_from_notification"
    private val mBinder = LocalBinder()
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    private val NOTIFICATION_ID = 123456789
    private var mChangingConfiguration = false
    private var mLocationRequest: LocationRequest? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var mLocationCallback: LocationCallback? = null
    private var mServiceHandler: Handler? = null
    private var currentLocation: Location? = null
    private var TAG = "LocationUpdatesService"


    override fun onCreate() {
        super.onCreate()
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.let { mLocationResult ->
                    onNewLocation(mLocationResult.lastLocation)
                }
            }
        }

        createLocationRequest()
        getLastLocation()

        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "Location Service started")
        val startedFromNotification =
            intent?.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)

        if (startedFromNotification!!) {
            removeLocationUpdates()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.e(TAG, "in onBind()")
        stopForeground(true)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        Log.e(TAG, "in onRebind()")
        stopForeground(true)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "Last client unbound from service")
        if (!mChangingConfiguration) {
            Log.i(TAG, "Starting foreground service")
            startForeground(NOTIFICATION_ID, serviceNotification())
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "stopped")
        mServiceHandler?.removeCallbacksAndMessages(null)
    }

    fun requestLocationUpdates() {
        Log.e(TAG, "Requesting location updates")
        startService(Intent(applicationContext, LocationUpdateService::class.java))
        try {
            mFusedLocationProviderClient?.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
        }
    }

    fun removeLocationUpdates() {
        Log.e(TAG, "Removing location updates")
        try {
            mFusedLocationProviderClient?.removeLocationUpdates(mLocationCallback!!)
            stopSelf()
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
        }
    }

    fun getLastLocation() {
        try {
            mFusedLocationProviderClient?.lastLocation?.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    currentLocation = task.result
                } else {
                    Log.e(TAG, "Failed to get location.")
                }
            }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }
    }

    fun onNewLocation(location: Location) {
        //Log.e(TAG, "New location: " + location)

        currentLocation = location
        val pushNotification = Intent("NotifyUser")
        pushNotification.putExtra("pinned_location_lat", currentLocation!!.latitude.toString())
        pushNotification.putExtra("pinned_location_long", currentLocation!!.longitude.toString())
        LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification)
        sendMessageToActivity()
    }

    private fun serviceNotification(): Notification {
        val mNotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        val intent = Intent(this, LocationUpdateService::class.java)
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val mChannel = NotificationChannel(
                "location_service_channel",
                name,
                NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager?.createNotificationChannel(mChannel)
        }
        val builder = NotificationCompat.Builder(this)
            .setContentTitle("Location Service")
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setWhen(System.currentTimeMillis())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("location_service_channel")
        } else {
            builder.priority = Notification.PRIORITY_HIGH
        }

        return builder.build()
    }

    fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest?.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest?.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        Log.e(TAG, "createLocationRequest")
    }

    inner class LocalBinder() : Binder() {
        internal val service: LocationUpdateService
            get() = this@LocationUpdateService
    }

    private fun sendMessageToActivity() {
        val intent = Intent("GPSLocationChanged")
        intent.putExtra("Latitude", currentLocation?.latitude.toString())
        intent.putExtra("Longitude", currentLocation?.longitude.toString())
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun stopService(name: Intent?): Boolean {
        return super.stopService(name)
    }
}