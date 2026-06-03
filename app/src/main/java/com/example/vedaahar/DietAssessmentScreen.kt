package com.example.vedaahar

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay

private val DietGold = Color(0xFFE8C97B)
private val DietWarmCard = Color(0xFFFFFBF4)
private val DietGreenGlow = Color(0xFF90C987)
private val DietDanger = Color(0xFFB85A45)
private val DietDangerSoft = Color(0xFFFFEFE9)
private val DietMint = Color(0xFFDDEFE2)
private val DietHoney = Color(0xFFFFE9B6)
private val DietRose = Color(0xFFFFDAD4)
private val DietSky = Color(0xFFDDEBFF)

private enum class DietStep(val title: String, val subtitle: String) {
    Goals("What would you like to improve?", "Choose your wellness goals"),
    Health("Any health concerns?", "This helps us personalise safely"),
    Vikriti("How are you feeling lately?", "Select what matches your body"),
    Agni("How is your digestion?", "Your digestion guides your meal plan"),
    Ama("Any of these symptoms?", "This helps us understand toxin load"),
    Lifestyle("Tell us about your lifestyle", "Small routines shape digestion"),
    Food("Your food choices", "We'll keep your plan realistic"),
    Rasa("What tastes do you crave?", "Ayurveda uses taste to understand imbalance"),
    Routine("Your daily routine", "Meal timing matters as much as food"),
    Mind("How do you feel mentally?", "Food also supports your mind"),
    Processing("Analysing your Ayurvedic profile...", "Building your personalised plan"),
    Result("Your Ayurvedic Diet Profile", "Personalised for your prakriti")
}

@Composable
fun DietAssessmentScreen(
    prakriti: String = "Vata-Pitta",
    onBack: () -> Unit,
    onEditPrakriti: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val selectedGoals = remember { mutableStateOf(setOf<String>()) }
    val healthConcerns = remember { mutableStateOf(setOf<String>()) }
    val femaleAnswers = remember { mutableStateMapOf("Pregnant?" to "No", "Breastfeeding?" to "No", "Menstrual irregularity?" to "No") }
    val vikritiSymptoms = remember { mutableStateOf(setOf<String>()) }
    var hunger by remember { mutableStateOf("") }
    var mealFeel by remember { mutableStateOf("") }
    val digestionIssues = remember { mutableStateOf(setOf<String>()) }
    val amaSymptoms = remember { mutableStateOf(setOf<String>()) }
    var workType by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("") }
    var stress by remember { mutableFloatStateOf(4f) }
    var sleepTime by remember { mutableStateOf("") }
    var wakeTime by remember { mutableStateOf("") }
    val exercises = remember { mutableStateOf(setOf<String>()) }
    var dietType by remember { mutableStateOf("") }
    val avoidFoods = remember { mutableStateOf(setOf<String>()) }
    val allergies = remember { mutableStateOf(setOf<String>()) }
    val dislikedFoods = remember { mutableStateOf(setOf<String>()) }
    var spice by remember { mutableFloatStateOf(1f) }
    val rasaEnjoyment = remember { mutableStateMapOf<String, Float>() }
    val rasaCravings = remember { mutableStateMapOf<String, Boolean>() }
    var breakfast by remember { mutableStateOf("") }
    var lunch by remember { mutableStateOf("") }
    var dinner by remember { mutableStateOf("") }
    var water by remember { mutableStateOf("") }
    var lateEating by remember { mutableStateOf("") }
    val mentalStates = remember { mutableStateOf(setOf<String>()) }

    val visibleSteps = DietStep.entries.take(10)
    val currentStep = DietStep.entries[stepIndex]
    val result = remember(
        selectedGoals.value,
        healthConcerns.value,
        vikritiSymptoms.value,
        hunger,
        mealFeel,
        digestionIssues.value,
        amaSymptoms.value,
        mentalStates.value
    ) {
        buildDietResult(
            prakriti = prakriti,
            goals = selectedGoals.value,
            conditions = healthConcerns.value,
            vikritiSymptoms = vikritiSymptoms.value,
            hunger = hunger,
            mealFeel = mealFeel,
            digestionIssues = digestionIssues.value,
            amaSymptoms = amaSymptoms.value,
            dietType = dietType,
            mentalStates = mentalStates.value
        )
    }

    LaunchedEffect(currentStep) {
        if (currentStep == DietStep.Processing) {
            delay(3900)
            stepIndex = DietStep.Result.ordinal
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF8ED), Cream, LightSage.copy(alpha = 0.55f))))
        ) {
            DietAmbientBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                if (currentStep != DietStep.Result && currentStep != DietStep.Processing) {
                    DietTopBar(
                        stepIndex = stepIndex,
                        totalSteps = visibleSteps.size,
                        onBack = {
                            if (stepIndex == 0) onBack() else stepIndex -= 1
                        }
                    )
                }

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                    label = "diet-assessment-step",
                    modifier = Modifier.weight(1f)
                ) { step ->
                    when (step) {
                        DietStep.Goals -> AssessmentStepScaffold(
                            step = step,
                            prakriti = prakriti,
                            onEditPrakriti = onEditPrakriti,
                            canContinue = selectedGoals.value.isNotEmpty(),
                            onContinue = { stepIndex += 1 }
                        ) {
                            GoalSelection(selected = selectedGoals.value, onChange = { selectedGoals.value = it })
                        }

                        DietStep.Health -> AssessmentStepScaffold(step, prakriti, onEditPrakriti, true, { stepIndex += 1 }) {
                            HealthConcernStep(
                                selected = healthConcerns.value,
                                femaleAnswers = femaleAnswers,
                                onSelectedChange = { healthConcerns.value = it }
                            )
                        }

                        DietStep.Vikriti -> AssessmentStepScaffold(step, prakriti, onEditPrakriti, vikritiSymptoms.value.isNotEmpty(), { stepIndex += 1 }) {
                            VikritiStep(selected = vikritiSymptoms.value, onChange = { vikritiSymptoms.value = it })
                        }

                        DietStep.Agni -> AssessmentStepScaffold(
                            step = step,
                            prakriti = prakriti,
                            onEditPrakriti = onEditPrakriti,
                            canContinue = hunger.isNotBlank() && mealFeel.isNotBlank() && digestionIssues.value.isNotEmpty(),
                            onContinue = { stepIndex += 1 }
                        ) {
                            AgniStep(
                                hunger = hunger,
                                mealFeel = mealFeel,
                                issues = digestionIssues.value,
                                onHunger = { hunger = it },
                                onMealFeel = { mealFeel = it },
                                onIssues = { digestionIssues.value = it }
                            )
                        }

                        DietStep.Ama -> AssessmentStepScaffold(step, prakriti, onEditPrakriti, true, { stepIndex += 1 }) {
                            AmaStep(selected = amaSymptoms.value, onChange = { amaSymptoms.value = it })
                        }

                        DietStep.Lifestyle -> AssessmentStepScaffold(
                            step = step,
                            prakriti = prakriti,
                            onEditPrakriti = onEditPrakriti,
                            canContinue = workType.isNotBlank() && activity.isNotBlank() && sleepTime.isNotBlank() && wakeTime.isNotBlank(),
                            onContinue = { stepIndex += 1 }
                        ) {
                            LifestyleStep(
                                workType = workType,
                                activity = activity,
                                stress = stress,
                                sleepTime = sleepTime,
                                wakeTime = wakeTime,
                                exercises = exercises.value,
                                onWorkType = { workType = it },
                                onActivity = { activity = it },
                                onStress = { stress = it },
                                onSleepTime = { sleepTime = it },
                                onWakeTime = { wakeTime = it },
                                onExercises = { exercises.value = it }
                            )
                        }

                        DietStep.Food -> AssessmentStepScaffold(step, prakriti, onEditPrakriti, dietType.isNotBlank(), { stepIndex += 1 }) {
                            FoodPreferenceStep(
                                dietType = dietType,
                                avoidFoods = avoidFoods.value,
                                allergies = allergies.value,
                                dislikedFoods = dislikedFoods.value,
                                spice = spice,
                                onDietType = { dietType = it },
                                onAvoid = { avoidFoods.value = it },
                                onAllergies = { allergies.value = it },
                                onDisliked = { dislikedFoods.value = it },
                                onSpice = { spice = it }
                            )
                        }

                        DietStep.Rasa -> AssessmentStepScaffold(step, prakriti, onEditPrakriti, true, { stepIndex += 1 }) {
                            RasaStep(enjoyment = rasaEnjoyment, cravings = rasaCravings)
                        }

                        DietStep.Routine -> AssessmentStepScaffold(
                            step = step,
                            prakriti = prakriti,
                            onEditPrakriti = onEditPrakriti,
                            canContinue = breakfast.isNotBlank() && lunch.isNotBlank() && dinner.isNotBlank() && water.isNotBlank() && lateEating.isNotBlank(),
                            onContinue = { stepIndex += 1 }
                        ) {
                            RoutineStep(
                                breakfast = breakfast,
                                lunch = lunch,
                                dinner = dinner,
                                water = water,
                                lateEating = lateEating,
                                onBreakfast = { breakfast = it },
                                onLunch = { lunch = it },
                                onDinner = { dinner = it },
                                onWater = { water = it },
                                onLateEating = { lateEating = it }
                            )
                        }

                        DietStep.Mind -> AssessmentStepScaffold(
                            step = step,
                            prakriti = prakriti,
                            onEditPrakriti = onEditPrakriti,
                            canContinue = mentalStates.value.isNotEmpty(),
                            onContinue = { stepIndex = DietStep.Processing.ordinal },
                            ctaText = "Create My Plan"
                        ) {
                            MindStep(selected = mentalStates.value, onChange = { mentalStates.value = it })
                        }

                        DietStep.Processing -> ProcessingStep()
                        DietStep.Result -> ResultStep(result = result, onBack = onBack)
                    }
                }
            }
        }
    }
}

@Composable
private fun DietTopBar(stepIndex: Int, totalSteps: Int, onBack: () -> Unit) {
    val progress = (stepIndex + 1).toFloat() / totalSteps.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("Step ${stepIndex + 1} of $totalSteps", color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(50)),
            color = ForestGreen,
            trackColor = LightSage
        )
        Text("${(progress * 100).toInt()}% Complete - ${motivationFor(stepIndex)}", color = SoftBlueGray, fontSize = 12.sp)
    }
}

@Composable
private fun AssessmentStepScaffold(
    step: DietStep,
    prakriti: String,
    onEditPrakriti: () -> Unit,
    canContinue: Boolean,
    onContinue: () -> Unit,
    ctaText: String = "Continue",
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrakritiStrip(prakriti = prakriti, onEdit = onEditPrakriti)
            PremiumDietCard {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DietBadge("Ayurvedic Diet Assessment")
                    Text(
                        step.title,
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 34.sp
                    )
                    Text(step.subtitle, color = SoftBlueGray, lineHeight = 22.sp)
                    content()
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Surface(color = Cream.copy(alpha = 0.96f), shadowElevation = 10.dp) {
            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .height(56.dp)
                    .shadow(10.dp, RoundedCornerShape(50), ambientColor = DietGreenGlow.copy(alpha = 0.22f), spotColor = ForestGreen.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestGreen,
                    contentColor = PureWhite,
                    disabledContainerColor = LightSage,
                    disabledContentColor = SageGreen
                )
            ) {
                Text(ctaText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PrakritiStrip(prakriti: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(LightSage, DietWarmCard)))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(ForestGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = DietGold, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Your selected prakriti", color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp)
            Text(prakriti, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        OutlinedButton(
            onClick = onEdit,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = PureWhite, contentColor = ForestGreen)
        ) {
            Text("Edit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalSelection(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    val goals = listOf(
        DietGoalOption("Weight Loss", "Light meals, steady metabolism", "Agni", DietRose),
        DietGoalOption("Weight Gain", "Nourishment and strength", "Bala", DietHoney),
        DietGoalOption("Better Digestion", "Gut comfort and regularity", "Agni", DietMint),
        DietGoalOption("Better Energy", "Daylong vitality and focus", "Ojas", DietHoney),
        DietGoalOption("Skin Glow", "Cooling, clear-food support", "Tejas", DietRose),
        DietGoalOption("Hair Health", "Mineral-rich nourishment", "Rasa", DietMint),
        DietGoalOption("Stress Relief", "Calm routine and grounding", "Sattva", DietHoney),
        DietGoalOption("Better Sleep", "Evening rhythm correction", "Nidra", DietSky),
        DietGoalOption("PCOS Support", "Hormonal balance support", "Cycle", DietRose),
        DietGoalOption("Diabetes Support", "Blood sugar aware meals", "Sugar", DietMint),
        DietGoalOption("Acidity Relief", "Cooling pitta balance", "Cool", DietSky),
        DietGoalOption("Immunity Boost", "Resilience and recovery", "Ojas", DietMint),
        DietGoalOption("Muscle Gain", "Protein-aware strength plan", "Bala", DietHoney)
    )
    GoalProgressPanel(selectedCount = selected.size)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compactCards = maxWidth < 360.dp
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            goals.forEach { goal ->
                val isSelected = goal.title in selected
                GoalOptionCard(
                    goal = goal,
                    selected = isSelected,
                    disabled = selected.size == 3 && !isSelected,
                    onClick = {
                        onChange(
                            when {
                                isSelected -> selected - goal.title
                                selected.size < 3 -> selected + goal.title
                                else -> selected
                            }
                        )
                    },
                    modifier = if (compactCards) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(0.48f)
                    }
                )
            }
        }
    }
    if (selected.size == 3) InsightCard("Focused plan", "Three goals selected. We'll prioritise these while building your meals.")
}

private data class DietGoalOption(
    val title: String,
    val subtitle: String,
    val tag: String,
    val accent: Color
)

@Composable
private fun GoalProgressPanel(selectedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(ForestGreen, DarkForestGreen)))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(PureWhite.copy(alpha = 0.14f))
                .border(BorderStroke(1.dp, PureWhite.copy(alpha = 0.22f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$selectedCount/3", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Select your top priorities", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                if (selectedCount == 0) "Pick up to three goals to tune your meal plan."
                else "Your choices shape foods, timing, and lifestyle tips.",
                color = PureWhite.copy(alpha = 0.78f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun GoalOptionCard(
    goal: DietGoalOption,
    selected: Boolean,
    disabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (selected) 1.015f else 1f, tween(160), label = "goal-card-scale")
    val borderColor = when {
        selected -> ForestGreen
        disabled -> BeigeBorder.copy(alpha = 0.38f)
        else -> BeigeBorder.copy(alpha = 0.85f)
    }
    val container = when {
        selected -> Brush.linearGradient(listOf(LightSage, PureWhite, goal.accent.copy(alpha = 0.5f)))
        disabled -> Brush.linearGradient(listOf(PureWhite.copy(alpha = 0.52f), Cream.copy(alpha = 0.7f)))
        else -> Brush.linearGradient(listOf(PureWhite, DietWarmCard))
    }
    Card(
        modifier = modifier
            .height(98.dp)
            .scale(scale)
            .clickable(enabled = !disabled || selected, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(if (selected) 1.6.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(container)
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) ForestGreen else goal.accent.copy(alpha = if (disabled) 0.35f else 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(19.dp))
                } else {
                    Text(goal.tag.take(2).uppercase(), color = ForestGreen.copy(alpha = if (disabled) 0.45f else 1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    goal.title,
                    color = if (disabled) SoftBlueGray.copy(alpha = 0.58f) else DarkForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    goal.subtitle,
                    color = if (disabled) SoftBlueGray.copy(alpha = 0.46f) else SoftBlueGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthConcernStep(
    selected: Set<String>,
    femaleAnswers: MutableMap<String, String>,
    onSelectedChange: (Set<String>) -> Unit
) {
    val concerns = listOf("Diabetes", "Thyroid", "PCOS", "IBS", "Acidity", "Constipation", "Gas/Bloating", "Obesity", "Hypertension", "Migraine", "Arthritis", "Skin Issues", "No condition")
    SectionLabel("Searchable health chips")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        concerns.forEach { concern ->
            DietChip(
                text = concern,
                selected = concern in selected,
                onClick = {
                    onSelectedChange(
                        when {
                            concern == "No condition" -> setOf("No condition")
                            concern in selected -> selected - concern
                            else -> (selected - "No condition") + concern
                        }
                    )
                }
            )
        }
    }
    SectionLabel("Female health")
    listOf("Pregnant?", "Breastfeeding?", "Menstrual irregularity?").forEach { question ->
        YesNoRow(question, femaleAnswers[question] ?: "No") { femaleAnswers[question] = it }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VikritiStep(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    val groups = listOf(
        "Vata Symptoms" to listOf("Dry skin", "Anxiety", "Overthinking", "Constipation", "Joint cracking", "Cold hands/feet", "Irregular appetite", "Insomnia"),
        "Pitta Symptoms" to listOf("Acidity", "Anger/irritation", "Excess hunger", "Loose stools", "Body heat", "Pimples", "Burning sensation"),
        "Kapha Symptoms" to listOf("Laziness", "Weight gain", "Water retention", "Sleepiness", "Slow digestion", "Mucus/cold", "Emotional eating")
    )
    groups.forEach { (title, symptoms) ->
        MiniPanel(title) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                symptoms.forEach { item ->
                    DietChip(item, item in selected) {
                        onChange(if (item in selected) selected - item else selected + item)
                    }
                }
            }
        }
    }
    if (selected.isNotEmpty()) InsightCard("Current imbalance detected", "${detectVikriti(selected)} increase")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgniStep(
    hunger: String,
    mealFeel: String,
    issues: Set<String>,
    onHunger: (String) -> Unit,
    onMealFeel: (String) -> Unit,
    onIssues: (Set<String>) -> Unit
) {
    SingleChoiceGroup("Hunger level", listOf("Very low", "Normal", "Excessive", "Irregular"), hunger, onHunger)
    SingleChoiceGroup("How do you feel after meals?", listOf("Light", "Heavy", "Sleepy", "Bloated"), mealFeel, onMealFeel)
    SectionLabel("Digestion issue?")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Gas", "Acidity", "Constipation", "Loose motion", "None").forEach { item ->
            DietChip(item, item in issues) {
                onIssues(
                    when {
                        item == "None" -> setOf("None")
                        item in issues -> issues - item
                        else -> (issues - "None") + item
                    }
                )
            }
        }
    }
    if (hunger.isNotBlank() && mealFeel.isNotBlank()) {
        InsightCard("Agni insight", "${classifyAgni(hunger, mealFeel, issues)} pattern detected.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmaStep(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    val items = listOf("White tongue coating", "Bad breath", "Laziness", "Brain fog", "Bloating", "Sticky stool", "Feeling heavy")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            DietChip(item, item in selected) { onChange(if (item in selected) selected - item else selected + item) }
        }
    }
    InsightCard(
        if (selected.size >= 3) "Ama detected" else "Ama check",
        if (selected.size >= 3) "We'll prioritise gentle gut cleansing." else "No major ama signs yet. Your plan can stay nourishing and light."
    )
}

@Composable
private fun LifestyleStep(
    workType: String,
    activity: String,
    stress: Float,
    sleepTime: String,
    wakeTime: String,
    exercises: Set<String>,
    onWorkType: (String) -> Unit,
    onActivity: (String) -> Unit,
    onStress: (Float) -> Unit,
    onSleepTime: (String) -> Unit,
    onWakeTime: (String) -> Unit,
    onExercises: (Set<String>) -> Unit
) {
    SingleChoiceGroup("Work Type", listOf("Student", "Office Worker", "Work From Home", "Homemaker", "Labor Intensive"), workType, onWorkType)
    SingleChoiceGroup("Activity Level", listOf("Sedentary", "Lightly Active", "Moderate", "Active", "Athlete"), activity, onActivity)
    SliderPanel("Stress Level", stress, "Calm", "High stress", onStress)
    SingleChoiceGroup("Sleep Time", listOf("Before 10 PM", "10-12 PM", "After 12 AM"), sleepTime, onSleepTime)
    SingleChoiceGroup("Wake Time", listOf("Before 6", "6-8", "After 8"), wakeTime, onWakeTime)
    MultiChoiceGroup("Exercise", listOf("None", "Walking", "Yoga", "Gym", "Sports"), exercises, onExercises)
}

@Composable
private fun FoodPreferenceStep(
    dietType: String,
    avoidFoods: Set<String>,
    allergies: Set<String>,
    dislikedFoods: Set<String>,
    spice: Float,
    onDietType: (String) -> Unit,
    onAvoid: (Set<String>) -> Unit,
    onAllergies: (Set<String>) -> Unit,
    onDisliked: (Set<String>) -> Unit,
    onSpice: (Float) -> Unit
) {
    SingleChoiceGroup("Diet Type", listOf("Vegetarian", "Eggetarian", "Non Vegetarian", "Vegan"), dietType, onDietType)
    MultiChoiceGroup("Avoid foods", listOf("Milk", "Gluten", "Soy", "Nuts", "Curd", "Paneer", "Rice", "Wheat"), avoidFoods, onAvoid)
    MultiChoiceGroup("Allergies", listOf("Milk", "Gluten", "Nuts", "Soy", "Sesame", "Seafood", "Eggs", "None"), allergies) {
        onAllergies(if ("None" in it) setOf("None") else it)
    }
    MultiChoiceGroup("Disliked foods", listOf("Karela", "Lauki", "Curd", "Banana", "Dal", "Rice"), dislikedFoods, onDisliked)
    SliderPanel("Spice tolerance", spice, "Low", "High", onSpice)
}

@Composable
private fun RasaStep(
    enjoyment: MutableMap<String, Float>,
    cravings: MutableMap<String, Boolean>
) {
    val rasas = listOf(
        "Madhura / Sweet" to "Rice, milk, sweets, banana",
        "Amla / Sour" to "Curd, lemon, pickle",
        "Lavana / Salty" to "Salted snacks",
        "Katu / Spicy" to "Chili, ginger",
        "Tikta / Bitter" to "Karela, neem",
        "Kashaya / Astringent" to "Tea, lentils"
    )
    rasas.forEach { (rasa, examples) ->
        MiniPanel(rasa) {
            Text(examples, color = SoftBlueGray, fontSize = 12.sp)
            SliderPanel(
                title = "Enjoy this taste",
                value = enjoyment[rasa] ?: 2f,
                start = "Never",
                end = "Always",
                onValueChange = { enjoyment[rasa] = it }
            )
            YesNoRow("Craving recently?", if (cravings[rasa] == true) "Yes" else "No") { cravings[rasa] = it == "Yes" }
        }
    }
    InsightCard("Taste analysis", "High spicy and sour craving can suggest Pitta aggravation.")
}

@Composable
private fun RoutineStep(
    breakfast: String,
    lunch: String,
    dinner: String,
    water: String,
    lateEating: String,
    onBreakfast: (String) -> Unit,
    onLunch: (String) -> Unit,
    onDinner: (String) -> Unit,
    onWater: (String) -> Unit,
    onLateEating: (String) -> Unit
) {
    SingleChoiceGroup("Breakfast time", listOf("Before 8", "8-10", "After 10", "Skip"), breakfast, onBreakfast)
    SingleChoiceGroup("Lunch time", listOf("Before 12", "12-2", "2-4", "After 4"), lunch, onLunch)
    SingleChoiceGroup("Dinner time", listOf("Before 7", "7-9", "After 9", "Late night"), dinner, onDinner)
    SingleChoiceGroup("Water Intake", listOf("<1L", "1-2L", "2-3L", "3L+"), water, onWater)
    SingleChoiceGroup("Late-night eating?", listOf("Yes", "No"), lateEating, onLateEating)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MindStep(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    val states = listOf("Calm", "Overthinking", "Anxiety", "Stress", "Anger", "Low mood")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        states.forEach { item ->
            DietChip(item, item in selected) {
                onChange(if (item in selected) selected - item else selected + item)
            }
        }
    }
    if (selected.isNotEmpty()) InsightCard("Manas mapping", "Your mental state will influence warming, cooling, or grounding food choices.")
}

@Composable
private fun ProcessingStep() {
    val pulse by rememberInfiniteTransition(label = "processing").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PremiumDietCard {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Canvas(modifier = Modifier.size(116.dp).scale(pulse)) {
                    drawCircle(Brush.radialGradient(listOf(DietGreenGlow.copy(alpha = 0.35f), Color.Transparent)), radius = size.minDimension / 2f)
                    drawCircle(ForestGreen.copy(alpha = 0.18f), radius = size.minDimension * 0.32f)
                    drawCircle(DietGold.copy(alpha = 0.72f), radius = size.minDimension * 0.22f, style = Stroke(width = 4f))
                    drawLine(ForestGreen, Offset(size.width / 2f, size.height * 0.28f), Offset(size.width / 2f, size.height * 0.72f), strokeWidth = 8f, cap = StrokeCap.Round)
                }
                Text("Analysing your Ayurvedic profile...", color = DarkForestGreen, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, textAlign = TextAlign.Center)
                listOf("Dosha Analysis", "Digestion Check", "Lifestyle Mapping", "Taste Analysis", "Seasonal Adjustment").forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(PureWhite, LightSage.copy(alpha = 0.72f))))
                            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(ForestGreen), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(item, color = DarkForestGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStep(result: DietResult, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Body Analysis", "Foods To Eat", "Foods To Avoid", "Meal Plan", "Lifestyle Tips", "Ayurvedic")
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack, text = "Dashboard")
            }
            PremiumDietCard {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DietBadge("Diet Plan Ready")
                    Text("Your Ayurvedic Diet Profile", color = DarkForestGreen, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 31.sp, lineHeight = 35.sp)
                    Text("Personalised for ${result.prakriti}", color = SoftBlueGray)
                    FlowChips(listOf(result.vikriti, result.agni, result.amaStatus, result.season))
                }
            }
            ScrollableTabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                contentColor = ForestGreen,
                edgePadding = 0.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
            when (tab) {
                0 -> BodyAnalysisTab(result)
                1 -> RecommendationTab(result.foodsToEat, positive = true)
                2 -> RecommendationTab(result.foodsToAvoid, positive = false)
                3 -> MealPlanTab(result)
                4 -> LifestyleTipsTab(result)
                else -> AyurvedicTab(result)
            }
        }
    }
}

@Composable
private fun BodyAnalysisTab(result: DietResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ResultMetric("Prakriti", result.prakriti)
        ResultMetric("Vikriti", result.vikriti)
        ResultMetric("Agni Type", result.agni)
        ResultMetric("Ama Status", result.amaStatus)
        InsightCard("Why this matters", result.analysis)
    }
}

@Composable
private fun RecommendationTab(items: List<String>, positive: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            ResultListCard(
                title = item,
                body = if (positive) "Supports your current dosha, digestion, and goal profile." else "May aggravate your current imbalance or digestion pattern.",
                positive = positive
            )
        }
    }
}

@Composable
private fun MealPlanTab(result: DietResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TimelineMeal("Breakfast", result.breakfast)
        TimelineMeal("Lunch", result.lunch)
        TimelineMeal("Dinner", result.dinner)
        TimelineMeal("Snacks", result.snacks)
    }
}

@Composable
private fun LifestyleTipsTab(result: DietResult) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        result.lifestyleTips.forEach { ResultListCard(it, "Small daily rhythm correction for better digestion.", positive = true) }
    }
}

@Composable
private fun AyurvedicTab(result: DietResult) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        result.ayurvedicTips.forEach { ResultListCard(it, "Consider with practitioner guidance where herbs are involved.", positive = true) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleChoiceGroup(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    SectionLabel(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { item -> DietChip(item, item == selected) { onSelected(item) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceGroup(title: String, options: List<String>, selected: Set<String>, onSelected: (Set<String>) -> Unit) {
    SectionLabel(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { item ->
            DietChip(item, item in selected) {
                onSelected(if (item in selected) selected - item else selected + item)
            }
        }
    }
}

@Composable
private fun SliderPanel(title: String, value: Float, start: String, end: String, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(title)
        Slider(value = value, onValueChange = onValueChange, valueRange = 1f..10f, steps = 8)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(start, color = SageGreen, fontSize = 11.sp)
            Text(end, color = SageGreen, fontSize = 11.sp)
        }
    }
}

@Composable
private fun YesNoRow(title: String, selected: String, onSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), color = DarkForestGreen, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DietChip("Yes", selected == "Yes") { onSelected("Yes") }
            DietChip("No", selected == "No") { onSelected("No") }
        }
    }
}

@Composable
private fun SelectableDietCard(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (selected) 1.02f else 1f, tween(160), label = "card-scale")
    Card(
        modifier = modifier
            .height(72.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) LightSage else PureWhite),
        border = BorderStroke(1.dp, if (selected) ForestGreen else BeigeBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, modifier = Modifier.weight(1f), color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp)
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DietChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) ForestGreen else LightSage.copy(alpha = 0.8f)
    val content = if (selected) PureWhite else ForestGreen
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(BorderStroke(1.dp, if (selected) ForestGreen else BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = content, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun MiniPanel(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PureWhite.copy(alpha = 0.76f))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.7f)), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp)
}

@Composable
private fun DietBadge(text: String) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(LightSage, DietWarmCard)))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.55f)), RoundedCornerShape(50))
            .padding(horizontal = 13.dp, vertical = 8.dp),
        color = ForestGreen,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.1.sp
    )
}

@Composable
private fun InsightCard(title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(LightSage.copy(alpha = 0.9f), PureWhite)))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(DietGold.copy(alpha = 0.38f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = DarkForestGreen, fontWeight = FontWeight.Bold)
            Text(body, color = SoftBlueGray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun PremiumDietCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = DietGold.copy(alpha = 0.11f), spotColor = DietGreenGlow.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(PureWhite, BeigeBorder.copy(alpha = 0.7f), DietGold.copy(alpha = 0.24f)))), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(items: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { DietChip(it, selected = true, onClick = {}) }
    }
}

@Composable
private fun ResultMetric(label: String, value: String) {
    ResultListCard(label, value, positive = true)
}

@Composable
private fun ResultListCard(title: String, body: String, positive: Boolean) {
    val tint = if (positive) ForestGreen else DietDanger
    val fill = if (positive) LightSage.copy(alpha = 0.74f) else DietDangerSoft
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PureWhite, fill)))
            .border(BorderStroke(1.dp, if (positive) BeigeBorder else DietDanger.copy(alpha = 0.28f)), RoundedCornerShape(20.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = DarkForestGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(body, color = SoftBlueGray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TimelineMeal(title: String, body: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(ForestGreen), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
            }
            Box(modifier = Modifier.width(2.dp).height(48.dp).background(DietGold.copy(alpha = 0.55f)))
        }
        Spacer(Modifier.width(12.dp))
        ResultListCard(title, body, positive = true)
    }
}

@Composable
private fun DietAmbientBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(listOf(DietGold.copy(alpha = 0.18f), Color.Transparent)),
            radius = size.width * 0.58f,
            center = Offset(size.width * 0.16f, size.height * 0.12f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(DietGreenGlow.copy(alpha = 0.16f), Color.Transparent)),
            radius = size.width * 0.52f,
            center = Offset(size.width * 0.9f, size.height * 0.34f)
        )
    }
}

private data class DietResult(
    val prakriti: String,
    val vikriti: String,
    val agni: String,
    val amaStatus: String,
    val season: String,
    val analysis: String,
    val foodsToEat: List<String>,
    val foodsToAvoid: List<String>,
    val breakfast: String,
    val lunch: String,
    val dinner: String,
    val snacks: String,
    val lifestyleTips: List<String>,
    val ayurvedicTips: List<String>
)

private fun buildDietResult(
    prakriti: String,
    goals: Set<String>,
    conditions: Set<String>,
    vikritiSymptoms: Set<String>,
    hunger: String,
    mealFeel: String,
    digestionIssues: Set<String>,
    amaSymptoms: Set<String>,
    dietType: String,
    mentalStates: Set<String>
): DietResult {
    val vikriti = detectVikriti(vikritiSymptoms)
    val agni = classifyAgni(hunger, mealFeel, digestionIssues)
    val ama = if (amaSymptoms.size >= 3) "Ama: Mild" else "Ama: Low"
    val pittaPlan = vikriti.contains("Pitta") || agni == "Tikshna Agni" || "Acidity" in conditions
    val kaphaPlan = vikriti.contains("Kapha") || "Weight Loss" in goals || "Obesity" in conditions
    val vataPlan = vikriti.contains("Vata") || mentalStates.any { it == "Anxiety" || it == "Overthinking" }
    val eat = when {
        pittaPlan -> listOf("Moong dal khichdi", "Bottle gourd", "Cucumber", "Coconut water", "Amla", "Steamed vegetables")
        kaphaPlan -> listOf("Barley khichdi", "Millet upma", "Moong soup", "Steamed greens", "Ginger coriander tea", "Light dal")
        vataPlan -> listOf("Warm rice porridge", "Ghee-tempered moong dal", "Cooked carrots", "Sesame-free soups", "Ripe banana", "Cumin ajwain water")
        else -> listOf("Seasonal vegetables", "Moong dal", "Rice kanji", "Fresh fruits", "Buttermilk at lunch", "Herbal water")
    }.filterNot { dietType == "Vegan" && it.contains("ghee", ignoreCase = true) }
    val avoid = when {
        pittaPlan -> listOf("Very spicy food", "Pickles", "Fried snacks", "Excess tea/coffee", "Curd at night", "Fermented sour foods")
        kaphaPlan -> listOf("Heavy sweets", "Cold drinks", "Deep fried snacks", "Late dinner", "Excess dairy", "Day sleeping")
        vataPlan -> listOf("Dry snacks", "Cold salads", "Irregular meals", "Excess caffeine", "Raw sprouts", "Late-night eating")
        else -> listOf("Overeating", "Late dinner", "Very cold drinks", "Repeated reheated food", "Excess sugar", "Mindless snacking")
    }
    return DietResult(
        prakriti = prakriti,
        vikriti = "$vikriti increase",
        agni = agni,
        amaStatus = ama,
        season = "Summer adjusted",
        analysis = "Your plan balances $prakriti with current $vikriti signs, $agni digestion, goal priorities, food preference, routine, mental state, season, and climate.",
        foodsToEat = eat,
        foodsToAvoid = avoid,
        breakfast = if (kaphaPlan) "Light millet upma with ginger tea." else "Warm moong dal chilla or rice porridge.",
        lunch = "Main balanced meal with dal, cooked vegetables, grain, and gentle spices.",
        dinner = "Light soup or khichdi before 8:30 PM.",
        snacks = if (pittaPlan) "Coconut water, soaked raisins, or fresh seasonal fruit." else "Roasted makhana, herbal tea, or fruit as per hunger.",
        lifestyleTips = listOf("Walk 10 minutes after lunch", "Keep dinner early and light", "Sip warm or room-temperature water", "Sleep before 10:30 PM when possible", "Practice 5 minutes of calm breathing"),
        ayurvedicTips = listOf("Use coriander and fennel for gentle digestion", "Prefer seasonal freshly cooked meals", "Consider Amla with practitioner guidance", "Yoga: gentle twists and forward folds", "Avoid aggressive detox without clinical advice")
    )
}

private fun detectVikriti(selected: Set<String>): String {
    val vata = setOf("Dry skin", "Anxiety", "Overthinking", "Constipation", "Joint cracking", "Cold hands/feet", "Irregular appetite", "Insomnia").count { it in selected }
    val pitta = setOf("Acidity", "Anger/irritation", "Excess hunger", "Loose stools", "Body heat", "Pimples", "Burning sensation").count { it in selected }
    val kapha = setOf("Laziness", "Weight gain", "Water retention", "Sleepiness", "Slow digestion", "Mucus/cold", "Emotional eating").count { it in selected }
    val scores = listOf("Vata" to vata, "Pitta" to pitta, "Kapha" to kapha).filter { it.second > 0 }
    val max = scores.maxOfOrNull { it.second } ?: return "Balanced"
    return scores.filter { it.second == max }.joinToString("-") { it.first }
}

private fun classifyAgni(hunger: String, mealFeel: String, issues: Set<String>): String {
    return when {
        hunger == "Normal" && mealFeel == "Light" && ("None" in issues || issues.isEmpty()) -> "Sama Agni"
        hunger == "Very low" || mealFeel in setOf("Heavy", "Sleepy") -> "Manda Agni"
        hunger == "Excessive" || "Acidity" in issues || "Loose motion" in issues -> "Tikshna Agni"
        hunger == "Irregular" || mealFeel == "Bloated" || "Gas" in issues || "Constipation" in issues -> "Vishama Agni"
        else -> "Sama Agni"
    }
}

private fun motivationFor(index: Int): String {
    return when (index) {
        0, 1 -> "Building your wellness profile"
        in 2..5 -> "Personalising your digestion map"
        in 6..8 -> "Almost there"
        else -> "Preparing your plan"
    }
}
