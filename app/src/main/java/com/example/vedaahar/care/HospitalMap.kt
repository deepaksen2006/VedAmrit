package com.example.vedaahar.care

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.vedaahar.BuildConfig
import com.example.vedaahar.network.dto.CareHospitalDto
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.log.Logger
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.module.http.HttpRequestUtil
import java.io.IOException
import java.util.concurrent.TimeUnit

internal const val MAP_USER_AGENT = "VedAmrit-Android"

@Composable
fun HospitalMap(
    hospitals: List<CareHospitalDto>, center: CarePoint?, selectedId: Long?, onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (BuildConfig.MAPTILER_API_KEY.isBlank()) {
        Card(modifier.testTag("care-map")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Map unavailable", style = MaterialTheme.typography.titleMedium)
                Text("The map is not configured for this build. You can still search the hospital directory and nearby results below.")
            }
        }
        return
    }
    var attempt by remember { mutableIntStateOf(0) }
    Column(modifier.testTag("care-map")) {
        key(attempt) {
            NativeHospitalMap(hospitals, center, selectedId, onSelect, Modifier.fillMaxWidth().weight(1f)) {
                attempt++
            }
        }
        Text("© MapTiler  © OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall)
    }
}

@Suppress("DEPRECATION") // MapLibre's built-in markers retain accessible titles and click selection.
@Composable
private fun NativeHospitalMap(
    hospitals: List<CareHospitalDto>, center: CarePoint?, selectedId: Long?, onSelect: (Long) -> Unit,
    modifier: Modifier, onRetry: () -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val select by rememberUpdatedState(onSelect)
    var status by remember { mutableStateOf("Loading map…") }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styled by remember { mutableStateOf(false) }
    val markerIds = remember { mutableMapOf<Long, Long>() }
    val alive = remember { java.util.concurrent.atomic.AtomicBoolean(true) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val mapView = remember {
        // HttpRequestUtil initializes SDK classes that require the application context.
        MapLibre.getInstance(context)
        // This client belongs only to MapLibre; it never shares JWT/token interceptors.
        Logger.setVerbosity(Logger.NONE)
        HttpRequestUtil.setLogEnabled(false)
        HttpRequestUtil.setOkHttpClient(OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).followRedirects(false)
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.scheme != "https" || request.url.host != "api.maptiler.com") {
                    throw IOException("Unsupported map host")
                }
                try {
                    val response = chain.proceed(request.newBuilder()
                        .header("User-Agent", "$MAP_USER_AGENT/${BuildConfig.VERSION_NAME}").build())
                    if (!response.isSuccessful) handler.post {
                        if (alive.get()) status = when (response.code) {
                            401, 403 -> "Map authorization failed. Check the Android map key configuration."
                            429 -> "Map request limit reached. Please retry later."
                            else -> "Map tiles could not be loaded. Please retry."
                        }
                    }
                    response
                } catch (error: IOException) {
                    // Camera changes and disposal routinely cancel obsolete tile requests.
                    // Those cancellations are not connectivity failures for the active map.
                    if (!chain.call().isCanceled()) handler.post {
                        if (alive.get()) status = "Map connection failed. Check your connection and retry."
                    }
                    throw error
                }
            }.build())
        MapView(context).apply {
            onCreate(null)
            addOnDidFailLoadingMapListener {
                if (alive.get()) status = "Map could not be loaded. Check your connection or map configuration."
            }
            // A rendered background frame is not proof that the requested tiles loaded.
            // Idle follows completion of the current camera/render/tile work.
            addOnDidBecomeIdleListener {
                if (alive.get() && styled && status == "Loading map…") status = ""
            }
            getMapAsync { ready ->
                if (alive.get()) {
                    map = ready
                    ready.uiSettings.isAttributionEnabled = true
                    ready.setOnMarkerClickListener { marker ->
                        markerIds[marker.id]?.let { select(it) }
                        false
                    }
                    ready.setStyle("https://api.maptiler.com/maps/streets-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}") {
                        if (alive.get()) styled = true
                    }
                }
            }
        }
    }

    DisposableEffect(mapView, owner) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) { mapView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> if (!resumed) { mapView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> if (resumed) { mapView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> if (started) { mapView.onStop(); started = false }
                else -> Unit
            }
        }
        val memory = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() = mapView.onLowMemory()
        }
        context.registerComponentCallbacks(memory)
        owner.lifecycle.addObserver(observer)
        onDispose {
            alive.set(false)
            owner.lifecycle.removeObserver(observer)
            context.unregisterComponentCallbacks(memory)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }
    LaunchedEffect(mapView, status) {
        if (status == "Loading map…") {
            delay(25_000)
            if (status == "Loading map…") status = "Map loading timed out. Check your connection and retry."
        }
    }
    LaunchedEffect(map, styled, hospitals, center, selectedId) {
        val ready = map ?: return@LaunchedEffect
        if (!styled) return@LaunchedEffect
        ready.removeAnnotations()
        markerIds.clear()
        val points = mutableListOf<LatLng>()
        hospitals.forEach { hospital ->
            hospital.approvedPoint()?.let { point ->
                val position = LatLng(point.latitude, point.longitude)
                val marker = ready.addMarker(MarkerOptions().position(position).title(hospital.name)
                    .snippet(hospital.address))
                markerIds[marker.id] = hospital.id
                points += position
                if (hospital.id == selectedId) ready.selectMarker(marker)
            }
        }
        val selected = hospitals.firstOrNull { it.id == selectedId }?.approvedPoint()
        if (status.isEmpty()) status = "Loading map…"
        when {
            selected != null -> ready.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(selected.latitude, selected.longitude), 14.0))
            points.distinct().size > 1 -> ready.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(), 48))
            points.isNotEmpty() -> ready.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 13.0))
            center != null -> ready.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(center.latitude, center.longitude), 11.0))
            else -> ready.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.5937, 78.9629), 3.0))
        }
    }
    Box(modifier.testTag(if (status.isEmpty()) "care-map-ready" else "care-map-pending")) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        if (status.isNotEmpty()) Surface(Modifier.fillMaxWidth(), tonalElevation = 4.dp) {
            Column(Modifier.padding(8.dp)) {
                Text(status, style = MaterialTheme.typography.bodySmall)
                if (status == "Loading map…") LinearProgressIndicator(Modifier.fillMaxWidth())
                else TextButton(onClick = onRetry) { Text("Retry map") }
            }
        }
    }
}
