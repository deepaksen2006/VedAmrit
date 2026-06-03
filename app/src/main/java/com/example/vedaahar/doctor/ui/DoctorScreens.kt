package com.example.vedaahar.doctor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vedaahar.BackButton
import com.example.vedaahar.doctor.domain.DoctorConsultation
import com.example.vedaahar.doctor.domain.VerifiedDoctor
import com.example.vedaahar.doctor.viewmodel.DoctorViewModel
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray

private val DoctorGold = Color(0xFFE8C97B)
private val DoctorWarmCard = Color(0xFFFFFBF4)

@Composable
fun DoctorModuleRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoctorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    if (state.isLoggedIn) {
        DoctorDashboardScreen(onBack = onBack, viewModel = viewModel, modifier = modifier)
    } else {
        DoctorAuthScreen(
            onBack = onBack,
            onLogin = viewModel::login,
            onRegister = viewModel::register,
            modifier = modifier
        )
    }
}

@Composable
fun PatientVerifiedDoctorsSection(
    doctors: List<VerifiedDoctor>,
    onConsultNow: (VerifiedDoctor) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Verified Ayurvedic Doctors")
        doctors.forEach { doctor ->
            DoctorPatientCard(doctor = doctor, onConsultNow = { onConsultNow(doctor) })
        }
    }
}

@Composable
private fun DoctorAuthScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var registering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var bams by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var clinic by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    DoctorSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DoctorBackButton(onBack)
            PremiumDoctorCard {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DoctorBadge(if (registering) "Doctor Registration" else "Doctor Login")
                    Text(
                        if (registering) "Join VedaAahar as a BAMS Doctor" else "Welcome back, Doctor",
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 31.sp,
                        lineHeight = 35.sp
                    )
                    Text(
                        if (registering) "Your profile appears in patient consult only after verification." else "Use email or phone with password to access consultations.",
                        color = SoftBlueGray,
                        lineHeight = 22.sp
                    )
                    if (registering) {
                        DoctorTextField("Full Name", name) { name = it }
                        DoctorTextField("Phone Number", phone) { phone = it }
                    }
                    DoctorTextField(if (registering) "Email" else "Email or Phone", email) { email = it }
                    DoctorTextField("Password", password, isPassword = true) { password = it }
                    if (registering) {
                        DoctorTextField("BAMS Registration Number", bams) { bams = it }
                        DoctorTextField("Specialization", specialization) { specialization = it }
                        DoctorTextField("Experience", experience) { experience = it }
                        DoctorTextField("Clinic Name", clinic) { clinic = it }
                        DoctorTextField("Consultation Fee", fee) { fee = it }
                        DoctorTextField("Bio/About", bio) { bio = it }
                        UploadPlaceholder("Profile Photo")
                        UploadPlaceholder("BAMS Degree Certificate")
                    }
                    Button(
                        onClick = if (registering) onRegister else onLogin,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
                    ) {
                        Text(if (registering) "Submit for Verification" else "Login", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { registering = !registering },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, BeigeBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DoctorWarmCard, contentColor = ForestGreen)
                    ) {
                        Text(if (registering) "Already registered? Login" else "New doctor? Register")
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorDashboardScreen(
    onBack: () -> Unit,
    viewModel: DoctorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Consultations", "Patients", "Diet Plans", "Profile")
    DoctorSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DoctorBackButton(onBack)
                when (tab) {
                    0 -> DashboardTab(viewModel)
                    1 -> ConsultationsTab(state.consultations)
                    2 -> PatientsTab(state.consultations)
                    3 -> DietPlansTab()
                    else -> ProfileTab(viewModel)
                }
            }
            NavigationBar(containerColor = DoctorWarmCard, modifier = Modifier.navigationBarsPadding()) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Filled.Home
                                    1 -> Icons.Filled.Favorite
                                    4 -> Icons.Filled.Person
                                    else -> Icons.Filled.Star
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTab(viewModel: DoctorViewModel) {
    val state by viewModel.uiState.collectAsState()
    HeaderCard("Doctor Dashboard", "Manage consultations, availability, and patient diet plans.")
    PremiumDoctorCard {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DoctorAvatar(state.fullName)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.fullName, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(state.specialization, color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(if (state.isVerified) "BAMS Verified" else "Verification Pending", color = ForestGreen, fontSize = 12.sp)
                }
                Switch(checked = state.isOnline, onCheckedChange = { viewModel.toggleOnline() })
            }
            MetricRow("Total consultations", state.consultations.size.toString())
            MetricRow("Availability", state.availability)
            MetricRow("Upcoming appointments", "Placeholder")
            MetricRow("Recent diet plans", state.dietPlans.size.toString())
        }
    }
    SectionTitle("Recent Patients")
    state.consultations.take(2).forEach { ConsultationCard(it) }
}

@Composable
private fun ConsultationsTab(items: List<DoctorConsultation>) {
    HeaderCard("Consultation Requests", "Review dosha reports, symptoms, notes, and diet plan needs.")
    items.forEach { ConsultationCard(it, expanded = true) }
}

@Composable
private fun PatientsTab(items: List<DoctorConsultation>) {
    HeaderCard("Patients", "Open patient records connected from the consult workflow.")
    items.forEach {
        PremiumDoctorCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(it.patientName, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                FlowLabels(listOf(it.doshaType, it.status, "Symptoms ready", "Diet history ready"))
                Text("Review: dosha report, symptoms, lifestyle data, and existing diet plan.", color = SoftBlueGray, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun DietPlansTab() {
    HeaderCard("Personalized Diet Generation", "Create Ayurvedic plans from prakriti, vikriti, agni, ama, lifestyle, and goals.")
    PremiumDoctorCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Prakriti", "Vikriti", "Agni", "Ama", "Breakfast", "Lunch", "Dinner", "Foods to eat", "Foods to avoid", "Hydration advice", "Yoga recommendation", "Sleep advice").forEach {
                DoctorTextField(it, "") {}
            }
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("Save Diet Plan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileTab(viewModel: DoctorViewModel) {
    val state by viewModel.uiState.collectAsState()
    var availability by remember(state.availability) { mutableStateOf(state.availability) }
    HeaderCard("Doctor Profile", "Update profile, verification details, and available timings.")
    PremiumDoctorCard {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DoctorAvatar(state.fullName)
            MetricRow("Name", state.fullName)
            MetricRow("Specialization", state.specialization)
            MetricRow("Clinic", state.clinicName)
            MetricRow("BAMS Verification", if (state.isVerified) "Verified" else "Pending")
            DoctorTextField("Available Timings", availability) { availability = it }
            Button(
                onClick = { viewModel.updateAvailability(availability) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("Update Availability", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DoctorPatientCard(doctor: VerifiedDoctor, onConsultNow: () -> Unit) {
    PremiumDoctorCard {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            DoctorAvatar(doctor.fullName)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(doctor.fullName, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(doctor.specialization, color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("${doctor.experience} • ${doctor.consultationFee}", color = SoftBlueGray, fontSize = 12.sp)
                Text(if (doctor.isOnline) "Online now" else "Offline", color = if (doctor.isOnline) ForestGreen else SoftBlueGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Button(
                onClick = onConsultNow,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("Consult", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConsultationCard(item: DoctorConsultation, expanded: Boolean = false) {
    PremiumDoctorCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DoctorAvatar(item.patientName)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.patientName, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(item.doshaType, color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                DoctorBadge(item.status)
            }
            Text(item.symptoms, color = SoftBlueGray, lineHeight = 20.sp)
            if (expanded) {
                FlowLabels(listOf("Dosha report", "Symptoms", "Lifestyle", "Existing diet plan"))
                DoctorTextField("Consultation notes", "") {}
                DoctorTextField("Wellness advice", "") {}
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
                ) {
                    Text("Generate Personalized Diet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(title: String, body: String) {
    PremiumDoctorCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DoctorBadge("VedaAahar Doctor")
            Text(title, color = DarkForestGreen, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp)
            Text(body, color = SoftBlueGray, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun DoctorSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF8ED), Cream, LightSage.copy(alpha = 0.5f))))
        ) {
            content()
        }
    }
}

@Composable
private fun PremiumDoctorCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = ForestGreen.copy(alpha = 0.12f), spotColor = DoctorGold.copy(alpha = 0.1f))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.72f)), RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DoctorWarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun DoctorTextField(label: String, value: String, isPassword: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        shape = RoundedCornerShape(18.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
    )
}

@Composable
private fun UploadPlaceholder(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LightSage.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, BeigeBorder), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text("Upload $text", color = ForestGreen, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DoctorBackButton(onBack: () -> Unit) {
    BackButton(onClick = onBack)
}

@Composable
private fun DoctorAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(ForestGreen, SageGreen))),
        contentAlignment = Alignment.Center
    ) {
        Text(name.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString(""), color = PureWhite, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DoctorBadge(text: String) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(LightSage)
            .border(BorderStroke(1.dp, BeigeBorder), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        color = ForestGreen,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
    )
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SoftBlueGray)
        Text(value, color = DarkForestGreen, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowLabels(items: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { DoctorBadge(it) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.7.sp)
}
