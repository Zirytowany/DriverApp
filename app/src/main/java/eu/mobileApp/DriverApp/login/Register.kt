package eu.mobileApp.DriverApp.login

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import eu.mobileApp.DriverApp.R

class Register : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private var ref = "https://getsetdb-default-rtdb.europe-west1.firebasedatabase.app"
    private var data: HashMap<String, String> = HashMap<String, String>()
    val TAG = "Rejestracja"
    var id: String? = null

    private val nameLiveData = MutableLiveData<String>()
    private val passLiveData = MutableLiveData<String>()
    private val passSecLiveData = MutableLiveData<String>()

    private val isValidLiveData = MediatorLiveData<Boolean>().apply {
        this.value = false
        addSource(nameLiveData) { name ->
            val pass = passLiveData.value
            val passSec = passSecLiveData.value
            this.value = validateForm(name, pass, passSec)
        }

        addSource(passLiveData) { pass ->
            val name = nameLiveData.value
            val passSec = passSecLiveData.value
            this.value = validateForm(name, pass, passSec)
        }

        addSource(passSecLiveData) { passSec ->
            val name = nameLiveData.value
            val pass = passLiveData.value
            this.value = validateForm(name, pass, passSec)
        }
    }

    private fun validateForm(name: String?, pass: String?, passSec: String?): Boolean {
        val isValidName = name != null && name.isNotBlank()
        val isValidPass = pass != null && pass.isNotBlank() && pass.length >= 6
        val isValidPassSec = passSec != null && passSec.isNotBlank() && pass == passSec
        return isValidName && isValidPass && isValidPassSec
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameLayout = findViewById<TextInputLayout>(R.id.name)
        val passLayout = findViewById<TextInputLayout>(R.id.pass)
        val passSecLayout = findViewById<TextInputLayout>(R.id.passSec)
        val zarej = findViewById<Button>(R.id.rejestr)

        database = FirebaseDatabase.getInstance(ref).getReference("Users")

        nameLayout.editText?.doOnTextChanged { text, _, _, _ ->
            nameLiveData.value = text?.toString()
        }

        passLayout.editText?.doOnTextChanged { text, _, _, _ ->
            passLiveData.value = text?.toString()
        }

        passSecLayout.editText?.doOnTextChanged { text, _, _, _ ->
            passSecLiveData.value = text?.toString()
        }

        isValidLiveData.observe(this) { isValid ->
            zarej.isEnabled = isValid
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
        } else {
            val telephonyManager =
                Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
            Log.d("TEST", telephonyManager)
            id = telephonyManager.toString()
        }

        zarej.setOnClickListener {
            if (nameLiveData.value != null) {
                database.child(nameLiveData.value.toString()).get().addOnSuccessListener {
                    data = it.value as HashMap<String, String>
                    Log.d(TAG, data.toString())
                }
                if (data.isEmpty()) {
                    val user = User(0,nameLiveData.value.toString(), passLiveData.value.toString())
                    database.child(nameLiveData.value.toString()).setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Zapisano", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                        Toast.makeText(this, "Błąd zapisu", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Istnieje już taki użytkownik", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}