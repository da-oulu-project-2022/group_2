package com.example.mobile

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Pair
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import com.androidplot.xy.*
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import java.io.*
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class TrainingActivity : AppCompatActivity() {
    companion object {  // ~ static JAVA
        private const val TAG = "TrainingActivity"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private var deviceId = ""
    private val fileName: String = "MyDevicesId.txt"

    private val api: PolarBleApi by lazy {
        // Notice PolarBleApi.ALL_FEATURES are enabled
        PolarBleApiDefaultImpl.defaultImplementation(applicationContext, PolarBleApi.ALL_FEATURES)
    }

    private var movementDisposable: Disposable? = null
    private var sdkModeEnableDisposable: Disposable? = null
    private var deviceConnected = false
    private var bluetoothEnabled = false

    private val SAMPLE_RATE = 52
    private val RANGE = 16
    private val MINIMUM_SPEED = 12.0
    // Buttons
    private lateinit var connectButton: Button
    private lateinit var movementButton: Button
    private lateinit var endTrainingButton: Button
    //private lateinit var textViewAccX: TextView
    private lateinit var textViewBattery: TextView
    private lateinit var imageViewBatteryLevel: ImageView
    //private lateinit var textViewPunchResult: TextView
    private lateinit var textViewSpeed: TextView
    private lateinit var backNavigation: TextView

    //private lateinit var textViewHr: TextView
    private lateinit var roundTimes: TextView

    private var sessionsInfoFileName: String = "session_count.txt"
    //private var sessionCountFile: File? = null
    private var sessionCount = 0

    private var roundNumber = 0
    private var dataReceived = false
    private var fos: FileOutputStream? = null
    private var sessionFileName: String? = null
    private var sessionFile: File? = null
    private var sessionOut: FileOutputStream? = null

    //private var sampleRate = 26 // Handling raw data in file - in Hz
    //private var range = 8;
    private var punchAnalyzer: PunchAnalyzer = PunchAnalyzer(SAMPLE_RATE, RANGE)
    private var punchID = 1
    private var punches : ArrayList<Pair<Double,Boolean>> = ArrayList()
    private val firebaseHandler = FirebaseHandler()

    // Latency
    //private val timeResponse = 7000
    private lateinit var textViewCountdown1: TextView
    private lateinit var textViewCountdown2: TextView

    private var heartRate: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())


        connectButton = findViewById(R.id.connect_button)
        movementButton = findViewById(R.id.movement_button)
        endTrainingButton = findViewById(R.id.end_training_button)
        //textViewAccX = findViewById(R.id.view_acc_X)
        textViewBattery = findViewById(R.id.view_battery)
        imageViewBatteryLevel = findViewById(R.id.ic_battery_level)
        //textViewPunchResult = findViewById(R.id.view_punch_result)
        textViewSpeed = findViewById(R.id.view_speed)
        //textViewHr = findViewById(R.id.view_hr)
        backNavigation = findViewById(R.id.training_nav_bar)
        textViewCountdown1 = findViewById(R.id.view_countdown_1)
        textViewCountdown2 = findViewById(R.id.view_countdown_2)
        roundTimes = findViewById(R.id.view_round)
        roundTimes.text = "(Round times)"




        // file, outputstream for acc data storage
        Log.d(TAG, "path: " + filesDir.absolutePath)
        /*
         * get deviceId from MyDevicesId.txt
         */
        try {
            var fin: FileInputStream? = null
            fin = openFileInput(fileName)
            var inputStreamReader: InputStreamReader = InputStreamReader(fin)
            val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder: StringBuilder = StringBuilder()
            var text: String? = null
            while (run {
                    text = bufferedReader.readLine()
                    text
                } != null) {
                stringBuilder.append(text)
                //text?.let { listDeviceId.add(it) }
                text?.let { deviceId = it }
            }
        } catch (ex: Exception) {
            Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
        }

//        /*
//         * decide whether endTrainingButton is visible or invisible
//         */
//        var noNeedAccount: Boolean = false
//
//        try {
//            var fin: FileInputStream? = null
//            fin = openFileInput("DoINeedAccount.txt")
//            var inputStreamReader: InputStreamReader = InputStreamReader(fin)
//            val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)
//
//            val stringBuilder: StringBuilder = StringBuilder()
//            var text: String? = null
//            while (run {
//                    text = bufferedReader.readLine()
//                    text
//                } != null) {
//                stringBuilder.append(text)
//                text?.let {
//                    noNeedAccount = it.contains("no")
//                }
//            }
//        } catch (ex: Exception) {
//            if (ex.message?.contains("No such file or directory") == true) {
//                noNeedAccount = false
//            }
//        }
//
//        if (noNeedAccount) {
//            endTrainingButton.visibility = Button.INVISIBLE
//        }

        // graph
        var plot: XYPlot = findViewById(R.id.view_plot)
        val seriesSpeedFormat = BarFormatter(Color.BLUE, Color.GRAY)
        //val seriesHrFormat = BarFormatter(Color.RED, Color.GRAY)

        //val tVals = mutableListOf(0)
        val speedVals = mutableListOf(0)
        //val hrVals = mutableListOf(0)

        /*
         * All ble devices discoverable by searchForDevice, api logging enabled
         * bluetooth connectivity, mainly api callback functions
         */
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
                    showToast("Phone Bluetooth off")
                }
            }

            // get id from connected device, toggle connect button
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: " + polarDeviceInfo.deviceId)
                deviceId = polarDeviceInfo.deviceId
                deviceConnected = true


                val buttonText = getString(R.string.disconnect_from_device, deviceId)
                toggleButtonDown(connectButton, buttonText)

            }

            // only logging
            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId)
            }

            // toggle connect button
            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId)
                deviceConnected = false
                val buttonText = getString(R.string.connect_to_device, deviceId)
                toggleButtonUp(connectButton, buttonText)
            }

            // only logging
            override fun streamingFeaturesReady(
                identifier: String, features: Set<PolarBleApi.DeviceStreamingFeature>
            ) {
                for (feature in features) {
                    Log.d(TAG, "Streaming feature $feature is ready")
                }
                sdkModeEnableDisposable = api.enableSDKMode(deviceId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            Log.d(TAG, "SDK mode enabled")
                            // at this point dispose all existing streams. SDK mode enable command
                            // stops all the streams but client is not informed. This is workaround
                            // for the bug.
                            //movementDisposable?.dispose()
                        },
                        { error ->
                            val errorString = "SDK mode enable failed: $error"
                            showToast(errorString)
                            Log.e(TAG, errorString)
                        }
                    )
            }

            // UUID logging
            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "uuid: $uuid value: $value")
            }

            // update battery textView
            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "Battery level $identifier $level%")
                val batteryLevelText = "$level%"

                imageViewBatteryLevel.visibility = ImageView.VISIBLE
                textViewBattery.visibility = TextView.VISIBLE

                textViewBattery.text = batteryLevelText
                if (level == 100) {
                    //textViewBattery.setPadding(14,0,0,0)
                    
                    textViewBattery.setTextColor(Color.GREEN)
                    imageViewBatteryLevel.setColorFilter(Color.GREEN)
                } else if (level > 60) {
                    textViewBattery.setTextColor(Color.GREEN)
                    imageViewBatteryLevel.setColorFilter(Color.GREEN)
                } else if (level > 30) {
                    textViewBattery.setTextColor(Color.YELLOW)
                    imageViewBatteryLevel.setColorFilter(Color.YELLOW)
                } else if (level >= 10) {
                    textViewBattery.setTextColor(Color.RED)
                    imageViewBatteryLevel.setColorFilter(Color.RED)
                    showToast("Sensor is low battery")
                } else {
                    textViewBattery.setPadding(22,0,0,0)
                }
            }

            // update hr data
            /*
            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                Log.d(TAG, "HR -----------------------" + data.hr)
                heartRate = data.hr
            }
            */

            // works only with oh10 anyways
            override fun polarFtpFeatureReady(s: String) {
                Log.d(TAG, "FTP ready")
            }
        })

        /*
         * Connect button
         * no connection -> connect
         * already connected -> disconnect
         */
        connectButton.text = getString(R.string.connect_to_device, deviceId)
        connectButton.setOnClickListener {
            try {
                if (deviceConnected) {
                    api.disconnectFromDevice(deviceId)

                    imageViewBatteryLevel.visibility = ImageView.INVISIBLE
                    textViewBattery.visibility = TextView.INVISIBLE
                } else {
                    api.connectToDevice(deviceId)

                }
            } catch (polarInvalidArgument: PolarInvalidArgument) {
                val attempt = if (deviceConnected) {
                    "disconnect"
                } else {
                    "connect"
                }
                Log.e(TAG, "Failed to $attempt. Reason $polarInvalidArgument ")
            }
        }

        /*
         * Acc stream button
         * Start stream, send Data to mainThread
         * toggle button
         */
        movementButton.setOnClickListener {
            val player = MediaPlayer.create(this, Settings.System.DEFAULT_NOTIFICATION_URI)

            val settingsMap : Map<PolarSensorSetting.SettingType, Int> = mapOf(
                PolarSensorSetting.SettingType.SAMPLE_RATE to SAMPLE_RATE,
                PolarSensorSetting.SettingType.RANGE to RANGE,
                PolarSensorSetting.SettingType.RESOLUTION to 16,
                PolarSensorSetting.SettingType.CHANNELS to 3)
            val streamSettings = PolarSensorSetting(settingsMap)

            val isDisposed = movementDisposable?.isDisposed ?: true
            if (isDisposed) {
                //textViewPunchResult.visibility = TextView.INVISIBLE
                textViewSpeed.visibility = TextView.INVISIBLE

                ++roundNumber
                var roundStartLine = "round," + roundNumber + "\n"
                sessionOut!!.write(roundStartLine.toByteArray())
                toggleButtonDown(movementButton, R.string.stop_movement_stream)

                roundTimes.text = getString(R.string.round_times, roundNumber.toString())

                showCountdown(textViewCountdown1, textViewCountdown2)

                Thread {
                    movementDisposable =
                        /*requestStreamSettings(deviceId, PolarBleApi.DeviceStreamingFeature.ACC)
                            .flatMap { settings: PolarSensorSetting ->
                                api.startAccStreaming(deviceId, settings)
                            }*/
                            api.startAccStreaming(deviceId, streamSettings).observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                { polarAccelerometerData: PolarAccelerometerData ->

                                    for (data in polarAccelerometerData.samples) {

                                        //Log.d(TAG, "ACC    x: ${data.x} y:  ${data.y} z: ${data.z}")
                                        var result: Pair<Double, Boolean> = punchAnalyzer.nextFrame(data.y, data.x, data.z)
                                        if(result.first > MINIMUM_SPEED) {
                                            Log.e(TAG,"Calculated punch velocity: " + result.first + "km/h")
                                            Log.e(TAG, "Calculated punch velocity: $punchID")

                                            //textViewHr.text = getString(R.string.hr, heartRate.toString())

                                            player.start()

                                            textViewSpeed.visibility = TextView.VISIBLE

                                            punchID= punchID + 1

                                            val df = DecimalFormat("#.#")
                                            textViewSpeed.text = getString(R.string.speed, df.format(result.first).toString())
                                            var punchString = "punch," + result.first.toString() + "," + result.second.toString() +"\n"
                                            sessionOut!!.write(punchString.toByteArray())
                                            dataReceived = true

                                            punches.add(result)

                                            // Graph
                                            speedVals.add(result.first.toInt())
                                            //hrVals.add(heartRate)

                                            plot.clear()
                                            val seriesSpeed: XYSeries = SimpleXYSeries(speedVals, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "speed - km/h")
                                            //val seriesHr: XYSeries = SimpleXYSeries(hrVals, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,"heart rate - BPM")
                                            plot.addSeries(seriesSpeed, seriesSpeedFormat)
                                            //plot.addSeries(seriesHr, seriesHrFormat)

                                            plot.setRangeStep(StepMode.INCREMENT_BY_VAL, 5.0)
                                            plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 1.0)
//                                            plot.setRangeBoundaries(0,120,BoundaryMode.FIXED)
//                                            plot.setDomainBoundaries(0,30,BoundaryMode.FIXED)

                                            plot.redraw()
                                        }
                                    }
                                },
                                { error: Throwable ->
                                    toggleButtonUp(movementButton, R.string.start_movement_stream)
                                    Log.e(TAG, "ACC stream failed. Reason $error")
                                },
                                {
                                    showToast("ACC stream complete")
                                    Log.d(TAG, "ACC stream complete")
                                }
                            )
                }.start()
            } else {
                toggleButtonUp(movementButton, R.string.start_movement_stream)
                // NOTE dispose will stop streaming if it is "running"
                movementDisposable?.dispose()
                if(dataReceived) {
                    Log.e("PUNCHES",punches.toString())
                    var totalPunches = punches.size
                    var correctPunches = 0
                    var avgSpeed = 0.0
                    for(i in punches){
                        println(i)
                        if(i.second)
                            ++correctPunches
                        avgSpeed += i.first
                    }
                    avgSpeed /= totalPunches
                    var avgHeartRate = 0.0
                    var incorrectPunches = totalPunches - correctPunches
                    var roundLength = 1.0
                    var roundEndLine = "round_info," + roundNumber + "," + roundLength + "," + totalPunches + "," + correctPunches + "," + incorrectPunches + "," + avgHeartRate + "," + avgSpeed + "\n"

                    sessionOut!!.write(roundEndLine.toByteArray())
                    punchID = 1
                    punches  = ArrayList()

                }
            }
        }

        // end training button
        endTrainingButton.setOnClickListener {
            val nextPage = Intent(this, StatisticActivity::class.java)
            startActivity(nextPage)
        }

        // permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }

        // navigation
        backNavigation.setOnClickListener {
            val homePage = Intent(this, HomeActivity::class.java)
            startActivity(homePage)
            finish()
        }

    }   // onCreate end


    override fun onStart() {
        var sessionInfoFile: File = File(filesDir.absolutePath, sessionsInfoFileName)
        val sessionInfoCreated :Boolean = sessionInfoFile.createNewFile()
        var currentSessionID = 0
        if (sessionInfoCreated) {
            Log.d(TAG, "No sessions yet")
            sessionCount = 1
            currentSessionID = sessionCount
            ++sessionCount
            var outString = sessionCount.toString()
            var countOutStream = FileOutputStream(sessionInfoFile)
            countOutStream.write(outString.toByteArray())
        }

        else {
            var countInStream = openFileInput(sessionsInfoFileName)
            var inputStreamReader = InputStreamReader(countInStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            try {
                sessionCount = Integer.parseInt(bufferedReader.readLine())
                currentSessionID = sessionCount
                ++sessionCount
                Log.d(TAG, "Session number: " + sessionCount)
                var countOutStream = FileOutputStream(sessionInfoFile)
                countOutStream.write(sessionCount.toString().toByteArray())
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

         if( firebaseHandler.getCurrentUser()?.email == null ) {
             sessionFileName=  "session_" + currentSessionID + ".txt"
        } else {
             sessionFileName= "database1.txt"

        }

        Log.e("FILENAME",sessionFileName.toString())

       // sessionFileName = "session_" + currentSessionID + ".txt"    // create file for current session
        sessionFile = File(filesDir.absolutePath, sessionFileName)
        sessionOut = FileOutputStream(sessionFile)

        Log.d(TAG, "Creating SessionFile")
        var sessionLine = "training," + currentSessionID + "," + Date() + "\n"
        sessionOut!!.write(sessionLine.toByteArray())


        super.onStart()
    }

    /*
     * Handling raw data
     */
    private fun readDataFile(fileName: String?, punchAnalyzer: PunchAnalyzer) {
        try {
            var fin: FileInputStream? = null
            fin = openFileInput(fileName)
            var inputStreamReader: InputStreamReader = InputStreamReader(fin)
            val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)

            val stringBuilder: StringBuilder = StringBuilder()
            var text: String? = null
            while (run {
                    text = bufferedReader.readLine()
                    text
                } != null) {
                stringBuilder.append(text)
                text?.let {
                    val values = it.split(",").toTypedArray()
                    val x = java.lang.Float.valueOf(values[0])
                    val y = java.lang.Float.valueOf(values[1]) // currently not used
                    val z = java.lang.Float.valueOf(values[2])
                    //punchAnalyzer.nextFrame(x, y, z)
                }
            }
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /*
     * check if settings for feature are available
     */
    private fun requestStreamSettings(identifier: String, feature: PolarBleApi.DeviceStreamingFeature): Flowable<PolarSensorSetting> {
        val availableSettings = api.requestStreamSettings(identifier, feature)
        val allSettings = api.requestFullStreamSettings(identifier, feature)
            .onErrorReturn { error: Throwable ->
                Log.w(TAG, "Full stream settings are not available for feature $feature. REASON: $error")
                PolarSensorSetting(emptyMap())
            }
        // Single -> reactive pattern(similar to observable in observer patter)
        return Single.zip(availableSettings, allSettings) { available: PolarSensorSetting, all: PolarSensorSetting ->
            if (available.settings.isEmpty()) {
                throw Throwable("Settings are not available")
            } else {
                Log.d(TAG, "Feature " + feature + " available settings " + available.settings)

                Log.d(TAG, "Feature " + feature + " all settings " + all.settings)

                return@zip android.util.Pair(available, all)
            }
        }
            .observeOn(AndroidSchedulers.mainThread())      // send available settings to mainThread, create settings dialog
            .toFlowable()
            .flatMap(
                Function { sensorSettings: android.util.Pair<PolarSensorSetting, PolarSensorSetting> ->
                    DialogUtility.showAllSettingsDialog(
                        this@TrainingActivity,
                        sensorSettings.first.settings,
                        sensorSettings.second.settings
                    ).toFlowable()
                } as io.reactivex.rxjava3.functions.Function<Pair<PolarSensorSetting, PolarSensorSetting>, Flowable<PolarSensorSetting>>
            )
    }

    /*
     * Activity lifecycle
     */
    public override fun onPause() {
        super.onPause()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public override fun onStop() {
        if(dataReceived) {
            var currentSession = sessionCount - 1
//            var sessionLine = "session_info," + currentSession +"\n"
//            sessionOut!!.write(sessionLine.toByteArray())
            var dataReader= DataReader()

            dataReader.DataHandler()
        }
        else{
            var sessionInfoFile: File = File(filesDir.absolutePath, sessionsInfoFileName)
            var countOutStream = FileOutputStream(sessionInfoFile)
            if(sessionCount > 1) {
                --sessionCount
            }
            countOutStream.write(sessionCount.toString().toByteArray())
        }
        super.onStop()
    }
    public override fun onResume() {
        super.onResume()
        api.foregroundEntered()
    }

    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
    }

    /*
     * Toggle button text
     */
    private fun toggleButtonDown(button: Button, text: String? = null) {
        toggleButton(button, true, text)
    }

    private fun toggleButtonDown(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, true, getString(resourceId))
    }

    private fun toggleButtonUp(button: Button, text: String? = null) {
        toggleButton(button, false, text)
    }

    private fun toggleButtonUp(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, false, getString(resourceId))
    }

    // set button color on toggle
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

    private fun showCountdown(view1: TextView, view2: TextView){
        Thread {
            var timeResponseSecs = 10000/1000

            for (i in 0..timeResponseSecs) {
                runOnUiThread {
                    if (i != timeResponseSecs) {
                        view1.visibility = TextView.VISIBLE
                        view2.visibility = TextView.VISIBLE

                        view1.text = "Training starts in"
                        view2.text = (timeResponseSecs - i).toString()
                    } else {
                        view1.visibility = TextView.INVISIBLE
                        view2.visibility = TextView.INVISIBLE
                    }
                }
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()

    }

    private fun disableAllButtons() {
        connectButton.isEnabled = false
    }

    private fun enableAllButtons() {
        connectButton.isEnabled = true
    }

//    private fun disposeAllStreams() {
//        accDisposable?.dispose()
//    }
}