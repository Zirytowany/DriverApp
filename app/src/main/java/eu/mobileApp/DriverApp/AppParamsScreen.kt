package eu.mobileApp.DriverApp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import eu.mobileApp.DriverApp.databinding.ActivityAppParamsScreenBinding
import eu.mobileApp.DriverApp.login.LoginDatabase

private var aid: String = ""
private lateinit var binding:ActivityAppParamsScreenBinding

class AppParamsScreen : AppCompatActivity() {

    val db by lazy{
        LoginDatabase.getDatabase(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_params_screen)
        binding= ActivityAppParamsScreenBinding.inflate(layoutInflater)
        val viewBind = binding.root
        setContentView(viewBind)

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
            Log.d("AndroidID", aid)
        }
        binding.aidText.text=aid
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
}