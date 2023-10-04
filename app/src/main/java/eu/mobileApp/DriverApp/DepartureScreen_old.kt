package eu.mobileApp.DriverApp
/*
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import eu.mobileApp.GetSetDB.comm.PendingAbsence
import eu.mobileApp.GetSetDB.comm.PostGetApi
import eu.mobileApp.GetSetDB.databinding.ActivityDepartureBinding
import eu.mobileApp.GetSetDB.login.Absence
import eu.mobileApp.GetSetDB.login.Departure
import eu.mobileApp.GetSetDB.login.UserApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class DepartureScreen_old : AppCompatActivity(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private lateinit var binding:ActivityDepartureBinding

    var depart_date=Calendar.getInstance()
    var arrival_date=Calendar.getInstance()
    var cal=Calendar.getInstance()
    val myFormat="yyyy-MM-dd HH:mm:ss"
    val sdf=SimpleDateFormat(myFormat, Locale.ENGLISH)
    var driverID=""
    var crewID=""
    var nextDepart=""
    var roadCarID=""
    var depart=0 //0-nie zmieniony /1-zmieniona data /2-zmieniona data i czas /3-zmieniony i wysłany
    var arrival=0 //0-nie zmieniony /1-zmieniona data /2-zmieniona data i czas /3- zmieniony i wysłany
    var date=""

    private val driverIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("driverID")
            Log.d("driver_id", message.toString())
            driverID=message.toString()
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

        val my_format="yyyy-MM-dd"
        val sdf_1=SimpleDateFormat(my_format, Locale.ENGLISH)
        date=sdf_1.format(cal.time)

        sendMessage("started")
        try {
            getData()
        }catch (e:Error){
            Log.d("getData", e.toString())
        }

        binding.departureBTN.setOnClickListener {
            depart=1
            DatePickerDialog(this, this, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            binding.confirm.visibility=View.VISIBLE
            //Log.d("Depart_arrival_d", depart.toString())
        }

        binding.arrivalBTN.setOnClickListener {
            arrival=1
            DatePickerDialog(this, this, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            binding.confirm.visibility=View.VISIBLE
            //Log.d("Depart_arrival_a", arrival.toString())
        }

        binding.confirm.setOnClickListener {
            Toast.makeText(this@DepartureScreen_old, "Zatwierdzono zmiany i wysłano", Toast.LENGTH_SHORT).show()
            try {
                putData()
            }catch (e:Error){
                Log.d("putData", e.toString())
            }
            binding.confirm.visibility=View.GONE
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
        if(isOnline(this@DepartureScreen_old)) {
            GlobalScope.launch {
                var client = OkHttpClient()
                var recogized: Boolean = true
                var waitApi: Boolean = true
                val postLocation =
                    PostGetApi(client, recogized, waitApi)
                try {
                    /*if (crewID != "") {
                        this@DepartureScreen.runOnUiThread {
                            binding.noLogin.text = "Brak wniosków w bazie"
                            binding.noLogin.visibility = View.VISIBLE
                        }
                    } else {
                        this@DepartureScreen.runOnUiThread {
                            binding.noLogin.text = "Proszę się zalogować"
                            binding.noLogin.visibility = View.VISIBLE
                        }
                    }*/

                    //Log.d("get_departure_screen_crewID:", crewID)
                    val departure = postLocation.getDeparture(crewID)
                    Log.d("get_departure_screen:", departure.toString())
                    if (departure.contains("RoadCardID")) {
                        val departure_json = Gson().fromJson(departure, Departure::class.java)
                        roadCarID = departure_json.RoadCardID
                        Log.d("get_departure_RoadCardID:", departure_json.RoadCardID.toString())

                        var temp=""
                        if (departure_json.DepartureArrivalComment.toString().contains("\n")){
                            var comment= departure_json.DepartureArrivalComment.toString().split(",")
                            for ( i in 0 .. comment.size-1){
                                    temp=temp+" "+comment[i]+"\n"
                            }
                        }
                        departure_json.DepartureArrivalComment=temp

                        this@DepartureScreen_old.runOnUiThread {
                            binding.departureConfirmed.visibility=View.VISIBLE
                            binding.planedArrivalDateTv.text =departure_json.DepartureArrivalComment.toString()
                            binding.planedArrivalDate.text =departure_json.DepartureArrivalCommentDT.toString()
                        }
                        if (!departure_json.DepartureDt.isNullOrEmpty()) {
                            //1
                            if (departure_json.DriverDepartureDt.isNullOrEmpty()) {
                                this@DepartureScreen_old.runOnUiThread {
                                    binding.departureRow.visibility = View.VISIBLE
                                    binding.departureTV.text=departure_json.DepartureComment
                                    binding.departureDateTV.visibility=View.GONE
                                    binding.departureConfirmed.visibility = View.GONE
                                    binding.arrivalRow.visibility = View.GONE
                                    binding.nextDeparturePlan.visibility = View.GONE
                                    binding.noLogin.visibility = View.GONE
                                    binding.departureConfirmed.visibility = View.GONE
                                }
                            }
                            if (!departure_json.DriverDepartureDt.isNullOrEmpty()) {
                                this@DepartureScreen_old.runOnUiThread {
                                    binding.departureRow.visibility = View.GONE
                                    binding.arrivalRow.visibility = View.GONE
                                    binding.departureConfirmed.visibility = View.VISIBLE
                                    binding.noLogin.visibility = View.GONE
                                }
                            }

                            //2
                            if (!departure_json.ArrivalDt.isNullOrEmpty() && departure_json.DriverArrivalDt.isNullOrEmpty()) {
                                this@DepartureScreen_old.runOnUiThread {
                                    binding.arrivalRow.visibility = View.VISIBLE
                                    binding.arrivalDateTV.visibility=View.GONE
                                    binding.arrivalTV.text=departure_json.ArrivalComment
                                }

                                if (!departure_json.DriverDepartureDt.isNullOrEmpty()) {
                                    this@DepartureScreen_old.runOnUiThread {
                                        binding.departureConfirmed.visibility = View.VISIBLE
                                    }
                                }
                            } else if (!departure_json.DriverArrivalDt.isNullOrEmpty()) {
                                this@DepartureScreen_old.runOnUiThread {
                                    binding.arrivalRow.visibility = View.GONE
                                }
                            }

                            //3
                            if (!departure_json.PlanArrivalDt.isNullOrEmpty() && departure_json.ArrivalDt.isNullOrEmpty()) {
                                this@DepartureScreen_old.runOnUiThread {
                                    binding.nextDeparturePlan.visibility = View.GONE
                                    binding.departureConfirmed.visibility = View.VISIBLE
                                    binding.planedArrivalDateTv.text =departure_json.DepartureArrivalComment.toString()
                                    binding.planedArrivalDate.text =departure_json.DepartureArrivalCommentDT.toString()
                                }
                            }
                            //4
                            if (!departure_json.NextDepartueDTPlan.isNullOrEmpty()) {
                                nextDepart = departure_json.NextDepartueDTPlan.toString()
                                this@DepartureScreen_old.runOnUiThread {
                                    binding.departureConfirmed.visibility = View.VISIBLE
                                    binding.planedArrivalDateTv.text =departure_json.DepartureArrivalComment.toString()
                                    binding.planedArrivalDate.text =departure_json.DepartureArrivalCommentDT.toString()
                                    binding.confirm.visibility = View.GONE
                                }
                            }
                        }
                        if(binding.planedArrivalDateTv.text=="" && !departure_json.DepartureArrivalComment.isNullOrEmpty()){
                            Log.d("get_departure", "refreshing data")
                            delay(500L)
                            getData()
                        }
                    } else {
                        this@DepartureScreen_old.runOnUiThread {
                            binding.departureRow.visibility = View.GONE
                            binding.arrivalRow.visibility = View.GONE
                            binding.confirm.visibility = View.GONE
                            binding.nextDeparturePlan.visibility = View.GONE
                            binding.noLogin.visibility = View.VISIBLE

                            this@DepartureScreen_old.runOnUiThread {
                                binding.noLogin.text = "Brak wniosków w bazie"
                                binding.noLogin.visibility = View.VISIBLE
                            }
                        }
                        Log.d("get_departure", "refreshing data")
                        delay(1000L)
                        getData()
                    }
                    if(departure.isNullOrEmpty()){
                        Log.d("get_departure", "refreshing data")
                        delay(1000L)
                        getData()
                    }
                } catch (e: Error) {
                    Log.e("Error_departScreen", e.toString())
                }
            }
        } else{
            Toast.makeText(this@DepartureScreen_old, "Brak połączenia z internetem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun putData(){
        if(isOnline(this@DepartureScreen_old)) {
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
                        //Log.d("Depart_arrival", depart.toString())
                        //Log.d("Depart_arrival", arrival.toString())
                        depart = 0
                        arrival = 0
                        //Log.d("Depart_arrival", depart.toString())
                        //Log.d("Depart_arrival", arrival.toString())
                    } else if (depart == 3 && arrival != 3) {
                        postLocation.putDeparture(
                            crewID,
                            roadCarID,
                            "",
                            sdf.format(depart_date.time)
                        )
                        //Log.d("Depart_arrival", depart.toString())
                        //Log.d("Depart_arrival", arrival.toString())
                        depart = 0
                        //Log.d("Depart_arrival", depart.toString())
                        //Log.d("Depart_arrival", arrival.toString())
                    } else if (depart != 3 && arrival == 3) {
                        postLocation.putDeparture(
                            crewID,
                            roadCarID,
                            sdf.format(arrival_date.time),
                            ""
                        )
                        //Log.d("Depart_arrival", depart.toString())
                        //Log.d("Depart_arrival", arrival.toString())
                        arrival = 0
                        //Log.d("Depart_arrival", depart.toString())
                        //Log.d("Depart_arrival", arrival.toString())
                    }

                    delay(3000L)
                    getData()
                } catch (e: Error) {
                    Log.e("Error", e.toString())
                }
            }
        }else{
                Toast.makeText(this@DepartureScreen_old, "Brak połączenia z internetem", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        if(depart==1){
            depart_date.set(Calendar.YEAR, year)
            depart_date.set(Calendar.MONTH, month)
            depart_date.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            depart=2
            TimePickerDialog(this,this, cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), true).show()
            //Log.d("Depart_arrival_d", depart.toString())
        }else if(arrival==1){
            arrival_date.set(Calendar.YEAR, year)
            arrival_date.set(Calendar.MONTH, month)
            arrival_date.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            arrival=2
            TimePickerDialog(this,this, cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), true).show()
            //Log.d("Depart_arrival_a", arrival.toString())
        }
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        if(depart==2){
            depart_date.set(Calendar.HOUR_OF_DAY, hourOfDay)
            depart_date.set(Calendar.MINUTE, minute)
            binding.departureDateTV.text=sdf.format(depart_date.time)
            depart=3
            //Log.d("Depart_arrival_d", depart.toString())
        }else if(arrival==2){
            arrival_date.set(Calendar.HOUR_OF_DAY, hourOfDay)
            arrival_date.set(Calendar.MINUTE, minute)
            binding.arrivalDateTV.text=sdf.format(arrival_date.time)
            arrival=3
            //Log.d("Depart_arrival_a", arrival.toString())
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

}*/