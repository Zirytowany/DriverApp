package eu.mobileApp.DriverApp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebViewClient
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import eu.mobileApp.DriverApp.databinding.ActivityAnkietyScreenBinding

class AnkietyScreen : AppCompatActivity() {
    private lateinit var binding: ActivityAnkietyScreenBinding
    var aid=""
    var driverID=""
    var crewID=""

    private val driverIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("driverID")
            Log.d("driver_id", message.toString())
            driverID=message.toString()
            Log.d("DriverID_receiver", driverID.toString())
            try {
                getData()
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
        setContentView(R.layout.activity_ankiety_screen)
        binding=ActivityAnkietyScreenBinding.inflate(layoutInflater)
        val viewBind = binding.root
        setContentView(viewBind)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            driverIDReceiver, IntentFilter("driverID")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            crewIDReceiver, IntentFilter("crewID")
        )

        sendMessage("started")

        binding.webView.webViewClient= WebViewClient()
        binding.webView.clearCache(true)
        binding.webView.settings.javaScriptEnabled=true
        binding.webView.settings.domStorageEnabled=true
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

    fun getData(){
        if(crewID!="" && driverID!="") {
            val telephonyID =
                Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
            aid = telephonyID.toString()
            binding.webView.loadUrl("url_string" +
                    "?DriverID=${driverID}&CrewID=${crewID}&AndroidID=${aid}")
        }
    }

    private fun sendMessage(msg: String) {
        val intent = Intent("departureScreen")
        intent.putExtra("departureScreen", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(driverIDReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(crewIDReceiver)
    }
}