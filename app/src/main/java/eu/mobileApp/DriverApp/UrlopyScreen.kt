package eu.mobileApp.DriverApp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import eu.mobileApp.DriverApp.comm.PostGetApi
import eu.mobileApp.DriverApp.databinding.ActivityUrlopyScreenBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class UrlopyScreen : AppCompatActivity() {
    private lateinit var binding: ActivityUrlopyScreenBinding
    var driverID=""
    var crewID=""
    var comment=""
    var pendingAbsenceID=mutableListOf<String>("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
    var absenceArray= mutableListOf<String>("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
    var aid=""

    private val driverIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("driverID")
            Log.d("driver_id", message.toString())
            driverID=message.toString()
            Log.d("DriverID_receiver", driverID.toString())
            try {
                getData()
                start()
            }catch (e:Error){
                Log.d("getData", e.toString())
            }
        }
    }

    private val crewIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("crewID")
            Log.d("crew_id", message.toString())
            crewID=message.toString()
            try {
                getData()
            }catch (e:Error){
                Log.d("getData", e.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_urlopy_screen)
        binding=ActivityUrlopyScreenBinding.inflate(layoutInflater)
        val viewBind = binding.root
        setContentView(viewBind)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            driverIDReceiver, IntentFilter("driverID")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            crewIDReceiver, IntentFilter("crewID")
        )

        binding.webView.webViewClient= WebViewClient()
        binding.webView.clearCache(true)
        binding.webView.settings.javaScriptEnabled=true
        binding.webView.settings.domStorageEnabled=true

        start()


        sendMessage("started")
        try {
            getData()
        }catch (e:Error){
            Log.d("getData", e.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(driverIDReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(crewIDReceiver)
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

    private fun sendMessage(msg: String) {
        val intent = Intent("departureScreen")
        intent.putExtra("departureScreen", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun putAbsence(comm:String, pendingAbsenceIDstr:String, id: Int){
        GlobalScope.launch {
            var client = OkHttpClient()
            var recogized: Boolean = true
            var waitApi: Boolean = true
            val postLocation =
                PostGetApi(client, recogized, waitApi)
            try {
                val pendingAbsence = postLocation.getPendingAbsence(driverID)
                if (pendingAbsence != "" || pendingAbsence != "[]") {
                    postLocation.putAbsence(pendingAbsenceIDstr, driverID, comm)
                }
            } catch (e: Error) {
                Log.e("Error_putAbsence", e.toString())
            }

            delay(500L)
            getData()
        }
        absenceArray=mutableListOf<String>("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
        pendingAbsenceID =mutableListOf<String>("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
        //absenceArray.set(id, "")
        //pendingAbsenceID.set(id, "")
        Log.d("pendingAbsence_testAbsenc", absenceArray.toString())
        Log.d("pendingAbsence_testID", pendingAbsenceID.toString())
        comment=""
    }

    private fun getData(){
        if(isOnline(this@UrlopyScreen)) {
            GlobalScope.launch {
                var client = OkHttpClient()
                var recogized: Boolean = true
                var waitApi: Boolean = true
                val postLocation =
                    PostGetApi(client, recogized, waitApi)
                try {
                    val pendingAbsence = postLocation.getPendingAbsence(driverID)
                    Log.d("pendingAbsence", pendingAbsence.toString())
                } catch (e: Error) {
                    Log.e("Error_departScreen", e.toString())
                }
            }
        } else{
            Toast.makeText(this@UrlopyScreen, "Brak połączenia z internetem", Toast.LENGTH_SHORT).show()
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

    private fun start(){
        if(crewID!="" && driverID!=""){
            //binding.webView.loadUrl("192.168.0.104/urlopy.html?"+crewID)
            //binding.webView.loadUrl("file:///android_asset/urlopy.html?"+crewID)
            val telephonyID =
                Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
            aid = telephonyID.toString()
            binding.webView.loadUrl("url_address"+
                    "?DriverID=${driverID}&CrewID=${crewID}&AndroidID=${aid}")
        }
    }

}