package eu.mobileApp.DriverApp.mapa

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import eu.mobileApp.DriverApp.MainScreen
import eu.mobileApp.DriverApp.R
import eu.mobileApp.DriverApp.comm.Order
import eu.mobileApp.DriverApp.comm.PostGetApi
import eu.mobileApp.DriverApp.databinding.ActivityMapScreenBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay

class MapScreen : AppCompatActivity() {
    private lateinit var binding: ActivityMapScreenBinding
    private val REQUEST_PERMISSION_REQUEST_CODE = 1
    private lateinit var map: MapView
    private var longitude=""
    private var latitude=""
    private lateinit var marker:Marker

    private var client = OkHttpClient()
    private var recogized: Boolean = true
    private var waitApi: Boolean = true

    private var cordsArrayPolygon= mutableListOf<Double>()
    private var cordsArrayLine= mutableListOf<Double>()
    private var arrSize=0
    private var geoPointsPolygon= ArrayList<GeoPoint>()
    private var geoPointsLine= ArrayList<GeoPoint>()

    private var crewID=""

    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        var mapPointsZA=ArrayList<GeoPoint>()
        var mapPointsWY=ArrayList<GeoPoint>()
        var mapPointsPAR=ArrayList<GeoPoint>()
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("geopoint").toString()
            Log.d("Geopoint_capture", message)
            //if (!message.isNullOrEmpty() && !message.contains("Places:[]")){
                try{
                    var mapJson=Gson().fromJson(message, Order::class.java)
                    //Log.d("Geopoint_capture_json", mapJson.toString())
                    val arrayMessage= message!!.split(":")
                    //Log.d("Geopoint_capture", arrayMessage.toString())
                    var i=0
                    var temp=""
                    var tempArray= mutableListOf<String>()
                    for (string in arrayMessage){
                        //Log.d("Geopoint_capture", string.toString())
                        if(string.contains("Coords")){
                            val a=arrayMessage[i+1].split("]")
                            for(points in a){
                                if(points.contains(",")){
                                    //Log.d("Geopoint_capture", a.toString())
                                    var j=0
                                    temp=points.replace("[", "")
                                    temp=temp.replace("]", "")
                                    temp=temp.replace("\"", "")
                                    temp=temp.replace("\\", "")
                                    tempArray= temp.split(",").toMutableList()
                                    Log.d("Geopoint_capture", tempArray.size.toString())
                                    if(tempArray.size%3!=0){
                                        if(tempArray.size%3==2){
                                            tempArray.drop(tempArray.size)
                                            tempArray.drop(tempArray.size)
                                        }
                                        if(tempArray.size%3==1){
                                            tempArray.drop(tempArray.size)
                                        }
                                    }
                                    for (s in tempArray){
                                        tempArray.set(j,s.replace(" ",""))
                                        //Log.d("Geopoint_capture", s.toString())
                                        j++
                                    }
                                    for (s in tempArray){
                                        if(s.isNotEmpty())
                                            if(s.contains(".") || s.contains("0")) {
                                                cordsArrayLine.add(s.toDouble())
                                            }
                                    }
                                    //Log.d("Geopoint_capture_line", cordsArrayLine.toString())

                                    //Log.d("Geopoint_capture", tempArray.toString())
                                    i=0
                                    while(i<cordsArrayLine.size-2){
                                        val geoPoint=GeoPoint(cordsArrayLine[i], cordsArrayLine[i+1])
                                        if(geoPointsLine.isEmpty()){
                                            geoPointsLine.add(geoPoint)
                                        }
                                        var j=0
                                        var find=true
                                        while(j <geoPointsLine.size){
                                            if(geoPointsLine[j]==geoPoint) {
                                                find=false
                                            }
                                            //Log.d("GeoPoint_geopoint", geoPoint.toString())
                                            j++
                                        }
                                        if (find){
                                            geoPointsLine.add(geoPoint)
                                            //Log.d("GeoPoint_add", "added")
                                        }
                                        i+=3
                                    }
                                    //Log.d("Geopoint_capture_add", geoPointsLine.toString())
                                    myPoint(cordsArrayLine[0], cordsArrayLine[1])
                                    drawLine()
                                }
                            }
                        }
                        i++
                    }

                    if(!mapJson.Places.isNullOrEmpty()){
                        for(place in mapJson.Places){
                            //Log.d("Geopoint_capture_places", place.toString())
                            if(!place.ShapeDescription.isNullOrEmpty()){
                                //Log.d("Geopoint_capture_places", place.GeoLat.toString())
                                //Log.d("Geopoint_capture_places", place.GeoLon.toString())
                                //Log.d("Geopoint_capture_places", place.Type.toString())

                                if(place.Type=="ZA"){
                                    mapPointsZA.add(GeoPoint(place.GeoLat.toDouble(),place.GeoLon.toDouble()))
                                }else if(place.Type=="WY"){
                                    mapPointsWY.add(GeoPoint(place.GeoLat.toDouble(),place.GeoLon.toDouble()))
                                }else if(place.Type=="PAR"){
                                    mapPointsPAR.add(GeoPoint(place.GeoLat.toDouble(),place.GeoLon.toDouble()))
                                }
                            }
                        }
                        Log.d("Geopoint_capture_places", mapPointsZA.toString())
                        placeMarkerZA(mapPointsZA)
                        Log.d("Geopoint_capture_places", mapPointsWY.toString())
                        placeMarkerWY(mapPointsWY)
                        Log.d("Geopoint_capture_places", mapPointsPAR.toString())
                        placeMarkerPAR(mapPointsPAR)
                    }
                } catch (e:Exception){
                    Log.d("Geopoin_capture_error", e.toString())
                }
            //}
        }
    }

    private val crewIDReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("crewID")
            //Log.d("crew_id_mapScreen", message.toString())
            crewID=message.toString()
            try {
                getMapPointsJson(crewID)
            }catch (e:Error){
                Log.d("getData", e.toString())
            }
        }
    }

    private val LocationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try{
                latitude = intent.getStringExtra("Latitude").toString()
                longitude = intent.getStringExtra("Longitude").toString()
                //myPoint(latitude.toDouble(), longitude.toDouble())
                myPoint(cordsArrayLine[0], cordsArrayLine[1])
            }catch (e:Exception){
                Log.d("MapScreen",e.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMapScreenBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_map_screen)
        val viewBind = binding.root
        setContentView(viewBind)

        var aid=""
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
        }

        sendMessage("started")
        try {
            getMapPointsJson(crewID)
        }catch (e:Error){
            Log.d("LoginError", e.toString())
        }

        var temp= intent.getStringExtra("longlat").toString().split(":")
        //Log.d("longlat", temp.toString())
        try{
            longitude=temp[0]
            latitude=temp[1]
            //Log.d("longlat", longitude)
            //Log.d("longlat", latitude)
        }catch (e:Exception){
            Log.d("MapScreen", e.toString())
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),REQUEST_PERMISSION_REQUEST_CODE)
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),REQUEST_PERMISSION_REQUEST_CODE)
        }

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        map = binding.map
        marker=Marker(map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val mapController = map.controller
        mapController.setZoom(9.0)
        try {
            if(latitude!="" || longitude!="") {
                val startPoint = GeoPoint(latitude.toDouble(), longitude.toDouble());
                mapController.setCenter(startPoint);
            }else{
                val startPoint = GeoPoint(53.1657294, 17.7012986);
                mapController.setCenter(startPoint);
            }
        }catch (e:Exception){
            Log.d("MapScreen", e.toString())
        }
        map.setMultiTouchControls(true)

        val dm : DisplayMetrics = this.resources.displayMetrics
        val scaleBarOverlay = ScaleBarOverlay(map)
        scaleBarOverlay.setCentred(true)
        scaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10)
        map.overlays.add(scaleBarOverlay)

        //drawPolygon()
        //drawLine()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            LocationReceiver, IntentFilter("GPSLocationChanged")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            messageReceiver, IntentFilter("geopoint")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            crewIDReceiver, IntentFilter("crewID")
        )
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


    override fun onDestroy() {
        super.onDestroy()
        Log.d("MapScreen", "closed")
    }

    private fun drawLine(){
        try{
            val line=Polyline()
            line.setPoints(geoPointsLine)
            //Log.d("Geopoint_capture_add", geoPointsLine.toString())
            //geoPointsLine= ArrayList<GeoPoint>()
            line.outlinePaint.color=Color.parseColor("#f00202")
            map.overlays.add(line);
            map.invalidate()
        }catch (e:Exception){
            Log.d("MapScreen_Line", e.toString())
        }
    }

    private fun drawPolygon(){
        try{
            val polygon=Polygon()
            var geoPointsPolygonTemp=geoPointsPolygon
            geoPointsPolygonTemp.add(geoPointsPolygon.get(0))
            //Log.d("geopoint_capture", geoPointsPolygonTemp.toString())
            polygon.fillPaint.color= Color.parseColor("#1EFFE70E")
            //polygon.fillPaint.color= Color.parseColor("#6ef082")
            polygon.setPoints(geoPointsPolygonTemp)
            geoPointsPolygon= ArrayList<GeoPoint>()
            geoPointsPolygonTemp=ArrayList<GeoPoint>()
            map.overlays.add(polygon);
            map.invalidate()
        }catch (e:Exception){
            Log.d("MapScreen_Polygon", e.toString())
        }
    }

    private fun placeMarkerZA(arrayList: ArrayList<GeoPoint>){
        try{
            for(point in arrayList){
                Log.d("Geopoint_capture_places", point.toString())
                val markerPoint = Marker(map)
                markerPoint.position = point
                markerPoint.icon = ContextCompat.getDrawable(this, R.drawable.baseline_start_24)
                markerPoint.title = "ZA"
                markerPoint.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                map.overlays.add(markerPoint)
                map.invalidate()
            }
        }catch (e:Exception){
            Log.d("MapScreen_markersZA", e.toString())
        }
    }

    private fun placeMarkerWY(arrayList: ArrayList<GeoPoint>){
        try{
            for(point in arrayList){
                Log.d("Geopoint_capture_places", point.toString())
                val markerPoint = Marker(map)
                markerPoint.position = point
                markerPoint.icon = ContextCompat.getDrawable(this, R.drawable.baseline_home_24)
                markerPoint.title = "WY"
                markerPoint.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                map.overlays.add(markerPoint)
                map.invalidate()
            }
        }catch (e:Exception){
            Log.d("MapScreen_markersWY", e.toString())
        }
    }

    private fun placeMarkerPAR(arrayList: ArrayList<GeoPoint>){
        try{
            for(point in arrayList){
                Log.d("Geopoint_capture_places", point.toString())
                val markerPoint = Marker(map)
                markerPoint.position = point
                markerPoint.icon = ContextCompat.getDrawable(this, R.drawable.baseline_local_parking_24)
                markerPoint.title = "PA"
                markerPoint.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                map.overlays.add(markerPoint)
                map.invalidate()
            }
        }catch (e:Exception){
            Log.d("MapScreen_markersPAR", e.toString())
        }
    }


    private fun myPoint(latitude: Double, longitude:Double){
        try{
            map.overlays.remove(marker)
            marker.position = GeoPoint(latitude,longitude)
            centerPosition(latitude, longitude)
            marker.icon = ContextCompat.getDrawable(this, R.drawable.baseline_arrow_drop_down_24)
            marker.title = "Twoja pozycja"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            map.overlays.add(marker)
            map.invalidate()
        }catch (e:Exception){
            Log.d("MapScreen", e.toString())
        }
    }

    private fun centerPosition(latitude: Double, longitude:Double){
        try {
            map.controller.setCenter(GeoPoint(latitude, longitude))
            map.invalidate()
        }catch (e:Exception){
            Log.d("MapScreen", e.toString())
        }
    }

    private fun sendMessage(msg: String) {
        val intent = Intent("mapScreenInfo")
        intent.putExtra("map", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    private fun getMapPointsJson(crewID: String){
        GlobalScope.launch(Dispatchers.Default) {
            try{
                //Log.d("GetMap", "test")
                val postLocation =
                    PostGetApi(client, recogized, waitApi)
                //val orderID=postLocation.getOrder(id)
                val json=postLocation.getOrder(crewID)
                //Log.d("GetMap", json.toString())
            }catch (e:Exception){
                Log.d("GetMapPoints_Error", e.toString())
            }
        }
    }
}