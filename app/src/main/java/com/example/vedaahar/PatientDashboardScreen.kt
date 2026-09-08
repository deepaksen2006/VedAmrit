package com.example.vedaahar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray
import com.example.vedaahar.dosha.DoshaResultStore
import com.example.vedaahar.dosha.SavedDoshaAssessment
import com.example.vedaahar.doctor.data.DoctorRepository
import com.example.vedaahar.doctor.ui.PatientVerifiedDoctorsSection
import com.example.vedaahar.document.ui.UploadMedicalDocumentSection

private val DashboardGold = Color(0xFFE8C97B)
private val DashboardWarmCard = Color(0xFFFFFBF4)
private val DashboardGreenGlow = Color(0xFF90C987)
private val DashboardInkBrown = Color(0xFF2B1D18)
private val DashboardHerbalDeep = Color(0xFF214B2B)
private val DashboardOlive = Color(0xFF4B6334)
private val FloatingNavSurface = Color(0xFFEDEDE8)
private val FloatingNavInactive = Color(0xFF62695F)
private val CinzelDecorative = FontFamily(Font(R.font.cinzel_decorative_regular))
private val WellnessParchment = Color(0xFFF7F3EC)
private val WellnessForest = Color(0xFF1C2A1A)
private val WellnessSage = Color(0xFF5E8B52)
private val WellnessAmber = Color(0xFFA07850)
private val WellnessSlate = Color(0xFF527B8B)
private val WellnessMuted = Color(0xFF687B62)
private const val DashboardPrefsName = "vedaahaar_onboarding"
private const val DashboardPatientFullNameKey = "patient_full_name"

private val NutritionLeafIcon: ImageVector = ImageVector.Builder(
    name = "NutritionLeafIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(5f, 12.5f)
        curveTo(5f, 7.8f, 9.7f, 4.5f, 17.8f, 5.2f)
        curveTo(18.3f, 12.9f, 14.1f, 18.5f, 9.2f, 18.5f)
        curveTo(6.7f, 18.5f, 5f, 16.2f, 5f, 12.5f)
        moveTo(8.2f, 15.8f)
        curveTo(10.8f, 12.5f, 13.3f, 10.1f, 16.4f, 7.8f)
        moveTo(13.2f, 18.5f)
        curveTo(13.2f, 20f, 12.2f, 21f, 10.8f, 21f)
        moveTo(18.3f, 10.7f)
        curveTo(20f, 11.4f, 20.9f, 12.9f, 20.5f, 14.5f)
        curveTo(20.1f, 16.2f, 18.5f, 17.1f, 16.8f, 16.7f)
    }
}.build()

private fun patientFirstName(fullName: String): String {
    return fullName
        .trim()
        .split(Regex("\\s+"))
        .firstOrNull { it.isNotBlank() }
        ?.replaceFirstChar { char -> char.titlecase() }
        ?: "Friend"
}

private fun patientInitials(fullName: String): String {
    val parts = fullName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "VA"
    }
}

@Composable
fun PatientDashboardScreen(
    modifier: Modifier = Modifier,
    onShoppingClick: () -> Unit = {},
    onYogaMeditationClick: () -> Unit = {},
    onHealthReminderClick: () -> Unit = {},
    onIngredientBookClick: () -> Unit = {},
    onCommunityCareClick: () -> Unit = {},
    onRetakeDoshaClick: () -> Unit = {},
    onDietPlanClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("Home") }
    val context = LocalContext.current
    val currentDoshaResult = DoshaResultStore.current(context)
    val patientFullName = remember {
        context.getSharedPreferences(DashboardPrefsName, android.content.Context.MODE_PRIVATE)
            .getString(DashboardPatientFullNameKey, null)
            ?.trim()
            .orEmpty()
    }

    val dashboardBackground = if (selectedTab == "Wellness") {
        Brush.verticalGradient(listOf(WellnessParchment, Color(0xFFFBF7EF), WellnessParchment))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFF8ED), Cream, LightSage.copy(alpha = 0.55f)))
    }

    Surface(modifier = modifier.fillMaxSize(), color = if (selectedTab == "Wellness") WellnessParchment else Cream) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dashboardBackground)
        ) {
            if (selectedTab != "Wellness") {
                LuxuryAmbientBackground()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    "Wellness" -> WellnessTabContent(
                        onYogaMeditationClick = onYogaMeditationClick,
                        onHealthReminderClick = onHealthReminderClick,
                        onIngredientBookClick = onIngredientBookClick
                    )
                    "Consult" -> ConsultTabContent(onCommunityCareClick = onCommunityCareClick)
                    else -> HomeTabContent(
                        currentDoshaResult = currentDoshaResult,
                        patientFullName = patientFullName,
                        onRetakeDoshaClick = onRetakeDoshaClick,
                        onDietPlanClick = onDietPlanClick,
                        onIngredientBookClick = onIngredientBookClick
                    )
                }
                Spacer(modifier = Modifier.height(122.dp))
            }

            FloatingBottomNavigationBar(
                selectedLabel = selectedTab,
                onTabSelected = {
                    if (it == "Shopping") {
                        onShoppingClick()
                    } else {
                        selectedTab = it
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun HomeTabContent(
    currentDoshaResult: SavedDoshaAssessment?,
    patientFullName: String,
    onRetakeDoshaClick: () -> Unit,
    onDietPlanClick: () -> Unit,
    onIngredientBookClick: () -> Unit
) {
    LuxuryEditorialHeader(patientFullName = patientFullName)
    GreetingSection(currentDoshaResult = currentDoshaResult, patientFullName = patientFullName)
    UploadMedicalDocumentSection()
    PremiumFeatureMosaic(
        currentDoshaResult = currentDoshaResult,
        onRetakeDoshaClick = onRetakeDoshaClick,
        onDietPlanClick = onDietPlanClick,
        onIngredientBookClick = onIngredientBookClick
    )
}

@Composable
private fun WellnessTabContent(
    onYogaMeditationClick: () -> Unit,
    onHealthReminderClick: () -> Unit,
    onIngredientBookClick: () -> Unit
) {
    WellnessToolsHeader()
    WellnessAvailabilityBadge()
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "YOUR PRACTICE",
        color = WellnessMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 3.4.sp
    )
    WellnessToolCard(
        title = "Yoga & Meditation",
        description = "Practice guided breathwork and calming daily movement.",
        icon = "🧘",
        accent = WellnessSage,
        onClick = onYogaMeditationClick
    )
    WellnessToolCard(
        title = "Ingredient Book",
        description = "Browse herbs, rituals, and safe-use natural wellness notes.",
        icon = "🌿",
        accent = WellnessAmber,
        onClick = onIngredientBookClick
    )
    WellnessToolCard(
        title = "Health Reminder",
        description = "Manage diet, yoga, medicine, and exercise reminders.",
        icon = "🔔",
        accent = WellnessSlate,
        onClick = onHealthReminderClick
    )
}

@Composable
private fun WellnessToolsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Good morning",
                color = WellnessMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "Wellness\nTools",
                color = WellnessForest,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 31.sp,
                lineHeight = 32.sp
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(WellnessSage),
            contentAlignment = Alignment.Center
        ) {
            Text("W", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun WellnessAvailabilityBadge() {
    Text(
        text = "3 tools available",
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(WellnessSage.copy(alpha = 0.2f))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        color = WellnessSage,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun WellnessToolCard(
    title: String,
    description: String,
    icon: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        label = "wellnessToolCardScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFFCF6))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.36f)), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = accent.copy(alpha = 0.15f),
                radius = 86f,
                center = Offset(size.width - 12f, 4f)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(4.dp)
                .background(accent)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, top = 20.dp, end = 58.dp, bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.17f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 17.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                color = WellnessForest,
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                color = WellnessMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Light,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(WellnessForest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = PureWhite,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ConsultTabContent(onCommunityCareClick: () -> Unit) {
    SectionTitle("Consult & Care")
    DashboardServiceCard(
        title = "Community Care",
        body = "Discover nearby hospitals, NGOs, and Ayurvedic support centers.",
        onClick = onCommunityCareClick
    )
    PatientVerifiedDoctorsSection(doctors = DoctorRepository().verifiedDoctors())
}

@Composable
private fun LuxuryAmbientBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(DashboardGold.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.1f),
                radius = size.width * 0.7f
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.18f, size.height * 0.1f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(DashboardGreenGlow.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.34f),
                radius = size.width * 0.5f
            ),
            radius = size.width * 0.5f,
            center = Offset(size.width * 0.88f, size.height * 0.34f)
        )
        repeat(9) { index ->
            val x = size.width * ((index * 29 % 100) / 100f)
            val y = size.height * (0.08f + (index * 13 % 72) / 100f)
            drawLine(
                color = DashboardGold.copy(alpha = 0.07f),
                start = Offset(x, y),
                end = Offset(x + size.width * 0.12f, y - size.height * 0.06f),
                strokeWidth = 1.4f,
                cap = StrokeCap.Round
            )
            drawCircle(DashboardGold.copy(alpha = 0.06f), radius = 18f, center = Offset(x + 18f, y - 8f))
        }
    }
}

@Composable
private fun LuxuryEditorialHeader(patientFullName: String) {
    val initials = patientInitials(patientFullName)
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, end = 82.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VEDAAHAAR",
                color = DashboardHerbalDeep,
                fontFamily = CinzelDecorative,
                fontWeight = FontWeight.Normal,
                fontSize = 38.sp,
                lineHeight = 40.sp,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "॥ आयुर्वेदो अमृतं ॥",
                color = DashboardInkBrown,
                fontFamily = FontFamily.Serif,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Preserving wellness through Ayurveda",
                color = DashboardInkBrown.copy(alpha = 0.86f),
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LuxuryDivider()
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF9EE).copy(alpha = 0.86f))
                    .border(BorderStroke(1.dp, DashboardGold.copy(alpha = 0.45f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color(0xFF75521E), modifier = Modifier.size(19.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-5).dp, y = 6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF94611C))
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(14.dp, CircleShape, ambientColor = DashboardGold.copy(alpha = 0.24f), spotColor = ForestGreen.copy(alpha = 0.14f))
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(DashboardGold, Color(0xFF8F6927))))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(DashboardInkBrown, DashboardHerbalDeep))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Color(0xFFFFF4D6), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun LuxuryDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.height(1.dp).width(88.dp).background(DashboardGold.copy(alpha = 0.55f)))
        Text("◇", color = DashboardGold, modifier = Modifier.padding(horizontal = 6.dp), fontSize = 12.sp)
        Box(modifier = Modifier.height(1.dp).width(88.dp).background(DashboardGold.copy(alpha = 0.55f)))
    }
}

@Composable
private fun GreetingSection(currentDoshaResult: SavedDoshaAssessment?, patientFullName: String) {
    val dosha = currentDoshaResult?.profileName ?: "Kapha"
    val firstName = patientFirstName(patientFullName)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hello, ",
                    color = DashboardHerbalDeep,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 32.sp
                )
                Text(
                    text = "$firstName.",
                    color = Color(0xFF9B762B),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 32.sp
                )
                Spacer(modifier = Modifier.width(7.dp))
                Icon(NutritionLeafIcon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Your wellness plan is ready • $dosha",
                color = DashboardInkBrown.copy(alpha = 0.86f),
                fontSize = 15.sp,
                lineHeight = 19.sp
            )
        }
        KaphaActiveChip(dosha = dosha)
    }
}

@Composable
private fun KaphaActiveChip(dosha: String) {
    Row(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(50), ambientColor = DashboardGreenGlow.copy(alpha = 0.28f), spotColor = ForestGreen.copy(alpha = 0.16f))
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(Color(0xFFEAF1D9), Color(0xFFF6F1DE))))
            .border(BorderStroke(1.dp, Color(0xFFD7D9B8)), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFF8F1DC))
                .border(BorderStroke(1.dp, DashboardGold.copy(alpha = 0.6f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(NutritionLeafIcon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("$dosha Active", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun TopFeaturesHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Top features", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("See all", color = ForestGreen, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun PremiumFeatureMosaic(
    currentDoshaResult: SavedDoshaAssessment?,
    onRetakeDoshaClick: () -> Unit,
    onDietPlanClick: () -> Unit,
    onIngredientBookClick: () -> Unit
) {
    val currentDosha = currentDoshaResult?.profileName ?: "Kapha"

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 340.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DietHeroCard(onClick = onDietPlanClick, modifier = Modifier.fillMaxWidth())
                SymptomCard(modifier = Modifier.fillMaxWidth())
                DoshaMiniCard(currentDosha = currentDosha, onClick = onRetakeDoshaClick, modifier = Modifier.fillMaxWidth())
                IngredientCard(onClick = onIngredientBookClick, modifier = Modifier.fillMaxWidth())
                LifestyleCard(modifier = Modifier.fillMaxWidth())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DietHeroCard(onClick = onDietPlanClick, modifier = Modifier.weight(1.32f))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SymptomCard(modifier = Modifier.fillMaxWidth())
                        DoshaMiniCard(currentDosha = currentDosha, onClick = onRetakeDoshaClick, modifier = Modifier.fillMaxWidth())
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    IngredientCard(onClick = onIngredientBookClick, modifier = Modifier.weight(1f))
                    LifestyleCard(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DietHeroCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(256.dp)
            .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = ForestGreen.copy(alpha = 0.18f), spotColor = DashboardGold.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(DashboardHerbalDeep, Color(0xFF285B33))))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(114.dp)
                .align(Alignment.TopEnd)
                .offset(x = 34.dp, y = (-24).dp)
                .clip(CircleShape)
                .background(DashboardOlive.copy(alpha = 0.86f))
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 30.dp)
                .clip(CircleShape)
                .background(ForestGreen.copy(alpha = 0.35f))
        )
        BotanicalLineArt(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(126.dp)
                .offset(x = 26.dp, y = 28.dp),
            tint = DashboardGold.copy(alpha = 0.34f)
        )
        MealBowlIllustration(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(128.dp)
                .offset(x = 30.dp, y = 38.dp)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            FeatureNumberPill("No. 1", dark = false)
            Spacer(modifier = Modifier.height(58.dp))
            Text(
                "Personalised\nDiet Plan",
                color = PureWhite,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "AI-generated Kapha meal guidance for your daily rhythm.",
                color = PureWhite.copy(alpha = 0.78f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = DashboardGold, contentColor = DashboardInkBrown),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("Generate Plan", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SymptomCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(176.dp)
            .shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = DashboardInkBrown.copy(alpha = 0.16f), spotColor = DashboardGold.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(26.dp))
            .background(DashboardInkBrown)
            .padding(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.TopEnd)
                .offset(x = 26.dp, y = (-34).dp)
                .clip(CircleShape)
                .background(Color(0xFF6D3D31).copy(alpha = 0.78f))
        )
        AiCircuitLines(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(116.dp)
                .offset(x = 10.dp, y = 12.dp)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            FeatureNumberPill("No. 2", dark = true)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Symptom\nEngine",
                color = PureWhite,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                "AI-guided next steps\nbased on your symptoms.",
                color = PureWhite.copy(alpha = 0.7f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(14.dp))
            MiniCta("Check now", background = Color(0xFFD36A46), content = PureWhite)
        }
    }
}

@Composable
private fun DoshaMiniCard(
    currentDosha: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(68.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = ForestGreen.copy(alpha = 0.12f), spotColor = DashboardGold.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FCF5)),
        border = BorderStroke(1.dp, Color(0xFFD7E5CF))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LightSage),
                contentAlignment = Alignment.Center
            ) {
                Icon(NutritionLeafIcon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dosha Test", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text("Current Dosha: $currentDosha", color = SageGreen, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun IngredientCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BottomFeatureCard(
        title = "Ingredient Book",
        body = "240+ herbs & rituals",
        action = "Browse",
        icon = Icons.Filled.Star,
        modifier = modifier,
        container = Color(0xFFF2FAF1),
        border = Color(0xFFD3E7D0),
        onClick = onClick
    )
}

@Composable
private fun LifestyleCard(modifier: Modifier = Modifier) {
    BottomFeatureCard(
        title = "Lifestyle Calc",
        body = "Score your wellness",
        action = "Calculate",
        icon = NutritionLeafIcon,
        modifier = modifier,
        container = Color(0xFFFFFBED),
        border = Color(0xFFEEDDAF)
    )
}

@Composable
private fun BottomFeatureCard(
    title: String,
    body: String,
    action: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    container: Color,
    border: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(112.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = ForestGreen.copy(alpha = 0.1f), spotColor = DashboardGold.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, border)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BotanicalLineArt(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(76.dp)
                    .offset(x = 18.dp, y = 20.dp),
                tint = ForestGreen.copy(alpha = 0.13f)
            )
            Column(modifier = Modifier.padding(13.dp)) {
                Icon(icon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(title, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(3.dp))
                Text(body, color = SoftBlueGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                MiniCta(text = action, background = LightSage, content = ForestGreen)
            }
        }
    }
}

@Composable
private fun WellnessSnapshotSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = DashboardGold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Today’s Wellness Snapshot", color = DashboardHerbalDeep, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View insights", color = DashboardInkBrown, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(5.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DashboardInkBrown, modifier = Modifier.size(15.dp))
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = DashboardGold.copy(alpha = 0.12f), spotColor = ForestGreen.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF2).copy(alpha = 0.94f)),
            border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.65f))
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                if (maxWidth < 360.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SnapshotMetric("Wellness Score", "78", "/100", "Good", 0.78f)
                        SnapshotMetric("Sleep Quality", "7h 30m", "", "Good", 0.72f)
                        SnapshotMetric("Energy Level", "High", "", "Keep going!", 0.88f)
                        SnapshotMetric("Mind State", "Calm", "", "Balanced", 0.82f)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SnapshotMetric("Wellness Score", "78", "/100", "Good", 0.78f, Modifier.weight(1f))
                        SnapshotMetric("Sleep Quality", "7h 30m", "", "Good", 0.72f, Modifier.weight(1f))
                        SnapshotMetric("Energy Level", "High", "", "Keep going!", 0.88f, Modifier.weight(1f))
                        SnapshotMetric("Mind State", "Calm", "", "Balanced", 0.82f, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotMetric(
    label: String,
    value: String,
    suffix: String,
    caption: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = DashboardInkBrown, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(8.dp))
        CircularWellnessProgress(progress = progress, modifier = Modifier.size(42.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = DashboardHerbalDeep, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1)
            if (suffix.isNotBlank()) {
                Text(suffix, color = DashboardInkBrown, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        Text(caption, color = ForestGreen, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CircularWellnessProgress(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 7.dp.toPx()
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        drawArc(
            color = LightSage,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(ForestGreen, DashboardGold, ForestGreen)),
            startAngle = -90f,
            sweepAngle = progress.coerceIn(0f, 1f) * 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun BotanicalLineArt(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier) {
        val stemStart = Offset(size.width * 0.18f, size.height * 0.9f)
        val stemEnd = Offset(size.width * 0.78f, size.height * 0.14f)
        drawLine(tint, stemStart, stemEnd, strokeWidth = 2.2f, cap = StrokeCap.Round)
        repeat(6) { index ->
            val t = (index + 1) / 7f
            val x = stemStart.x + (stemEnd.x - stemStart.x) * t
            val y = stemStart.y + (stemEnd.y - stemStart.y) * t
            val side = if (index % 2 == 0) -1f else 1f
            drawCircle(tint, radius = size.minDimension * 0.06f, center = Offset(x + side * size.width * 0.11f, y))
            drawLine(tint, Offset(x, y), Offset(x + side * size.width * 0.12f, y - size.height * 0.04f), strokeWidth = 1.8f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun MealBowlIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(Color(0xFFB57D2D).copy(alpha = 0.9f), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.58f, size.height * 0.62f))
        drawCircle(Color(0xFFF2D38A), radius = size.minDimension * 0.32f, center = Offset(size.width * 0.58f, size.height * 0.58f))
        drawCircle(Color(0xFFDEE08D), radius = size.minDimension * 0.08f, center = Offset(size.width * 0.46f, size.height * 0.48f))
        drawCircle(Color(0xFFE9B95F), radius = size.minDimension * 0.07f, center = Offset(size.width * 0.62f, size.height * 0.44f))
        drawCircle(Color(0xFF5E8E43), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.72f, size.height * 0.52f))
    }
}

@Composable
private fun AiCircuitLines(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val lineColor = Color(0xFFD86F4C).copy(alpha = 0.27f)
        repeat(5) { index ->
            val y = size.height * (0.18f + index * 0.15f)
            drawLine(lineColor, Offset(size.width * 0.1f, y), Offset(size.width * 0.9f, y + size.height * 0.08f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawCircle(lineColor, radius = 3.2f, center = Offset(size.width * (0.2f + index * 0.12f), y))
        }
    }
}

@Composable
private fun FeatureNumberPill(text: String, dark: Boolean) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (dark) Color(0xFF6D3D31) else DashboardOlive)
            .border(BorderStroke(1.dp, DashboardGold.copy(alpha = 0.45f)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = DashboardGold,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp
    )
}

@Composable
private fun MiniCta(text: String, background: Color, content: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = content, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        Spacer(modifier = Modifier.width(5.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun DashboardFeatureCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Star,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(144.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = ForestGreen.copy(alpha = 0.1f), spotColor = DashboardGold.copy(alpha = 0.08f)),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardWarmCard),
        border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.72f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(LightSage),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = DarkForestGreen, fontWeight = FontWeight.Bold, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp))
            Text(body, color = SoftBlueGray, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DashboardServiceCard(title: String, body: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(ForestGreen, Color(0xFF48723A))))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column {
            Text(title, color = PureWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(body, color = PureWhite.copy(alpha = 0.86f), lineHeight = 21.sp)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.8.sp)
}

@Composable
private fun DashboardChip(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(LightSage)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.55f)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        color = ForestGreen,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp
    )
}

@Composable
private fun PremiumDashboardCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = DashboardGold.copy(alpha = 0.1f), spotColor = DashboardGreenGlow.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardWarmCard),
        border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun RoundIcon(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(LightSage),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
data class DashboardNavItem(
    val label: String,
    val icon: ImageVector
)

@Composable
private fun FloatingBottomNavigationBar(
    selectedLabel: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        DashboardNavItem("Home", Icons.Filled.Home),
        DashboardNavItem("Wellness", Icons.Filled.Favorite),
        DashboardNavItem("Consult", Icons.Filled.Add),
        DashboardNavItem("Shopping", Icons.Filled.ShoppingCart),
        DashboardNavItem("Profile", Icons.Filled.Person)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(42.dp),
                ambientColor = ForestGreen.copy(alpha = 0.18f),
                spotColor = ForestGreen.copy(alpha = 0.14f)
        ),
        shape = RoundedCornerShape(42.dp),
        color = FloatingNavSurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.82f)),
        tonalElevation = 10.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                FloatingNavItem(
                    item = item,
                    selected = selectedLabel == item.label,
                    onClick = { onTabSelected(item.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: DashboardNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val iconContainerSize by animateDpAsState(
        targetValue = if (selected) 58.dp else 42.dp,
        label = "navIconSize"
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 30.dp else 26.dp,
        label = "navInnerIconSize"
    )
    val activeColor by animateColorAsState(
        targetValue = if (selected) ForestGreen else Color.Transparent,
        label = "navActiveColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) PureWhite else FloatingNavInactive,
        label = "navIconColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) ForestGreen else FloatingNavInactive,
        label = "navTextColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        label = "navPressScale"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconContainerSize)
                .scale(scale)
                .clip(CircleShape)
                .background(activeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Text(
            text = item.label,
            modifier = Modifier.padding(top = 2.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}




