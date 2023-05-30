package com.example.accidentdetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.*
import android.telephony.SmsManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.accidentdetection.AlertSystem.Distance
import com.example.accidentdetection.Firebase.ProfileDetailsActivity
import com.example.accidentdetection.LocationAndMaps.MapsFragment
import com.example.accidentdetection.LocationAndMaps.PermissionUtils
import com.example.accidentdetection.LocationAndMaps.Vals
import com.example.accidentdetection.SMS.sendSms
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.*

@Suppress("DEPRECATION", "DeferredResultUnused")
class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager : SensorManager
    private lateinit var xval : TextView
    private lateinit var yval : TextView
    private lateinit var zval : TextView
    private lateinit var xgval : TextView
    private lateinit var ygval : TextView
    private lateinit var zgval : TextView
    var lastUpdatedLocation : String=""
    var lastUpdatedLat : Double =0.0
    var lastUpdatedLong : Double=0.0

    private var ameter : Sensor? = null
    private var gmeter : Sensor? = null

    lateinit var countDownTimer: CountDownTimer
    internal val initialCountDown : Long = 20000
    internal val countDownInterval : Long = 1000
//    internal var countOn : Boolean = false
    internal var countOn : Boolean = true
    private var counting = 0
    lateinit var dialogLayout :View
    internal var isDialogBoxLaunched : Boolean = false

    lateinit var locationRes : Location



    private lateinit var permissionLauncher : ActivityResultLauncher<Array<String>>
    private var isSendSmsPermissionGranted = false



    companion object{
        const val LOCATION_PERMISSION_REQUEST_CODE=24
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //set up status bar color

        if (Build.VERSION.SDK_INT >= 21) {
            val window = this.window
            window.addFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = this.resources.getColor(R.color.ms_black_txt)
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        xval = findViewById(R.id.x_value)
        yval = findViewById(R.id.y_value)
        zval=findViewById(R.id.z_value)

        xgval=findViewById(R.id.x_g_value)
        ygval=findViewById(R.id.y_g_value)
        zgval=findViewById(R.id.z_g_value)

        //start sensor activity
        setUpSensor()
        //load google maps location
        loadMap()


        //load user details
        loadUserDetails()

        //profile pic
        val pp : ImageView =  findViewById(R.id.profile_image)
        pp.setOnClickListener {
            val intent =Intent(this,ProfileDetailsActivity::class.java)
            startActivity(intent)
        }

        //SMS permission
        sendSms().checkSmsPermission(this)

        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permissions ->
            isSendSmsPermissionGranted=permissions[Manifest.permission.SEND_SMS]?: isSendSmsPermissionGranted
        }
        requestSmsPermission()
    }


    private fun requestSmsPermission(){
        isSendSmsPermissionGranted = ContextCompat.checkSelfPermission(this,Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val permissionRequest: MutableList<String>  = ArrayList()
        if(!isSendSmsPermissionGranted){
            permissionRequest.add(Manifest.permission.SEND_SMS)
        }

        if(permissionRequest.isNotEmpty()){
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
    }

    //Sensor Related methods
    private fun setUpSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        ameter = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).also {
            sensorManager.registerListener(this,it,SensorManager.SENSOR_DELAY_UI,SensorManager.SENSOR_DELAY_UI)
        }

        gmeter = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).also {
            sensorManager.registerListener(this,it,SensorManager.SENSOR_DELAY_UI)
        }

    }

    override fun onSensorChanged(event: SensorEvent?) {

        var count=0
        if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            val x=event.values[0]
            val y=event.values[1]
            val z = event.values[2]

            xval.text=x.toString()
            yval.text=y.toString()
            zval.text=z.toString()

            val xD = x.toDouble()
            val yD = y.toDouble()
            val zD = z.toDouble()

//            if(x.toInt() > 2 && count == 0){
//                count=2
//                stopListening()
//
//            }

            if(checkThreshold(xD,yD,zD)){
                stopListening()
            }
        }

        else if(event?.sensor?.type == Sensor.TYPE_GYROSCOPE){
            val x=event.values[0]
            val y=event.values[1]
            val z = event.values[2]

            xgval.text=x.toString()
            ygval.text=y.toString()
            zgval.text=z.toString()
        }
    }

    override fun onAccuracyChanged(event: Sensor?, p1: Int) {
    }

    private fun checkThreshold(x : Double,y:Double,z:Double) : Boolean{
        val ax2 = Math.pow(x,2.0)
        val ay2 = Math.pow(y,2.0)
        val az2 = Math.pow(z,2.0)
        val aSum = ax2+ay2+az2
        val acc = Math.sqrt(aSum)
        val GVAL = acc / 9.806
        val progressBar : ProgressBar = findViewById(R.id.acc_progress_bar)
        progressBar.progress= (GVAL*25).toInt()
        val accValue : TextView = findViewById(R.id.acc_value)
        accValue.text= (GVAL*25).toInt().toString()

        if(GVAL > 4){
            Toast.makeText(this,"$GVAL",Toast.LENGTH_LONG).show()
            return true
        }
        return false
    }

    //Code For launching Dialog Box
    private fun stopListening(){
        if(!isDialogBoxLaunched){
            sensorManager.unregisterListener(this,ameter)
            sensorManager.unregisterListener(this,gmeter)
            launchDialogBox()
        }
    }

    private fun launchDialogBox(){
        isDialogBoxLaunched=true

        dialogLayout = LayoutInflater.from(this).inflate(R.layout.custom_dialog,null)
        val mBuilder = AlertDialog.Builder(this)
                .setView(dialogLayout)
        val mAlertDialog = mBuilder.show()
        mAlertDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        timerStart()

        dialogLayout.findViewById<Button>(R.id.nohelpBtn).setOnClickListener {

            setUpSensor()
            isDialogBoxLaunched=false

            mAlertDialog.dismiss()
            countDownTimer.cancel()
        }
        dialogLayout.findViewById<Button>(R.id.helpBtn).setOnClickListener {

            sendSms().sendSms(this,lastUpdatedLocation,lastUpdatedLat,lastUpdatedLong)
            countOn=false
            mAlertDialog.dismiss()
            fireBaseNumber()
        }

    }
    fun fireBaseNumber() {
        // Get the current user from Firebase Authentication
        val firebaseAuth = FirebaseAuth.getInstance()
        val fireStore = FirebaseFirestore.getInstance()
        val uid = firebaseAuth.uid
        if (uid != null) {
            fireStore.collection("User").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val phoneNumberFB = document.getString("EMC")
                        val message = "Hello, this is a test SMS."
                        val smsManager = SmsManager.getDefault()
                        val mapLink =
                            ("https://www.google.com/maps/search/?api=1&query=" + lastUpdatedLat.toString()
                                    + "," + lastUpdatedLong.toString())
                        smsManager.sendTextMessage(
                            phoneNumberFB, null, mapLink.toString(),
                            null, null
                        )
                    } else {
                        Log.d("FIRESTORE", "No such document")
                    }
                }
        }
    }



    //Timer-Class
    private fun timerStart(){
        val initialTimeLeft = initialCountDown / 1000
//        val tt  = dialogLayout.findViewById<TextView>(R.id.timeTv)
//        tt.text=initialTimeLeft.toString()

        val tt  = dialogLayout.findViewById<TextView>(R.id.secondsTv)
        tt.text=initialTimeLeft.toString()

//        countOn=true

        countDownTimer= object : CountDownTimer(initialCountDown,countDownInterval){
            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = millisUntilFinished/1000
                tt.text=timeLeft.toString()
                counting= timeLeft.toInt()
            }

            override fun onFinish() {
                if(countOn){
                    sendSms().sendSms(applicationContext,lastUpdatedLocation,lastUpdatedLat,lastUpdatedLong)
                }
                countDownTimer.cancel()
//                countOn=false
                countOn=true
            }

        }

        countDownTimer.start()

    }






    //Code - Maps

    private fun loadMap(){
        //check Location Permission
        when {
            PermissionUtils.isAccessFineLocationGranted(this) -> {
                when {
                    PermissionUtils.isLocationEnabled(this) -> {
//                        setUpFragment()
                        setUpLocationListener()


                    }
                    else -> {
                        PermissionUtils.showGPSNotEnabledDialog(this)
                    }
                }
            }
            else -> {
                PermissionUtils.requestAccessFineLocationPermission(
                    this,
                    24
                )
            }
        }
    }

    private fun setUpFragment() {
        val fragment : Fragment = MapsFragment()
        val transaction : FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.myMapView,fragment)
        transaction.commit()
    }

    private fun setUpLocationListener() {

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        //gets location every 4 minutes
        val locationRequest = LocationRequest().setInterval(2000).setFastestInterval(2000).
        setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        setUpFragment()

        val handlerThread = HandlerThread("LocationCallbackThread")
        handlerThread.start()
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {

                //NEW
                var previousLocation: Location? = null

                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)

                    for (location in locationResult.locations) {
                        val lat : TextView = findViewById(R.id.lat_value)
                        val long : TextView = findViewById(R.id.long_value)
                        lastUpdatedLocation =getCity(location.latitude,location.longitude).toString()
                        val loc : TextView = findViewById(R.id.loc_value)
                        val speedMeter : TextView = findViewById(R.id.speed_value)
                        val speedProgressBar :  ProgressBar = findViewById(R.id.speed_progress_bar)

                        Vals.lati=location.latitude
                        Vals.longi=location.longitude



                        //calculate speed (Km/H)
                        locationRes=locationResult.lastLocation
                        var speedToInt = locationRes.speed  //original speed
                        var speedHrit = Math.round((speedToInt*3.6)).toDouble().toInt()

                        if(locationRes != null){
                            if(locationRes.hasSpeed()){
                                speedToInt = locationRes.speed
                                speedHrit= Math.round((speedToInt*3.6)).toDouble().toInt()
                            }
                        }


                        runOnUiThread {
                            lat.text = location.latitude.toString()
                            long.text = location.longitude.toString()
                            loc.text= lastUpdatedLocation.toString()
                            alertSystem(Vals.lati,Vals.longi)
                            speedMeter.text= speedHrit.toString()
                            speedProgressBar.progress=(speedHrit*0.55).toInt()
                        }

                        lastUpdatedLat=location.latitude
                        lastUpdatedLong=location.longitude


                        locationRes=locationResult.lastLocation



                    }

                }
            },
            handlerThread.looper
        )


    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            setUpLocationListener()

                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Permission Not granted",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun getCity(lat: Double,long:Double) : String{
        var cityName=""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var address = geoCoder.getFromLocation(lat,long,3)
        var area = address[0].getAddressLine(0).toString()
        var city = address[0].locality
        var state = address[0].adminArea
        var postalCode = address[0].postalCode
        var knownName = address[0].featureName

        cityName = area + ","+city+","+state+","+postalCode+","+knownName
        return cityName
    }

    private fun alertSystem(lat: Double,long : Double){
        val d  : Distance= Distance.getInstance()
        val check : Boolean= d.caldistance(lat,long)
        Handler().postDelayed({
            if(check == true){
                val my = Toast(this)
                my.apply {
                    val layout : View = LinearLayout.inflate(applicationContext,
                        R.layout.custom_toast_accident_alert,null)
                    duration=Toast.LENGTH_SHORT
                    setGravity(Gravity.TOP,0,0)
                    view=layout
                }.show()
            }
        }, 3000)

    }


    private fun loadUserDetails(){
        val auth = FirebaseAuth.getInstance()
        val nameTxt : TextView = findViewById(R.id.usernameTV)
        val usrProfilePic : ImageView = findViewById(R.id.profile_image)

        //cloud firestore
        val firestore = FirebaseFirestore.getInstance()
        val ref=firestore.collection("User").document(auth.uid.toString())
        ref.get()
            .addOnSuccessListener {document->
                if(document != null){
                    val usr = document.get("Name")
                    nameTxt.text= usr.toString()
                }
                else {
                    Log.d("FIRESTORE", "No such document")
                }
            }

        //storage
        val localFile = File.createTempFile("tempImage","jpg")
        FirebaseStorage.getInstance().getReference("profilePicUploads/${auth.currentUser!!.uid}/${auth.currentUser!!.uid}.jpg")
            .getFile(localFile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                usrProfilePic.setImageBitmap(bitmap)
            }
    }

}