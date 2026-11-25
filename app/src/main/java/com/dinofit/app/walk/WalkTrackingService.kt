package com.dinofit.app.walk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.dinofit.app.MainActivity
import com.dinofit.app.R
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.data.WalkRepository
import com.dinofit.app.core.model.LatLng
import com.dinofit.app.core.sensor.StepCounterSource
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enregistre le trajet d'une marche au GPS, même écran éteint.
 * Service « au premier plan » : Android affiche une notification tant qu'il tourne.
 */
@AndroidEntryPoint
class WalkTrackingService : Service() {

    @Inject lateinit var walks: WalkRepository
    @Inject lateinit var stepSource: StepCounterSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var trackingJob: Job? = null
    private var walkId: Long? = null
    private lateinit var locationManager: LocationManager

    // Toutes les méthodes sont écrites : sur Android 10 et moins, une lambda ferait planter onStatusChanged.
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onLocation(location)

        @Deprecated("Requis avant Android 11")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LocationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Marches", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Affichée pendant l'enregistrement d'une marche"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
        } else {
            startTracking()
        }
        // Pas de redémarrage automatique : Android l'interdit pour le GPS en arrière-plan.
        // Si le système coupe le service, la marche reste « en cours » et reprend à la réouverture de l'app.
        return START_NOT_STICKY
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }
        // Appelé à chaque demande de démarrage, même si le suivi tourne déjà (exigé par Android).
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification(if (trackingJob == null) "Recherche du signal GPS…" else "Enregistrement du trajet"),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (e: Exception) {
            stopSelf()
            return
        }
        if (trackingJob != null) return

        trackingJob = scope.launch {
            val id = walks.startOrResume()
            walkId = id

            // Un point toutes les 3 s environ ; le filtre du dépôt écarte le bruit.
            for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (hasLocationPermission() && runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)) {
                    runCatching { locationManager.requestLocationUpdates(provider, 3_000L, 0f, listener, Looper.getMainLooper()) }
                }
            }

            // Pas faits pendant la marche : différence du podomètre depuis le départ.
            launch {
                var base: Long? = null
                stepSource.rawCounter().collect { raw ->
                    if (base == null) base = raw - walks.stepsOf(id)
                    val steps = (raw - (base ?: raw)).coerceAtLeast(0).toInt()
                    walks.updateSteps(id, steps)
                }
            }

            // Met à jour la notification (distance, durée) toutes les 10 s.
            while (isActive) {
                walks.current(id)?.let { w ->
                    val minutes = ((System.currentTimeMillis() - w.startedAt) / 60_000L).toInt()
                    notify("${Formats.distance(w.distanceM)} en ${Formats.duration(minutes)}, ${Formats.int(w.steps)} pas")
                }
                delay(10_000)
            }
        }
    }

    private fun onLocation(location: Location) {
        val id = walkId ?: return
        scope.launch {
            walks.addPoint(
                id,
                LatLng(location.latitude, location.longitude),
                System.currentTimeMillis(),
                if (location.hasAccuracy()) location.accuracy else 50f,
            )
        }
    }

    private fun stopTracking() {
        runCatching { locationManager.removeUpdates(listener) }
        trackingJob?.cancel()
        trackingJob = null
        scope.launch {
            walks.finishActive()
            walkId = null
            ServiceCompat.stopForeground(this@WalkTrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching { locationManager.removeUpdates(listener) }
        scope.cancel()
        super.onDestroy()
    }

    private fun notify(text: String) {
        val canPost = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (canPost) getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, WalkTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle("Marche en cours")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "Terminer", stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val ACTION_START = "com.dinofit.app.walk.START"
        const val ACTION_STOP = "com.dinofit.app.walk.STOP"
        private const val CHANNEL_ID = "walks"
        private const val NOTIFICATION_ID = 42
    }
}

/** Démarre / arrête l'enregistrement depuis l'interface (l'app doit être à l'écran). */
@Singleton
class WalkTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun start() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, WalkTrackingService::class.java).setAction(WalkTrackingService.ACTION_START),
        )
    }

    fun stop() {
        context.startService(Intent(context, WalkTrackingService::class.java).setAction(WalkTrackingService.ACTION_STOP))
    }
}
