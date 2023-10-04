package eu.mobileApp.DriverApp
/*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.opengl.Visibility
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import eu.mobileApp.GetSetDB.comm.PendingAbsence
import eu.mobileApp.GetSetDB.comm.PostGetApi
import eu.mobileApp.GetSetDB.databinding.ActivityUrlopyScreenBinding
import eu.mobileApp.GetSetDB.login.Departure
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class UrlopyScreen_old : AppCompatActivity() {
    private lateinit var binding: ActivityUrlopyScreenBinding
    var driverID=""
    var crewID=""
    var comment=""
    var pendingAbsenceID=mutableListOf<String>("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
    var absenceArray= mutableListOf<String>("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")

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

        binding.commentBTN0.setOnClickListener {
            comment=binding.absenceEdit0.text.toString()
            if(comment!=""){
                binding.absenceEdit0.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                binding.urlopLayout0.visibility=View.GONE
                binding.confirm.visibility=View.GONE
                binding.commentBTN0.visibility= View.GONE
                binding.absenceEdit0.visibility= View.GONE
                putAbsence(comment, pendingAbsenceID[0], 0)
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN0.setOnClickListener {
            binding.urlopLayout0.visibility= View.GONE
            binding.commentBTN0.visibility= View.GONE
            binding.absenceEdit0.visibility= View.GONE
            binding.absenceEdit0.setText("")
            putAbsence(comment, pendingAbsenceID[0], 0)
        }

        binding.rejectBTN0.setOnClickListener {
            binding.commentBTN0.visibility= View.VISIBLE
            binding.absenceEdit0.visibility= View.VISIBLE
        }

        binding.commentBTN1.setOnClickListener {
            comment=binding.absenceEdit1.text.toString()
            if(comment!=""){
                binding.absenceEdit1.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                binding.urlopLayout1.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN1.visibility= View.GONE
                binding.absenceEdit1.visibility= View.GONE
                putAbsence(comment, pendingAbsenceID[1], 1)
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN1.setOnClickListener {
            binding.urlopLayout1.visibility= View.GONE
            binding.commentBTN1.visibility= View.GONE
            binding.absenceEdit1.visibility= View.GONE
            binding.absenceEdit1.setText("")
            putAbsence(comment, pendingAbsenceID[1], 1)
        }

        binding.rejectBTN1.setOnClickListener {
            binding.commentBTN1.visibility= View.VISIBLE
            binding.absenceEdit1.visibility= View.VISIBLE
        }


        binding.commentBTN2.setOnClickListener {
            comment=binding.absenceEdit2.text.toString()
            if(comment!=""){
                binding.absenceEdit2.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                binding.urlopLayout2.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN2.visibility= View.GONE
                binding.absenceEdit2.visibility= View.GONE
                putAbsence(comment, pendingAbsenceID[2], 2)
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN2.setOnClickListener {
            binding.urlopLayout2.visibility= View.GONE
            binding.commentBTN2.visibility= View.GONE
            binding.absenceEdit2.visibility= View.GONE
            binding.absenceEdit2.setText("")
            putAbsence(comment, pendingAbsenceID[2], 3)

        }

        binding.rejectBTN2.setOnClickListener {
            binding.commentBTN2.visibility= View.VISIBLE
            binding.absenceEdit2.visibility= View.VISIBLE
        }


        binding.commentBTN3.setOnClickListener {
            comment=binding.absenceEdit3.text.toString()
            if(comment!=""){
                binding.absenceEdit3.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                binding.urlopLayout3.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN3.visibility= View.GONE
                binding.absenceEdit3.visibility= View.GONE
                putAbsence(comment, pendingAbsenceID[3], 3)
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN3.setOnClickListener {
            binding.urlopLayout3.visibility= View.GONE
            binding.commentBTN3.visibility= View.GONE
            binding.absenceEdit3.visibility= View.GONE
            binding.absenceEdit3.setText("")
            putAbsence(comment, pendingAbsenceID[3], 3)
        }

        binding.rejectBTN3.setOnClickListener {
            binding.commentBTN3.visibility= View.VISIBLE
            binding.absenceEdit3.visibility= View.VISIBLE
        }


        binding.commentBTN4.setOnClickListener {
            comment=binding.absenceEdit4.text.toString()
            if(comment!=""){
                binding.absenceEdit4.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[4], 4)
                binding.urlopLayout4.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN4.visibility= View.GONE
                binding.absenceEdit4.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN4.setOnClickListener {
            binding.urlopLayout4.visibility= View.GONE
            binding.commentBTN4.visibility= View.GONE
            binding.absenceEdit4.visibility= View.GONE
            binding.absenceEdit4.setText("")
            putAbsence(comment, pendingAbsenceID[4], 4)
        }

        binding.rejectBTN4.setOnClickListener {
            binding.commentBTN4.visibility= View.VISIBLE
            binding.absenceEdit4.visibility= View.VISIBLE
        }


        binding.commentBTN5.setOnClickListener {
            comment=binding.absenceEdit5.text.toString()
            if(comment!=""){
                binding.absenceEdit5.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[5], 5)
                binding.urlopLayout5.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN5.visibility= View.GONE
                binding.absenceEdit5.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN5.setOnClickListener {
            binding.urlopLayout5.visibility= View.GONE
            binding.commentBTN5.visibility= View.GONE
            binding.absenceEdit5.visibility= View.GONE
            binding.absenceEdit5.setText("")
            putAbsence(comment, pendingAbsenceID[5], 5)
        }

        binding.rejectBTN5.setOnClickListener {
            binding.commentBTN5.visibility= View.VISIBLE
            binding.absenceEdit5.visibility= View.VISIBLE
        }


        binding.commentBTN6.setOnClickListener {
            comment=binding.absenceEdit6.text.toString()
            if(comment!=""){
                binding.absenceEdit6.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[6], 6)
                binding.urlopLayout6.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN6.visibility= View.GONE
                binding.absenceEdit6.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN6.setOnClickListener {
            binding.urlopLayout6.visibility= View.GONE
            binding.commentBTN6.visibility= View.GONE
            binding.absenceEdit6.visibility= View.GONE
            binding.absenceEdit6.setText("")
            putAbsence(comment, pendingAbsenceID[6], 6)
        }

        binding.rejectBTN6.setOnClickListener {
            binding.commentBTN6.visibility= View.VISIBLE
            binding.absenceEdit6.visibility= View.VISIBLE
        }


        binding.commentBTN7.setOnClickListener {
            comment=binding.absenceEdit7.text.toString()
            if(comment!=""){
                binding.absenceEdit7.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[7], 7)
                binding.urlopLayout7.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN7.visibility= View.GONE
                binding.absenceEdit7.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN7.setOnClickListener {
            binding.urlopLayout7.visibility= View.GONE
            binding.commentBTN7.visibility= View.GONE
            binding.absenceEdit7.visibility= View.GONE
            binding.absenceEdit7.setText("")
            putAbsence(comment, pendingAbsenceID[7], 7)
        }

        binding.rejectBTN7.setOnClickListener {
            binding.commentBTN7.visibility= View.VISIBLE
            binding.absenceEdit7.visibility= View.VISIBLE
        }


        binding.commentBTN8.setOnClickListener {
            comment=binding.absenceEdit8.text.toString()
            if(comment!=""){
                binding.absenceEdit8.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[8], 8)
                binding.urlopLayout8.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN8.visibility= View.GONE
                binding.absenceEdit8.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN8.setOnClickListener {
            binding.urlopLayout8.visibility= View.GONE
            binding.commentBTN8.visibility= View.GONE
            binding.absenceEdit8.visibility= View.GONE
            binding.absenceEdit8.setText("")
            putAbsence(comment, pendingAbsenceID[8], 8)
        }

        binding.rejectBTN8.setOnClickListener {
            binding.commentBTN8.visibility= View.VISIBLE
            binding.absenceEdit8.visibility= View.VISIBLE
        }


        binding.commentBTN9.setOnClickListener {
            comment=binding.absenceEdit9.text.toString()
            if(comment!=""){
                binding.absenceEdit9.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[9], 9)
                binding.urlopLayout9.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN9.visibility= View.GONE
                binding.absenceEdit9.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN9.setOnClickListener {
            binding.urlopLayout9.visibility= View.GONE
            binding.commentBTN9.visibility= View.GONE
            binding.absenceEdit9.visibility= View.GONE
            binding.absenceEdit9.setText("")
            putAbsence(comment, pendingAbsenceID[9], 9)
        }

        binding.rejectBTN9.setOnClickListener {
            binding.commentBTN9.visibility= View.VISIBLE
            binding.absenceEdit9.visibility= View.VISIBLE
        }


        binding.commentBTN10.setOnClickListener {
            comment=binding.absenceEdit10.text.toString()
            if(comment!=""){
                binding.absenceEdit10.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[10], 10)
                binding.urlopLayout10.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN10.visibility= View.GONE
                binding.absenceEdit10.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN10.setOnClickListener {
            binding.urlopLayout10.visibility= View.GONE
            binding.commentBTN10.visibility= View.GONE
            binding.absenceEdit10.visibility= View.GONE
            binding.absenceEdit10.setText("")
            putAbsence(comment, pendingAbsenceID[10], 10)
        }

        binding.rejectBTN10.setOnClickListener {
            binding.commentBTN10.visibility= View.VISIBLE
            binding.absenceEdit10.visibility= View.VISIBLE
        }


        binding.commentBTN11.setOnClickListener {
            comment=binding.absenceEdit11.text.toString()
            if(comment!=""){
                binding.absenceEdit11.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[11], 11)
                binding.urlopLayout11.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN11.visibility= View.GONE
                binding.absenceEdit11.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN11.setOnClickListener {
            binding.urlopLayout11.visibility= View.GONE
            binding.commentBTN11.visibility= View.GONE
            binding.absenceEdit11.visibility= View.GONE
            binding.absenceEdit11.setText("")
            putAbsence(comment, pendingAbsenceID[11], 11)
        }

        binding.rejectBTN11.setOnClickListener {
            binding.commentBTN11.visibility= View.VISIBLE
            binding.absenceEdit11.visibility= View.VISIBLE
        }


        binding.commentBTN12.setOnClickListener {
            comment=binding.absenceEdit12.text.toString()
            if(comment!=""){
                binding.absenceEdit12.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[12], 12)
                binding.urlopLayout12.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN12.visibility= View.GONE
                binding.absenceEdit12.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN12.setOnClickListener {
            binding.urlopLayout12.visibility= View.GONE
            binding.commentBTN12.visibility= View.GONE
            binding.absenceEdit12.visibility= View.GONE
            binding.absenceEdit12.setText("")
            putAbsence(comment, pendingAbsenceID[12], 12)
        }

        binding.rejectBTN12.setOnClickListener {
            binding.commentBTN12.visibility= View.VISIBLE
            binding.absenceEdit12.visibility= View.VISIBLE
        }


        binding.commentBTN13.setOnClickListener {
            comment=binding.absenceEdit13.text.toString()
            if(comment!=""){
                binding.absenceEdit13.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[13], 13)
                binding.urlopLayout13.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN13.visibility= View.GONE
                binding.absenceEdit13.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN13.setOnClickListener {
            binding.urlopLayout13.visibility= View.GONE
            binding.commentBTN13.visibility= View.GONE
            binding.absenceEdit13.visibility= View.GONE
            binding.absenceEdit13.setText("")
            putAbsence(comment, pendingAbsenceID[13], 13)
        }

        binding.rejectBTN13.setOnClickListener {
            binding.commentBTN13.visibility= View.VISIBLE
            binding.absenceEdit13.visibility= View.VISIBLE
        }


        binding.commentBTN14.setOnClickListener {
            comment=binding.absenceEdit14.text.toString()
            if(comment!=""){
                binding.absenceEdit14.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[14], 14)
                binding.urlopLayout14.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN14.visibility= View.GONE
                binding.absenceEdit14.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN14.setOnClickListener {
            binding.urlopLayout14.visibility= View.GONE
            binding.commentBTN14.visibility= View.GONE
            binding.absenceEdit14.visibility= View.GONE
            binding.absenceEdit14.setText("")
            putAbsence(comment, pendingAbsenceID[14], 14)
        }

        binding.rejectBTN14.setOnClickListener {
            binding.commentBTN14.visibility= View.VISIBLE
            binding.absenceEdit14.visibility= View.VISIBLE
        }


        binding.commentBTN15.setOnClickListener {
            comment=binding.absenceEdit15.text.toString()
            if(comment!=""){
                binding.absenceEdit15.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[15], 15)
                binding.urlopLayout15.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN15.visibility= View.GONE
                binding.absenceEdit15.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN15.setOnClickListener {
            binding.urlopLayout15.visibility= View.GONE
            binding.commentBTN15.visibility= View.GONE
            binding.absenceEdit15.visibility= View.GONE
            binding.absenceEdit15.setText("")
            putAbsence(comment, pendingAbsenceID[15], 15)
        }

        binding.rejectBTN15.setOnClickListener {
            binding.commentBTN15.visibility= View.VISIBLE
            binding.absenceEdit15.visibility= View.VISIBLE
        }


        binding.commentBTN16.setOnClickListener {
            comment=binding.absenceEdit16.text.toString()
            if(comment!=""){
                binding.absenceEdit16.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[16], 16)
                binding.urlopLayout16.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN16.visibility= View.GONE
                binding.absenceEdit16.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN16.setOnClickListener {
            binding.urlopLayout16.visibility= View.GONE
            binding.commentBTN16.visibility= View.GONE
            binding.absenceEdit16.visibility= View.GONE
            binding.absenceEdit16.setText("")
            putAbsence(comment, pendingAbsenceID[16], 16)
        }

        binding.rejectBTN16.setOnClickListener {
            binding.commentBTN16.visibility= View.VISIBLE
            binding.absenceEdit16.visibility= View.VISIBLE
        }


        binding.commentBTN17.setOnClickListener {
            comment=binding.absenceEdit17.text.toString()
            if(comment!=""){
                binding.absenceEdit17.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[17], 17)
                binding.urlopLayout17.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN17.visibility= View.GONE
                binding.absenceEdit17.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN17.setOnClickListener {
            binding.urlopLayout17.visibility= View.GONE
            binding.commentBTN17.visibility= View.GONE
            binding.absenceEdit17.visibility= View.GONE
            binding.absenceEdit17.setText("")
            putAbsence(comment, pendingAbsenceID[17], 17)
        }

        binding.rejectBTN17.setOnClickListener {
            binding.commentBTN17.visibility= View.VISIBLE
            binding.absenceEdit17.visibility= View.VISIBLE
        }


        binding.commentBTN18.setOnClickListener {
            comment=binding.absenceEdit18.text.toString()
            if(comment!=""){
                binding.absenceEdit18.setText("")
                Toast.makeText(this@UrlopyScreen_old, "Zapisano uzasadnienie", Toast.LENGTH_SHORT).show()
                putAbsence(comment, pendingAbsenceID[18], 18)
                binding.urlopLayout18.visibility= View.GONE
                binding.confirm.visibility= View.GONE
                binding.commentBTN18.visibility= View.GONE
                binding.absenceEdit18.visibility= View.GONE
            }else{
                Toast.makeText(this@UrlopyScreen_old, "Aby odrzucić urlop musisz podać uzasadnienie", Toast.LENGTH_SHORT).show()
            }
        }

        binding.confirm.visibility=View.GONE

        binding.confirmBTN18.setOnClickListener {
            binding.urlopLayout18.visibility= View.GONE
            binding.commentBTN18.visibility= View.GONE
            binding.absenceEdit18.visibility= View.GONE
            binding.absenceEdit18.setText("")
            putAbsence(comment, pendingAbsenceID[18], 18)
        }

        binding.rejectBTN18.setOnClickListener {
            binding.commentBTN18.visibility= View.VISIBLE
            binding.absenceEdit18.visibility= View.VISIBLE
        }


        sendMessage("started")
        try {
            getData()
            Log.d("getData", "test")
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

    private fun putAbsence(comm:String, pendingAbsenceIDStr:String, id: Int){
        GlobalScope.launch {
            var client = OkHttpClient()
            var recogized: Boolean = true
            var waitApi: Boolean = true
            val postLocation =
                PostGetApi(client, recogized, waitApi)
            try {
                val pendingAbsence = postLocation.getPendingAbsence(driverID)
                if (pendingAbsence != "" || pendingAbsence != "[]") {
                    postLocation.putAbsence(pendingAbsenceIDStr, driverID, comm)
                    //Log.d("put_pendingAbsence", pendingAbsenceID+" "+driverID+" komentarz: "+comm)
                }
            } catch (e: Error) {
                Log.e("Error_putAbsence", e.toString())
            }

            delay(500L)
            getData()
        }
        absenceArray=mutableListOf<String>("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
        pendingAbsenceID=mutableListOf<String>("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
        //absenceArray.set(id, "")
        //pendingAbsenceID.set(id, "")
        Log.d("pendingAbsence_testAbsenc", absenceArray.toString())
        Log.d("pendingAbsence_testID", pendingAbsenceID.toString())
        comment=""
    }

    private fun getData(){
        if(isOnline(this@UrlopyScreen_old)) {
            GlobalScope.launch {
                var client = OkHttpClient()
                var recogized: Boolean = true
                var waitApi: Boolean = true
                val postLocation =
                    PostGetApi(client, recogized, waitApi)
                try {
                    if (driverID != "" && absenceArray[0].isNullOrEmpty() ){
                        this@UrlopyScreen_old.runOnUiThread {
                            binding.noLogin.text = "Brak wniosków w bazie"
                            binding.noLogin.visibility = View.VISIBLE
                        }
                    } else {
                        this@UrlopyScreen_old.runOnUiThread {
                            binding.noLogin.text = "Proszę się zalogować"
                            binding.noLogin.visibility = View.VISIBLE
                        }
                    }
                    val pendingAbsence = postLocation.getPendingAbsence(driverID)
                    //val absence= postLocation.getAbsence(driverUID)
                    Log.d("pendingAbsence", pendingAbsence.toString())
                    //Log.d("pendingAbsence", absence.toString())
                    if (!pendingAbsence.isNullOrEmpty()) {
                        if (pendingAbsence == "[]") {
                            this@UrlopyScreen_old.runOnUiThread {
                                binding.urlopLayout0.visibility = View.GONE
                                binding.absenceDate0.text = pendingAbsence.toString()
                            }
                        } else //if(nextDepart!="")
                        {
                            this@UrlopyScreen_old.runOnUiThread {
                                binding.noLogin.visibility = View.GONE
                                //binding.nextDeparturePlan.visibility=View.VISIBLE
                                binding.urlopLayout0.visibility = View.VISIBLE
                                binding.absenceDate0.text = ""
                            }
                            try {
                                val temp = pendingAbsence.substring(1, pendingAbsence.length - 1)
                                Log.d("pendingAbsence_", temp.toString())
                                if(temp.contains("},")){
                                    val temp1=temp.split("},")
                                    //Log.d("pendingAbsence_temp", temp1.toString())
                                    for (i in 0 until temp1.size){
                                        Log.d("pendingAbsence_i", i.toString())
                                        if (temp1[i].contains("}")){
                                            absenceArray.set(i, temp1[i])
                                            //Log.d("pendingAbsence_itemp", temp1[i].toString())
                                        }else{
                                            absenceArray.set(i, temp1[i]+"}")
                                        }
                                    }
                                    Log.d("pendingAbsence_test", absenceArray.toString())

                                    if(!absenceArray[0].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[0], PendingAbsence::class.java)
                                        pendingAbsenceID[0] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout0.visibility=View.VISIBLE
                                            binding.absenceDate0.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[1].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[1], PendingAbsence::class.java)
                                        pendingAbsenceID[1] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout1.visibility=View.VISIBLE
                                            binding.absenceDate1.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[2].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[2], PendingAbsence::class.java)
                                        pendingAbsenceID[2] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout2.visibility=View.VISIBLE
                                            binding.absenceDate2.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[3].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[3], PendingAbsence::class.java)
                                        pendingAbsenceID[3] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout3.visibility=View.VISIBLE
                                            binding.absenceDate3.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[4].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[4], PendingAbsence::class.java)
                                        pendingAbsenceID[4] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout4.visibility=View.VISIBLE
                                            binding.absenceDate4.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[5].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[5], PendingAbsence::class.java)
                                        pendingAbsenceID[5] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout5.visibility=View.VISIBLE
                                            binding.absenceDate5.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[6].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[6], PendingAbsence::class.java)
                                        pendingAbsenceID[6] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout6.visibility=View.VISIBLE
                                            binding.absenceDate6.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[7].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[7], PendingAbsence::class.java)
                                        pendingAbsenceID[7] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout7.visibility=View.VISIBLE
                                            binding.absenceDate7.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[8].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[8], PendingAbsence::class.java)
                                        pendingAbsenceID[8] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout8.visibility=View.VISIBLE
                                            binding.absenceDate8.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[9].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[9], PendingAbsence::class.java)
                                        pendingAbsenceID[9] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout9.visibility=View.VISIBLE
                                            binding.absenceDate9.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[10].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[10], PendingAbsence::class.java)
                                        pendingAbsenceID[10] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout10.visibility=View.VISIBLE
                                            binding.absenceDate10.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[11].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[11], PendingAbsence::class.java)
                                        pendingAbsenceID[11] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout11.visibility=View.VISIBLE
                                            binding.absenceDate11.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[12].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[12], PendingAbsence::class.java)
                                        pendingAbsenceID[12] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout12.visibility=View.VISIBLE
                                            binding.absenceDate12.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[13].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[13], PendingAbsence::class.java)
                                        pendingAbsenceID[13] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout13.visibility=View.VISIBLE
                                            binding.absenceDate13.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[14].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[14], PendingAbsence::class.java)
                                        pendingAbsenceID[14] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout14.visibility=View.VISIBLE
                                            binding.absenceDate14.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[15].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[15], PendingAbsence::class.java)
                                        pendingAbsenceID[15] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout15.visibility=View.VISIBLE
                                            binding.absenceDate15.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[16].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[16], PendingAbsence::class.java)
                                        pendingAbsenceID[16] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout16.visibility=View.VISIBLE
                                            binding.absenceDate16.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[17].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[17], PendingAbsence::class.java)
                                        pendingAbsenceID[17] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout17.visibility=View.VISIBLE
                                            binding.absenceDate17.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                    if(!absenceArray[18].isNullOrEmpty()){
                                        val pendingAbsenceJson = Gson().fromJson(absenceArray[18], PendingAbsence::class.java)
                                        pendingAbsenceID[18] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout18.visibility=View.VISIBLE
                                            binding.absenceDate18.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }

                                }else{
                                    if (absenceArray[0].isNullOrEmpty() && absenceArray[1].isNullOrEmpty()){
                                        val pendingAbsenceJson =
                                            Gson().fromJson(temp, PendingAbsence::class.java)
                                        pendingAbsenceID[0] = pendingAbsenceJson.ID
                                        Log.d("pendingAbsence", pendingAbsenceJson.toString())
                                        this@UrlopyScreen_old.runOnUiThread {
                                            binding.urlopLayout0.visibility=View.VISIBLE
                                            binding.absenceDate0.text =
                                                "${pendingAbsenceJson.DateFrom.toString()} - ${pendingAbsenceJson.DateTo.toString()}"
                                        }
                                    }
                                }
                            } catch (e: Error) {
                                Log.e("Error_pendingAbsence", e.toString())
                            }
                        }
                    } else {
                        this@UrlopyScreen_old.runOnUiThread {
                            binding.urlopLayout0.visibility = View.GONE
                            binding.absenceDate0.text = pendingAbsence.toString()

                            if (driverID != "") {
                                this@UrlopyScreen_old.runOnUiThread {
                                    binding.noLogin.text = "Brak wniosków w bazie"
                                    binding.noLogin.visibility = View.VISIBLE
                                }
                            } else {
                                this@UrlopyScreen_old.runOnUiThread {
                                    binding.noLogin.text = "Proszę się zalogować"
                                    binding.noLogin.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                } catch (e: Error) {
                    Log.e("Error_departScreen", e.toString())
                }
            }
        } else{
            Toast.makeText(this@UrlopyScreen_old, "Brak połączenia z internetem", Toast.LENGTH_SHORT).show()
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

    /*
    private fun generate_layout(){
        val parent=binding.tableLayout
        val subparent=TableRow(this)
        subparent.layoutParams.height=TableRow.LayoutParams.WRAP_CONTENT
        subparent.layoutParams.width=TableRow.LayoutParams.MATCH_PARENT
        subparent.gravity=TableRow.TEXT_ALIGNMENT_CENTER
        val subsubparent=LinearLayout(this)

        subsubparent.id=View.generateViewId()

    }
     */
}
*/