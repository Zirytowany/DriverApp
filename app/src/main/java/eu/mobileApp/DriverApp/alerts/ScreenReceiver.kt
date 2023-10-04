package eu.mobileApp.DriverApp.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ScreenReceiver : BroadcastReceiver() {
    var counter = 0
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            Log.d("SCREEN", "OFF")
            sendMessageToActivity(context, "OFF")
        } else if (intent.action == Intent.ACTION_SCREEN_ON) {
            Log.d("SCREEN", "ON")
            sendMessageToActivity(context, "ON")
            counter++
            if (counter % 3 == 0) {
                Log.d("SCREEN", "count to $counter")
                sendMessageToActivity(context, "ekran")
            }
        }
    }

    private fun sendMessageToActivity(context: Context, msg: String) {
        val intent = Intent("screenStatus")
        intent.putExtra("screen", msg)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}