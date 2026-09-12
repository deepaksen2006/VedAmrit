package com.example.vedaahar.dosha

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vedaahar.BackButton
import com.example.vedaahar.R
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GoldMist = Color(0xFFE8C97B)
private val WarmCard = Color(0xFFFFFBF4)
private val GreenGlow = Color(0xFF90C987)

private val doshaArchetypes = listOf(
    "Vata-Pitta",
    "Pitta-Kapha",
    "Vata-Kapha",
    "Tri-Doshic",
    "Vata",
    "Pitta",
    "Kapha"
)

@Composable
fun RetakeDoshaAssessmentRoute(
    onBackToDashboard: () -> Unit,
    onContinueToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoshaAssessmentViewModel = viewModel(
        factory = DoshaAssessmentViewModel.Factory(LocalContext.current, "dosha_retake_assessment")
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentResult = DoshaResultStore.current(context)
    val historyCount = DoshaResultStore.historyCount(context)

    RetakeDoshaAssessmentScreen(
        uiState = uiState,
        currentResult = currentResult,
        historyCount = historyCount,
        onBackToDashboard = onBackToDashboard,
        onContinueToDashboard = onContinueToDashboard,
        onSelectConstitution = viewModel::selectConstitution,
        modifier = modifier
    )
}

@Composable
fun DoshaAssessmentRoute(
    onBackToWelcome: () -> Unit,
    onContinueToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoshaAssessmentViewModel = viewModel(
        factory = DoshaAssessmentViewModel.Factory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    DoshaAssessmentScreen(
        uiState = uiState,
        onBackToWelcome = onBackToWelcome,
        onContinueToDashboard = onContinueToDashboard,
        onSelectConstitution = viewModel::selectConstitution,
        modifier = modifier
    )
}

@Composable
fun DoshaAssessmentScreen(
    uiState: DoshaAssessmentUiState,
    onBackToWelcome: () -> Unit,
    onContinueToDashboard: () -> Unit,
    onSelectConstitution: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeProfileName by remember(uiState.result?.profileName) {
        mutableStateOf(uiState.result?.profileName ?: "Vata-Pitta")
    }
    val activeResult = remember(activeProfileName) {
        DoshaScoringEngine.resultForProfile(activeProfileName)
    }

    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(modifier = Modifier.fillMaxSize()) {
            AmbientAyurvedaBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Assessment Header
                AssessmentHeader(onBackToWelcome = onBackToWelcome, badgeText = "Prakriti Profile")

                // Hero Card
                ConstitutionHeroCard()

                // Interactive Constitution Archetype Selector
                Text(
                    text = "EXPLORE AYURVEDIC CONSTITUTIONS",
                    color = SageGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )

                ScrollableTabRow(
                    selectedTabIndex = doshaArchetypes.indexOf(activeProfileName).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    contentColor = ForestGreen,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    doshaArchetypes.forEach { name ->
                        val isSelected = name == activeProfileName
                        Tab(
                            selected = isSelected,
                            onClick = {
                                activeProfileName = name
                                onSelectConstitution(name)
                            },
                            text = {
                                Text(
                                    text = name,
                                    color = if (isSelected) ForestGreen else SoftBlueGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                )
                            }
                        )
                    }
                }

                // Active Constitution Result Card
                ResultCard(
                    result = activeResult,
                    onSaveAndContinue = {
                        DoshaResultStore.save(context, activeResult)
                        onContinueToDashboard()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun RetakeDoshaAssessmentScreen(
    uiState: DoshaAssessmentUiState,
    currentResult: SavedDoshaAssessment?,
    historyCount: Int,
    onBackToDashboard: () -> Unit,
    onContinueToDashboard: () -> Unit,
    onSelectConstitution: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedProfileName by remember(currentResult?.profileName) {
        mutableStateOf(currentResult?.profileName ?: "Vata-Pitta")
    }
    val updatedResult = remember(selectedProfileName) {
        DoshaScoringEngine.resultForProfile(selectedProfileName)
    }

    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(modifier = Modifier.fillMaxSize()) {
            AmbientAyurvedaBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RetakeHeader(onBackToDashboard = onBackToDashboard)

                // Comparison Card
                RetakeComparisonCard(
                    currentResult = currentResult,
                    updatedResult = updatedResult,
                    historyCount = historyCount
                )

                // Constitution Switcher / Tuner
                Text(
                    text = "SELECT UPDATED CONSTITUTION",
                    color = SageGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )

                ScrollableTabRow(
                    selectedTabIndex = doshaArchetypes.indexOf(selectedProfileName).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    contentColor = ForestGreen,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    doshaArchetypes.forEach { name ->
                        val isSelected = name == selectedProfileName
                        Tab(
                            selected = isSelected,
                            onClick = {
                                selectedProfileName = name
                                onSelectConstitution(name)
                            },
                            text = {
                                Text(
                                    text = name,
                                    color = if (isSelected) ForestGreen else SoftBlueGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                )
                            }
                        )
                    }
                }

                // Profile Details Card
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PillBadge(text = "Updated Constitution Insights")
                        Text(
                            text = updatedResult.profileName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = DarkForestGreen,
                                fontFamily = FontFamily.Serif,
                                fontSize = 34.sp,
                                lineHeight = 38.sp
                            )
                        )
                        Text(
                            text = updatedResult.description.overview,
                            style = MaterialTheme.typography.bodyMedium.copy(color = SoftBlueGray, lineHeight = 22.sp)
                        )

                        Dosha.entries.forEach { dosha ->
                            ResultMeter(
                                dosha = dosha.displayName,
                                score = updatedResult.percentages.getValue(dosha),
                                maxScore = 100
                            )
                        }

                        ResultDetails(updatedResult.description)

                        Button(
                            onClick = {
                                DoshaResultStore.save(context, updatedResult)
                                onContinueToDashboard()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .shadow(10.dp, RoundedCornerShape(50), ambientColor = GreenGlow.copy(alpha = 0.22f), spotColor = GreenGlow.copy(alpha = 0.18f)),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
                        ) {
                            Text("Update Profile & Return to Dashboard", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AssessmentHeader(
    onBackToWelcome: () -> Unit,
    badgeText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.vedamrit_logo),
                contentDescription = "VedAmrit logo",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarmCard),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("VedAmrit", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Ayurvedic Constitution Analysis", color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.4.sp)
            }
        }

        BackButton(onClick = onBackToWelcome)
    }
}

@Composable
private fun RetakeHeader(onBackToDashboard: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.vedamrit_logo),
                contentDescription = "VedAmrit logo",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarmCard),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("VedAmrit", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Dosha Balance Tracker", color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.4.sp)
            }
        }

        BackButton(onClick = onBackToDashboard, text = "Dashboard")
    }
}

@Composable
private fun ConstitutionHeroCard() {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            FloatingLeafCluster(modifier = Modifier.align(Alignment.TopEnd).size(112.dp), alpha = 0.22f)
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PillBadge(text = "Ayurvedic Constitution Explorer")
                Text(
                    text = "Understand the Tridosha Blueprint of Your Body",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 32.sp
                    )
                )
                Text(
                    text = "In Ayurveda, every individual possesses a unique constitutional blend (Prakriti) of Vata (air/space), Pitta (fire/water), and Kapha (earth/water). Explore constitutional profiles below and save your active dosha.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SoftBlueGray,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniInfoCard(
                        title = "Vata",
                        body = "Movement & creativity",
                        modifier = Modifier.weight(1f)
                    )
                    MiniInfoCard(
                        title = "Pitta",
                        body = "Transformation & focus",
                        modifier = Modifier.weight(1f)
                    )
                    MiniInfoCard(
                        title = "Kapha",
                        body = "Structure & stamina",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(
    result: DoshaResult,
    onSaveAndContinue: () -> Unit
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp)) {
            PillBadge(text = "Active Constitution Profile", icon = "AI")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "AYURVEDIC CONSTITUTION",
                style = MaterialTheme.typography.labelSmall.copy(color = SageGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.profileName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontSize = 38.sp,
                        lineHeight = 44.sp
                    )
                )
                DoshaAura(modifier = Modifier.size(72.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = result.description.overview,
                style = MaterialTheme.typography.bodyMedium.copy(color = SoftBlueGray, lineHeight = 23.sp)
            )
            Spacer(modifier = Modifier.height(18.dp))

            Dosha.entries.forEach { dosha ->
                ResultMeter(
                    dosha = dosha.displayName,
                    score = result.percentages.getValue(dosha),
                    maxScore = 100
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultChip("Dominant: ${result.dominant.displayName}")
                result.secondary?.let { ResultChip("Secondary: ${it.displayName}") }
                ResultChip(result.profileType.name)
            }

            Spacer(modifier = Modifier.height(20.dp))
            ResultDetails(result.description)
            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onSaveAndContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(10.dp, RoundedCornerShape(50), ambientColor = GreenGlow.copy(alpha = 0.22f), spotColor = GreenGlow.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("Save Constitution & Continue to Dashboard", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RetakeComparisonCard(
    currentResult: SavedDoshaAssessment?,
    updatedResult: DoshaResult,
    historyCount: Int
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PillBadge(text = "Dosha Balance Tracker")
            Text(
                text = "Previous Constitution vs Updated",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp
            )
            Text(
                text = "Saved assessments in history: $historyCount • Last saved: ${formatDoshaDateTime(currentResult?.savedAtMillis ?: System.currentTimeMillis())}",
                color = SageGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ComparisonDoshaCard(
                    label = "Previous Saved",
                    name = currentResult?.profileName ?: "None Saved",
                    modifier = Modifier.weight(1f)
                )
                ComparisonDoshaCard(
                    label = "Updated Target",
                    name = updatedResult.profileName,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Previous percentages vs Updated percentages", color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 1.3.sp)
            Dosha.entries.forEach { dosha ->
                ComparisonMeter(
                    dosha = dosha.displayName,
                    previousPercent = currentResult?.percentages?.get(dosha) ?: 0,
                    newPercent = updatedResult.percentages.getValue(dosha)
                )
            }
        }
    }
}

@Composable
private fun ComparisonDoshaCard(label: String, name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(PureWhite, LightSage.copy(alpha = 0.65f))))
            .border(BorderStroke(1.dp, BeigeBorder), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label.uppercase(), color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp)
        Text(name, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun ComparisonMeter(dosha: String, previousPercent: Int, newPercent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dosha, color = DarkForestGreen, fontWeight = FontWeight.Bold)
            Text("$previousPercent% → $newPercent%", color = ForestGreen, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { newPercent / 100f },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
            color = ForestGreen,
            trackColor = LightSage
        )
    }
}

@Composable
private fun ResultDetails(description: DoshaResultDescription) {
    val rows = listOf(
        "Personality Traits" to description.personalityTraits,
        "Digestion & Agni" to description.digestionStyle,
        "Emotional Tendencies" to description.emotionalTendencies,
        "Energy & Stamina" to description.energyBehavior,
        "Constitutional Strengths" to description.strengths,
        "Imbalance Indicators" to description.imbalanceRisks,
        "Balancing Dinacharya" to description.lifestyleSuggestions,
        "Food Philosophy (Pathya)" to description.foodRecommendations
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (title, body) ->
            MiniInfoCard(title = title, body = body)
        }
    }
}

@Composable
private fun PremiumCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = GoldMist.copy(alpha = 0.1f), spotColor = GreenGlow.copy(alpha = 0.08f))
            .border(
                BorderStroke(1.dp, Brush.linearGradient(listOf(PureWhite, BeigeBorder.copy(alpha = 0.7f), GoldMist.copy(alpha = 0.25f)))),
                RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun MiniInfoCard(title: String, body: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(PureWhite, LightSage.copy(alpha = 0.72f))))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.82f)), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(SageGreen.copy(alpha = 0.24f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(color = DarkForestGreen, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium.copy(color = SoftBlueGray, lineHeight = 20.sp, fontSize = 12.sp))
        }
    }
}

@Composable
private fun PillBadge(text: String, icon: String? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(LightSage, WarmCard)))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.55f)), RoundedCornerShape(50))
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Text(text = icon, color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = ForestGreen,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        )
    }
}

@Composable
private fun ResultMeter(dosha: String, score: Int, maxScore: Int) {
    val animated by animateFloatAsState(score.toFloat() / maxScore.toFloat(), tween(650, easing = FastOutSlowInEasing), label = "result-$dosha")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dosha, color = DarkForestGreen, fontWeight = FontWeight.Bold)
            Text("$score%", color = ForestGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
            color = ForestGreen,
            trackColor = LightSage
        )
    }
}

@Composable
private fun ResultChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(LightSage)
            .border(BorderStroke(1.dp, BeigeBorder), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = ForestGreen,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    )
}

fun formatDoshaDateTime(savedAtMillis: Long): String {
    if (savedAtMillis <= 0L) return "Not available"
    return SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(savedAtMillis))
}

@Composable
private fun DoshaAura(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "aura").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "aura-pulse"
    )
    Canvas(modifier = modifier.scale(pulse)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GreenGlow.copy(alpha = 0.26f), Color.Transparent),
                center = center,
                radius = size.minDimension / 2f
            ),
            radius = size.minDimension / 2f
        )
        drawCircle(GoldMist.copy(alpha = 0.5f), radius = size.minDimension * 0.36f, style = Stroke(width = 2f))
        drawCircle(ForestGreen.copy(alpha = 0.16f), radius = size.minDimension * 0.24f)
        drawLine(ForestGreen.copy(alpha = 0.5f), Offset(center.x, center.y - 22f), Offset(center.x, center.y + 26f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawCircle(ForestGreen.copy(alpha = 0.7f), radius = 7f, center = Offset(center.x, center.y - 31f))
        drawCircle(GoldMist.copy(alpha = 0.38f), radius = 2.4f, center = Offset(size.width * 0.2f, size.height * 0.25f))
        drawCircle(GoldMist.copy(alpha = 0.34f), radius = 2f, center = Offset(size.width * 0.82f, size.height * 0.32f))
        drawCircle(GoldMist.copy(alpha = 0.28f), radius = 1.8f, center = Offset(size.width * 0.72f, size.height * 0.72f))
    }
}

@Composable
private fun AmbientAyurvedaBackground() {
    val drift by rememberInfiniteTransition(label = "background-drift").animateFloat(
        initialValue = -16f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Cream, Color(0xFFFFFCF6), Color(0xFFF2EBDD))))
        drawCircle(Brush.radialGradient(listOf(GreenGlow.copy(alpha = 0.18f), Color.Transparent)), radius = size.width * 0.55f, center = Offset(size.width * 0.78f, size.height * 0.18f + drift))
        drawCircle(Brush.radialGradient(listOf(GoldMist.copy(alpha = 0.18f), Color.Transparent)), radius = size.width * 0.5f, center = Offset(size.width * 0.12f, size.height * 0.72f - drift))
        repeat(22) { index ->
            val x = (index * 47 % size.width.toInt()).toFloat()
            val y = ((index * 83 % size.height.toInt()).toFloat() + drift * (if (index % 2 == 0) 1f else -1f))
            drawCircle(GoldMist.copy(alpha = 0.18f), radius = if (index % 3 == 0) 3.4f else 2.1f, center = Offset(x, y))
        }
    }
}

@Composable
private fun FloatingLeafCluster(modifier: Modifier = Modifier, alpha: Float = 0.4f) {
    Canvas(modifier = modifier) {
        repeat(5) { index ->
            val left = size.width * (0.18f + index * 0.12f)
            val top = size.height * (0.18f + (index % 2) * 0.18f)
            drawOval(
                color = SageGreen.copy(alpha = alpha),
                topLeft = Offset(left, top),
                size = Size(size.width * 0.18f, size.height * 0.34f)
            )
            drawLine(
                color = ForestGreen.copy(alpha = alpha),
                start = Offset(left + size.width * 0.09f, top + size.height * 0.26f),
                end = Offset(size.width * 0.86f, size.height * 0.82f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DoshaAssessmentPreview() {
    DoshaAssessmentScreen(
        uiState = DoshaAssessmentUiState(),
        onBackToWelcome = {},
        onContinueToDashboard = {}
    )
}
