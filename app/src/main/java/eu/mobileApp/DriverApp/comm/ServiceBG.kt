package eu.mobileApp.DriverApp.comm

import android.app.*
import android.content.*
import android.content.Intent.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.ConnectivityManager.*
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import eu.mobileApp.DriverApp.R
import eu.mobileApp.DriverApp.StartingScreen
import eu.mobileApp.DriverApp.alerts.LocationUpdateService
import eu.mobileApp.DriverApp.alerts.ScreenReceiver
import eu.mobileApp.DriverApp.alerts.SendSMS
import eu.mobileApp.DriverApp.login.Cookie
import kotlinx.coroutines.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ServiceBG : Service(), SensorEventListener {
    val TAG = "Send_Service"
    private val channel_id = "FService"
    private val channel_com = "FService comm"

    private var latitude = "0.0"
    private var longitude = "0.0"
    private var aid = ""
    private var handlerLocation: Handler = Handler()
    private var handlerSMS = Handler()

    private var i = 0
    private var j = 0
    private var smsIter = 0
    private val locations = mutableListOf<String>()
    private var wait = true
    private var screenOff = false
    private var deviceLocked = false
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null

    private var broadcastReceiver: BroadcastReceiver? = null

    private var mService: LocationUpdateService? = null
    private var mBound: Boolean = false

    private var sURL = "url_string"
    private var client = OkHttpClient()
    private var recogized: Boolean = true
    private var token_csrf: String = ""
    private var waitApi: Boolean = true

    private var numberSMS = "112" //numer bramki sms
    private var localizationNetInterval = 60
    private var postDelayMils = 10000
    private var sosButtonInactivityTime = 10000
    private var smsButtonSensitivity = 100
    private var localizationSMSInterval = 120 //5min
    private var sms = SendSMS()

    private var doOnce = true

    private var waitSMS = true

    private var delayedLocationCond = true
    private var smsSendCond = true
    private var configCorCond = true
    private var postDelayCond = true

    private var lastState = 0

    private var screenReceiver = ScreenReceiver()
    private lateinit var sensorManager: SensorManager
    private var state = 0
    private var counter_screen = 0
    private var state_screen = 0
    private var last_state_screen = 0
    private var last_valueY = 0.0
    private var last_valueX = 0.0
    private var valueX = 0.0
    private var valueY = 0.0
    private var last_valuesY = arrayListOf<Double>()
    private var last_valuesX = arrayListOf<Double>()
    private var last_state = 0
    private var counter = 0
    private var timer_state = 0
    private var stop_data = 1

    private var swipingCounter = 3
    private var screenCounter = 3
    private var swipingTimer = 4000
    private var swipingLowerAcceler = -15
    private var swipingUpperAcceler = 15
    private var swipingLog = "on"
    private var timerScreenState = true
    private var screenBlocked = false
    private var screenTimer = 4000

    private var sosInactivityPeriod = 4000

    private var sendBySwiping = true
    private var acStarted = false

    private var online = true

    private var listenMessage = true
    var phoneNumber = ""

    private var firstLaunch=true
    private var lastBatteryState=true

    private var logged=false

    private var driverID=""
    private var crewID=""
    private var cookie=""

    private var config_json=Config(
        localizationNetInterval,
        localizationSMSInterval,
        sosInactivityPeriod,
        smsButtonSensitivity,
        swipingCounter,
        screenCounter,
        screenTimer,
        swipingTimer,
        swipingLowerAcceler,
        swipingUpperAcceler,
        swipingLog,
        phoneNumber
    )


    private fun timerScreen(delayed: Int) {
        GlobalScope.launch(Dispatchers.Default) {
            if (timerScreenState) {
                timerScreenState = false
                listenMessage = true
                Log.d("ServiceBG_timerScreen", "started")
                delay(delayed.toLong())
                state_screen = 0
                last_state_screen = 0
                counter_screen = 0
                if (!sendBySwiping)
                    timerInactive(sosInactivityPeriod)
                Log.d("ServiceBG_timerScreen", "ended")
                timerScreenState = true
            }
        }
    }

    private fun timerInactive(delayed: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            Log.d("ServiceBG_timerInactive", "started")
            stop()
            sendMessageToActivity("button:false")
            //Log.d("TestBTN", "wysłono false")
            Log.d("TestBTNN", waitSMS.toString())
            Log.d("ServiceBG_timerInactive", "delayed = $delayed")
            delay(delayed.toLong())
            smsCooldown()
            Log.d("ServiceBG_timerInactive", "ended")
        }
    }

    private fun cooldown() {
        stop_data = 1
        last_state_screen = 0
        state_screen = 0
        counter = 0
        counter_screen = 0
        screenBlocked = false
        sendBySwiping = true
    }

    private fun stop() {
        screenBlocked = true
        waitSMS = false
        sendBySwiping = false
        stop_data = 0
        counter = 0
        counter_screen = 0
    }

    private fun timerAcc(delayed: Int) {
        GlobalScope.launch(Dispatchers.Default) {
            Log.d("ServiceBG_timerAcc", "started")
            delay(delayed.toLong())
            timer_state = 0
            state = 0
            counter = 0
            last_valueY = 0.0
            last_valuesY.clear()
            acStarted = false
            Log.d("ServiceBG_timerAcc", "ended")
        }
    }

    private val UserReceiver:BroadcastReceiver=object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val msg=intent?.getStringExtra("logged")
            if(msg=="true"||msg=="false"){
                logged=msg.toBoolean()
            }
        }

    }


    private val LocationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            latitude = intent.getStringExtra("Latitude").toString()
            longitude = intent.getStringExtra("Longitude").toString()
            if (smsIter % 2 == 0) {
                doOnce = true
                smsIter = 0
            }
            smsIter++

            if (isOnline(this@ServiceBG) && !online) {
                getConfig(aid)
                //getConfigJson(aid)
                Log.d("Konfiguracja", "zaczytano")
            }
            online = isOnline(this@ServiceBG)
        }
    }

    private fun delayedLocation() {
        GlobalScope.launch(Dispatchers.Default) {
            Log.d("runnablePost", localizationNetInterval.toString())
            if (isOnline(this@ServiceBG)) {
                try {
                    startLocation("normal", "", "", "", "")
                }catch (e: Error){
                    Log.d("startLocation_Error", e.toString())
                }
            } else {
                locations.add(latitude)
                locations.add(longitude)
                locations.add(getDateTimeLocal())
                locations.add(getDateTimeUTC())
                i++
            }
            if (delayedLocationCond) {
                delay((localizationNetInterval*1000).toLong())
                //checkNetworkSavings()
                delayedLocation()
            } else {
                joinAll()
                cancel()
            }
        }
    }

    private fun smsSend() {
        GlobalScope.launch(Dispatchers.Default) {
            //Log.d("runnableSMS", "weszło")
            //Log.d("runnableSMS", localizationSMSInterval.toString())
            if (!isOnline(this@ServiceBG)) {
                Log.d("runnableSMS", "nie ma internetu")
                sms.sendSMS(
                    numberSMS,
                    "SMSMobilePos;" + latitude + ";" + longitude + ";${getDateTimeUTC()};$aid"
                )
                doOnce = false
            }
            if (smsSendCond) {
                delay((localizationSMSInterval*1000).toLong())
                smsSend()
            } else {
                joinAll()
                cancel()
            }
        }
    }

    private fun configCor() {
        GlobalScope.launch(Dispatchers.Default) {
            try {
                if(isOnline(this@ServiceBG)) {
                    getConfig(aid)
                }
            }catch (e: Error){
                Log.d("getConfig_Error", e.toString())
            }
            /*try {
                if(isOnline(this@ServiceBG)) {
                    //getConfigJson(aid)
                }
            }catch (e: Error){
                Log.d("getConfigJson_Error", e.toString())
            }*/
            if (configCorCond) {
                delay(900000L)
                configCor()
            } else {
                joinAll()
                cancel()
            }
        }
    }

    private fun postDelay() {
        //Log.d("postDelay", "started")
        //Log.d("postDelay", "i=$i")
        //Log.d("postDelay", "j=$j")
        //Log.d("put_location_Delay", "locations.size=${locations.size}")
        GlobalScope.launch(Dispatchers.Default) {
            if (isOnline(this@ServiceBG)) {
                if (i > 0) {
                    j = i
                    i = 0
                } else if (j > 0) {
                    try {
                        startLocation(
                            "delayed",
                            locations[0],
                            locations[1],
                            locations[2],
                            locations[3]
                        )
                    }catch (e: Error){
                        Log.d("startLocation_Error", e.toString())
                    }
                    //zmienić czas na ten z tablicy
                    ///Log.d("put_location_Delay", "delayed, ${locations[0]}, ${locations[1]}, ${locations[2]}, ${locations[3]}")
                    locations.removeAt(0)
                    locations.removeAt(0)
                    locations.removeAt(0)
                    locations.removeAt(0)
                    j--
                }
            }
            if (!wait) {
                if (longitude != "") {
                    try {
                        startLocation("normal", "", "", "", "")
                    }catch (e: Error){
                        Log.d("startLocation_Error", e.toString())
                    }
                }
            }
            if (postDelayCond) {
                delay(postDelayMils.toLong())
                postDelay()
            } else {
                joinAll()
                cancel()
            }
        }
    }

    private fun checkNetworkSavings() {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
            if (isActiveNetworkMetered) {
                when (restrictBackgroundStatus) {
                    RESTRICT_BACKGROUND_STATUS_ENABLED -> {
                        Log.d("Oszczędzanie_danych", "Oszczędzanie danych jest włączone")
                        sendNotification("Oszczędzanie danych jest włączone")
                    }
                    RESTRICT_BACKGROUND_STATUS_WHITELISTED -> {
                        Log.d(
                            "Oszczędzanie_danych",
                            "Oszczędzanie danych jest wyłączone dla tej aplikacji"
                        )
                        sendNotification("Oszczędzanie danych jest wyłączone dla tej aplikacji")
                    }
                    RESTRICT_BACKGROUND_STATUS_DISABLED -> {
                        Log.d("Oszczędzanie_danych", "Oszczędzanie danych jest wyłączone")
                        sendNotification("Oszczędzanie danych jest wyłączone")
                    }
                }
            } else {
                Log.d("Oszczędzanie_danych", "Transmisja danych jest wyłączona")
                sendNotification("Transmisja danych jest wyłączona")
            }
        }
    }

    private fun energySaving(){
        val powerManager = this@ServiceBG.getSystemService(POWER_SERVICE) as PowerManager
        Log.d("PowerSave", powerManager.isPowerSaveMode.toString())
        if(firstLaunch){
            if (powerManager.isPowerSaveMode) {
                sendNotification("Włączone oszczędzanie Baterii")
            } else {
                sendNotification("Wyłączone oszczędzanie Baterii")
            }
            lastBatteryState=powerManager.isPowerSaveMode
            firstLaunch=false
        }
        if(powerManager.isPowerSaveMode!=lastBatteryState) {
            if (powerManager.isPowerSaveMode) {
                sendNotification("Włączone oszczędzanie Baterii")
            } else {
                sendNotification("Wyłączone oszczędzanie Baterii")
            }
        }
        lastBatteryState=powerManager.isPowerSaveMode
    }

    private val code: Runnable = object : Runnable {
        override fun run() {
            var locationServ = LocationServices.getFusedLocationProviderClient(this@ServiceBG)
            try {
                locationServ?.lastLocation?.addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        currentLocation = task.result
                    } else {
                        Log.d(TAG, "Failed to get location.")
                    }
                }
            } catch (unlikely: SecurityException) {
                Log.d(TAG, "Lost location permission.$unlikely")
            }
        }
    }

    private fun smsCooldown() {
        Log.d("TestBTNN", "wysłano true")
        waitSMS = true
        Log.d("TestBTNN", waitSMS.toString())
        sendMessageToActivity("button:true")
        cooldown()
    }

    private fun delayedSmsCooldown(delayed: Int) {
        Log.d("delayedSMSCooldown", "started")
        GlobalScope.launch(Dispatchers.Default) {
            Log.d("delayedSMSCooldown", "delay = $delayed")
            delay(delayed.toLong())
            smsCooldown()
            Log.d("delayedSMSCooldown", "ended")
        }
    }



    init {
        Log.d(TAG, "SERVICE IS RUNNING")
    }

    companion object {
        fun startService(
            context: Context,
            message: String,
            name: String,
            pass: String,
            logged: Boolean
        ) {
            val startIntent = Intent(context, ServiceBG::class.java)
            startIntent.putExtra("inputExtra", message)
            startIntent.putExtra("name", name)
            startIntent.putExtra("pass", pass)
            startIntent.putExtra("logged", false)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, ServiceBG::class.java)
            context.stopService(stopIntent)
        }
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("sendMessage")
            val msg = message.toString()
            //Log.d("Message from activity", msg)
            //Log.d("Message from activity", waitSMS.toString())
            if (msg == "send" && waitSMS) {
                if (isOnline(this@ServiceBG)) {
                    try {
                        startLocation("alert", "", "", "", "")
                        Toast.makeText(
                            this@ServiceBG,
                            "Wysłano alarm z pozycją",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }catch (e: Error){
                        Log.d("startLocation_Error", e.toString())
                    }
                } else {
                    sendNotification("Wysłano alarm z pozycją")
                    Toast.makeText(this@ServiceBG, "Wysłano alarm z pozycją", Toast.LENGTH_SHORT)
                        .show()
                    Log.d("test.sms", "Wysłano alarm z pozycją")
                    sms.sendSMS(
                        numberSMS,
                        "SMSMobileAlarm;" + latitude + ";" + longitude + ";${getDateTimeUTC()};$aid"
                    )
                }
                waitSMS = false
                stop()
                sendMessageToActivity("button:false")
                //Log.d("TestBTNN", "wysłono false")
                //Log.d("TestBTNN", waitSMS.toString())
                screenBlocked = true

                //Log.d("Screen_state",state_screen.toString())
                call(phoneNumber)
                //dial(phoneNumber)

                //do poprawu na coroutine
                delayedSmsCooldown(sosInactivityPeriod)

            }else if(msg=="started"){
                sendDriverId(driverID)
                if (waitSMS){
                    sendMessageToActivity("button:true")
                }else{
                    sendMessageToActivity("button:false")
                }
            }
        }
    }

    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("screen")
            Log.d("SCREEN_MESSAGE", message.toString())
            if (message.equals("ON")) {
                state_screen = 2
                if (state_screen != last_state_screen && last_state_screen != 0 && !screenBlocked) {
                    timerScreen(screenTimer)
                }
                //Log.d("ServiceBG_timerS", "State_screen=$state_screen")
            }
            if (message.equals("OFF")) {
                state_screen = 1
                if (state_screen != last_state_screen && last_state_screen != 0 && !screenBlocked) {
                    timerScreen(screenTimer)
                }
                //Log.d("ServiceBG_timerS", "State_screen=$state_screen")
            }
            if (state_screen != last_state_screen && last_state_screen != 0 && !screenBlocked) {
                counter_screen++
                state_screen = 0
                last_state_screen = 0
            }

            if (counter_screen == screenCounter && sendBySwiping) {
                listenMessage = false
                sendBySwiping = false
                state_screen = 0
                last_state_screen = 0
                counter_screen = 0
                //Log.d("Screen_state", state_screen.toString())
                if (isOnline(this@ServiceBG)) {
                    try {
                        startLocation("alert", "", "", "", "")
                        //Log.d("SMS_", "wysłano POST z pozycją")
                    }catch (e: Error){
                        Log.d("startLocation_Error", e.toString())
                    }
                } else {
                    sendNotification("Wysłano alarm z pozycją")
                    sms.sendSMS(
                        numberSMS,
                        "SMSMobileAlarm;" + latitude + ";" + longitude + ";${getDateTimeUTC()};$aid"
                    )
                    //Log.d("SMS_", "wysłano SMS z pozycją")
                }
                if(state_screen==2){
                    call(phoneNumber)
                }else if(state_screen==1) {
                    dial(phoneNumber)
                }
                lastState = 1

            }
            last_state_screen = state_screen
        }
    }

    private val mapMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("map")
            //Log.d("MAP_MESSAGE", message.toString())
            if(message.equals("started")){
                sendCrewId(crewID)
                try {
                    getMapPoints(crewID)
                }catch (e: Error){
                    Log.d("getMapPoints_Error", e.toString())
                }
            }
        }
    }

    private val departureScreenReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("departureScreen")
            //Log.d("get_departure_crewUIDdsepartScreen_started", message.toString())
            if(message.equals("started")){
                //Log.d("get_departure_crewUID_background_send", crewUID)
                sendCrewId(crewID)
                sendDriverId(driverID)
            }
        }
    }

    private val loginScreenReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("LoginScreen")
            Log.d("GET_cookie_service", message.toString())
            if(message.equals("started")){
                sendCookie(cookie)
            }
        }
    }

    private val driverIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("driverID")
            //Log.d("driver_id", message.toString())
            driverID=message.toString()
        }
    }

    private val crewIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("crewID")
            //Log.d("get_departure_crew_id_backgroud", message.toString())
            crewID=message.toString()
        }
    }

    private val cookieReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("sendCookie")
            //Log.d("get_departure_crew_id_backgroud", message.toString())
            cookie=message.toString()
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val input = intent?.getStringExtra("inputExtra")
        createNottificationChannel()
        createNottificationChannel2()
        val notificationIntent = Intent(this, StartingScreen::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification =
            NotificationCompat.Builder(this, channel_id).setContentTitle(getText(R.string.app_name))
                .setContentText(input)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()

        startForeground(1, notification)
        GlobalScope.launch {
            getCookie(aid)
        }
        val url="url_string"
        val cookieManager: CookieManager = CookieManager.getInstance()
        cookieManager.acceptCookie()
        val test= cookieManager.getCookie(url)
        if(!test.isNullOrEmpty()){
            var temp=test.split(";")
            for(item in temp){
                item.replace("//s".toRegex(), "")
                if(item.contains("DriverID")){
                    var str=item.split("=")
                    Log.d("GET_cookie_driver", str[1].toString())
                    driverID=str[1]
                }
                if(item.contains("CrewID")){
                    var str=item.split("=")
                    Log.d("GET_cookie_crev", str[1].toString())
                    crewID=str[1]
                }
                if(item.contains("AndroidID")){
                    var str=item.split("=")
                    Log.d("GET_cookie_aid", str[1].toString())
                }
                if(item.contains("Cookie")){
                    var str=item.split("=")
                    Log.d("GET_cookie_cookie", str[1].toString())
                }
            }
            Log.d("GET_cookie_serv", test.toString())
        }
        //return START_STICKY
        return START_NOT_STICKY
    }

    private fun createNottificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channel_id,
                "FService Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNottificationChannel2() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channel_com,
                "FService Comm Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        initVariables()

        try {
            getMapPoints(crewID)
        }catch (e: Error){
            Log.d("getMapPoints_Error", e.toString())
        }



        //checkNetworkSavings()
        //energySaving()


        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        if (screenReceiver != null) {
            registerReceiver(screenReceiver, intentFilter)
        }

        mFusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this@ServiceBG)
        getLastLocation()

        val telephonyManager =
            Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        Log.d("TEST", telephonyManager)
        aid = telephonyManager.toString()
        //aid="d29c7f7a217d978b"

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver, IntentFilter("FromActivity")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            LocationReceiver, IntentFilter("GPSLocationChanged")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            messageReceiver, IntentFilter("screenStatus")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mapMessageReceiver, IntentFilter("mapScreenInfo")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            driverIDReceiver, IntentFilter("driverUID")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            crewIDReceiver, IntentFilter("crewUID")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            departureScreenReceiver, IntentFilter("departureScreen")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            loginScreenReceiver, IntentFilter("LoginScreen")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            cookieReceiver, IntentFilter("sendCookie")
        )

        registerScreenLockStateBroadcastReceiver()
        initializeService()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {

                when (intent?.action) {
                    "NotifyUser" -> {
                        try {
                            val lat = intent.getStringExtra("pinned_location_lat")
                            val long = intent.getStringExtra("pinned_location_long")
                            longitude = long.toString()
                            latitude = lat.toString()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        setUpSensor()

        sendMessageToActivity("button:true")


        configCor()
        delayedLocation()
        postDelay()
        smsSend()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        val intentFilter = IntentFilter()
        intentFilter.addAction("NotifyUser")
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).registerReceiver(it, intentFilter)
        }
    }

    private var mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: LocationUpdateService.LocalBinder =
                service as LocationUpdateService.LocalBinder
            mService = binder.service
            mBound = true
            mService?.requestLocationUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBound = false
        }
    }

    private fun initializeService() {
        bindService(
            Intent(this, LocationUpdateService::class.java),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun sendMessageToActivity(msg: String) {
        val intent = Intent("GPSLocationUpdates")
        intent.putExtra("Status", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendDataToActivity(msg: String) {
        val intent = Intent("valuesAccelerometer")
        intent.putExtra("values", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendGeoPoints(msg: String) {
        val intent = Intent("geopoint")
        intent.putExtra("geopoint", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun sendDriverId(msg:String){
        val intent=Intent("driverID")
        intent.putExtra("driverID", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun sendCrewId(msg:String){
        val intent=Intent("crewID")
        intent.putExtra("crewID", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun sendCookie(msg:String){
        val intent=Intent("cookie")
        intent.putExtra("cookie", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(LocationReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mapMessageReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(driverIDReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(crewIDReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(departureScreenReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loginScreenReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(cookieReceiver)
        unregisterReceiver(screenReceiver)

        handlerLocation.removeCallbacksAndMessages(null)
        handlerSMS.removeCallbacksAndMessages(null)
        mService?.stopSelf()

        configCorCond = false
        delayedLocationCond = false
        postDelayCond = false
        smsSendCond = false
        sensorManager.unregisterListener(this)
        this.cacheDir.deleteRecursively()
        Log.d(TAG, "KILLED")
    }

    fun sendNotification(message: String) {
        val builder = NotificationCompat.Builder(this, channel_com)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(2, builder.build())
        }
    }

    private fun getDateTimeLocal(): String {
        val utc1 = OffsetDateTime.now()
        return utc1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString()
    }

    private fun getDateTimeUTC(): String {
        val current = OffsetDateTime.now(ZoneOffset.UTC)
        return current.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString()
    }

    private fun isOnline(context: Context?): Boolean {
        if (context == null) return false
        val connectiviManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectiviManager.getNetworkCapabilities(connectiviManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectiviManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected)
                return true
        }
        return false
    }

    fun startLocation(state: String, lat:String, long:String, localDate:String, utcDate:String) {
        //energySaving()
        if(state=="delayed"){
            GlobalScope.launch {
                Log.d("startLocation", "IN")
                var postLocation =
                    PostGetApi(client, recogized, waitApi)
                when (postLocation.postLocation(
                    lat,
                    long,
                    localDate,
                    utcDate,
                    state,
                    aid
                )) {
                    "No_device" -> {
                        sendNotification("Tego urządzenia nie ma w bazie danych")
                    }
                    "DB_error" -> {
                        sendNotification("Błąd połączenia z bazą danych")
                    }
                    "Unknown_error" -> {
                        sendNotification("Błąd zapytania")
                    }
                    "Saved" -> {
                        //sendNotification("Zapisano lokalizację")
                        Log.d("LocationTest", "Zapisano lokalizację")
                    }
                    "Bad_id" -> {
                        sendNotification("Błąd wysyłania pozycji")
                    }
                    "AlertSaved" -> {
                        sendNotification("Wysłano alarm z pozycją")
                    }
                }
            }
        }else{
            GlobalScope.launch {
                Log.d("startLocation", "IN")
                var postLocation =
                    PostGetApi(client, recogized, waitApi)
                when (postLocation.postLocation(
                    latitude,
                    longitude,
                    getDateTimeLocal(),
                    getDateTimeUTC(),
                    state,
                    aid
                )) {
                    "No_device" -> {
                        sendNotification("Tego urządzenia nie ma w bazie danych")
                    }
                    "DB_error" -> {
                        sendNotification("Błąd połączenia z bazą danych")
                    }
                    "Unknown_error" -> {
                        sendNotification("Błąd zapytania")
                    }
                    "Saved" -> {
                        //sendNotification("Zapisano lokalizację")
                        Log.d("LocationTest", "Zapisano lokalizację")
                    }
                    "Bad_id" -> {
                        sendNotification("Błąd wysyłania pozycji")
                    }
                    "AlertSaved" -> {
                        sendNotification("Wysłano alarm z pozycją")
                    }
                }
            }
        }
    }

    /*fun sendData(data: String, id: String) {
        if (isOnline(this@ServiceBG)) {
            GlobalScope.launch {
                try {
                    val postLocation =
                        PostGetApi(client, recogized, waitApi)
                    postLocation.sendParameters(data, id)
                } catch (e: Error) {
                    Log.d("Error", e.toString())
                }
            }
        }
    }*/

    fun registerScreenLockStateBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)

        val screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val strAction = intent.action
                val kgMgr = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

                if (strAction.equals(Intent.ACTION_SCREEN_OFF)) {
                    screenOff = true
                } else if (strAction.equals(Intent.ACTION_SCREEN_ON)) {
                    screenOff = false
                }
                deviceLocked =
                    !(strAction.equals(Intent.ACTION_USER_PRESENT) && !kgMgr.inKeyguardRestrictedInputMode())
            }
        }
        applicationContext.registerReceiver(screenOffReceiver, intentFilter)
    }

    fun getLastLocation() {
        handlerLocation.post(code)
    }

    override fun stopService(name: Intent?): Boolean {
        return super.stopService(name)
    }

    fun initVariables() {
        sURL = "url_string"
        client = OkHttpClient()
        recogized = true
        token_csrf = ""
        wait = true
    }

    private fun getConfig(id: String) {
        GlobalScope.launch {
            try {
                val postLocation =
                    PostGetApi(client, recogized, waitApi)
                val config = postLocation.getStringConfig(id)
                    .replace("{", "")
                    .replace("}", "")
                    .replace("\\s+".toRegex(), "")
                    .replace("[", "")
                    .replace("]", "")
                    .split(",")
                //Log.d("Konfig_SBG", config.toString())
                for (c in config) {
                    val row = c.replace("\"", "").split(":")
                    println("ServiceBG" + row.toString())
                    when (row[0]) {
                        "LocalizationNetInterval" -> {
                            //Log.d("config_service_localizationNetInterval_przed:", localizationNetInterval.toString())
                            //localizationNetInterval = row[1].toInt() * 1000
                            localizationNetInterval = row[1].toInt()
                            //Log.d("config_service_localizationNetInterval_po:", localizationNetInterval.toString())
                        }
                        "LocalizationSMSInterval" -> {
                            //Log.d("config_service_localizationSMSInterval_przed:", localizationSMSInterval.toString())
                            //localizationSMSInterval = row[1].toInt() * 1000
                            localizationSMSInterval = row[1].toInt()
                            //Log.d("config_service_localizationSMSInterval_po:", localizationSMSInterval.toString())
                        }
                        "SOSInactivityPeriod" -> {
                            //Log.d("config_service_sosInactivityPeriod_przed:", sosInactivityPeriod.toString())
                            sosInactivityPeriod = row[1].toInt()
                            //Log.d("config_service_sosInactivityPeriod_po:", sosInactivityPeriod.toString())
                        }
                        "SOSButtonInactiveTime" -> {
                            //Log.d("config_service_sosButtonInactivityTime_przed:", sosButtonInactivityTime.toString())
                            sosButtonInactivityTime = row[1].toInt()
                            //Log.d("config_service_sosButtonInactivityTime_po:", sosButtonInactivityTime.toString())
                        }
                        "SMSButtonSensitivity" -> {
                            //Log.d("config_service_smsButtonSensitivity_przed:", smsButtonSensitivity.toString())
                            smsButtonSensitivity = row[1].toInt()
                            //Log.d("config_service_smsButtonSensitivity_po:", smsButtonSensitivity.toString())
                            sendMessageToActivity("btnDelay:" + smsButtonSensitivity)
                        }
                        "SwipingCounter" -> {
                            //Log.d("config_service_swipingCounter_przed:", swipingCounter.toString())
                            swipingCounter = row[1].toInt()
                            //Log.d("config_service_swipingCounter_po:", swipingCounter.toString())
                        }
                        "ScreenCounter" -> {
                            //Log.d("config_service_screenCounter_przed:", screenCounter.toString())
                            screenCounter = row[1].toInt()
                            //Log.d("config_service_screenCounter_po:", screenCounter.toString())
                        }
                        "SwipingTimer" -> {
                            //Log.d("config_service_swipingTimer_przed:", swipingTimer.toString())
                            swipingTimer = row[1].toInt()
                            //Log.d("config_service_swipingTimer_po:", swipingTimer.toString())
                        }
                        "SwipingLowerAcceler" -> {
                            //Log.d("config_service_swipingLowerAcceler_przed:", swipingLowerAcceler.toString())
                            swipingLowerAcceler = row[1].toInt()
                            //Log.d("config_service_swipingLowerAcceler_po:", swipingLowerAcceler.toString())
                        }
                        "SwipingUpperAcceler" -> {
                            //Log.d("config_service_swipingUpperAcceler_przed:", swipingUpperAcceler.toString())
                            swipingUpperAcceler = row[1].toInt()
                            //Log.d("config_service_swipingUpperAcceler_po:", swipingUpperAcceler.toString())
                        }
                        "SwipingLog" -> {
                            //Log.d("config_service_swipingLog_przed:", swipingLog.toString())
                            swipingLog = row[1]
                            //Log.d("config_service_swipingLog_po:", swipingLog.toString())
                        }
                        "ScreenTimer" -> {
                            //Log.d("config_service_screenTimer_przed:", screenTimer.toString())
                            screenTimer = row[1].toInt()
                            //Log.d("config_service_screenTimer_po:", screenTimer.toString())
                        }
                        "SOSPhoneNumber"->{
                            //Log.d("config_service_phoneNumber_przed:", phoneNumber.toString())
                            Log.d("phoneNumber", row[1])
                            phoneNumber=row[1]
                            //Log.d("config_service_phoneNumber_po:", phoneNumber.toString())
                        }
                    }
                }

                token_csrf=postLocation.postDeviceToken(id)
            } catch (e: Error) {
                Log.d("Error", e.toString())
            }
        }
    }

    /*private fun getConfigJson(id: String) {
        GlobalScope.launch {
            val postLocation =
                PostGetApi(client, recogized, waitApi)
            config_json = postLocation.getJsonConfig(id)
            sendMessageToActivity("btnDelay:" + config_json.SMSButtonSensitivity)
            Log.d("Konfig_SBG_", config_json.toString())
        }
    }*/

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && stop_data == 1) {
            if (event.values[0] >= 10 || event.values[0] <= -10)
                valueX = event.values[0].toDouble()
            if (event.values[1] >= 10 || event.values[1] <= -10)
                valueY = event.values[1].toDouble()
            if (valueY > swipingUpperAcceler && last_valueY < 0 && stop_data == 1) {
                if (state == 0 && !acStarted) {
                    timerAcc(swipingTimer)
                    acStarted = true
                }
                state = 1
                last_valuesY.add(valueY)
                last_valuesY.add(last_valueY)
            }
            if (valueY < swipingLowerAcceler && last_valueY > 0) {
                if (state == 0 && !acStarted) {
                    timerAcc(swipingTimer)
                    acStarted = true
                }
                state = 2
                last_valuesY.add(valueY)
                last_valuesY.add(last_valueY)
            }
            if (state != 0 && stop_data == 1) {
                counter++
                state = 0
                last_valueY = 0.0
                last_valueX = 0.0
            }

            if (counter == swipingCounter && sendBySwiping) {
                if (isOnline(this@ServiceBG)) {
                        startLocation("alert", "", "", "", "")
                } else {
                    sendNotification("Wysłano alarm z pozycją")
                    sms.sendSMS(
                        numberSMS,
                        "SMSMobileAlarm;" + latitude + ";" + longitude + ";${getDateTimeUTC()};$aid"
                    )

                }
                lastState = 1
                stop_data = 0
                sendBySwiping = false
                sendDataToActivity(last_valuesY.toString())
                //sendData(last_valuesY.toString(), aid)
                timerInactive(sosInactivityPeriod)
                counter = 0
                last_valuesY.clear()
                last_valuesX.clear()
                timer_state = 0
                state = 0
                last_state = 0
                valueY = 0.0
                last_valueY = 0.0
                valueX = 0.0
                last_valueX = 0.0

                call(phoneNumber)
                //dial()

            }
            last_state = state
            last_valueY = valueY
            last_valueX = valueX
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    private fun setUpSensor() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).also {
            sensorManager.registerListener(
                this, it,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    private fun dial(number: String) {
        val telephoneSchema = "tel:"
        val preservedCharacter = "+"
        val countryName = "48"
        if(number!="") {
            if(number.contains("+")){
                val phoneDialUri =
                    Uri.parse(telephoneSchema + number)
                val phoneDialIntent = Intent(Intent.ACTION_DIAL).also {
                    it.setData(phoneDialUri)
                    it.setAction(ACTION_VIEW)
                    it.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    it.addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                this@ServiceBG.startActivity(phoneDialIntent)
            }else {
                val phoneDialUri =
                    Uri.parse(telephoneSchema + preservedCharacter + countryName + number)
                val phoneDialIntent = Intent(Intent.ACTION_DIAL).also {
                    it.setData(phoneDialUri)
                    it.setAction(ACTION_VIEW)
                    it.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    it.addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                this@ServiceBG.startActivity(phoneDialIntent)
            }
        }else{
            Toast.makeText(this@ServiceBG, "Brak numeru", Toast.LENGTH_SHORT).show()
        }
    }

    private fun call(number:String) {
        val telephoneSchema = "tel:"
        val preservedCharacter = "+"
        val countryName = "48"
        if(number!="") {
            if(number.contains("+")){
                val phoneCallUri =
                    Uri.parse(telephoneSchema + number)
                val phoneCallIntent = Intent(Intent.ACTION_CALL).also {
                    it.setData(phoneCallUri)
                    it.setAction(ACTION_VIEW)
                    it.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    it.addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                this@ServiceBG.startActivity(phoneCallIntent)
            }else {
                val phoneCallUri =
                    Uri.parse(telephoneSchema + preservedCharacter + countryName + number)
                val phoneCallIntent = Intent(Intent.ACTION_CALL).also {
                    it.setData(phoneCallUri)
                    it.setAction(ACTION_VIEW)
                    it.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    it.addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                this@ServiceBG.startActivity(phoneCallIntent)
            }
        }else{
            Toast.makeText(this@ServiceBG, "Brak numeru", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMapPoints(id: String){
        GlobalScope.launch(Dispatchers.Default) {
            try{
                Log.d("GetMap", "test")
                val postLocation =
                    PostGetApi(client, recogized, waitApi)
                //val id=postLocation.getOrderID()
                val json=postLocation.getOrder(id)
                //val json=postLocation.getMap(orderID)
                sendGeoPoints(json)
            }catch (e:Exception){
                Log.d("GetMapPoints_Error", e.toString())
            }
        }
    }

    fun getCookie(aid:String) {
        var dtURL = "url_string"+aid
        Log.d("GET_cookie", aid)
        val urlPOST = dtURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        val requestPost =
            Request.Builder().url(urlPOST).header("token", "token_value").build()

        client.newCall(requestPost).execute().use { response ->
            cookie = response.body!!.string()
            val cod = response.code.toString()
            Log.d("GET_cookie", cookie)
            Log.d("GET_cookie", cod)
            //sendCookie(cookie)
            if(cookie.contains("Cookie")){
                try {
                    var cookieJSON = Gson().fromJson(cookie, Cookie::class.java)
                    crewID=cookieJSON.CrewID
                    driverID=cookieJSON.CrewID


                    Log.d("GET_cookie_inter", cookie)
                }catch (e:Exception){
                    Log.d("LoginScreen_EXCEPTION", e.toString())
                }
            }
        }
    }
}