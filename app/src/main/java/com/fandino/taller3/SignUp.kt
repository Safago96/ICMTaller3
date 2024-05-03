package com.fandino.taller3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.fandino.taller3.databinding.ActivitySignupBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import android.content.ContentValues.TAG
import android.text.TextUtils
import androidx.core.net.toUri
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage

class SignUp : AppCompatActivity() {

    companion object{
        const val PATH_USERS="users/"
    }
    private lateinit var bindingSignUp: ActivitySignupBinding
    
    private lateinit var authentication: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    
    private lateinit var activityResultLauncherCamara: ActivityResultLauncher<Intent>
    private lateinit var LaunchGalleryResult: ActivityResultLauncher<Intent>
    
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    
    private val database = FirebaseDatabase.getInstance()
    private lateinit var dbReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        bindingSignUp = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(bindingSignUp.root)
        authentication = Firebase.auth
        storage = Firebase.storage

        SaveImage(null)
        
        val galleryBtn = bindingSignUp.gallery
        val profileImg = bindingSignUp.loadedPic
        val signUpBtn = bindingSignUp.signUpBtn
        val email = bindingSignUp.emailInput
        val password = bindingSignUp.passwordInput
        val cameraBtn = bindingSignUp.camera


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        currentLocation()
        LocationPermission()

        EnableCameraLauncherResult(profileImg)
        EnableGalleryLauncherResult(profileImg)

        cameraBtn.setOnClickListener {
            PermissionCamera()
        }

        galleryBtn.setOnClickListener {
            PermissionGallery()
        }

        signUpBtn.setOnClickListener {
            verifyExistingEmail(email.text.toString()){ emailStatus ->
                if (validation() && !emailStatus){
                    signUpAuthentication(email.text.toString(), password.text.toString())
                }
            }
        }
    }

    private fun verifyExistingEmail(correo: String, onComplete: (Boolean) -> Unit) {
        authentication.fetchSignInMethodsForEmail(correo)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods?.isEmpty() == true) {
                        Toast.makeText(this, "Email is not registered, please continue", Toast.LENGTH_SHORT).show()
                        onComplete(false)
                    } else {
                        Toast.makeText(this, "Email is already registered, please login", Toast.LENGTH_SHORT).show()
                        onComplete(true)
                    }
                } else {
                    Toast.makeText(this, "Error verifying email", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
    }

    private fun validation(): Boolean{
        var valid = true

        val uriImg = getUriImg()
        if(uriImg == null){
            Toast.makeText(this, "Image required", Toast.LENGTH_SHORT).show()
            valid = false
        }

        val name = bindingSignUp.nameInput.text.toString()
        if(TextUtils.isEmpty(name)){
            bindingSignUp.nameInput.error = "Required"
            valid = false
        }else{
            bindingSignUp.nameInput.error = null
        }

        val lastName = bindingSignUp.lastNameInput.text.toString()
        if(TextUtils.isEmpty(lastName)){
            bindingSignUp.lastNameInput.error = "Required"
            valid = false
        }else{
            bindingSignUp.lastNameInput.error = null
        }

        val email = bindingSignUp.emailInput.text.toString()
        if(TextUtils.isEmpty(email)){
            bindingSignUp.emailInput.error = "Required"
            valid = false
        }else{
            bindingSignUp.emailInput.error = null
        }

        val password = bindingSignUp.passwordInput.text.toString()
        if(TextUtils.isEmpty(password)){
            bindingSignUp.passwordInput.error = "Required"
            valid = false
        }else{
            bindingSignUp.passwordInput.error = null
        }

        val id = bindingSignUp.idInput.text.toString()
        if(TextUtils.isEmpty(id)){
            bindingSignUp.idInput.error = "Required"
            valid = false
        }else{
            bindingSignUp.idInput.error = null
        }

        return valid
    }


    private fun PermissionCamera(){
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                Permissions.CAMERA_PERMISSION_CODE
            )
        } else {
            launchCamera()
        }
    }

    private fun PermissionGallery(){
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                Permissions.GALLERY_PERMISSION_CODE
            )
        } else {
            launchGallery()
        }
    }

    private fun LocationPermission(){
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                 startLocationUpdates()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                    requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    Permissions.LOCATION_PERMISSION_CODE
                )
            }
            else -> {
                    requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    Permissions.LOCATION_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            Permissions.CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchCamera()
                } else {
                    Toast.makeText(this, "Permission not granted by user", Toast.LENGTH_SHORT).show()
                }
            }
            Permissions.GALLERY_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchGallery()
                } else {
                    Toast.makeText(this, "Permission not granted by user", Toast.LENGTH_SHORT).show()
                }
            }
            Permissions.LOCATION_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Location Enabled", Toast.LENGTH_SHORT).show()
                    startLocationUpdates()
                } else {
                    Toast.makeText(this, "Permission not granted by user", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun launchCamera(){
        val intentCamara = Intent("android.media.action.IMAGE_CAPTURE")
        activityResultLauncherCamara.launch(intentCamara)
    }

    private fun EnableCameraLauncherResult(pic: ImageView){
        activityResultLauncherCamara = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                val bmImg = result.data?.extras?.get("data") as? Bitmap
                if(bmImg != null){
                    val URIimg = MediaStore.Images.Media.insertImage(
                        contentResolver,
                        bmImg,
                        "Image",
                        "Profile Pic"
                    )
                    Glide.with(this).load(bmImg).into(pic)
                    SaveImage(URIimg.toString())
                    getUriImg()
                }
            }else{
                Toast.makeText(this, "Image could not be loaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchGallery(){
        val intentGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        LaunchGalleryResult.launch(intentGallery)
    }

    private fun EnableGalleryLauncherResult(pic: ImageView){
        LaunchGalleryResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                val uri = result.data?.data
                Glide.with(this).load(uri).into(pic)
                SaveImage(uri.toString())
                getUriImg()
            }else{
                Toast.makeText(this, "Image could not be loaded", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun SaveImage(uri: String?) {
        val sharedPreferences = getSharedPreferences("image-preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("uri_pic", uri)
        editor.apply()
    }

    private fun getUriImg(): String? {
        val sharedPreferences = getSharedPreferences("image-preferences", MODE_PRIVATE)
        Log.i("URI", sharedPreferences.getString("uri_pic", null).toString())
        return sharedPreferences.getString("uri_pic", null)
    }


    private fun currentLocation(){
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                Log.i("LOCATION", "Update")
                if(location!=null) {
                    bindingSignUp.Longitude.text = location.longitude.toString()
                    bindingSignUp.Latitude.text = location.latitude.toString()
                }else {
                    Log.i("LOCATION", "Null Update")
                }
            }
        }
    }

    private fun startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }

    private fun createLocationRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

    private fun signUpAuthentication(email: String, password: String){
        authentication.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this){task ->
                if(task.isSuccessful){
                    Log.d(TAG, "Create User Email: onComplete: " + task.isSuccessful)
                    val user = authentication.currentUser
                    if(user != null){
                        val updateUser = UserProfileChangeRequest.Builder()
                        updateUser.setDisplayName(email)
                        user.updateProfile(updateUser.build())
                        updateUI(user)
                        registerUserRTDB()
                        registerUserFBStorage()
                    }
                }else{
                    Toast.makeText(this, "Failed SignUp", Toast.LENGTH_SHORT).show()
                    task?.exception?.message?.let { Log.w(TAG, it)}
                }
            }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if(currentUser != null){
            val startIntent = Intent(this, Map::class.java)
            startIntent.putExtra("User", currentUser.email)
            startActivity(startIntent)
        }
    }


    private fun registerUserRTDB(){
        val name = bindingSignUp.nameInput
        val lastName = bindingSignUp.lastNameInput
        val id = bindingSignUp.idInput
        val latitude = bindingSignUp.Latitude
        val longitude = bindingSignUp.Longitude

        val registerUser = User()
        registerUser.name = name.text.toString()
        registerUser.lastName = lastName.text.toString()
        registerUser.id = id.text.toString().toLong()
        registerUser.latitude = latitude.text.toString().toDouble()
        registerUser.longitude = longitude.text.toString().toDouble()
        registerUser.available = true
        dbReference = database.getReference(PATH_USERS+authentication.currentUser!!.uid)
        dbReference.setValue(registerUser)
    }


    private fun registerUserFBStorage(){
        val uriImage = getUriImg()
        val RefImage = storage.reference.child("images/${authentication.currentUser!!.uid}")
        RefImage.putFile(uriImage!!.toUri())
            .addOnSuccessListener(object: OnSuccessListener<UploadTask.TaskSnapshot>{
                override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot){
                    Log.i("STORAGE", "Image loaded Succesfully")
                }
            })
            .addOnFailureListener(object: OnFailureListener {
                override fun onFailure(exception: Exception){

                }
            })
    }


    override fun onPause() {
        super.onPause()
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        }
    }
}