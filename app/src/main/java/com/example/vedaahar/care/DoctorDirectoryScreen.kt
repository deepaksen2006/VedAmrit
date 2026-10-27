package com.example.vedaahar.care

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vedaahar.BackButton
import com.example.vedaahar.network.dto.CareDoctorDto
import com.example.vedaahar.ui.theme.ForestGreen

@Composable
fun DoctorDirectoryScreen(onBack: () -> Unit, modifier: Modifier = Modifier, model: DoctorDirectoryViewModel = viewModel()) {
    val state by model.state.collectAsState()
    var text by rememberSaveable { mutableStateOf(state.search.text) }
    var specialty by rememberSaveable { mutableStateOf(state.search.specialty) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        var stopped = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stopped = true
            if (event == Lifecycle.Event.ON_RESUME && stopped) { stopped = false; model.refresh() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    fun back() { if (state.selectedId != null) model.directory() else onBack() }
    fun dismissInput() { focus.clearFocus(); keyboard?.hide() }
    BackHandler { back() }
    Surface(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().testTag("doctor-directory"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { BackButton(onClick = ::back, text = if (state.selectedId != null) "Doctor directory" else "Community Care") }
            item {
                Text(if (state.selectedId == null) "Verified Ayurvedic doctors" else "Doctor profile",
                    style = MaterialTheme.typography.headlineMedium, color = ForestGreen, modifier = Modifier.semantics { heading() })
                Text("Credentials reviewed by an authorized verifier. Booking is unavailable.")
            }
            if (state.selectedId == null) {
                item {
                    OutlinedTextField(text, { text = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Doctor name or specialty") }, singleLine = true)
                    OutlinedTextField(specialty, { specialty = it.take(140) }, Modifier.fillMaxWidth(), label = { Text("Specialty filter (optional)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { dismissInput(); model.search(state.search.copy(text = text, specialty = specialty)) }) { Text("Search doctors") }
                        OutlinedButton(onClick = { dismissInput(); text = ""; specialty = ""; model.search(DoctorSearch()) }) { Text("Clear filters") }
                    }
                    if (state.search.hospitalId != null) {
                        Text("Hospital: ${state.search.hospitalName}")
                        TextButton(onClick = { model.search(state.search.copy(hospitalId = null, hospitalName = "")) }) { Text("All hospitals") }
                    }
                }
                if (state.loading) item { LoadingDoctors("Loading doctors…") }
                state.error?.let { error -> item { DirectoryError(error) { if (state.page > 0) model.loadMore() else model.refresh() } } }
                if (!state.loading && state.error == null && state.doctors.isEmpty()) item {
                    Text("No verified doctors found. Try another name or specialty.", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                items(state.doctors, key = { it.id }) { doctor ->
                    Card(onClick = { dismissInput(); model.openProfile(doctor.id) }, modifier = Modifier.fillMaxWidth().testTag("doctor-card-${doctor.id}")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(doctor.fullName, style = MaterialTheme.typography.titleMedium, color = ForestGreen)
                            Text("Verified credentials")
                            Text(doctor.specialization)
                            Text(doctor.qualifications.joinToString(" • "))
                            Text(doctor.affiliations.joinToString("; ") { "${it.hospitalName}, ${it.city}" })
                            Text("View profile", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                if (state.hasMore && !state.loading) item { OutlinedButton(onClick = model::loadMore) { Text("Load more doctors") } }
                item { OutlinedButton(onClick = model::refresh, enabled = !state.loading) { Text("Refresh doctors") } }
            } else {
                if (state.profileLoading) item { LoadingDoctors("Loading doctor profile…") }
                state.profileError?.let { error -> item { DirectoryError(error, model::refresh) } }
                state.profile?.let { doctor -> item { DoctorInformation(doctor) { id, name ->
                    dismissInput(); text = ""; specialty = ""; model.search(DoctorSearch(hospitalId = id, hospitalName = name))
                } } }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun LoadingDoctors(label: String) {
    Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(label)
    }
}

@Composable
private fun DirectoryError(message: String, retry: () -> Unit) {
    Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    OutlinedButton(onClick = retry) { Text("Retry doctors") }
}

@Composable
private fun DoctorInformation(doctor: CareDoctorDto, onHospital: (Long, String) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("doctor-profile"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(doctor.fullName, style = MaterialTheme.typography.headlineSmall, color = ForestGreen, modifier = Modifier.semantics { heading() })
        Text("Verified credentials • reviewed ${doctor.credentialsReviewedAt.orEmpty().take(10)}")
        Text("Specialty: ${doctor.specialization}")
        Text("Documented qualifications: ${doctor.qualifications.joinToString(" • ")}")
        Text("Registration: ${doctor.registrationNumber}")
        Text("Issuing council: ${doctor.registrationCouncil}")
        Text("Experience: ${doctor.experienceYears} years")
        if (doctor.bio.isNotBlank()) Text(doctor.bio)
        Text("Verified hospital affiliations", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        doctor.affiliations.forEach { affiliation ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(affiliation.hospitalName, style = MaterialTheme.typography.titleMedium)
                    Text(affiliation.city)
                    TextButton(onClick = { onHospital(affiliation.hospitalId, affiliation.hospitalName) }) { Text("Doctors at this hospital") }
                }
            }
        }
    }
}
