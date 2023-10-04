package eu.mobileApp.DriverApp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import eu.mobileApp.DriverApp.login.LoginDatabase
import eu.mobileApp.DriverApp.R
import kotlinx.coroutines.*
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class StartingScreen : AppCompatActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isReadPhonePermissionGranted = false
    private var isSendSmsPermissionGranted = false
    private var isFineLocationPermissionGranted = false
    private var isCoarseLocationPermissionGranted = false
    private var isBackgroundLocationPermissionGranted = false
    private var isCallPhonePermissionGranted = false
    private var isAlertWindowPermissionGranted = false
    private var isAlertsEnabled=false

    private lateinit var id: String
    private lateinit var sURL: String
    private lateinit var client: OkHttpClient
    private lateinit var token_csrf: String
    private lateinit var device_token: String

    private lateinit var androidID: TextView

    private val MAKE_CALL_PERMISSION_REQUEST_CODE = 1

    val db by lazy{
        LoginDatabase.getDatabase(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starting_screen)

        this.cacheDir.deleteRecursively()

        getConfig()

        androidID = findViewById(R.id.AndroidID)
        GlobalScope.launch(Dispatchers.IO) {
            val test1= withContext(Dispatchers.IO){
                return@withContext db.LoginDAO().deleteExpired()
            }
        }


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
                isCallPhonePermissionGranted =
                    permissions[Manifest.permission.CALL_PHONE] ?: isCallPhonePermissionGranted
                isAlertWindowPermissionGranted =
                    permissions[Manifest.permission.SYSTEM_ALERT_WINDOW]
                        ?: isAlertWindowPermissionGranted
                isBackgroundLocationPermissionGranted =
                    permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION]
                        ?: isBackgroundLocationPermissionGranted
                isAlertsEnabled=permissions[Manifest.permission.POST_NOTIFICATIONS]?:isAlertsEnabled

            }
        requestPermission()
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
        isCallPhonePermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        isAlertWindowPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        ) == PackageManager.PERMISSION_GRANTED
        isBackgroundLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        isAlertsEnabled=ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED

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
        if (!isCallPhonePermissionGranted) {
            permissionRequest.add(Manifest.permission.CALL_PHONE)
        }
        if (!isAlertWindowPermissionGranted) {
            permissionRequest.add(Manifest.permission.SYSTEM_ALERT_WINDOW)
        }
        if(!isBackgroundLocationPermissionGranted){
            permissionRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if(!isAlertsEnabled){
            permissionRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }

        isLocationEnabled()
        val telephonyManager =
            Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        id = telephonyManager.toString()
        androidID.text = "Android ID: $id"
        try {
            start()
        }catch (e:Error){
            Log.d("startError", e.toString())
        }
    }


    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getToken() {
        //Pobranie tokena
        /*val urlGET =
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
        }

        //Potwierdzenie urządzenia
        token_csrf = token_csrf.replace("\"", "")
        val urlPOST =
            sURL.toHttpUrlOrNull()?.newBuilder()?.addQueryParameter("action_sign_in_device", "1")
                ?.addQueryParameter("device_token", id)?.build().toString()
        val sb = StringBuilder()
        sb.append("token_value").append(token_csrf)
        val string = sb.toString()
        println("String " + string)
        println("token: $token_csrf")
        val json = "{\"ID\":$id}"
        println(json)
        val formBody =
            FormBody.Builder().add("action_sign_in_device", "1").add("device_token", id).build()

        val requestPost =
            Request.Builder().url(urlPOST).header("token", string).post(formBody).build()

        client.newCall(requestPost).execute().use { response ->
            device_token = response.body!!.string()
            val cod = response.code.toString()
            Log.d("POST_token", device_token)
            Log.d("POSTcode", cod)
            /*this.runOnUiThread {
                logIn.visibility= View.VISIBLE
            }*/

*/

            var dtURL = "url_address"
            val urlPOST = dtURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
            val formBody =
                FormBody.Builder().add("device_token", id).build()

            val requestPost =
                Request.Builder().url(urlPOST).header("token", "token_value").post(formBody).build()

            try{
                client.newCall(requestPost).execute().use { response ->
                    device_token = response.body!!.string()
                    val cod = response.code.toString()
                    Log.d("POST_device_token", device_token)
                    Log.d("POST_device_token_code", cod)
                }

                if (device_token == "This device doesn't exists in the databse.") {
                    this.runOnUiThread {
                        Toast.makeText(
                            this,
                            "Tego urządzenia nie ma w bazie danych",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }catch (e: Exception){
                Log.d("POST_device_token_error", e.toString())
            }
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

    private fun start() {
        lifecycleScope.launch(Dispatchers.IO) {
            val intent = Intent(this@StartingScreen, MainScreen::class.java)
            if (isOnline(this@StartingScreen)) {
                val config = getStringConfig(id)
                    .replace("{", "")
                    .replace("}", "")
                    .replace("\\s+".toRegex(), "")
                    .replace("[", "")
                    .replace("]", "")
                    .split(",")
                Log.d("Konfig", config.toString())
                for (c in config) {
                    val row = c.replace("\"", "").split(":")
                    println(row.toString())
                    when (row[0]) {
                        "LocalizationNetInterval" -> {
                            println(row[1])
                            intent.putExtra("LocalizationNetInterval", row[1])
                        }
                        "LocalizationSMSInterval" -> {
                            println(row[1])
                            intent.putExtra("LocalizationSMSInterval", row[1])
                        }
                        "SOSButtonInactiveTime" -> {
                            println(row[1])
                            intent.putExtra("SOSButtonInactiveTime", row[1])
                        }
                        "SMSButtonSensitivity" -> {
                            println(row[1])
                            intent.putExtra("SMSButtonSensitivity", row[1])
                        }
                    }
                }
                try {
                    getToken()
                }catch (e:Error){
                    Log.d("getTokenError", e.toString())
                }
                intent.putExtra("device_token", device_token)
                intent.putExtra("token_csrf", device_token)
                Log.d("Config", "wysłano")
            } else {
                this@StartingScreen.runOnUiThread {
                    Toast.makeText(
                        this@StartingScreen,
                        "Brak połączenia z internetem",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            if (ContextCompat.checkSelfPermission(
                    this@StartingScreen,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("TestPerm", "granted")
                startActivity(intent)
                finish()
            } else {
                delay(2000L)
                try {
                    start()
                }catch (e:Error){
                    Log.d("startError", e.toString())
                }
            }
        }
    }

    private fun getStringConfig(id: String): String {
        sURL="url_string"
        val urlGET =
            sURL.toHttpUrlOrNull()?.newBuilder()
                //?.addQueryParameter("show_device_config", "1")
                //?.addQueryParameter("AndroidID", id)
                ?.build().toString()
        val request =
            Request.Builder().url(urlGET).header("token", "token_value")
                .build()

        var config = ""

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("config_start_ERROR", "Unexpected code $response")
                }

                for ((name, value) in response.headers) {
                    Log.d("config_start_HEADERS_RESPONSE", "$name: $value")
                }
                config = response.body!!.string()
               // Log.d("Config_start ", config)
                var config_cod = response.code
                Log.d("Config_start ", config_cod.toString())
            }
        } catch (e: Exception) {
            Log.e("Error", e.toString())
        }
        return config
    }

    private fun getConfig() {
        id = ""
        sURL = "url_string"
        client = OkHttpClient()
        token_csrf = ""
        device_token = ""
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MAKE_CALL_PERMISSION_REQUEST_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "You can call the number by clicking on the button",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }
}