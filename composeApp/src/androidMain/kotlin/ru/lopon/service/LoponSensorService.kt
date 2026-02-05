package ru.lopon.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.lopon.MainActivity
import ru.lopon.platform.AndroidBleAdapter
import ru.lopon.platform.BleConnectionState
import ru.lopon.platform.BleDevice

class LoponSensorService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var bleAdapter: AndroidBleAdapter? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var reconnectJob: Job? = null

    private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Idle)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private var lastConnectedDeviceId: String? = null
    private var isRecording = false

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "lopon_sensor_channel"
        private const val CHANNEL_NAME = "Lopon Sensor Connection"

        private const val ACTION_START = "ru.lopon.action.START_SENSOR_SERVICE"
        private const val ACTION_STOP = "ru.lopon.action.STOP_SENSOR_SERVICE"
        private const val EXTRA_DEVICE_ID = "device_id"

        private const val RECONNECT_DELAY_MS = 3000L
        private const val MAX_RECONNECT_ATTEMPTS = 5

        fun start(context: Context, deviceId: String? = null) {
            val intent = Intent(context, LoponSensorService::class.java).apply {
                action = ACTION_START
                deviceId?.let { putExtra(EXTRA_DEVICE_ID, it) }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }


        fun stop(context: Context) {
            val intent = Intent(context, LoponSensorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        bleAdapter = AndroidBleAdapter(applicationContext)

        scope.launch {
            bleAdapter?.connectionState?.collect { state ->
                updateNotification(state)

                if (state is BleConnectionState.Disconnected && isRecording) {
                    attemptReconnect()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                acquireWakeLock()

                val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
                if (deviceId != null) {
                    connectToDevice(deviceId)
                }

                _serviceState.value = ServiceState.Running
            }
            ACTION_STOP -> {
                stopRecording()
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopRecording()
        reconnectJob?.cancel()
        scope.cancel()
        bleAdapter?.release()
        releaseWakeLock()

        super.onDestroy()
    }


    inner class LocalBinder : Binder() {
        val service: LoponSensorService
            get() = this@LoponSensorService
    }


    fun getBleAdapter(): AndroidBleAdapter? = bleAdapter

    fun connectToDevice(deviceId: String) {
        lastConnectedDeviceId = deviceId

        scope.launch {
            bleAdapter?.connect(deviceId)
        }
    }

    fun startRecording() {
        isRecording = true

        scope.launch {
            bleAdapter?.startSensor()
        }
    }

    fun stopRecording() {
        isRecording = false
        reconnectJob?.cancel()

        scope.launch {
            bleAdapter?.stopSensor()
            bleAdapter?.disconnect()
        }
    }

    fun observeSensorData() = bleAdapter?.observeWheelData()

    fun observeConnectionState() = bleAdapter?.connectionState

    fun getConnectedDevice(): BleDevice? = bleAdapter?.connectedDevice?.value

    private fun startForegroundWithNotification() {
        val notification = createNotification(BleConnectionState.Disconnected)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows sensor connection status"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(connectionState: BleConnectionState): Notification {
        val contentText = when (connectionState) {
            is BleConnectionState.Connected -> "Датчик подключён"
            is BleConnectionState.Connecting -> "Подключение к датчику..."
            is BleConnectionState.Disconnected -> "Датчик отключён"
            is BleConnectionState.Error -> "Ошибка: ${connectionState.message}"
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lopon")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(connectionState: BleConnectionState) {
        val notification = createNotification(connectionState)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Lopon::SensorWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun attemptReconnect() {
        val deviceId = lastConnectedDeviceId ?: return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var attempts = 0

            while (isRecording && attempts < MAX_RECONNECT_ATTEMPTS) {
                attempts++
                delay(RECONNECT_DELAY_MS * attempts)

                val result = bleAdapter?.connect(deviceId)
                if (result?.isSuccess == true) {
                    bleAdapter?.startSensor()
                    return@launch
                }
            }

            _serviceState.value = ServiceState.Error("Failed to reconnect after $MAX_RECONNECT_ATTEMPTS attempts")
        }
    }
}

sealed class ServiceState {
    data object Idle : ServiceState()
    data object Running : ServiceState()
    data class Error(val message: String) : ServiceState()
}
