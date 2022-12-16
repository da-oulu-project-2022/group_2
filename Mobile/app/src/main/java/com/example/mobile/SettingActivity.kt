package com.example.mobile

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.DrawableCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.android.synthetic.main.activity_setting.*
import java.io.FileOutputStream
import java.util.*


class SettingActivity : AppCompatActivity() {
    companion object {  // ~ static JAVA
        private const val TAG = "SettingActivity"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
        lateinit var aManager:AudioManager
    }

    private var deviceId = ""
    private val fileName: String = "MyDevicesId.txt"

    private val api: PolarBleApi by lazy {
        // Notice PolarBleApi.ALL_FEATURES are enabled
        PolarBleApiDefaultImpl.defaultImplementation(applicationContext, PolarBleApi.ALL_FEATURES)
    }

    private var scanDisposable: Disposable? = null
    private var bluetoothEnabled = false

    private lateinit var scanButton: Button
    private lateinit var backNavigation: TextView
    private lateinit var foundSensors: TextView

    // ATTENTION! list devices ID of Polar at index 1st to n
    private val listDeviceId = mutableListOf<String>("select one device")

    private var numberOfSensors : Int = 0

    private lateinit var auth: FirebaseAuth

    //NOTIFICATION BUTTON
    val CHANNEL_ID = "channelID"
    val CHANNEL_NAME = "channelname"
    val NOTIFICATION_ID = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())

        scanButton = findViewById(R.id.scan_button)
        backNavigation = findViewById(R.id.setting_nav_bar)
        foundSensors = findViewById(R.id.found_sensors)
        val startTraining = findViewById<Button>(R.id.start_training_btn)


        foundSensors.visibility = TextView.INVISIBLE

        api.setPolarFilter(false)
        api.setApiLogger { s: String -> Log.d(API_LOGGER_TAG, s) }
        api.setApiCallback(object : PolarBleApiCallback() {
            // disable buttons when phone bluetooth off
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
                bluetoothEnabled = powered
                if (powered) {
                    enableAllButtons()
                    showToast("Phone Bluetooth on")
                } else {
                    disableAllButtons()
                    showToast("Phone Bluetooth off. Please, turn on bluetooth and sensor")
                }
            }

            // only logging
            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId)
            }

            // only logging
            override fun streamingFeaturesReady(
                identifier: String, features: Set<PolarBleApi.DeviceStreamingFeature>
            ) {
                for (feature in features) {
                    Log.d(TAG, "Streaming feature $feature is ready")
                }
            }

            // UUID logging
            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "uuid: $uuid value: $value")
            }

            /*
            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }
            */

            override fun polarFtpFeatureReady(s: String) {
                Log.d(TAG, "FTP ready")
            }
        })

        /*
         * spinner: display all devices of Polar that the mobile have just scan
         */
        val spinner: Spinner = findViewById(R.id.spinner)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, listDeviceId
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (listDeviceId[position] != "select one device") {
                    deviceId = listDeviceId[position]

                    try {
                        var fout: FileOutputStream = openFileOutput(fileName, MODE_PRIVATE)
                        fout.write("$deviceId".toByteArray())
                        fout.close()
                    } catch (ex: Exception) {
                        //Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                        Log.e("hihihaha", "Error: ${ex.message}")
                    }
                }
            }
        }

        /*
         * Scan button: find all devices of Polar
         */
        scanButton.setOnClickListener {
            val isDisposed = scanDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(scanButton, R.string.scanning_devices)
                scanDisposable = api.searchForDevice()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarDeviceInfo: PolarDeviceInfo ->
                            listDeviceId.add("${polarDeviceInfo.deviceId}")

                            numberOfSensors += 1
                            foundSensors.visibility = TextView.VISIBLE
                            foundSensors.text = getString(R.string.show_the_number_of_sensor, numberOfSensors.toString())

                            showToast("Founded your sensors")
                            // Log.d(TAG, "polar device found id: " + polarDeviceInfo.deviceId + " address: " + polarDeviceInfo.address + " rssi: " + polarDeviceInfo.rssi + " name: " + polarDeviceInfo.name + " isConnectable: " + polarDeviceInfo.isConnectable)
                        },
                        { error: Throwable ->
                            toggleButtonUp(scanButton, "Scan devices")
                            showToast("$error", "warning")
                            // Log.e(TAG, "Device scan failed. Reason $error")
                        },
                        {
                            toggleButtonUp(scanButton, "Scan devices")
                            scanDisposable?.dispose()
                            showToast("complete")
                            // Log.d(TAG, "complete")
                        }
                    )
            } else {
                toggleButtonUp(scanButton, "Scan devices")
                foundSensors.visibility = TextView.INVISIBLE
                numberOfSensors = 0
                scanDisposable?.dispose()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }

        /*
         * back button: navigation home screen
         */
        backNavigation.setOnClickListener {
            val homePage = Intent(this, HomeActivity::class.java)
            startActivity(homePage)
            finish()
        }

        /*
         * Navigation to Training Activity
         */
        startTraining.setOnClickListener {
            val homePage = Intent(this, TrainingActivity::class.java)
            startActivity(homePage)
            finish()
        }

        //
        //CREATE SOUND BUTTON
        //
        val soundBtn = findViewById<Switch>(R.id.sound_btn)
        val aManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        soundBtn.setOnCheckedChangeListener { Switch , isChecked ->
            if (isChecked) {
                // The switch is enabled/checked
                aManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Toast.makeText(this, "Sound Mode On", Toast.LENGTH_LONG).show()
            } else {
                // The switch is disabled
                aManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                Toast.makeText(this, "Vibrate Mode On", Toast.LENGTH_LONG).show()
            }

            }

        //
        //CREATE NOTIFICATION
        //
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Punch Results")
            .setContentText("This is your punch result")
            .setSmallIcon(R.drawable.ic_notifications)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = NotificationManagerCompat.from (this)

        notification_btn.setOnCheckedChangeListener { Switch , isChecked ->
            if (isChecked) {
                // The switch is enabled/checked
                notificationManager.notify(NOTIFICATION_ID, notification)
                Toast.makeText(this, "Notification Mode On", Toast.LENGTH_LONG).show()
            } else {
                // The switch is disabled
                Toast.makeText(this, "Notification Mode Off", Toast.LENGTH_LONG).show()
            }

        }


        //SIGN OUT BUTTON
        auth = Firebase.auth
        signOut_Tv.setOnClickListener{
            Log.e(signOut().toString(), "log out succeed")
            signOut()
            val homePage = Intent(this, SignInActivity::class.java)
            startActivity(homePage)
            finish()
        }


    }   // onCreate end

    private fun signOut() {
        // [START auth_sign_out]
        Firebase.auth.signOut()
        // [END auth_sign_out]
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        api.foregroundEntered()
    }

    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
    }

    private fun createNotificationChannel(){
        if (Build. VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            val channel = NotificationChannel (CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT).apply{
                    lightColor = Color.RED
                    enableLights(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun toggleButtonDown(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, true, getString(resourceId))
    }

    private fun toggleButtonUp(button: Button, text: String? = null) {
        toggleButton(button, false, text)
    }

    private fun toggleButton(button: Button, isDown: Boolean, text: String? = null) {
        if (text != null) button.text = text

        var buttonDrawable = button.background
        buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
        if (isDown) {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryDarkColor))
        } else {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryColor))
        }
        button.background = buttonDrawable
    }

    private fun showToast(message: String, status: String = "info") {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)

//         toast.setGravity(Gravity.CENTER, 0, 0)
//
//        val view = toast.view
//        // view!!.setBackgroundResource(android.R.drawable.alert_light_frame)
//        val text = view!!.findViewById<View>(android.R.id.message) as TextView
////        if (text != null)
////        {
////            toast.setGravity(Gravity.CENTER, 0, 0)
////
////        }
//        if (status != "info") {  // info or warning as Log function
//            text.setTextColor(Color.RED)
//        }
//        text.textSize = 20F
        toast.show()
    }

    private fun disableAllButtons() {
    }

    private fun enableAllButtons() {
    }






}