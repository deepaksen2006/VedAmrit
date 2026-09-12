package com.example.vedaahar

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray

private val CommunityBackground = Color(0xFFF5FAF1)
private val CommunityCard = Color(0xFFFFFEFA)
private val CommunityPanel = Color(0xFFFDF8EF)
private val CareBlue = Color(0xFF265E9F)
private val CareRed = Color(0xFFC93333)
private val CareMint = Color(0xFF0C7F47)
private val SearchBorder = Color(0xFFD8E4F0)
private val WarningBackground = Color(0xFFFFF8DB)
private val WarningBorder = Color(0xFFE7B83E)
private val WarningText = Color(0xFF8C5A00)
private val EmergencyRed = Color(0xFFB3261E)
private val SoftGold = Color(0xFFE4BF65)

private enum class CareCategory(val label: String, val query: String, val color: Color) {
    All("All", "hospital clinic ngo pharmacy ayurvedic emergency", ForestGreen),
    Hospital("Hospital", "hospital", CareRed),
    Clinic("Clinic", "clinic doctors", CareBlue),
    Ngo("NGO", "ngo healthcare", Color(0xFF7356A6)),
    Pharmacy("Pharmacy", "pharmacy", Color(0xFF0C7F77)),
    Ayurveda("Ayurvedic Center", "ayurvedic clinic", CareMint),
    Emergency("Emergency", "emergency hospital", EmergencyRed)
}

private data class HealthcarePlace(
    val name: String,
    val category: CareCategory,
    val distance: String,
    val address: String,
    val status: String,
    val rating: String,
    val phone: String,
    val timing: String,
    val emergency: Boolean,
    val about: String
)

private val DemoHealthcarePlaces = listOf(
    HealthcarePlace(
        name = "Arogya Multispeciality Hospital",
        category = CareCategory.Hospital,
        distance = "1.2 km",
        address = "MG Road, near Central Market",
        status = "Open now",
        rating = "4.4",
        phone = "+91 98765 43210",
        timing = "24 hours",
        emergency = true,
        about = "General hospital with emergency intake, diagnostics, pharmacy access, and outpatient care."
    ),
    HealthcarePlace(
        name = "Swasthya Family Clinic",
        category = CareCategory.Clinic,
        distance = "800 m",
        address = "Green Avenue, Sector 4",
        status = "Open until 8 PM",
        rating = "4.2",
        phone = "+91 91234 56780",
        timing = "9 AM - 8 PM",
        emergency = false,
        about = "Primary care clinic for fever, digestion, wellness consultation, and routine checkups."
    ),
    HealthcarePlace(
        name = "Jan Seva Health NGO",
        category = CareCategory.Ngo,
        distance = "2.4 km",
        address = "Community Hall Road",
        status = "Open now",
        rating = "4.6",
        phone = "+91 90123 45678",
        timing = "10 AM - 6 PM",
        emergency = false,
        about = "Community health support, awareness camps, and subsidized medical assistance."
    ),
    HealthcarePlace(
        name = "MedPlus Pharmacy",
        category = CareCategory.Pharmacy,
        distance = "500 m",
        address = "Opposite City Bus Stand",
        status = "Open now",
        rating = "4.1",
        phone = "+91 99887 76655",
        timing = "8 AM - 11 PM",
        emergency = false,
        about = "Nearby pharmacy for prescriptions, OTC medicines, first-aid supplies, and wellness basics."
    ),
    HealthcarePlace(
        name = "Vedam Ayurveda Center",
        category = CareCategory.Ayurveda,
        distance = "1.8 km",
        address = "Lotus Street, Herbal Square",
        status = "Open until 7 PM",
        rating = "4.7",
        phone = "+91 93456 78901",
        timing = "8 AM - 7 PM",
        emergency = false,
        about = "Ayurvedic consultation, prakriti care, panchakarma guidance, and lifestyle support."
    )
)

@Composable
fun CommunityCareScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var locationSearch by remember { mutableStateOf("") }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    val requestLocation = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    val hasMapKey = BuildConfig.MAPTILER_API_KEY.isNotBlank()
    var emergencyMode by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf(DemoHealthcarePlaces.first()) }
    val visiblePlaces = remember(emergencyMode) {
        if (emergencyMode) {
            DemoHealthcarePlaces.filter { it.emergency || it.category == CareCategory.Pharmacy || it.category == CareCategory.Clinic }
        } else {
            DemoHealthcarePlaces
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = CommunityBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(CommunityBackground, Cream, LightSage.copy(alpha = 0.5f))))
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BackButton(onClick = onBack, text = "Consult")

            LiveCareLocatorCard(
                locationSearch = locationSearch,
                onLocationSearchChange = { locationSearch = it },
                locationGranted = locationGranted,
                hasMapKey = hasMapKey,
                emergencyMode = emergencyMode,
                places = visiblePlaces,
                onRequestLocation = requestLocation,
                onEmergencyToggle = { emergencyMode = !emergencyMode },
                onPlaceSelected = { selectedPlace = it }
            )

            LocationResultsSection(
                hasMapKey = hasMapKey,
                places = visiblePlaces,
                selectedPlace = selectedPlace,
                onPlaceSelected = { selectedPlace = it }
            )
            Spacer(modifier = Modifier.height(100.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun LiveCareLocatorCard(
    locationSearch: String,
    onLocationSearchChange: (String) -> Unit,
    locationGranted: Boolean,
    hasMapKey: Boolean,
    emergencyMode: Boolean,
    places: List<HealthcarePlace>,
    onRequestLocation: () -> Unit,
    onEmergencyToggle: () -> Unit,
    onPlaceSelected: (HealthcarePlace) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CommunityCard),
        border = BorderStroke(1.dp, SageGreen.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionKicker("LIVE CARE LOCATOR")
                    Text(
                        text = "Find nearby healthcare services",
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = "Discover hospitals, clinics, NGOs, pharmacies, Ayurvedic centers, and emergency care with MapTiler and OpenStreetMap data.",
                        color = ForestGreen.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = onRequestLocation,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreen, contentColor = PureWhite),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text("Use My Location", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onEmergencyToggle,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
                    ) {
                        Text(if (emergencyMode) "Exit Emergency" else "Emergency Help", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (emergencyMode) {
                EmergencyAccessPanel(onPlaceSelected = onPlaceSelected)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocatorSearchField(
                    value = locationSearch,
                    onValueChange = onLocationSearchChange,
                    placeholder = "Search a city, area, or healthcare location",
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )
                Button(
                    onClick = { },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = locationSearch.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = PureWhite,
                        disabledContainerColor = SageGreen.copy(alpha = 0.28f),
                        disabledContentColor = ForestGreen.copy(alpha = 0.55f)
                    )
                ) {
                    Text("Next", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 620.dp) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CommunityMapPanel(
                            locationGranted = locationGranted,
                            hasMapKey = hasMapKey,
                            modifier = Modifier.weight(1.35f)
                        )
                        NearbyResultsPanel(modifier = Modifier.weight(0.9f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CommunityMapPanel(
                            locationGranted = locationGranted,
                            hasMapKey = hasMapKey,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NearbyResultsPanel(
                            places = places.take(3),
                            onPlaceSelected = onPlaceSelected,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityMapPanel(
    locationGranted: Boolean,
    hasMapKey: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 280.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF5FA)),
        border = BorderStroke(1.dp, Color(0xFFD5E3EF))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            if (hasMapKey) {
                CommunityCareMap(locationGranted = locationGranted)
            } else {
                EmptyMapPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun NearbyResultsPanel(
    places: List<HealthcarePlace> = DemoHealthcarePlaces.take(3),
    onPlaceSelected: (HealthcarePlace) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 280.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CommunityPanel),
        border = BorderStroke(1.dp, SageGreen.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SectionKicker("NEARBY RESULTS")
                Text(
                    text = "${places.size} care options nearby",
                    color = DarkForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                places.forEach { place ->
                    CompactCareCard(place = place, onClick = { onPlaceSelected(place) })
                }
            }
        }
    }
}

@Composable
private fun LocationResultsSection(
    hasMapKey: Boolean,
    places: List<HealthcarePlace>,
    selectedPlace: HealthcarePlace,
    onPlaceSelected: (HealthcarePlace) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionKicker("LOCATION RESULTS")
        Text(
            text = "Nearby hospitals, clinics, NGOs, pharmacies, and Ayurvedic care",
            color = DarkForestGreen,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 28.sp
        )
        Text(
            text = "The list below updates from your searched map location, so the cards stay tied to the area you explore above.",
            color = ForestGreen.copy(alpha = 0.85f),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            places.forEach { place ->
                HealthcareResultCard(place = place, onClick = { onPlaceSelected(place) })
            }
        }

        HealthcareDetailsPreview(place = selectedPlace, hasMapKey = hasMapKey)

        SmartCareShortcuts()
    }
}

@Composable
private fun CategoryChip(category: CareCategory, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) category.color else PureWhite
    val content = if (selected) PureWhite else category.color
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(container)
            .border(1.dp, category.color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.label, color = content, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FilterChoiceRow(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(text = title, color = SoftBlueGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = option == selected
                Text(
                    text = option,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) ForestGreen else LightSage.copy(alpha = 0.65f))
                        .clickable { onSelected(option) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (isSelected) PureWhite else ForestGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmergencyAccessPanel(onPlaceSelected: (HealthcarePlace) -> Unit) {
    val emergencyPlace = DemoHealthcarePlaces.first { it.emergency }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EmergencyRed),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("EMERGENCY MODE", color = PureWhite.copy(alpha = 0.8f), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("Nearest emergency hospital is ${emergencyPlace.distance} away", color = PureWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EmergencyAction("Emergency Hospital") { onPlaceSelected(emergencyPlace) }
                EmergencyAction("Ambulance") { }
                EmergencyAction("Nearby Clinic") { onPlaceSelected(DemoHealthcarePlaces[1]) }
                EmergencyAction("Pharmacy") { onPlaceSelected(DemoHealthcarePlaces[3]) }
            }
        }
    }
}

@Composable
private fun EmergencyAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PureWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        color = EmergencyRed,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun CompactCareCard(place: HealthcarePlace, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PureWhite.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(place.category.color.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Text(place.category.label.first().toString(), color = place.category.color, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(place.name, color = DarkForestGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
            Text("${place.category.label} - ${place.distance}", color = SoftBlueGray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HealthcareResultCard(place: HealthcarePlace, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CommunityCard),
        border = BorderStroke(1.dp, SageGreen.copy(alpha = 0.24f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(place.category.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(place.category.label.first().toString(), color = place.category.color, fontWeight = FontWeight.Black)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(place.name, color = DarkForestGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                    Text("${place.category.label} - ${place.distance}", color = place.category.color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(place.address, color = SoftBlueGray, fontSize = 12.sp, lineHeight = 17.sp)
                    Text("${place.status} - Rating ${place.rating}", color = ForestGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniActionButton("Call", ForestGreen)
                MiniActionButton("Directions", CareBlue)
                if (place.emergency) MiniActionButton("Emergency", EmergencyRed)
            }
        }
    }
}

@Composable
private fun MiniActionButton(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun HealthcareDetailsPreview(place: HealthcarePlace, hasMapKey: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CommunityPanel),
        border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionKicker("SELECTED CARE DETAILS")
            Text(place.name, color = DarkForestGreen, fontSize = 22.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(place.category.label, place.category.color)
                StatusChip(place.distance, ForestGreen)
                StatusChip(if (place.emergency) "Emergency available" else "Emergency unknown", if (place.emergency) EmergencyRed else SoftBlueGray)
            }
            DetailRow("Address", place.address)
            DetailRow("Phone", place.phone)
            DetailRow("Timing", place.timing)
            Text(place.about, color = SoftBlueGray, fontSize = 13.sp, lineHeight = 19.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniActionButton("Call Now", ForestGreen)
                MiniActionButton("Get Directions", CareBlue)
                MiniActionButton("Save", SoftGold)
                MiniActionButton("Share", SageGreen)
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFEFF5FA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hasMapKey) "Map preview follows the selected healthcare location." else "Map preview needs a MapTiler key.",
                    color = SoftBlueGray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.width(82.dp), color = SageGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, modifier = Modifier.weight(1f), color = DarkForestGreen, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun SmartCareShortcuts() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CommunityCard),
        border = BorderStroke(1.dp, SageGreen.copy(alpha = 0.24f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionKicker("SMART CARE")
            Text("Saved and recent places", color = DarkForestGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("Recently visited clinic", CareBlue)
                StatusChip("Saved pharmacy", CareMint)
                StatusChip("Nearest hospital shortcut", EmergencyRed)
            }
        }
    }
}

@Composable
private fun LocatorSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = DarkForestGreen, fontSize = 13.sp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FBFF))
            .border(1.dp, SearchBorder, RoundedCornerShape(12.dp)),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Q", color = CareBlue.copy(alpha = 0.65f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isBlank()) {
                        Text(text = placeholder, color = SoftBlueGray.copy(alpha = 0.68f), fontSize = 12.sp)
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun FilterPill(text: String) {
    Row(
        modifier = Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FBFF))
            .border(1.dp, SearchBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = text, color = CareBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text("V", color = SoftBlueGray, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MapStatusBanner(hasMapKey: Boolean) {
    val text = if (hasMapKey) {
        "MapTiler is connected. Search or use your location to load nearby care points."
    } else {
        "MapTiler key is missing. Add VITE_MAPTILER_KEY to enable the locator."
    }
    val container = if (hasMapKey) LightSage.copy(alpha = 0.55f) else WarningBackground
    val border = if (hasMapKey) SageGreen.copy(alpha = 0.4f) else WarningBorder
    val content = if (hasMapKey) ForestGreen else WarningText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("!", color = content, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(text = text, color = content, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun EmptyMapPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEFF5FA)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MapTiler key is missing. Add VITE_MAPTILER_KEY to enable the locator.",
            modifier = Modifier.padding(24.dp),
            color = CareBlue,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SectionKicker(text: String) {
    Text(
        text = text,
        color = SageGreen,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CommunityCareMap(locationGranted: Boolean) {
    val html = remember(locationGranted) {
        communityCareMapHtml(
            apiKey = BuildConfig.MAPTILER_API_KEY,
            canUseLocation = locationGranted
        )
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp)),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?
                    ) {
                        callback?.invoke(origin, locationGranted, false)
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.setGeolocationEnabled(true)
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                loadDataWithBaseURL("https://vedaahaar.community-care.local/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://vedaahaar.community-care.local/", html, "text/html", "UTF-8", null)
        }
    )
}

private fun communityCareMapHtml(apiKey: String, canUseLocation: Boolean): String {
    val locationFlag = if (canUseLocation) "true" else "false"
    return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body, #map { height: 100%; margin: 0; background: #eff5fa; font-family: system-ui, -apple-system, Segoe UI, sans-serif; }
            .status {
              position: absolute; left: 12px; right: 12px; bottom: 12px; z-index: 1000;
              background: rgba(20,46,31,.9); color: #fffefa; border-radius: 14px;
              padding: 9px 12px; font-size: 12px; line-height: 1.35;
              box-shadow: 0 10px 30px rgba(20,46,31,.16);
            }
            .leaflet-popup-content { font-size: 13px; line-height: 1.35; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <div id="status" class="status">Allow location or search above to explore nearby care.</div>
          <script>
            const apiKey = "$apiKey";
            const canUseLocation = $locationFlag;
            let userLat = 20.5937;
            let userLon = 78.9629;
            let markers = [];
            const map = L.map('map', { zoomControl: false }).setView([userLat, userLon], 5);
            L.control.zoom({ position: 'bottomright' }).addTo(map);
            L.tileLayer('https://api.maptiler.com/maps/streets-v2/{z}/{x}/{y}.png?key=' + apiKey, {
              tileSize: 512,
              zoomOffset: -1,
              minZoom: 1,
              attribution: '&copy; MapTiler &copy; OpenStreetMap contributors'
            }).addTo(map);

            function setStatus(message) {
              document.getElementById('status').innerText = message;
            }

            function clearMarkers() {
              markers.forEach(marker => map.removeLayer(marker));
              markers = [];
            }

            function addUserMarker() {
              L.circleMarker([userLat, userLon], {
                radius: 8, color: '#142e1f', fillColor: '#6a8a6d', fillOpacity: 1, weight: 3
              }).addTo(map).bindPopup('Your location');
            }

            async function searchCare(query) {
              clearMarkers();
              addUserMarker();
              setStatus('Searching nearby hospitals, NGOs, and Ayurvedic clinics...');
              const url = 'https://api.maptiler.com/geocoding/' + encodeURIComponent(query) + '.json?key=' + apiKey + '&types=poi&limit=12&proximity=' + userLon + ',' + userLat;
              try {
                const response = await fetch(url);
                const data = await response.json();
                const features = data.features || [];
                features.forEach(feature => {
                  if (!feature.center) return;
                  const lon = feature.center[0];
                  const lat = feature.center[1];
                  const name = feature.text || feature.place_name || 'Care location';
                  const address = feature.place_name || 'Nearby result';
                  const marker = L.marker([lat, lon]).addTo(map).bindPopup('<b>' + name + '</b><br>' + address);
                  markers.push(marker);
                });
                if (features.length > 0) {
                  const group = L.featureGroup(markers);
                  map.fitBounds(group.getBounds().pad(0.25));
                  setStatus('Found ' + features.length + ' live care results. Tap markers for details.');
                } else {
                  map.setView([userLat, userLon], 13);
                  setStatus('No live results in this radius. Try another area or category.');
                }
              } catch (error) {
                map.setView([userLat, userLon], 12);
                setStatus('Could not load live care results. Check internet or API key restrictions.');
              }
            }

            function locateUser() {
              if (!navigator.geolocation || !canUseLocation) {
                setStatus('Location permission is not available yet. Showing India map fallback.');
                searchCare('hospital ngo ayurvedic clinic');
                return;
              }
              setStatus('Resolving your location...');
              navigator.geolocation.getCurrentPosition(position => {
                userLat = position.coords.latitude;
                userLon = position.coords.longitude;
                map.setView([userLat, userLon], 13);
                searchCare('hospital ngo ayurvedic clinic');
              }, () => {
                setStatus('Location unavailable. Showing India map fallback.');
                searchCare('hospital ngo ayurvedic clinic');
              }, { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 });
            }

            setTimeout(locateUser, 500);
          </script>
        </body>
        </html>
    """.trimIndent()
}
