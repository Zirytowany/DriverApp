package eu.mobileApp.DriverApp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.util.Rational
import android.view.*
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.internal.ContextUtils
import eu.mobileApp.DriverApp.alerts.LocationUpdateService
import eu.mobileApp.DriverApp.comm.PostGetApi
import eu.mobileApp.DriverApp.comm.ServiceBG
import eu.mobileApp.DriverApp.databinding.ActivityMainScreenBinding
import eu.mobileApp.DriverApp.login.LogInScreen
import eu.mobileApp.DriverApp.mapa.MapScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request


class MainScreen : AppCompatActivity() {
    private lateinit var binding: ActivityMainScreenBinding

    private var handlerDelayed: Handler = Handler()
    private val TAG = "GPS"
    private var latitude = ""
    private var longitude = ""
    private var id: String? = ""
    private var passInt: String? = ""
    private lateinit var token_csrf: String
    private var device_token=""
    private var aid: String = ""
    private var toggle: Boolean = false
    private var mService: LocationUpdateService? = null
    private var mBound: Boolean = false

    private val MY_PERMISSIONS_REQUEST_LOCATION = 68
    private val REQUEST_CHECK_SETTINGS = 129

    private var broadcastReceiver: BroadcastReceiver? = null

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isReadPhonePermissionGranted = false
    private var isSendSmsPermissionGranted = false
    private var isFineLocationPermissionGranted = false
    private var isCoarseLocationPermissionGranted = false
    private var isBackgroundLocationPermissionGranted = false
    private var isCallPhonePermissionGranted = false
    private var isAlertWindowPermissionGranted = false

    lateinit var str: List<String>
    var telephonyManager: TelephonyManager? = null
    private var level = mutableListOf<String>()

    private var client = OkHttpClient()

    private var postMils = 120000 //"LocalizationNetInterval" 2min
    private var smsDelay = 300000 //"LocalizationSMSInterval" 5min
    private var btnDelay = 400  //"SOSButtonSensitivity"
    private var startertBtnDelay=btnDelay
    private var sendSMSDelay = 2000 //"SOSButtonInactiveTime"

    private var btnActive = true
    private var btnLastState = true

    private var recogized: Boolean = true
    private var waitApi: Boolean = true

    private var strLevel=4
    private var cellSignall=0

    private var driverID=""
    private var crewID=""

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var message = intent.getStringExtra("Status")
            //Log.d("Message", message.toString())

            if (message!!.contains("btnDelay")) {
                val strTemp = message.split(":")
                btnDelay = strTemp[1].toInt()
                binding.btnSendData.setCustomLongClickListener(btnDelay) {
                    sendMessage("send")
                    setText()
                    binding.btnSendData.isEnabled = false
                    binding.btnSendData.setBackgroundColor(Color.GRAY)
                    true
                }
            } else if (message.contains("button:")) {
                val strTemp = message.split(":")
                if (strTemp[1].toBoolean() != btnLastState) {
                    btnActive = strTemp[1].toBoolean()
                    binding.btnSendData.isEnabled = btnActive
                }
                btnLastState = strTemp[1].toBoolean()
                if (btnActive) {
                    binding.btnSendData.setBackgroundColor(Color.RED)
                } else {
                    binding.btnSendData.setBackgroundColor(Color.GRAY)
                }
                //Log.d("TestBTN", btnActive.toString())
            } else {
                Toast.makeText(this@MainScreen, message.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var dataReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("values")
            //Log.d("VALUES", "przechwycono $message")
            //binding.directionTextView.text = "Alarm aktywowały wartości:\n$message"
        }
    }

    private val driverIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("driverID")
            Log.d("GET_cookie_driver_id", message.toString())
            driverID=message.toString()
            /*var menu=View.Meu
            menuInflater.inflate(R.menu.options_user_logged, )*/
        }
    }

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action!!.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
                val gpsState = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                if (gpsState) {
                    binding.btnGPSstate.backgroundTintList = getColorStateList(R.color.green)
                } else {
                    binding.btnGPSstate.backgroundTintList = getColorStateList(R.color.red)
                }
            }
        }
    }


    private val postDelayed: Runnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            try {
                strLevel = telephonyManager!!.signalStrength?.level!!
                //Log.d("signalStrength", strLevel.toString())
                if (strLevel>0) {
                    cellSignall=strLevel
                } else {
                    cellSignall=0
                }
                /*
                str = telephonyManager!!.signalStrength?.cellSignalStrengths.toString()
                    .split("\\s".toRegex())
                //Log.d("StringStrength2", str.toString())
                for (s in str) {
                    if (s.contains("evel")) {
                        level = s.split("=".toRegex()).toMutableList()
                        if (level[0].equals("level") || level[0].equals("Level") || level[0].equals(
                                "mLevel") || level[0].equals("evel")
                        ) {
                            if (level[1].contains("]")) {
                                level[1] = level[1].dropLast(1)
                            }
                            if (level[1] != "" && level[1].toInt() > 0) {
                                binding.btnCell.backgroundTintList =
                                    getColorStateList(R.color.green)
                            } else {
                                binding.btnCell.backgroundTintList = getColorStateList(R.color.red)
                            }
                        }
                    } else {
                        binding.btnCell.backgroundTintList = getColorStateList(R.color.red)
                    }
                }*/
            } catch (e: Exception) {
                //Log.d("SignalStrength", e.toString())
            }

            if (cellSignall>0) {
                binding.btnCell.backgroundTintList =
                    getColorStateList(R.color.green)
            } else {
                binding.btnCell.backgroundTintList = getColorStateList(R.color.red)
            }

            if (isOnline(this@MainScreen)) {
                binding.btnOnline.backgroundTintList = getColorStateList(R.color.green)
            } else {
                binding.btnOnline.backgroundTintList = getColorStateList(R.color.red)
            }

            setText()
            handlerDelayed.postDelayed(this, 5000)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainScreenBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main_screen)
        val viewBind = binding.root
        setContentView(viewBind)

        registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        sendMessage("started")

        telephonyManager = this.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#9e9e9e")))

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                isReadPhonePermissionGranted = permissions[Manifest.permission.READ_PHONE_STATE]
                    ?: isReadPhonePermissionGranted
                isSendSmsPermissionGranted =
                    permissions[Manifest.permission.SEND_SMS] ?: isSendSmsPermissionGranted
                isFineLocationPermissionGranted =
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION]
                        ?: isFineLocationPermissionGranted
                isCoarseLocationPermissionGranted =
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION]
                        ?: isCoarseLocationPermissionGranted
                isBackgroundLocationPermissionGranted =
                    permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION]
                        ?: isBackgroundLocationPermissionGranted
                isCallPhonePermissionGranted =
                    permissions[Manifest.permission.CALL_PHONE] ?: isCallPhonePermissionGranted
                isAlertWindowPermissionGranted =
                    permissions[Manifest.permission.SYSTEM_ALERT_WINDOW]
                        ?: isAlertWindowPermissionGranted
            }
        requestPermission()

        val url="url_string"+
                "?AndroidID=${aid}"
        val cookieManager: CookieManager = CookieManager.getInstance()
        cookieManager.acceptCookie()
        //cookieManager.acceptThirdPartyCookies(binding.webView)
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
        }

        id = intent.getStringExtra("id").toString()
        passInt = intent.getStringExtra("pass").toString()
        token_csrf = intent.getStringExtra("token_csrf").toString()
        device_token = intent.getStringExtra("device_token").toString()
        toggle = intent.getStringExtra("toggle").toBoolean()

        try {
            postMils = intent.getStringExtra("LocalizationNetInterval")!!.toInt()
            sendSMSDelay = intent.getStringExtra("LocalizationSMSInterval")!!.toInt()
            smsDelay = intent.getStringExtra("SOSButtonInactiveTime")!!.toInt()
            btnDelay = intent.getStringExtra("SMSButtonSensitivity")!!.toInt()
        } catch (e: Exception) {
            Log.e("Error", e.toString())
        }

        Log.d(TAG, device_token)
        Log.d(TAG, token_csrf)

        var running = false

        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (ServiceBG.equals(service.service.className)) {
                running = true
                break
            }
            false
        }
        if (!running)
            ServiceBG.startService(
                this,
                "${getText(R.string.app_name)} działa w tle...",
                id!!,
                passInt!!,
                true
            )

        if (btnActive)
            binding.btnSendData.setBackgroundColor(Color.RED)
        binding.btnSendData.isEnabled = btnActive

        try {
            strLevel = telephonyManager!!.signalStrength?.level!!
            //Log.d("signalStrength", strLevel.toString())
            if (strLevel>0) {
                cellSignall=strLevel
            } else {
                cellSignall=0
            }

            /*
            str = telephonyManager!!.signalStrength?.cellSignalStrengths.toString()
                .split("\\s".toRegex())
            //Log.d("StringStrength2", str.toString())
            for (s in str) {
                if (s.contains("evel")) {
                    level = s.split("=".toRegex()).toMutableList()
                    if (level[0].equals("level") || level[0].equals("Level") || level[0].equals("mLevel")) {
                        if (level[1].contains("]")) {
                            level[1] = level[1].dropLast(1)
                        }
                        if (level[1] != "" && level[1].toInt() > 0) {
                            binding.btnCell.backgroundTintList = getColorStateList(R.color.green)
                        } else {
                            binding.btnCell.backgroundTintList = getColorStateList(R.color.red)
                        }
                    }
                }
            }*/
        } catch (e: Exception) {
            //Log.d("SignalStrenght", e.toString())
        }

        if (cellSignall>0) {
            binding.btnCell.backgroundTintList =
                getColorStateList(R.color.green)
        } else {
            binding.btnCell.backgroundTintList = getColorStateList(R.color.red)
        }

        val locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsState = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (gpsState) {
            binding.btnGPSstate.backgroundTintList = getColorStateList(R.color.green)
        } else {
            binding.btnGPSstate.backgroundTintList = getColorStateList(R.color.red)
        }

        binding.btnLogIn.visibility = View.INVISIBLE

        if (toggle) {
            binding.btnLogIn.visibility = View.GONE
            //logOut.visibility = View.VISIBLE
        }

        /*btn_login.setOnClickListener {
            val intent = Intent(this, LogInScreen::class.java)
            startActivity(intent)
            this.finish()
        }*/

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
        } else {
            val telephonyID =
                Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
            aid = telephonyID.toString()
            //aid = "d29c7f7a217d978b"
            //Log.d("AndroidID", aid)
        }
        binding.tvAndroidID.text = aid
        binding.tvAndroidID.visibility = View.INVISIBLE
        binding.tvAndroid.visibility = View.INVISIBLE
        binding.btnCopy.visibility = View.INVISIBLE

        binding.btnCopy.setOnClickListener {
            copyTextToClipboard()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                111
            )
        }

        binding.btnSendData.setCustomLongClickListener(btnDelay) {
            sendMessage("send")
            setText()
            binding.btnSendData.isEnabled = false
            binding.btnSendData.setBackgroundColor(Color.GRAY)
            true
        }

        binding.btnLogout.setOnClickListener {
            wyloguj(this)
        }

        LocalBroadcastManager.getInstance(ContextUtils.getActivity(this)!!).registerReceiver(
            mMessageReceiver, IntentFilter("GPSLocationUpdates")
        )

        LocalBroadcastManager.getInstance(ContextUtils.getActivity(this)!!).registerReceiver(
            dataReceiver, IntentFilter("valuesAccelerometer")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            driverIDReceiver, IntentFilter("driverID")
        )

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainScreen,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        } else {
            Log.e("MainActivity:", "Location Permission Already Granted")
            if (getLocationMode() == 3) {
                Log.e("MainActivity:", "Already set High Accuracy Mode")
                initializeService()
            } else {
                Log.e("MainActivity:", "Alert Dialog Shown")
                showAlertDialog(this@MainScreen)
            }
        }

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

        val actionBar = supportActionBar
        supportActionBar?.setDisplayShowTitleEnabled(false)
        actionBar?.setDisplayShowCustomEnabled(true)

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.custom_action_bar, null)
        actionBar?.setCustomView(view)

        try {
            getConfig(aid)
        }catch (e:Error){
            //Log.d("getConfig", e.toString())
        }
            start()
        handlerDelayed.post(postDelayed)
    }

    private fun copyTextToClipboard() {
        val textToCopy = binding.tvAndroidID.text
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
    }

    private fun requestPermission() {
        isReadPhonePermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        isSendSmsPermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        isFineLocationPermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        isCoarseLocationPermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        isBackgroundLocationPermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        isCallPhonePermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        isAlertWindowPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        ) == PackageManager.PERMISSION_GRANTED
        val permissionRequest: MutableList<String> = ArrayList()

        if (!isReadPhonePermissionGranted) {
            permissionRequest.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (!isSendSmsPermissionGranted) {
            permissionRequest.add(Manifest.permission.SEND_SMS)
        }
        if (!isFineLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (!isCoarseLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!isBackgroundLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if(!isCallPhonePermissionGranted){
            permissionRequest.add((Manifest.permission.CALL_PHONE))
        }
        if(!isAlertWindowPermissionGranted){
            permissionRequest.add((Manifest.permission.SYSTEM_ALERT_WINDOW))
            if(!Settings.canDrawOverlays(this)){
                showOverlayDialog(this)
            }
        }
        if (permissionRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        binding.btnSendData.isEnabled = btnActive
        val intentFilter = IntentFilter()
        intentFilter.addAction("NotifyUser")
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        binding.btnSendData.isEnabled = btnActive
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.e("MainActivity:", "Location Permission Granted")
                    if (getLocationMode() == 3) {
                        Log.e("MainActivity:", "Already set High Accuracy Mode")
                        initializeService()
                    } else {
                        Log.e("MainActivity:", "Alert Dialog Shown")
                        showAlertDialog(this@MainScreen)
                    }
                } else {
                    ///Wyłączenie funkcjonalności jeżeli nie zostanie przyznane uprawnienie
                }
                return
            }
        }
    }

    private fun showAlertDialog(context: Context?) {
        try {
            context?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle(it.resources.getString(R.string.app_name))
                    .setMessage("W celu poprawnego działania aplikacji proszę włączyć dostęp do lokalizacji")
                    .setPositiveButton(it.resources.getString(android.R.string.ok)) { dialog, which ->
                        dialog.dismiss()
                        startActivityForResult(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                            REQUEST_CHECK_SETTINGS
                        )
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showOverlayDialog(context: Context?) {
        try {
            context?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle(it.resources.getString(R.string.app_name))
                    .setMessage("W celu poprawnego działania aplikacji proszę włączyć wyświtlanie nad innymi aplkikacjami / wyświetlanie na wierzchu")
                    .setPositiveButton(it.resources.getString(android.R.string.ok)) { dialog, which ->
                        dialog.dismiss()
                        startActivityForResult(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
                            REQUEST_CHECK_SETTINGS
                        )
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocationMode(): Int {
        return Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            initializeService()
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
        // bindService(Intent(this, LocationUpdateService::class.java), mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
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

    private fun setText() {
        binding.tvLocation.setText("Wys.: " + latitude + ", Szer.: " + longitude)
    }

    fun wyloguj(context: Context) {
        /*ServiceBG.stopService(this)
        val stopIntent = Intent(context, MainScreen::class.java)
        context.stopService(stopIntent)
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        mService?.stopSelf()*/
        val stopIntent = Intent(context, MainScreen::class.java)
        context.stopService(stopIntent)
        sendDriverId("")
        finish()
        val intent = Intent(context, StartingScreen::class.java)
        context.startActivity(intent)
    }

    override fun onDestroy() {
        Log.d("MainScreen", "KILLED")
        handlerDelayed.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        enterPipMode(this)
    }

    private fun enterPipMode(activity: Activity) {
        val aspect_ratio= Rational(2,3)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspect_ratio)
            .build()
        activity?.enterPictureInPictureMode(params)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (device_token==""){
            menuInflater.inflate(R.menu.options_no_db_idr, menu)
        }else if(device_token == "This device doesn't exists in the database."){
            menuInflater.inflate(R.menu.options_no_db_idr, menu)
        }else{
            if(driverID==""){
                menuInflater.inflate(R.menu.options_no_user, menu)
            }else{
                menuInflater.inflate(R.menu.options_user_logged, menu)
            }
        }
        return true
    }

    fun onGroupItemClick(item: MenuItem) {
        if(device_token==""){
            when (item.toString()) {
                "Zamknij" -> {
                    if (mBound) {
                        unbindService(mServiceConnection)
                        mBound = false
                    }
                    mService?.stopSelf()
                    ServiceBG.stopService(this)
                    finish()
                }
                "Zmień wygląd" -> {
                    chooseThemeDialog()
                }
                "Wyświetl mapę" -> {
                    val intent = Intent(this, MapScreen::class.java)
                    intent.putExtra("longlat", longitude + ":" + latitude)
                    startActivity(intent)
                    //this.finish()
                }
                "home" -> {
                    val intent = Intent(this, MainScreen::class.java)
                    startActivity(intent)
                    finish()
                }

                "Informacje" -> {
                    val intent = Intent(this, AppParamsScreen::class.java)
                    startActivity(intent)
                    //finish()
                }
            }

        }else if (device_token == "This device doesn't exists in the database."){
            when (item.toString()) {
                "Zamknij" -> {
                    if (mBound) {
                        unbindService(mServiceConnection)
                        mBound = false
                    }
                    mService?.stopSelf()
                    ServiceBG.stopService(this)
                    finish()
                }
                "Zmień wygląd" -> {
                    chooseThemeDialog()
                }
                "Wyświetl mapę" -> {
                    val intent = Intent(this, MapScreen::class.java)
                    intent.putExtra("longlat", longitude + ":" + latitude)
                    startActivity(intent)
                    //this.finish()
                }
                "home" -> {
                    val intent = Intent(this, MainScreen::class.java)
                    startActivity(intent)
                    finish()
                }

                "Informacje" -> {
                    val intent = Intent(this, AppParamsScreen::class.java)
                    startActivity(intent)
                    //finish()
                }
            }
        }else{
            if(driverID=="") {
                when (item.toString()) {
                    "Zaloguj" -> {
                        val intent = Intent(this, LogInScreen::class.java)
                        startActivity(intent)
                        //this.finish()
                    }
                    "Zamknij" -> {
                        if (mBound) {
                            unbindService(mServiceConnection)
                            mBound = false
                        }
                        mService?.stopSelf()
                        ServiceBG.stopService(this)
                        finish()
                    }
                    "Zmień wygląd" -> {
                        chooseThemeDialog()
                    }
                    "home" -> {
                        val intent = Intent(this, MainScreen::class.java)
                        startActivity(intent)
                        finish()
                    }

                    "Informacje" -> {
                        val intent = Intent(this, AppParamsScreen::class.java)
                        startActivity(intent)
                        //finish()
                    }
                }
            }else{
                when (item.toString()) {
                    "Wyloguj" -> {
                        wyloguj(this)
                    }
                    "Zamknij" -> {
                        if (mBound) {
                            unbindService(mServiceConnection)
                            mBound = false
                        }
                        mService?.stopSelf()
                        ServiceBG.stopService(this)
                        finish()
                    }
                    "Zmień wygląd" -> {
                        chooseThemeDialog()
                    }
                    "home" -> {
                        val intent = Intent(this, MainScreen::class.java)
                        startActivity(intent)
                        finish()
                    }
                    "Informacje" -> {
                        val intent = Intent(this, AppParamsScreen::class.java)
                        startActivity(intent)
                        //finish()
                    }
                    "Więcej"->{
                        val intent=Intent(this,WebMenuScreen::class.java)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun sendMessage(msg: String) {
        val intent = Intent("FromActivity")
        intent.putExtra("sendMessage", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun getToken() {
        //Pobranie tokena
        /*
        val urlGET =
            sURL.toHttpUrlOrNull()?.newBuilder()?.addQueryParameter("create_csrf_token", "1")
                ?.build().toString()
        val request =
            Request.Builder().url(urlGET).header("token", "token_value")
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.d("GET_ERROR", "Unexpected code $response")
            }

            for ((name, value) in response.headers) {
                Log.d("HEADERS_RESPONSE", "$name: $value")
            }
            token_csrf = response.body!!.string()
            Log.d("TOKEN: ", token_csrf)
        }*/

        //Potwierdzenie urządzenia
        //token_csrf = token_csrf.replace("\"", "")
        var dtURL = "url_string"
        val urlPOST = dtURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        val formBody =
            FormBody.Builder().add("device_token", aid).build()

        val requestPost =
            Request.Builder().url(urlPOST).header("token", "token_value").post(formBody).build()

        client.newCall(requestPost).execute().use { response ->
            device_token = response.body!!.string()
            Log.d("GetPOST_token", device_token)
            val cod = response.code.toString()
            Log.d("GetPOSTcode", cod)
            /*this.runOnUiThread {
                logIn.visibility= View.VISIBLE
            }*/


            if (device_token == "This device doesn't exists in the database.") {
                this.runOnUiThread {
                    Toast.makeText(
                        this,
                        "Tego urządzenia nie ma w bazie danych",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.tvAndroidID.visibility = View.VISIBLE
                    binding.tvAndroid.visibility = View.VISIBLE
                    binding.btnCopy.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun start() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isOnline(this@MainScreen))
                try{
                getToken()
                }catch (e:Error){
                    Log.d("startError", e.toString())
                }
            else {
                this@MainScreen.runOnUiThread {
                    Toast.makeText(
                        this@MainScreen,
                        "Brak połączenia z internetem",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun chooseThemeDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Wybierz schemat kolorów")
        val styles = arrayOf("Jasny", "Ciemny", "Domyślny")
        val checkedItem = 0

        builder.setSingleChoiceItems(styles, checkedItem) { dialog, which ->
            when (which) {
                0 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    delegate.applyDayNight()
                    dialog.dismiss()
                }
                1 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    delegate.applyDayNight()
                    dialog.dismiss()
                }
                2 -> {
                    AppCompatDelegate.setDefaultNightMode((AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
                    delegate.applyDayNight()
                    dialog.dismiss()
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun View.setCustomLongClickListener(btnDelay:Int, listener: () -> Unit) {
        //Log.d("Test_Listener", btnDelay.toString())
        if(startertBtnDelay!=btnDelay){
            setOnTouchListener(object : View.OnTouchListener {
                private var longClickDuration = btnDelay.toLong()
                private var handler = Handler()

                override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                    if (event?.action == MotionEvent.ACTION_DOWN) {
                        handler.postDelayed({ listener.invoke() }, longClickDuration)
                    } else if (event?.action == MotionEvent.ACTION_UP) {
                        handler.removeCallbacksAndMessages(null)
                    }
                    return true
                }
            })
        }else{
            setOnTouchListener(object : View.OnTouchListener {
                private var longClickDuration = startertBtnDelay.toLong()
                private var handler = Handler()

                override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                    if (event?.action == MotionEvent.ACTION_DOWN) {
                        handler.postDelayed({ listener.invoke() }, longClickDuration)
                    } else if (event?.action == MotionEvent.ACTION_UP) {
                        handler.removeCallbacksAndMessages(null)
                    }
                    return true
                }
            })
        }
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
                Log.d("Konfig_SBG", config.toString())
                for (c in config) {
                    val row = c.replace("\"", "").split(":")
                    println("ServiceBG" + row.toString())
                    when (row[0]) {
                        "LocalizationNetInterval" -> {
                            println(row[1])
                            postMils = row[1].toInt() * 1000
                        }
                        "LocalizationSMSInterval" -> {
                            println(row[1])
                            sendSMSDelay = row[1].toInt() * 1000
                        }
                        "SOSButtonInactiveTime" -> {
                            println(row[1])
                            smsDelay = row[1].toInt()

                        }
                        "SMSButtonSensitivity" -> {
                            println(row[1])
                            btnDelay = row[1].toInt()
                        }
                    }
                }
            } catch (e: Error) {
                Log.e("Error", e.toString())
            }
        }
    }

    fun sendDriverId(msg:String){
        val intent=Intent("driverID")
        intent.putExtra("driverID", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}