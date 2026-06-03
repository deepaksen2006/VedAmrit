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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GoldMist = Color(0xFFE8C97B)
private val WarmCard = Color(0xFFFFFBF4)
private val GreenGlow = Color(0xFF90C987)

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
        onContinueToDashboard = {
            viewModel.restart()
            onContinueToDashboard()
        },
        onOptionSelected = viewModel::selectOption,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
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
        onOptionSelected = viewModel::selectOption,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onRestart = viewModel::restart,
        modifier = modifier
    )
}

@Composable
fun DoshaAssessmentScreen(
    uiState: DoshaAssessmentUiState,
    onBackToWelcome: () -> Unit,
    onContinueToDashboard: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(modifier = Modifier.fillMaxSize()) {
            AmbientAyurvedaBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                AssessmentHeader(onBackToWelcome = onBackToWelcome, questionText = "Q${uiState.currentQuestion.id} of ${uiState.questions.size}")
                Spacer(modifier = Modifier.height(24.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 760.dp
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            IntroCard(modifier = Modifier.weight(0.9f))
                            AssessmentStage(
                                uiState = uiState,
                                onOptionSelected = onOptionSelected,
                                onContinueToDashboard = onContinueToDashboard,
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onRestart = onRestart,
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    } else {
                        AssessmentStage(
                            uiState = uiState,
                            onOptionSelected = onOptionSelected,
                            onContinueToDashboard = onContinueToDashboard,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onRestart = onRestart
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
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
    onOptionSelected: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(modifier = Modifier.fillMaxSize()) {
            AmbientAyurvedaBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                RetakeHeader(onBackToDashboard = onBackToDashboard)
                Spacer(modifier = Modifier.height(24.dp))

                if (currentResult == null) {
                    MissingDoshaState(onBackToDashboard = onBackToDashboard)
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val wide = maxWidth >= 760.dp
                        if (wide) {
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                RetakeIntroPanel(
                                    currentResult = currentResult,
                                    historyCount = historyCount,
                                    modifier = Modifier.weight(0.95f)
                                )
                                RetakeAssessmentStage(
                                    uiState = uiState,
                                    onOptionSelected = onOptionSelected,
                                    onContinueToDashboard = onContinueToDashboard,
                                    onPrevious = onPrevious,
                                    onNext = onNext,
                                    modifier = Modifier.weight(1.15f)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                RetakeIntroPanel(currentResult = currentResult, historyCount = historyCount)
                                RetakeAssessmentStage(
                                    uiState = uiState,
                                    onOptionSelected = onOptionSelected,
                                    onContinueToDashboard = onContinueToDashboard,
                                    onPrevious = onPrevious,
                                    onNext = onNext
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
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
                painter = painterResource(id = R.drawable.vedaahaar_logo),
                contentDescription = "VedaAhaar logo",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarmCard),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("VedaAhaar", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Dashboard Wellness Tool", color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.6.sp)
            }
        }

        BackButton(onClick = onBackToDashboard, text = "Dashboard")
    }
}

@Composable
private fun MissingDoshaState(onBackToDashboard: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PillBadge(text = "Dosha Data Missing")
            Text(
                text = "Your current dosha profile is not available yet.",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 32.sp
            )
            Text(
                text = "Please complete the first-time dosha assessment before using the dashboard retake flow.",
                color = SoftBlueGray,
                lineHeight = 22.sp
            )
            Button(
                onClick = onBackToDashboard,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("Back to Dashboard", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RetakeIntroPanel(
    currentResult: SavedDoshaAssessment,
    historyCount: Int,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PillBadge(text = "Dosha Test", icon = "LEAF")
            Text(
                text = "Retake your Ayurvedic body type assessment.",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 34.sp
            )
            Text(
                text = "This dashboard version reuses the same dosha questionnaire from onboarding so you can update your profile later without affecting the original first-time flow.",
                color = SoftBlueGray,
                lineHeight = 23.sp
            )

            RetakeInfoCard(
                title = "Current Result",
                headline = currentResult.profileName,
                body = currentResult.overview,
                icon = Icons.Filled.Star
            )
            RetakeInfoCard(
                title = "Last Updated",
                headline = formatDoshaDateTime(currentResult.savedAtMillis),
                body = "Saved assessments in history: $historyCount",
                icon = Icons.Filled.Star
            )
            RetakeInfoCard(
                title = "Comparison Ready",
                headline = "",
                body = "When you submit, we compare your previous saved dosha result with the new one so changes are easy to track.",
                icon = Icons.Filled.Star
            )
        }
    }
}

@Composable
private fun RetakeInfoCard(title: String, headline: String, body: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WarmCard)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.85f)), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.size(34.dp).background(LightSage, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title.uppercase(), color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp)
            if (headline.isNotBlank()) {
                Text(headline, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(body, color = SoftBlueGray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun RetakeAssessmentStage(
    uiState: DoshaAssessmentUiState,
    onOptionSelected: (String) -> Unit,
    onContinueToDashboard: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.result,
        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(220)) },
        label = "retake-assessment-stage",
        modifier = modifier
    ) { result ->
        if (result != null) {
            RetakeResultCard(
                result = result,
                previous = uiState.previousSavedResult,
                savedAtMillis = uiState.savedAtMillis,
                onContinueToDashboard = onContinueToDashboard
            )
        } else {
            QuestionCard(
                uiState = uiState,
                onOptionSelected = onOptionSelected,
                onPrevious = onPrevious,
                onNext = onNext,
                nextText = if (uiState.isLastQuestion) "Submit Test" else "Next"
            )
        }
    }
}

@Composable
private fun AssessmentHeader(
    onBackToWelcome: () -> Unit,
    questionText: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (compact) {
                BackButton(onClick = onBackToWelcome)
                PillBadge(text = questionText)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.vedaahaar_logo),
                        contentDescription = "VedaAhaar logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(WarmCard),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "VedaAhaar",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = DarkForestGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Required Onboarding Step",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SageGreen,
                                letterSpacing = 0.6.sp
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackButton(onClick = onBackToWelcome)
                    PillBadge(text = questionText, icon = "AI")
                }
            }
        }
    }
}

@Composable
private fun IntroCard(modifier: Modifier = Modifier) {
    PremiumCard(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            FloatingLeafCluster(modifier = Modifier.align(Alignment.TopEnd).size(112.dp), alpha = 0.22f)
            Column(modifier = Modifier.padding(22.dp)) {
                PillBadge(text = "Before Dashboard Access")
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Let's identify the dosha profile that shapes your care journey.",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 38.sp
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "This quick 20-question assessment helps personalize your Ayurvedic experience. Complete it once, save your result, and then continue directly to the dashboard.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SoftBlueGray,
                        lineHeight = 23.sp
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 420.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MiniInfoCard(
                                title = "Experience",
                                body = "Calm onboarding with AI-guided Ayurvedic scoring.",
                                modifier = Modifier.fillMaxWidth()
                            )
                            MiniInfoCard(
                                title = "What Happens Next",
                                body = "Your constitution shapes diet, routine, and care nudges.",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MiniInfoCard(
                                title = "Experience",
                                body = "Calm onboarding with AI-guided Ayurvedic scoring.",
                                modifier = Modifier.weight(1f)
                            )
                            MiniInfoCard(
                                title = "What Happens Next",
                                body = "Your constitution shapes diet, routine, and care nudges.",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentStage(
    uiState: DoshaAssessmentUiState,
    onOptionSelected: (String) -> Unit,
    onContinueToDashboard: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.result,
        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(220)) },
        label = "assessment-stage",
        modifier = modifier
    ) { result ->
        if (result != null) {
            ResultCard(result = result, onContinueToDashboard = onContinueToDashboard)
        } else {
            QuestionCard(
                uiState = uiState,
                onOptionSelected = onOptionSelected,
                onPrevious = onPrevious,
                onNext = onNext
            )
        }
    }
}

@Composable
private fun QuestionCard(
    uiState: DoshaAssessmentUiState,
    onOptionSelected: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    nextText: String = if (uiState.isLastQuestion) "Reveal Result" else "Next"
) {
    val scope = rememberCoroutineScope()
    var isMovingToNextQuestion by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "dosha-progress"
    )

    LaunchedEffect(uiState.currentQuestion.id, uiState.result) {
        isMovingToNextQuestion = false
    }

    fun handleOptionSelect(optionId: String) {
        if (isMovingToNextQuestion) return

        isMovingToNextQuestion = true
        onOptionSelected(optionId)
        scope.launch {
            delay(300)
            onNext()
        }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillBadge(text = uiState.currentQuestion.category.title)
                QuestionCounterCard(
                    questionText = "Q${uiState.currentQuestion.id} of ${uiState.questions.size}"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "DOSHA ANALYSIS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SageGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            val isLongSingleWordTitle = uiState.currentQuestion.title.length > 8 && !uiState.currentQuestion.title.contains(" ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = uiState.currentQuestion.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontSize = if (isLongSingleWordTitle) 34.sp else 42.sp,
                        lineHeight = if (isLongSingleWordTitle) 38.sp else 46.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                DoshaAura(modifier = Modifier.size(if (isLongSingleWordTitle) 86.dp else 112.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "PROGRESS",
                    style = MaterialTheme.typography.labelSmall.copy(color = SageGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                )
                Text(
                    text = "${(uiState.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = ForestGreen, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50)),
                color = SageGreen,
                trackColor = LightSage
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Choose the option that feels most like you.",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF4F5962), lineHeight = 24.sp)
            )

            Spacer(modifier = Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                uiState.currentQuestion.options.forEachIndexed { index, option ->
                    OptionCard(
                        option = option,
                        optionKey = ('A' + index).toString(),
                        selected = option.id == uiState.selectedOptionId,
                        enabled = !isMovingToNextQuestion,
                        onClick = { handleOptionSelect(option.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            WhyCard(text = uiState.currentQuestion.whyWeAsk)
            Spacer(modifier = Modifier.height(20.dp))
            NavigationButtons(
                canGoPrevious = uiState.canGoPrevious,
                canGoNext = uiState.selectedOptionId != null && !isMovingToNextQuestion,
                nextText = nextText,
                showNext = false,
                onPrevious = onPrevious,
                onNext = onNext
            )
        }
    }
}

@Composable
private fun QuestionCounterCard(questionText: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, BeigeBorder), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(questionText, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Choose the option that feels most like you.", color = SoftBlueGray, fontSize = 10.sp)
    }
}

@Composable
private fun OptionCard(
    option: DoshaOption,
    optionKey: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (selected) 1.015f else 1f, tween(180), label = "option-scale")
    val background = if (selected) Color(0xFFF4FAEE) else WarmCard
    val borderBrush = if (selected) {
        Brush.linearGradient(listOf(ForestGreen, GreenGlow, GoldMist.copy(alpha = 0.62f)))
    } else {
        Brush.linearGradient(listOf(BeigeBorder.copy(alpha = 0.75f), GoldMist.copy(alpha = 0.28f)))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(if (selected) 8.dp else 2.dp, RoundedCornerShape(22.dp), ambientColor = GreenGlow.copy(alpha = 0.16f), spotColor = GreenGlow.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .border(BorderStroke(if (selected) 1.4.dp else 1.dp, borderBrush), RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (selected) ForestGreen else LightSage)
                .border(BorderStroke(1.dp, if (selected) GreenGlow else BeigeBorder), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = optionKey,
                color = if (selected) PureWhite else ForestGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = DarkForestGreen,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 23.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${option.description} tendency",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        AnimatedRadio(selected = selected)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(result: DoshaResult, onContinueToDashboard: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp)) {
            PillBadge(text = "Assessment Complete")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Final Dosha Type",
                color = SageGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.6.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = result.profileName,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = DarkForestGreen,
                    fontFamily = FontFamily.Serif,
                    fontSize = 38.sp,
                    lineHeight = 44.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = result.description.overview,
                style = MaterialTheme.typography.bodyMedium.copy(color = SoftBlueGray, lineHeight = 23.sp)
            )
            Spacer(modifier = Modifier.height(18.dp))

            Dosha.entries.forEach { dosha ->
                ResultMeter(
                    dosha = dosha.displayName,
                    score = result.scores.getValue(dosha),
                    maxScore = result.scores.values.sum().coerceAtLeast(1)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultChip("Dominant: ${result.dominant.displayName}")
                result.secondary?.let { ResultChip("Secondary: ${it.displayName}") }
                ResultChip(result.profileType.name)
            }

            Spacer(modifier = Modifier.height(20.dp))
            ResultDetails(result.description)
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onContinueToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(10.dp, RoundedCornerShape(50), ambientColor = GreenGlow.copy(alpha = 0.22f), spotColor = GreenGlow.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("Personalized Diet & Lifestyle", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RetakeResultCard(
    result: DoshaResult,
    previous: SavedDoshaAssessment?,
    savedAtMillis: Long?,
    onContinueToDashboard: () -> Unit
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PillBadge(text = "Result Summary")
            Text(
                text = "Previous Dosha vs New Dosha",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 36.sp
            )
            Text(
                text = "Saved ${formatDoshaDateTime(savedAtMillis ?: System.currentTimeMillis())}",
                color = SageGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ComparisonDoshaCard(
                    label = "Previous Dosha",
                    name = previous?.profileName ?: "No previous result",
                    modifier = Modifier.weight(1f)
                )
                ComparisonDoshaCard(
                    label = "New Dosha",
                    name = result.profileName,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Previous percentages vs New percentages", color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp)
            Dosha.entries.forEach { dosha ->
                ComparisonMeter(
                    dosha = dosha.displayName,
                    previousPercent = previous?.percentages?.get(dosha) ?: 0,
                    newPercent = result.percentages.getValue(dosha)
                )
            }

            Text(result.description.overview, color = SoftBlueGray, lineHeight = 22.sp)
            Button(
                onClick = onContinueToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(10.dp, RoundedCornerShape(50), ambientColor = GreenGlow.copy(alpha = 0.22f), spotColor = GreenGlow.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("Back to Dashboard", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
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
            Text("$previousPercent% -> $newPercent%", color = ForestGreen, fontWeight = FontWeight.Bold)
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
        "Personality" to description.personalityTraits,
        "Digestion" to description.digestionStyle,
        "Emotion" to description.emotionalTendencies,
        "Energy" to description.energyBehavior,
        "Strengths" to description.strengths,
        "Imbalance Risks" to description.imbalanceRisks,
        "Lifestyle" to description.lifestyleSuggestions,
        "Food" to description.foodRecommendations
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (title, body) ->
            MiniInfoCard(title = title, body = body)
        }
    }
}

@Composable
private fun NavigationButtons(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    nextText: String,
    showNext: Boolean = true,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, if (canGoPrevious) ForestGreen else BeigeBorder),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = WarmCard, contentColor = ForestGreen)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Previous")
        }
        if (showNext) {
            Button(
                onClick = onNext,
                enabled = canGoNext,
                modifier = Modifier
                    .weight(1.18f)
                    .shadow(8.dp, RoundedCornerShape(50), ambientColor = GreenGlow.copy(alpha = 0.2f), spotColor = GreenGlow.copy(alpha = 0.16f)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite, disabledContainerColor = LightSage, disabledContentColor = SageGreen)
            ) {
                Text(nextText)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
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
                .size(34.dp)
                .background(SageGreen.copy(alpha = 0.24f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(color = DarkForestGreen, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium.copy(color = SoftBlueGray, lineHeight = 21.sp))
        }
    }
}

@Composable
private fun WhyCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(LightSage.copy(alpha = 0.86f), PureWhite)))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.85f)), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).background(SageGreen.copy(alpha = 0.42f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = ForestGreen)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text("Why we ask this?", style = MaterialTheme.typography.bodyMedium.copy(color = DarkForestGreen, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium.copy(color = SoftBlueGray, lineHeight = 22.sp))
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
private fun AnimatedRadio(selected: Boolean) {
    val innerScale by animateFloatAsState(if (selected) 1f else 0f, tween(180), label = "radio")
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(BorderStroke(3.dp, if (selected) ForestGreen else Color(0xFFADB1B1)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .scale(innerScale)
                .background(ForestGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
        }
    }
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
private fun ResultMeter(dosha: String, score: Int, maxScore: Int) {
    val animated by animateFloatAsState(score.toFloat() / maxScore.toFloat(), tween(650, easing = FastOutSlowInEasing), label = "result-$dosha")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dosha, color = DarkForestGreen, fontWeight = FontWeight.Bold)
            Text("$score", color = ForestGreen, fontWeight = FontWeight.Bold)
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
        uiState = DoshaAssessmentUiState(answers = mapOf(1 to "medium")),
        onBackToWelcome = {},
        onContinueToDashboard = {},
        onOptionSelected = {},
        onPrevious = {},
        onNext = {},
        onRestart = {}
    )
}



