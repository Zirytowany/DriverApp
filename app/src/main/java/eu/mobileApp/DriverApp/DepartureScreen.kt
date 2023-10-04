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
import eu.mobileApp.DriverApp.databinding.ActivityDepartureBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.*

class DepartureScreen : AppCompatActivity() {
    private lateinit var binding:ActivityDepartureBinding

    var depart_date=Calendar.getInstance()
    var arrival_date=Calendar.getInstance()
    var cal=Calendar.getInstance()
    val myFormat="yyyy-MM-dd HH:mm:ss"
    val sdf=SimpleDateFormat(myFormat, Locale.ENGLISH)
    var driverID=""
    var crewID=""
    var roadCarID=""
    var depart=0 //0-nie zmieniony /1-zmieniona data /2-zmieniona data i czas /3-zmieniony i wysłany
    var arrival=0 //0-nie zmieniony /1-zmieniona data /2-zmieniona data i czas /3- zmieniony i wysłany
    var date=""
    var aid=""

    private val driverIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("driverID")
            Log.d("driver_id", message.toString())
            driverID=message.toString()
            start()
        }
    }

    private val crewIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("crewID")
            Log.d("crew_id", message.toString())
            crewID=message.toString()
            try {
                getData()
                start()
            }catch (e:Error){
                Log.d("getData", e.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_departure)
        binding = ActivityDepartureBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_departure)
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

        //binding.webView.loadUrl("192.168.0.104/test.html?"+aid.toString())
        start()


        val my_format="yyyy-MM-dd"
        val sdf_1=SimpleDateFormat(my_format, Locale.ENGLISH)
        date=sdf_1.format(cal.time)

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

    private fun getData(){
        if(isOnline(this@DepartureScreen)) {
            GlobalScope.launch {
                var client = OkHttpClient()
                var recogized: Boolean = true
                var waitApi: Boolean = true
                val postLocation =
                    PostGetApi(client, recogized, waitApi)
                try {
                    val departure = postLocation.getDeparture(crewID)
                    Log.d("get_departure_screen:", departure.toString())
                    if (departure.contains("RoadCardID")) {
                    } else {
                        Log.d("get_departure", "refreshing data")
                        //delay(1000L)
                        //getData()
                    }
                    if(departure.isNullOrEmpty()){
                        Log.d("get_departure", "refreshing data")
                        //delay(1000L)
                        //getData()
                    }
                } catch (e: Error) {
                    Log.e("Error_departScreen", e.toString())
                }
            }
        } else{
            Toast.makeText(this@DepartureScreen, "Brak połączenia z internetem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun putData(){
        if(isOnline(this@DepartureScreen)) {
            GlobalScope.launch {
                var client = OkHttpClient()
                var recogized: Boolean = true
                var waitApi: Boolean = true
                val postLocation =
                    PostGetApi(client, recogized, waitApi)
                try {
                    if (depart == 3 && arrival == 3) {
                        postLocation.putDeparture(
                            crewID,
                            roadCarID,
                            sdf.format(arrival_date.time),
                            sdf.format(depart_date.time)
                        )
                        depart = 0
                        arrival = 0
                    } else if (depart == 3 && arrival != 3) {
                        postLocation.putDeparture(
                            crewID,
                            roadCarID,
                            "",
                            sdf.format(depart_date.time)
                        )
                        depart = 0
                    } else if (depart != 3 && arrival == 3) {
                        postLocation.putDeparture(
                            crewID,
                            roadCarID,
                            sdf.format(arrival_date.time),
                            ""
                        )
                        arrival = 0
                    }
                    delay(3000L)
                    getData()
                } catch (e: Error) {
                    Log.e("Error", e.toString())
                }
            }
        }else{
                Toast.makeText(this@DepartureScreen, "Brak połączenia z internetem", Toast.LENGTH_SHORT).show()
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
        if (crewID!="" && driverID!="") {
            val telephonyID =
                Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
            aid = telephonyID.toString()
            binding.webView.loadUrl("string_url"+
                    "?DriverID=${driverID}&CrewID=${crewID}&AndroidID=${aid}")
        }

    }

}