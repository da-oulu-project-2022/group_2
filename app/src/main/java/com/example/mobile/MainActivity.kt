package com.example.mobile

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {  // ~ static JAVA
        private const val TAG = "MainActivity"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private var startingTime: Long = 0
    private var dataFile: File? = null
    var fileWriter: FileWriter? = null
    private var isRecording: Boolean? = null

    // ATTENTION! Replace with the device ID from your device.
    private var deviceId = "B5E5C221"

    private val api: PolarBleApi by lazy {
        // Notice PolarBleApi.ALL_FEATURES are enabled
        PolarBleApiDefaultImpl.defaultImplementation(applicationContext, PolarBleApi.ALL_FEATURES)
    }

    private var movementDisposable: Disposable? = null

    private var deviceConnected = false
    private var bluetoothEnabled = false

    private lateinit var editTextDeviceId: EditText
    private lateinit var addButton: Button
    private lateinit var connectButton: Button
    private lateinit var movementButton: Button
    private lateinit var textViewAccX: TextView

    private val fileName: String = "MyDevicesId.txt"
    private val listDeviceId = mutableListOf<String>("select your id device", "B5E5C221")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)





        editTextDeviceId = findViewById(R.id.input_device_id)
        addButton = findViewById(R.id.add_button)
        addButton.setOnClickListener {
            listDeviceId.add("${editTextDeviceId.text}")
            try {
                var fout: FileOutputStream= openFileOutput(fileName, MODE_APPEND)
                fout.write("${editTextDeviceId.text}\n".toByteArray())
                fout.close()
            } catch (ex: Exception) {
                Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
            }

            Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show()
            editTextDeviceId.setText("")
        }

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
                text?.let { listDeviceId.add(it) }
            }
        } catch (ex: Exception) {
            Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
        }

        val spinner: Spinner = findViewById(R.id.spinner)
//        // Create an ArrayAdapter using the string array and a default spinner layout
//        ArrayAdapter.createFromResource(
//            this,
//            R.array.device_id_array,
//            android.R.layout.simple_spinner_item
//        ).also { adapter ->
//            // Specify the layout to use when the list of choices appears
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//            // Apply the adapter to the spinner
//            spinner.adapter = adapter
//        }

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
                deviceId = if (listDeviceId[position] === "select your id device") listDeviceId[1] else listDeviceId[position]
                connectButton.text = getString(R.string.connect_to_device, deviceId)
            }
        }

        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())
        connectButton = findViewById(R.id.connect_button)
        movementButton = findViewById(R.id.movement_button)
        textViewAccX = findViewById(R.id.view_acc_X)

        api.setPolarFilter(false)
        api.setApiLogger { s: String -> Log.d(API_LOGGER_TAG, s) }
        api.setApiCallback(object : PolarBleApiCallback() {
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

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: " + polarDeviceInfo.deviceId)
                deviceId = polarDeviceInfo.deviceId
                deviceConnected = true
                val buttonText = getString(R.string.disconnect_from_device, deviceId)
                toggleButtonDown(connectButton, buttonText)
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId)
                deviceConnected = false
                val buttonText = getString(R.string.connect_to_device, deviceId)
                toggleButtonUp(connectButton, buttonText)
            }

            override fun streamingFeaturesReady(
                identifier: String, features: Set<PolarBleApi.DeviceStreamingFeature>
            ) {
                for (feature in features) {
                    Log.d(TAG, "Streaming feature $feature is ready")
                }
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

            override fun polarFtpFeatureReady(s: String) {
                Log.d(TAG, "FTP ready")
            }
        })

        connectButton.text = getString(R.string.connect_to_device, deviceId)
        connectButton.setOnClickListener {
            try {
                if (deviceConnected) {
                    api.disconnectFromDevice(deviceId)
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

        movementButton.setOnClickListener {
            val isDisposed = movementDisposable?.isDisposed ?: true
            if (isDisposed) {
                startingTime = System.currentTimeMillis();
                val formatter = SimpleDateFormat("dd_MM_yyyy_HH_mm_ss")
                val date = Date()
                val dateString: String = formatter.format(date).toString()
                Log.d(TAG, dateString)
                val fileName = "$dateString.csv"
                val samples = getDir("samples", Context.MODE_PRIVATE)

                try {
                    dataFile = File(samples, fileName)
                    fileWriter = FileWriter(dataFile)

                    fileWriter!!.write("time m/s^2,x,y,z\n")
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e("TAG","error")
                }
//                fileWriter?.close()

                toggleButtonDown(movementButton, R.string.stop_movement_stream)
                movementDisposable = requestStreamSettings(deviceId, PolarBleApi.DeviceStreamingFeature.ACC)
                    .flatMap { settings: PolarSensorSetting ->
                        api.startAccStreaming(deviceId, settings)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarAccelerometerData: PolarAccelerometerData ->
                            for (data in polarAccelerometerData.samples) {
                                val currentTimeMillis = System.currentTimeMillis()

                                val timeSinceStart = currentTimeMillis - startingTime
                                Log.d(TAG, "ACC   time: $timeSinceStart x: ${data.x} y:  ${data.y} z: ${data.z}")
                                var saveString: String = timeSinceStart.toString() +"," + data.x.toString() + "," + data.y.toString() + "," + data.z.toString() + "\n"
                                try {
                                    fileWriter!!.write(saveString)
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                                textViewAccX.text = "X: ${data.x.toString()}"
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
            } else {
                toggleButtonUp(movementButton, R.string.start_movement_stream)
                // NOTE dispose will stop streaming if it is "running"

                Log.e("TAG","Save")
                fileWriter?.close()

                movementDisposable?.dispose()
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
    }

    private fun requestStreamSettings(identifier: String, feature: PolarBleApi.DeviceStreamingFeature): Flowable<PolarSensorSetting> {
        val availableSettings = api.requestStreamSettings(identifier, feature)
        val allSettings = api.requestFullStreamSettings(identifier, feature)
            .onErrorReturn { error: Throwable ->
                Log.w(TAG, "Full stream settings are not available for feature $feature. REASON: $error")
                PolarSensorSetting(emptyMap())
            }
        return Single.zip(availableSettings, allSettings) { available: PolarSensorSetting, all: PolarSensorSetting ->
            if (available.settings.isEmpty()) {
                throw Throwable("Settings are not available")
            } else {
                Log.d(TAG, "Feature " + feature + " available settings " + available.settings)
                Log.d(TAG, "Feature " + feature + " all settings " + all.settings)
                return@zip android.util.Pair(available, all)
            }
        }
            .observeOn(AndroidSchedulers.mainThread())
            .toFlowable()
            .flatMap(
                Function { sensorSettings: android.util.Pair<PolarSensorSetting, PolarSensorSetting> ->
                    DialogUtility.showAllSettingsDialog(
                        this@MainActivity,
                        sensorSettings.first.settings,
                        sensorSettings.second.settings
                    ).toFlowable()
                } as io.reactivex.rxjava3.functions.Function<Pair<PolarSensorSetting, PolarSensorSetting>, Flowable<PolarSensorSetting>>
            )
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