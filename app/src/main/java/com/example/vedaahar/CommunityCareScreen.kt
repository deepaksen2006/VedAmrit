package com.example.vedaahar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.vedaahar.care.*
import com.example.vedaahar.ui.theme.ForestGreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.Locale

@Composable
fun CommunityCareScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}, model: CareDirectoryViewModel = viewModel(),
                        doctorModel: DoctorDirectoryViewModel? = null) {
    var doctorsVisible by rememberSaveable { mutableStateOf(false) }
    if (doctorsVisible) DoctorDirectoryScreen(onBack = { doctorsVisible = false }, modifier = modifier, model = doctorModel ?: viewModel())
    else HospitalDirectoryScreen(modifier, onBack, model, onDoctors = { doctorsVisible = true })
}

@Composable
private fun HospitalDirectoryScreen(modifier: Modifier, onBack: () -> Unit, model: CareDirectoryViewModel, onDoctors: () -> Unit) {
    val state by model.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var search by rememberSaveable { mutableStateOf(state.search.text) }
    var radius by rememberSaveable { mutableStateOf("20") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var coordinatesVisible by rememberSaveable { mutableStateOf(false) }
    var locationBusy by remember { mutableStateOf(false) }
    var locationJob by remember { mutableStateOf<Job?>(null) }
    var inputMessage by remember { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) locationJob?.cancel()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); locationJob?.cancel() }
    }

    fun radiusValue(): Double? = radius.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.01..100.0 }
    fun dismissInput() { focus.clearFocus(); keyboard?.hide() }
    fun searchNearby(point: CarePoint) {
        locationJob?.cancel()
        val km = radiusValue()
        if (km == null || !point.isValid) inputMessage = "Enter valid coordinates and a radius from 0.01 to 100 km."
        else { dismissInput(); inputMessage = null; model.search(CareSearch(search, point, km)) }
    }
    fun locate() {
        val requestedRadius = radiusValue()
        val requestedText = search
        if (requestedRadius == null) {
            inputMessage = "Enter a radius from 0.01 to 100 km."
            return
        }
        locationJob?.cancel()
        dismissInput()
        locationJob = scope.launch {
            locationBusy = true
            inputMessage = null
            try {
                val point = currentCareLocation(context)
                if (point == null) inputMessage = "Location unavailable. Enable device location, search by name or city, or enter coordinates."
                else model.search(CareSearch(requestedText, point, requestedRadius))
            } finally { locationBusy = false }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locate()
        else inputMessage = "Location permission was denied. Search by name or city, or enter coordinates."
    }

    Surface(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().testTag("care-list"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BackButton(onClick = onBack, text = "Consult") }
            item {
                Text("Community Care", style = MaterialTheme.typography.headlineMedium, color = ForestGreen)
                Text("Find verified hospitals. Map pins require approved locations. Booking is unavailable.")
                OutlinedButton(onClick = onDoctors) { Text("Browse verified doctors") }
            }
            item {
                OutlinedTextField(value = search, onValueChange = { search = it.take(200) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Hospital name or city") }, singleLine = true)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { locationJob?.cancel(); dismissInput(); inputMessage = null; model.search(CareSearch(search)) }) { Text("Search directory") }
                    OutlinedButton(onClick = { locationJob?.cancel(); dismissInput(); search = ""; inputMessage = null; model.search(CareSearch()) }) { Text("Clear") }
                }
            }
            item {
                OutlinedTextField(value = radius, onValueChange = { radius = it.take(6) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Nearby radius (0.01–100 km)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !locationBusy, onClick = {
                        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                        if (granted) locate() else permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }) { Text(if (locationBusy) "Locating…" else "Use my location") }
                    TextButton(onClick = { coordinatesVisible = !coordinatesVisible }) { Text("Coordinates") }
                }
                Text("Your location is sent to VedAmrit only when you request nearby search.", style = MaterialTheme.typography.bodySmall)
            }
            if (coordinatesVisible) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(latitude, { latitude = it.take(24) }, Modifier.fillMaxWidth(), label = { Text("Latitude (−90 to 90)") }, singleLine = true)
                    OutlinedTextField(longitude, { longitude = it.take(24) }, Modifier.fillMaxWidth(), label = { Text("Longitude (−180 to 180)") }, singleLine = true)
                    OutlinedButton(onClick = { searchNearby(CarePoint(latitude.toDoubleOrNull() ?: Double.NaN,
                        longitude.toDoubleOrNull() ?: Double.NaN)) }) { Text("Search near coordinates") }
                }
            }
            inputMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            item {
                HospitalMap(hospitals = state.hospitals, center = state.search.center, selectedId = state.selectedId,
                    onSelect = model::select, modifier = Modifier.fillMaxWidth().height(300.dp))
            }
            item {
                val center = state.search.center
                Text(if (center == null) "Verified hospital directory" else
                    String.format(Locale.US, "Within %.2f km of %.4f, %.4f", state.search.radiusKm, center.latitude, center.longitude),
                    style = MaterialTheme.typography.titleMedium)
                if (state.loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading hospitals…")
                }
                if (!state.loading && state.error == null && state.hospitals.isEmpty()) {
                    Text(if (center == null) "No verified hospitals found. Try another name or city."
                        else "No verified hospitals with approved locations were found in this radius.")
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { if (state.page > 0) model.loadMore() else model.refresh() }) { Text("Retry") }
                }
                if (state.hospitals.isNotEmpty()) Text("Showing ${state.hospitals.size} of ${state.count} results. Pins show loaded hospitals with approved locations.",
                    style = MaterialTheme.typography.bodySmall)
            }
            state.hospitals.firstOrNull { it.id == state.selectedId }?.let { selected ->
                item {
                    Card(border = BorderStroke(2.dp, ForestGreen)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Selected hospital", style = MaterialTheme.typography.labelLarge)
                            HospitalInformation(selected)
                        }
                    }
                }
            }
            items(state.hospitals, key = { it.id }) { hospital ->
                Card(onClick = { model.select(hospital.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { HospitalInformation(hospital) }
                }
            }
            if (state.hasMore && !state.loading) item { OutlinedButton(onClick = model::loadMore) { Text("Load more") } }
            item { OutlinedButton(onClick = model::refresh, enabled = !state.loading) { Text("Refresh results") } }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun HospitalInformation(hospital: com.example.vedaahar.network.dto.CareHospitalDto) {
    Text(hospital.name, style = MaterialTheme.typography.titleMedium, color = ForestGreen)
    Text("Verified identity • " + if (hospital.approvedPoint() != null) "Approved location" else "Location awaiting approval")
    Text(listOf(hospital.address, hospital.city, hospital.state, hospital.postalCode).filter { it.isNotBlank() }.joinToString(", "))
    hospital.distanceKm?.takeIf { it.isFinite() && it >= 0 }?.let { Text(String.format(Locale.US, "%.2f km away", it)) }
    if (hospital.phone.isNotBlank()) Text("Phone: ${hospital.phone}")
    if (hospital.description.isNotBlank()) Text(hospital.description)
    if (hospital.facilities.isNotEmpty()) Text(hospital.facilities.joinToString(" • "))
    hospital.locationAttribution?.takeIf { it.isNotBlank() && hospital.approvedPoint() != null }?.let {
        Text(android.text.Html.fromHtml(it, android.text.Html.FROM_HTML_MODE_LEGACY).toString(), style = MaterialTheme.typography.bodySmall)
    }
}
