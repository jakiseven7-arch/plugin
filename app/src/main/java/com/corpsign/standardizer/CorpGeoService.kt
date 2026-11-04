package com.corpsign.standardizer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servico em foreground que registra um TestProvider ("CorpMockProvider") no
 * LocationManager e injeta periodicamente as coordenadas da sede (Ararangua).
 *
 * Tambem monitora a configuracao "mock_location_package" via ContentObserver:
 * se o app nao for o provedor de mock location ativo, guia o usuario ate a
 * tela de configuracao correspondente.
 */
class CorpGeoService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var injectJob: Job? = null
    private var locationManager: LocationManager? = null
    private var contentObserver: android.database.ContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Padrão Corporativo Ativo: Araranguá"))
        registerMockProvider()
        observeMockLocationSetting()
        startInjection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        injectJob?.cancel()
        unregisterMockProvider()
        contentObserver?.let { contentResolver.unregisterContentObserver(it) }
    }

    // ------------------------------------------------------------------ //
    // Provider de mock location
    // ------------------------------------------------------------------ //

    private fun registerMockProvider() {
        val lm = locationManager ?: return
        try {
            if (lm.allProviders.contains(CorpProfile.MOCK_PROVIDER_NAME)) {
                lm.removeTestProvider(CorpProfile.MOCK_PROVIDER_NAME)
            }
        } catch (_: Exception) { }
        try {
            lm.addTestProvider(
                CorpProfile.MOCK_PROVIDER_NAME,
                false, false, false, false,
                true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            )
            lm.setTestProviderEnabled(CorpProfile.MOCK_PROVIDER_NAME, true)
            Log.d(TAG, "CorpMockProvider registrado")
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao registrar provider: ${e.message}")
        }
    }

    private fun unregisterMockProvider() {
        try {
            locationManager?.removeTestProvider(CorpProfile.MOCK_PROVIDER_NAME)
        } catch (_: Exception) { }
    }

    private fun injectLocation() {
        val lm = locationManager ?: return
        try {
            val loc = Location(CorpProfile.MOCK_PROVIDER_NAME).apply {
                latitude = CorpProfile.LATITUDE
                longitude = CorpProfile.LONGITUDE
                altitude = CorpProfile.ALTITUDE
                accuracy = 5f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
            }
            lm.setTestProviderLocation(CorpProfile.MOCK_PROVIDER_NAME, loc)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao injetar localizacao: ${e.message}")
        }
    }

    private fun startInjection() {
        injectJob = scope.launch {
            while (isActive) {
                injectLocation()
                delay(2000L)
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Monitoramento do mock_location_package
    // ------------------------------------------------------------------ //

    private fun observeMockLocationSetting() {
        contentObserver = object : android.database.ContentObserver(android.os.Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                checkMockLocationPermission()
            }
        }
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("mock_location"),
            true,
            contentObserver!!
        )
        // Tambem observa a chave especifica de package (Android 6+)
        try {
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor("mock_location_app"),
                true,
                contentObserver!!
            )
        } catch (_: Exception) { }
        checkMockLocationPermission()
    }

    private fun checkMockLocationPermission() {
        val activePackage = try {
            Settings.Secure.getString(contentResolver, "mock_location_app")
        } catch (_: Exception) { null }

        if (activePackage != packageName) {
            // Guia o usuario para as configuracoes de localizacao simulada
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(fallback)
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Notificacao de foreground
    // ------------------------------------------------------------------ //

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "CorpSign Geo", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CorpSign Standardizer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    companion object {
        private const val TAG = "CorpGeoService"
        private const val CHANNEL_ID = "corpsign_geo"
        private const val NOTIF_ID = 1001
    }
}