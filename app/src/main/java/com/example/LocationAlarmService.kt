package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import android.content.Context
import android.os.PowerManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationAlarmService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val channelId = "LocationAlarmChannel"
    private val alertChannelId = "AlarmAlertChannel"
    private val notificationId = 1
    
    private var isAlarmTriggered = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(NotificationManager::class.java)

        // Acquire WakeLock to keep CPU alive during background sleep modes
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CommuteWake::BackgroundLocationWakeLock").apply {
                acquire()
            }
            Log.d("LocationAlarmService", "Partial WakeLock successfully acquired.")
        } catch (e: Exception) {
            Log.e("LocationAlarmService", "Failed to acquire WakeLock", e)
        }

        createNotificationChannels()

        AppRepository.isAlarmActive.onEach { isActive ->
            if (!isActive) {
                stopSelf() // Stop service when alarm is deactivated
            }
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        requestLocationUpdates()
        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Commute Wake is Active")
            .setContentText("Scanning for your stop...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            AppRepository.currentLocation.value = location
            
            checkAlarmCondition(location)
            updateNotificationWithDistance(location)
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationAlarmService", "Missing location permissions", e)
        }
    }

    private fun checkAlarmCondition(currentLoc: Location) {
        val targetPos = AppRepository.targetLocation.value ?: return
        if (!AppRepository.isAlarmActive.value) return
        if (isAlarmTriggered) return

        val targetLoc = Location("").apply {
            latitude = targetPos.latitude
            longitude = targetPos.longitude
        }

        val distanceInMeters = currentLoc.distanceTo(targetLoc)

        // Dynamic Radius Logic
        // We want ~3 minutes of warning.
        // speed is in m/s. 
        val speed = if (currentLoc.hasSpeed()) currentLoc.speed else 0f
        val waitTimeSeconds = 180f
        val minRadius = 500f
        
        val newDynamicRadius = maxOf(minRadius, speed * waitTimeSeconds)
        AppRepository.dynamicRadius.value = newDynamicRadius

        if (distanceInMeters <= newDynamicRadius) {
            triggerAlarm()
        }
    }

    private fun triggerAlarm() {
        isAlarmTriggered = true
        AppRepository.isAlarmActive.value = false // Deactivate logically once triggered
        
        AlarmController.startAlarm(this)
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alertNotification = NotificationCompat.Builder(this, alertChannelId)
            .setContentTitle("Wake Up!")
            .setContentText("You are approaching your destination.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, alertNotification)
    }

    private fun updateNotificationWithDistance(location: Location) {
        val targetPos = AppRepository.targetLocation.value ?: return
        if (!AppRepository.isAlarmActive.value) return
        
        val targetLoc = Location("").apply {
            latitude = targetPos.latitude
            longitude = targetPos.longitude
        }
        val distance = location.distanceTo(targetLoc)
        val rad = AppRepository.dynamicRadius.value
        
        val distanceText = if (distance > 1000) {
            String.format("%.1f km", distance / 1000)
        } else {
            String.format("%.0f m", distance)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Your Stop is $distanceText Away")
            .setContentText("Triggering at ${rad.toInt()}m | Keep resting safely")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val alertChannel = NotificationChannel(
                alertChannelId,
                "Alarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("LocationAlarmService", "WakeLock successfully released.")
            }
        } catch (e: Exception) {
            Log.e("LocationAlarmService", "Failed to release WakeLock", e)
        }
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
