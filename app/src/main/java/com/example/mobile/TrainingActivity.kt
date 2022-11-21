package com.example.mobile

import android.Manifest
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
import java.util.*


class TrainingActivity : AppCompatActivity() {
    companion object {  // ~ static JAVA
        private const val TAG = "TrainingActivity"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private var deviceId = ""

    private val api: PolarBleApi by lazy {
        // Notice PolarBleApi.ALL_FEATURES are enabled
        PolarBleApiDefaultImpl.defaultImplementation(applicationContext, PolarBleApi.ALL_FEATURES)
    }

    private var autoConnectDisposable: Disposable? = null
    private var scanDisposable: Disposable? = null
    private var movementDisposable: Disposable? = null

    private var deviceConnected = false
    private var bluetoothEnabled = false

    private lateinit var autoConnectButton: Button
    private lateinit var scanButton: Button
    private lateinit var connectButton: Button
    private lateinit var movementButton: Button
    private lateinit var textViewAccX: TextView

    // ATTENTION! Replace with the device ID from your device at index 1st of array below.
    private val listDeviceId = mutableListOf<String>("select one device", "B5E5C221")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())

        autoConnectButton = findViewById(R.id.auto_connect_button)
        scanButton = findViewById(R.id.scan_button)
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
                deviceId = if (listDeviceId[position] == "select one device") listDeviceId[1] else listDeviceId[position]
                connectButton.text = getString(R.string.connect_to_device, deviceId)
            }
        }

        autoConnectButton.setOnClickListener {
            if (autoConnectDisposable != null) {
                autoConnectDisposable?.dispose()
            }
            autoConnectDisposable = api.autoConnectToDevice(-60, "180D", null)
                .subscribe(
                    {
                        showToast("auto connect search complete")
                        // Log.d(TAG, "auto connect search complete")
                    },
                    { throwable: Throwable -> Log.e(TAG, "" + throwable.toString()) }
                )
        }

        scanButton.setOnClickListener {
            val isDisposed = scanDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(scanButton, R.string.scanning_devices)
                scanDisposable = api.searchForDevice()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarDeviceInfo: PolarDeviceInfo ->
                            listDeviceId.add("${polarDeviceInfo.deviceId}")
                            showToast("Found and added ${polarDeviceInfo.deviceId}")
                            // Log.d(TAG, "polar device found id: " + polarDeviceInfo.deviceId + " address: " + polarDeviceInfo.address + " rssi: " + polarDeviceInfo.rssi + " name: " + polarDeviceInfo.name + " isConnectable: " + polarDeviceInfo.isConnectable)
                        },
                        { error: Throwable ->
                            toggleButtonUp(scanButton, "Scan devices")
                            showToast("$error")
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
                scanDisposable?.dispose()
            }
        }

        connectButton.text = getString(R.string.connect_to_device, listDeviceId[1])
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
                toggleButtonDown(movementButton, R.string.stop_movement_stream)
                movementDisposable = requestStreamSettings(deviceId, PolarBleApi.DeviceStreamingFeature.ACC)
                    .flatMap { settings: PolarSensorSetting ->
                        api.startAccStreaming(deviceId, settings)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarAccelerometerData: PolarAccelerometerData ->
                            for (data in polarAccelerometerData.samples) {
                                Log.d(TAG, "ACC    x: ${data.x} y:  ${data.y} z: ${data.z}")
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
                        this@TrainingActivity,
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