package eu.mobileApp.DriverApp.comm

import android.util.Log
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class PostGetApi(
    private var client: OkHttpClient,
    private var recognized: Boolean,
    private var wait: Boolean,
    private var logged: Boolean = false
) {
    private var device_token = ""
    private var jsonJWT = JSONObject()
    private var json = JSONObject()
    private var token_csrf = ""
    private var sURL = "url_1"
    private var newURL = "url_2"

    /*
    private var client = OkHttpClient()
    private var recogized: Boolean = true
    private lateinit var token_csrf: String
    private lateinit var device_token: String
    private var wait: Boolean = true
*/
    fun postLocation(
        latitude: String,
        longitude: String,
        dateLocal: String,
        dateUTC: String,
        status: String,
        id: String
    ): String {

        var dtURL = "url_endpoint"
        val urlPOST = dtURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        val formBody =
            FormBody.Builder().add("device_token", id).build()

        val requestPost =
            Request.Builder().url(urlPOST).header("token", "token_value").post(formBody).build()

        client.newCall(requestPost).execute().use { response ->
           /* device_token = response.body!!.string()
            val cod = response.code.toString()
            Log.d("POST_device_token", device_token)
            Log.d("POST_device_token_code", cod)
        }

        client.newCall(requestSign).execute().use { response ->
*/
            for ((name, value) in response.headers) {
                Log.d("HEADERS_RESPONSE_MOBILE_UUID", "$name: $value")
            }
            device_token = response.body!!.string().toString()
            token_csrf=device_token
            //Log.d("POST_UUID", device_token.toString())
            val cod = response.code.toString()
            Log.d("POST_ID_code", cod)
            recognized = true

            if (device_token == "This device doesn't exists in the databse.") {
                recognized = false
                return ("No_device")
            }

            if (recognized) {
                newURL="url_2"
                if (status == "alert") {
                    val urlPOST =
                        //sURL.toHttpUrlOrNull()?.newBuilder()?.addQueryParameter("ID", device_token)?.build().toString()
                        newURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
                    val sb = StringBuilder()
                    sb.append("token_value")
                    val string = sb.toString()
                    var formBody =
                        FormBody.Builder()
                            .add("AndroidID", id)
                            .add("AlarmDate", dateLocal)
                            .add("AlarmUTCDate", dateUTC)
                            .add("Latitude", latitude)
                            .add("Longitude", longitude).build()
                    if (logged) {
                        formBody =
                            FormBody.Builder()
                                .add("AndroidID", id)
                                .add("AlarmDate", dateLocal)
                                .add("AlarmUTCDate", dateUTC)
                                .add("Latitude", latitude)
                                .add("Longitude", longitude)
                                .add("jwt", jsonJWT.get("jwt").toString())
                                .add("ID", Base64.getEncoder().encodeToString(id.toByteArray()))
                                .build()
                    }

                    val requestPost =
                        Request.Builder().url(urlPOST).post(formBody).header("token", string).build()

                    client.newCall(requestPost).execute().use { response ->
                        val odp = response.body!!.string()
                        val cod = response.code.toString()
                        for ((name, value) in response.headers) {
                            Log.d("POST_alert_HEADERS_RESPONSE", "$name: $value")
                        }
                        Log.d("POST_alert", odp)
                        Log.d("POST_alert_code", cod)
                        if (odp == "Alarm has been saved successfuly." || cod.contains("20")) {
                            if (cod.contains("20")) {
                                wait = true
                                return ("AlertSaved")
                            } else if (cod == "404" && odp == "This device doesn't exists in the database!") {
                                return ("Bad_id")
                            }
                        } else if (odp == "Device location updated failed.") {
                            return ("DB_error")
                        } else if (odp == "There is no 'Latitude' value in the form!") {
                            wait = false
                        } else {
                            return ("Unknown_error")
                        }
                    }

                } else {
                    newURL="url_2"
                    val urlPOST =
                        newURL.toHttpUrlOrNull()?.newBuilder()
                            ?.build().toString()
                    val sb = StringBuilder()
                    sb.append("token_value")
                    val string = sb.toString()
                    var formBody =
                        FormBody.Builder()
                            .add("MobileDeviceID", device_token)
                            .add("AndroidID", id)
                            .add("Latitude", latitude)
                            .add("Longitude", longitude)
                            .add("DateUTC", dateUTC)
                            .add("DateLocal", dateLocal)
                            .add("Status", status)
                            .build()
                    if (logged) {
                        FormBody.Builder()
                            .add("MobileDeviceID", device_token)
                            .add("AndroidID", id)
                            .add("Latitude", latitude)
                            .add("Longitude", longitude)
                            .add("DateUTC", dateUTC)
                            .add("DateLocal", dateLocal)
                            .add("Status", status)
                            .add("jwt", jsonJWT.get("jwt").toString())
                            .add("ID", Base64.getEncoder().encodeToString(id.toByteArray()))
                            .build()
                    }

                    val requestPost =
                        Request.Builder().url(urlPOST).header("token", string).put(formBody)
                            .build()

                    try {
                        client.newCall(requestPost).execute().use { response ->
                            val odp = response.body!!.string()
                            val cod = response.code.toString()
                            Log.d("PUT_location_response", odp)
                            Log.d("Put_location_code", cod)
                            if (odp.contains("Device location has been saved.") || cod.contains("20")) {
                                if (cod.contains("20")) {
                                    wait = true
                                    return ("Saved")
                                } else if (cod == "404" && odp == "This device doesn't exists in the database!") {
                                    return ("Bad_id")
                                }
                            } else if (odp == "Device location updated failed.") {
                                return ("DB_error")
                            } else if (odp == "There is no 'Latitude' value in the form!") {
                                wait = false
                            } else {
                                return ("Unknown_error")
                            }
                        }
                    } catch (unlikely: Exception) {
                        Log.d("Test ", "Coś poszło nie tak: $unlikely")
                    }
                }
            }
        }
        return ("end")
    }

    private fun getDateTimeUTC1(): String {
        val utc1 = OffsetDateTime.now()
        return utc1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString()
    }

    private fun getDateTimeUTC(): String {
        val current = OffsetDateTime.now(ZoneOffset.UTC)
        return current.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString()
    }

    fun getStringConfig(id: String): String {
        var config = ""
        try {
            sURL="url"+id
            val urlGET =
                sURL.toHttpUrlOrNull()?.newBuilder()
                    //?.addQueryParameter("show_device_config", "1")
                    //?.addQueryParameter("AndroidID", id)
                    ?.build().toString()
            var request =
                Request.Builder().url(urlGET).header("token", "token_value")
                    .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("Config_GET_ERROR", "Unexpected config code $response")
                }

                for ((name, value) in response.headers) {
                    Log.d("Config_HEADERS_RESPONSE", "$name: $value")
                }
                config = response.body!!.string()
                try {
                    config = config.substring(1, config.length - 1)
                    val response_json = Gson().fromJson(config, Config::class.java)
                } catch (e: Exception) {
                    Log.d("Config_test_json: ", e.toString())
                }

            }
        } catch (e: Exception) {
            Log.d("Config_:Error", e.toString())
        }
        return config
    }

    fun getStringConfigtest(id: String): String {
        var config = ""
        try {
            val urlGET =
                newURL.toHttpUrlOrNull()?.newBuilder()?.addQueryParameter("device_config", id)
                    ?.addQueryParameter("AndroidID", id)
                    ?.build().toString()
            var request =
                Request.Builder().url(urlGET).header("token", "token_value").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("GET_ERROR", "Unexpected config code $response")
                }

                for ((name, value) in response.headers) {
                    Log.d("HEADERS_RESPONSE", "$name: $value")
                }
                config = response.body!!.string()
                Log.d("Config_test: ", config)
                try {
                    config = config.substring(1, config.length - 1)
                    val response_json = Gson().fromJson(config, Config::class.java)
                    Log.d("Config_test_json: ", response_json.SOSPhoneNumber)
                } catch (e: Exception) {
                    Log.d("Config_test_json: ", e.toString())
                }

            }
        } catch (e: Exception) {
            Log.d("Error", e.toString())
        }
        return config
    }

    fun getJsonConfig(id: String): Config {
        var config = ""
        sURL="url"+id
        val urlGET =
            sURL.toHttpUrlOrNull()?.newBuilder()
                //?.addQueryParameter("show_device_config", "1")
                //?.addQueryParameter("AndroidUUID", id)
                ?.build().toString()
        var request =
            Request.Builder().url(urlGET).header("token", "token_value")
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.d("GET_ERROR", "Unexpected config code $response")
            }

            for ((name, value) in response.headers) {
                Log.d("HEADERS_RESPONSE", "$name: $value")
            }
            config = response.body!!.string()
            //Log.d("Config_test: ", config)
            config = config.substring(1, config.length - 1)
            val response_json = Gson().fromJson(config, Config::class.java)
            Log.d("Config_test_json: ", response_json.SOSPhoneNumber)
            return response_json
        }
    }

    /*fun sendParameters(array: String, id: String) {
        //var string=""
        val urlPOST = sURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        val sb = StringBuilder()
        sb.append("token_value;").append(token_csrf)
        val str = sb.toString()
        println("datetime: ${getDateTimeUTC1()}")
        val formBody =
            FormBody.Builder()
                .add("store_device_swiping_log", "1")
                .add("ID", id)
                .add("LogDate", getDateTimeUTC1())
                .add("LogData", array).build()

        val requestPost =
            Request.Builder().url(urlPOST).header("token", str).post(formBody).build()

        client.newCall(requestPost).execute().use { response ->
            val odp = response.body!!.string()
            val cod = response.code.toString()
            Log.d("POST_data", odp)
            Log.d("POST_datacode", cod)
        }
    }*/

    /*fun getOrderID():String{
        val urlGET =
            sURL.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("show_orders_for_driver", "1")
                ?.build().toString()
        val request =
            Request.Builder().url(urlGET).header("token", "token_value").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.d("GET_ERROR", "Unexpected order code $response")
            }

            for ((name, value) in response.headers) {
                Log.d("HEADERS_RESPONSE", "$name: $value")
            }
            val data = response.body!!.string()
            Log.d("Order_API_data: ", data)
            val code = response.code
            Log.d("Order_API_code: ", code.toString())
            return data
        }
    }*/

    fun getOrder(id:String): String {
        val urlGET =
            newURL+"/order/"+id.toHttpUrlOrNull()?.newBuilder()
                //?.addQueryParameter("show_orders_for_driver", "1")
                ?.build().toString()
        val request =
            Request.Builder().url(urlGET).header("token", "token_value").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.d("GET_ERROR", "Unexpected order code $response")
            }

            for ((name, value) in response.headers) {
                Log.d("HEADERS_RESPONSE", "$name: $value")
            }

            var data = response.body!!.string()

            Log.d("Order_API_data: ", data)
            val code = response.code
            Log.d("Order_API_code: ", code.toString())
            return data
        }
    }
/*
    fun getMap(orderID: String): String {
        val urlGET =
            sURL.toHttpUrlOrNull()?.newBuilder()?.addQueryParameter("order_details", "1")
                ?.addQueryParameter("OrderID", orderID)
                ?.build().toString()
        val request =
            Request.Builder().url(urlGET).header("token", "token_value").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.d("GET_ERROR", "Unexpected map code $response")
            }

            for ((name, value) in response.headers) {
                Log.d("HEADERS_RESPONSE", "$name: $value")
            }
            var rest = response.body!!.string()
            var data = rest.toString()
            Log.d("Data_API: ", data)
            return data
        }
    }*/
/*
    fun getMapJson(orderID: String): Order {
        val urlGET =
            sURL.toHttpUrlOrNull()?.newBuilder()?.addQueryParameter("order_details", "1")
                ?.addQueryParameter("OrderID", orderID)
                ?.build().toString()
        val request =
            Request.Builder().url(urlGET).header("token", "token_value").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.d("GET_ERROR", "Unexpected map code $response")
            }

            for ((name, value) in response.headers) {
                Log.d("HEADERS_RESPONSE", "$name: $value")
            }
            var rest = response.body!!.string()
            var data = rest.toString()
            Log.d("Data_API_: ", data)
            try{
                Log.d("Data_API_: ", data1)
                val test = Gson().fromJson(data1, Order::class.java)
                Log.d("Data_API_json: ", test.toString())
            }catch (e: Exception){
                Log.d("Data_API_json: ", e.toString())
            }

            val test = Gson().fromJson(data, Order::class.java)
            return test
        }
    }*/

    fun passLogin(login: String, pass: String, id: String) {
        var loginURL = "url_value"
        val urlPOST = loginURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        val sb = StringBuilder()
        sb.append("token_value")
        val str = sb.toString()
        println("datetime: ${getDateTimeUTC1()}")
        val formBody =
            FormBody.Builder()
                .add("Userlogin", login)
                .add("Password", pass)
                .add("AndroidID", id).build()

        val requestPost =
            Request.Builder().url(urlPOST).header("token", str).post(formBody).build()

        client.newCall(requestPost).execute().use { response ->
            val odp = response.body!!.string()
            json = JSONObject(odp)
            jsonJWT = json.get("JWT") as JSONObject
            val cod = response.code.toString()
            //Log.d("POST_data_login", odp)
            //Log.d("POST_data_login", jsonJWT.toString())
            //Log.d("POST_data_login", json.get("Cookie").toString())
            Log.d("POST_datacode_login", cod)
        }
    }

    fun getAbsence(UID: String):String{
        var loginURL = "url_string"+UID
        val urlTest = loginURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        val requestPost =
            Request.Builder().url(urlTest).header("token", "token_value").build()
        var absence=""
        client.newCall(requestPost).execute().use { response ->
            val odp = response.body!!.string()
            val cod = response.code.toString()
            //Log.d("time_Absence_data", odp)
            Log.d("time_Absence_datacode", cod)
            absence=odp
        }
        return absence
    }

    fun putAbsence(absenceID:String, ID:String, comment:String){
        var loginURL = "url_string"
        val urlTest = loginURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        var status=0
        if(comment!=""){
            status=-2
        }
        else{
            status=2
        }

        val formBody =
            FormBody.Builder()
                .add("AbsenceID", absenceID)
                .add("CrewID", ID)
                .add("Status", status.toString())
                .add("Comment", comment)
                .build()

        val requestPost =
            Request.Builder().url(urlTest).header("token", "token_value").put(formBody).build()

        client.newCall(requestPost).execute().use { response ->
            val odp = response.body!!.string()
            val cod = response.code.toString()
            Log.d("put_pendingAbsence", odp)
            Log.d("put_pendingAbsence", cod)
            //Log.d("time_Absence_put_template", "AbsenceUID: $absenceUID CrewUID: $crewUID" + " Status: ${status.toString()} Comment: $comment")
        }
    }

    fun getDeparture(crewID:String):String{
        var loginURL = "url_string"+crewID
        val urlTest = loginURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        val requestPost =
            Request.Builder().url(urlTest).header("token", "token_value").build()
        var departure=""
        Log.d("get_departure_crewID", crewID)
        client.newCall(requestPost).execute().use { response ->
            val odp = response.body!!.string()
            val cod = response.code.toString()
            Log.d("get_departure_data", odp)
            Log.d("get_departure_datacode", cod)
            departure=odp
        }
        return departure
    }

    fun putDeparture(crewID: String,
                     roadCardID: String,
                     arrivalDT: String, departureDT:String){
        var loginURL = "url_string"
        val urlTest = loginURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()

        val formBody =
            FormBody.Builder()
                .add("CrewUID", crewID)
                .add("RoadCardUID", roadCardID)
                .add("DriverArrivalDt", arrivalDT)
                .add("DriverDepartureDt", departureDT)
                .build()

        val requestPost =
            Request.Builder().url(urlTest).header("token", "token_value").put(formBody).build()

        client.newCall(requestPost).execute().use { response ->
            val odp = response.body!!.string()
            val cod = response.code.toString()
            Log.d("time_Departure_put_data", odp)
            Log.d("time_Departure_put_datacode", cod)
            //Log.d("time_Departure_put_template", requestPost.toString())
        }
    }

    fun getPendingAbsence(ID: String):String{
        var loginURL = "url_string"+ID
        val urlTest = loginURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        var absence=""
        val requestPost =
            Request.Builder().url(urlTest).header("token", "token_value").build()

        client.newCall(requestPost).execute().use { response ->
            val odp = response.body!!.string()
            val cod = response.code.toString()
            absence=odp
           // Log.d("time_PendingAbsence_data", odp)
            Log.d("time_PendingAbsence_datacode", cod)
        }
        return absence
    }

    fun postDeviceToken(aid:String):String{
        var odp=""
        var dtURL = "url_string"
        val urlPOST = dtURL.toHttpUrlOrNull()?.newBuilder()?.build().toString()
        val formBody =
            FormBody.Builder().add("device_token", aid).build()

        val requestPost =
            Request.Builder().url(urlPOST).header("token", "token_value").post(formBody).build()

        client.newCall(requestPost).execute().use { response ->
            odp = response.body!!.string()
            val cod = response.code.toString()
            //Log.d("POST_device_token", odp)
            Log.d("POST_device_token_code", cod)
        }

        return odp
    }

}