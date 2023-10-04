package eu.mobileApp.DriverApp.login

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import eu.mobileApp.DriverApp.*
import eu.mobileApp.DriverApp.comm.PostGetApi
import eu.mobileApp.DriverApp.databinding.ActivityLogInScreenBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.*
import kotlin.collections.ArrayList

const val DB_NAME="user.db"

class LogInScreen : AppCompatActivity() {

    private var sURL = "url_string"
    private var client = OkHttpClient()
    private var device_token: String = ""
    private var cookie: String = ""
    private var aid=""

    private lateinit var binding:ActivityLogInScreenBinding
/*
    private lateinit var btnID: Button
    private lateinit var tv_ID: TextView
    private lateinit var nameLayout: TextInputLayout
    private lateinit var passLayout: TextInputLayout
    private lateinit var logIn: Button
    private lateinit var rejestr: Button*/

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isReadPhonePermissionGranted = false
    private var isSendSmsPermissionGranted = false
    private var isFineLocationPermissionGranted = false
    private var isCoarseLocationPermissionGranted = false
    private var recogized: Boolean = true
    private var waitApi: Boolean = true

    private var found=false

    private val channel_com = "Error"


    private val cookieReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("cookie")
            Log.d("GET_cookie_receiver", message.toString())
            cookie=message.toString()
            Log.d("GET_cookie_receiver", cookie.toString())
            try {
                if(!cookie.isNullOrEmpty() && cookie!="" && cookie!="[]") {
                    initialize()
                }else{
                    lifecycleScope.launch(Dispatchers.IO) {
                        this@LogInScreen.runOnUiThread {
                            initialize()
                        }
                        getCookie()
                    }
                }
            }catch (e:Error){
                Log.d("getData", e.toString())
            }
        }
    }

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLogInScreenBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            cookieReceiver, IntentFilter("cookie")
        )

        binding.webView.webViewClient= WebViewClient()
        binding.webView.clearCache(true)
        binding.webView.settings.javaScriptEnabled=true
        binding.webView.settings.domStorageEnabled=true
        binding.webView.settings.setGeolocationEnabled(true)
        binding.webView.webChromeClient= ModifiedWebChromeClient()


        //initialize()

        sendMessage("started")

        val telephonyID =
            Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        aid = telephonyID.toString()
        //aid = "af00468fa6ece4ed"


        createNottificationChannel()

        //setContentView(R.layout.activity_log_in_screen)
        /*nameLayout = findViewById(R.id.name)
        passLayout = findViewById(R.id.pass)
        logIn = findViewById(R.id.login)
        rejestr = findViewById(R.id.restr)*/



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
            }
        requestPermission()

        val postLocation =
            PostGetApi(client, recogized, waitApi)
    }

    @SuppressLint("HardwareIds")
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

        if (permissionRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
        val telephonyManager =
            Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        Log.d("TEST", telephonyManager)
        aid = telephonyManager.toString()
        //aid = "af00468fa6ece4ed"
        start()
    }

    fun isOnline(context: Context?): Boolean {
        if (context == null) return false
        val connectiviManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        return false
    }

    fun getToken() {
        var dtURL = "string_url"
        val urlPOST = dtURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        val formBody =
            FormBody.Builder().add("device_token", aid).build()

        val requestPost =
            Request.Builder().url(urlPOST).header("token", "token_value").post(formBody).build()

        client.newCall(requestPost).execute().use { response ->
            device_token = response.body!!.string()
            val cod = response.code.toString()
            //Log.d("POST_token", device_token)
            Log.d("POST_code", cod)


            if (device_token == "This device doesn't exists in the database.") {
                this.runOnUiThread {
                    Toast.makeText(
                        this,
                        "Tego urządzenia nie ma w bazie danych",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun getCookie() {
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
            sendCookie(cookie)
        }
    }

    private fun sendMessage(msg: String) {
        val intent = Intent("LoginScreen")
        intent.putExtra("LoginScreen", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d("GET_cookie_serv", msg.toString())
    }

    private fun sendCookie(msg: String) {
        val intent = Intent("sendCookie")
        intent.putExtra("sendCookie", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun start() {
        if(isOnline(this@LogInScreen)){
            try {
                lifecycleScope.launch(Dispatchers.IO) {
                    getToken()
                    //getCookie()
                }
            }catch (e: Exception){
                Log.d("LoginScreen_Error", e.toString())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    fun onGroupItemClick(item: MenuItem) {
        when (item.toString()) {
            "home"->{
                val intent = Intent(this, MainScreen::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    fun sendNotification(message: String) {
        val builder = NotificationCompat.Builder(this, channel_com)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(3, builder.build())
        }
    }

    private fun createNottificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channel_com,
                "Błąd ekranu logowania",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }
    //private val cookieJar=MyPersistentCookieJar(this)
    //private val okHttpClient=OkHttpClient().newBuilder().cookieJar(cookieJar).build()

    private fun initialize(){
        val url="url_string"
        val cookieManager: CookieManager = CookieManager.getInstance()
        cookieManager.acceptCookie()
        cookieManager.acceptThirdPartyCookies(binding.webView)
        val test= cookieManager.getCookie(url)
        if(cookie.contains("Cookie")){
            try {
                var cookieJSON = Gson().fromJson(cookie, Cookie::class.java)
                Log.d("GET_cookie_inter", cookie)
                cookieManager.setCookie(url, "Cookie=${cookieJSON.Cookie}")
                cookieManager.setCookie(url, "DriverID=${cookieJSON.DriverID}")
                cookieManager.setCookie(url, "CrewID=${cookieJSON.CrewID}")
                cookieManager.setCookie(url, "AndroidID=${cookieJSON.AndroidID}")
                cookieManager.setCookie(url, "Issued=${cookieJSON.Issued}")
                cookieManager.setCookie(url, "Expires=${cookieJSON.Expires}")
            }catch (e:Exception){
                Log.d("LoginScreen_EXCEPTION", e.toString())
            }
        }else if(!cookie.isNullOrEmpty() && cookie!="" && cookie!="[]"){
            if(cookie.contains("[")){
                cookie=cookie.replace("[", "")
            }
            if(cookie.contains("]")){
                cookie=cookie.replace("]", "")
            }
            if(cookie.contains(":")){
                cookie=cookie.replace(":", "=")
            }
            if(cookie.contains(",")){
                cookie=cookie.replace(",", ";")
            }
            Log.d("GET_cookie_inter", cookie)
            cookieManager.setCookie(url, cookie)
        }
        binding.webView.loadUrl(url)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(cookieReceiver)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()){
            binding.webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    class ModifiedWebChromeClient: WebChromeClient() {
        override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
            callback!!.invoke(origin, true, false);
        }
    }

}