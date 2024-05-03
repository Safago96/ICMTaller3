package com.fandino.taller3

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fandino.taller3.databinding.ActivityMapBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import java.io.InputStream
import androidx.appcompat.widget.Toolbar

class Map : AppCompatActivity() {

    companion object{
        const val PATH_USERS="users/"
    }

    private lateinit var bindingMap: ActivityMapBinding


    private val database = FirebaseDatabase.getInstance()
    private lateinit var dbReference: DatabaseReference
    private lateinit var authentication: FirebaseAuth

    private lateinit var auth: FirebaseAuth
    private var location: Location? = null
    private lateinit var changeService: Availability


    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private val startPoint = org.osmdroid.util.GeoPoint(4.62850, -74.06471)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindingMap = ActivityMapBinding.inflate(layoutInflater)
        setContentView(bindingMap.root)
        authentication = Firebase.auth
        auth = authentication
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        dbReference = database.getReference(PATH_USERS + auth.currentUser!!.uid)
        dbReference.child("available").setValue(true)

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName
        bindingMap.osmMap.setTileSource(TileSourceFactory.MAPNIK)
        bindingMap.osmMap.setMultiTouchControls(true)
        changeService = Availability(this)
        changeService.startListening()

        permissions()
        updateMarker()


        bindingMap.exitBtn.setOnClickListener {
            authentication.signOut()
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return when (item.itemId)
        {
            R.id.menuLogOut ->
            {
                dbReference.child("available").setValue(false)
                auth.signOut()
                val intentLogOut = Intent(this, Login::class.java)
                intentLogOut.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                changeService.stopListening()
                startActivity(intentLogOut)
                finish()

                true
            }
            R.id.menuToggleStatus ->
            {
                //myRef = database.getReference(PATH_USERS + auth.currentUser!!.uid)
                dbReference.child("available").get().addOnSuccessListener { availableSnapshot ->
                    val isAvailable = availableSnapshot.getValue(Boolean::class.java) ?: false
                    dbReference.child("available").setValue(!isAvailable)
                    val statusText = if (!isAvailable) "available" else "not available"
                    Toast.makeText(this, "Now you're $statusText", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menuAvailableUsers ->
            {
                val intentAvailableUsers = Intent(this, ActiveUsers::class.java)
                changeService.stopListening()
                startActivity(intentAvailableUsers)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun permissions(){
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                Permissions.LOCATION_PERMISSION_CODE)
        } else {
            currentLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            Permissions.LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    currentLocation()
                }else{
                    Toast.makeText(this, "Permission not granted by user", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun currentLocation() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 10f, locationListener)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("MapsActivity", "Location updated: $location")
            latitude = location.latitude
            longitude = location.longitude
            Log.i("LISTENER", "Latitude: $latitude, Longitude: $longitude")
            actualizarMarcadorUbiActual(latitude, longitude)
            startPoint.latitude = latitude
            startPoint.longitude = longitude
            updateUserLocation(latitude, longitude)
        }
    }

    private var marker: Marker? = null
    private fun actualizarMarcadorUbiActual(latitude: Double, longitude: Double) {
        if (marker != null){
            marker?.let { bindingMap.osmMap.overlays.remove(it)}
        }

        val geopoint = GeoPoint(latitude, longitude)
        marker = Marker(bindingMap.osmMap).apply{
            icon = changeIconSize(resources.getDrawable(R.drawable.location))
            position = geopoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        bindingMap.osmMap.overlays.add(marker)

        val mapController: IMapController = bindingMap.osmMap.controller
        mapController.animateTo(geopoint)
        mapController.setZoom(18.0)
    }

    private fun updateMarker(){
        val JSONdata = JSONObject(loadJSONFromAsset())
        val locationData = JSONdata.getJSONObject("locations")

        for( i in locationData.keys()){
            val data1 = locationData.getJSONObject(i)
            val JSONLatitude = data1.getDouble("latitude")
            val JSONLongitude = data1.getDouble("longitude")
            val JSONname = data1.getString("name")

            val geopoint1 = GeoPoint(JSONLatitude, JSONLongitude)
            val JSONMarker = Marker(bindingMap.osmMap).apply {
                icon = changeIconSize(resources.getDrawable(R.drawable.marker))
                position = geopoint1
                title = JSONname
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            bindingMap.osmMap.overlays.add(JSONMarker)
        }
    }

    private fun changeIconSize(icon: Drawable): Drawable {
        val bitmap = (icon as BitmapDrawable).bitmap
        val newBM = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
        return BitmapDrawable(resources, newBM)
    }

    private fun updateUserLocation(latitude: Double, longitude: Double){
        val uId = authentication.currentUser?.uid
        if (uId != null){
            val currentUser = database.getReference(PATH_USERS + uId)
            currentUser.child("latitude").setValue(latitude)
            currentUser.child("longitude").setValue(longitude)
        }
    }

    private fun loadJSONFromAsset(): String? {
        var json: String? = null
        try{
            val istream: InputStream = assets.open( "locations.json")
            val size: Int = istream.available()
            val buffer = ByteArray(size)
            istream.read(buffer)
            istream.close()
            json = String(buffer, Charsets. UTF_8)
        }catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    override fun onResume() {
        super.onResume()
        bindingMap.osmMap.onResume()
        val mapController: IMapController = bindingMap.osmMap.controller
        mapController.setZoom(15.0)
        mapController.setCenter(this.startPoint)
    }

    override fun onPause() {
        super.onPause()
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
        bindingMap.osmMap.onPause()
    }
}