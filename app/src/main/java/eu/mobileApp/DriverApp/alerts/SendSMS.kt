package eu.mobileApp.DriverApp.alerts

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

class SendSMS : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    fun sendSMS(number: String, text: String) {
        val sms = SmsManager.getDefault()
        try {
            sms.sendTextMessage(number, "ME", text, null, null)
        } catch (e:Exception){
            Log.d("SMS_class", e.toString())
        }
    }
}