package com.example.vedaahar.diet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.agentic.*
import com.example.vedaahar.assessment.AssessmentSyncStatus
import com.example.vedaahar.profile.StoredPatientProfile
import com.example.vedaahar.ui.theme.*
import kotlinx.coroutines.launch

private val DietOutline = Color(0xFFE0E8E0)
private val DietMuted = Color(0xFF52635A)
private val DietCaution = Color(0xFFFFF4DF)
private val DietCautionText = Color(0xFF7B4309)

@Composable
fun PersonalizedDietContent(
    state: AgenticPlanUiState,
    profile: StoredPatientProfile?,
    syncStatus: AssessmentSyncStatus?,
    assessmentsComplete: Boolean,
    onBack: () -> Unit,
    onGenerate: () -> Unit,
    onRefresh: () -> Unit,
    onTakeAssessment: () -> Unit,
    modifier: Modifier = Modifier,
    pdfActions: @Composable () -> Unit = {},
    details: @Composable () -> Unit = {}
) {
    val saved = (state as? AgenticPlanUiState.Success)?.plan
    val data = remember(saved, profile) { saved?.personalizedDietData(profile) }
    val usable = data?.usable == true && syncStatus == AssessmentSyncStatus.SYNCED && assessmentsComplete
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedDay by rememberSaveable(saved?.id, saved?.assessmentVersion, saved?.profileVersion) { mutableIntStateOf(1) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val busy = state is AgenticPlanUiState.Loading || (state as? AgenticPlanUiState.Success)?.refreshing == true
    Surface(modifier.fillMaxSize(), color = Color.White, contentColor = DarkForestGreen) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkForestGreen)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LeafMark(Modifier.size(22.dp))
                            Text("VedAmrit", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                                fontSize = 22.sp, color = DarkForestGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = { scope.launch { list.animateScrollToItem(8) } },
                        modifier = Modifier.semantics { contentDescription = "Jump to diet report download" }.size(48.dp)) {
                        Canvas(Modifier.size(24.dp)) {
                            val stroke = 2.dp.toPx()
                            drawLine(ForestGreen, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height * .65f), stroke)
                            drawLine(ForestGreen, Offset(size.width * .25f, size.height * .4f), Offset(size.width / 2, size.height * .65f), stroke)
                            drawLine(ForestGreen, Offset(size.width * .75f, size.height * .4f), Offset(size.width / 2, size.height * .65f), stroke)
                            drawLine(ForestGreen, Offset(0f, size.height * .75f), Offset(0f, size.height), stroke)
                            drawLine(ForestGreen, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                            drawLine(ForestGreen, Offset(size.width, size.height * .75f), Offset(size.width, size.height), stroke)
                        }
                    }
                }
                Text("Ancient Wisdom for Modern Living", fontSize = 10.sp, color = DietMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("personalized-diet-list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                item(key = "title") {
                    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("My Personalized Diet Plan", fontFamily = FontFamily.Serif, fontSize = 28.sp,
                            lineHeight = 33.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                        Text("Based on your Prakriti, Vikriti, Agni and health assessment",
                            color = DietMuted, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
                item(key = "status") {
                    PlanStatusBanner(state)
                }
                item(key = "warnings") {
                    val warnings = data?.warnings.orEmpty()
                    if (warnings.isNotEmpty()) {
                        Card(Modifier.widthIn(max = 840.dp).fillMaxWidth().testTag("diet-warnings"),
                            colors = CardDefaults.cardColors(containerColor = DietCaution),
                            shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Validation & safety notes", color = DietCautionText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                warnings.forEach { Text("• $it", color = DietCautionText, fontSize = 13.sp, lineHeight = 19.sp) }
                            }
                        }
                    }
                    if (state is AgenticPlanUiState.Error) {
                        Text(state.message, color = Color(0xFF962F29), fontSize = 14.sp, lineHeight = 20.sp,
                            modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().testTag("diet-error"))
                    }
                }
                item(key = "controls") {
                    BoxWithConstraints(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.3f
                        val generateButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                            Button(onClick = onGenerate,
                                enabled = !busy && state !is AgenticPlanUiState.Pending && assessmentsComplete && syncStatus == AssessmentSyncStatus.SYNCED,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = Color.White),
                                modifier = buttonModifier.heightIn(min = 48.dp).testTag("diet-generate"),
                                shape = RoundedCornerShape(14.dp)) { Text("Generate", fontWeight = FontWeight.SemiBold) }
                        }
                        val refreshButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                            OutlinedButton(onClick = onRefresh, enabled = !busy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                                border = BorderStroke(1.dp, DietOutline), shape = RoundedCornerShape(14.dp),
                                modifier = buttonModifier.heightIn(min = 48.dp).testTag("diet-refresh")) {
                                Text("Refresh", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            generateButton(Modifier.fillMaxWidth())
                            refreshButton(Modifier.fillMaxWidth())
                        } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            generateButton(Modifier.weight(1f))
                            refreshButton(Modifier.weight(1f))
                        }
                    }
                }
                item(key = "days") {
                    DayTabs(selectedDay, { selectedDay = it })
                }
                item(key = "meals") {
                    if (usable) {
                        val day = data?.day(selectedDay)
                        if (day != null) SavedDayCard(day)
                        else EmptyDietCard("Day $selectedDay", "No meal schedule for this day was included in the saved plan.")
                    } else {
                        val message = when {
                            !assessmentsComplete -> "Complete Prakriti, Vikriti and Agni to create your personalized plan."
                            syncStatus != AssessmentSyncStatus.SYNCED -> "Sync or restore all three assessments before loading personalized meals."
                            state is AgenticPlanUiState.Loading -> "Loading your saved plan…"
                            state is AgenticPlanUiState.Pending -> state.message
                            state is AgenticPlanUiState.Error -> "Your personalized meals could not be loaded. Check the message above and retry Refresh."
                            saved != null -> "This plan is stale, restricted or did not pass validation. Personalized recommendations are not shown as usable guidance."
                            else -> "No saved personalized plan is loaded. Generate a plan or refresh an existing one."
                        }
                        EmptyDietCard("Your saved meal plan", message)
                        if (!assessmentsComplete) {
                            TextButton(onClick = onTakeAssessment, modifier = Modifier.heightIn(min = 48.dp)) { Text("Take assessments", color = ForestGreen) }
                        }
                    }
                }
                item(key = "guidance") {
                    if (usable && data != null) FoodGuidance(data)
                }
                item(key = "recommendations") {
                    if (usable) {
                        val lifestyle = data?.content?.lifestyle
                        if (lifestyle != null) SavedRecommendations(lifestyle)
                    }
                }
                item(key = "pdf") {
                    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().testTag("diet-report-actions")) { pdfActions() }
                }
                item(key = "details") {
                    Card(Modifier.widthIn(max = 840.dp).fillMaxWidth(),
                        border = BorderStroke(1.dp, DietOutline), shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = { showDetails = !showDetails },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("diet-details-toggle")) {
                                Text("Plan Details & Sync", color = DarkForestGreen, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f))
                                Text(if (showDetails) "−" else "+", color = ForestGreen, fontSize = 22.sp)
                            }
                            if (showDetails) details()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeafMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * .13f, size.height * .9f)
            cubicTo(size.width * .1f, size.height * .2f, size.width * .65f, size.height * .13f, size.width * .9f, 0f)
            cubicTo(size.width, size.height * .67f, size.width * .48f, size.height, size.width * .13f, size.height * .9f)
        }
        drawPath(path, Color(0xFF7BAA41))
        drawLine(Color.White, Offset(size.width * .14f, size.height * .9f),
            Offset(size.width * .7f, size.height * .22f), strokeWidth = 1.8.dp.toPx())
    }
}

@Composable
private fun PlanStatusBanner(state: AgenticPlanUiState) {
    val plan = (state as? AgenticPlanUiState.Success)?.plan
    val approved = plan?.doctorStatus == "APPROVED"
    Card(Modifier.widthIn(max = 840.dp).fillMaxWidth().testTag("diet-status"),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = LightSage)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).background(ForestGreen, CircleShape).padding(10.dp)) { LeafMark(Modifier.fillMaxSize()) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(when {
                        approved -> "Doctor-reviewed plan"
                        plan != null -> "AI-Generated Draft — Not Doctor Reviewed"
                        state is AgenticPlanUiState.Loading -> "Loading your personalized plan"
                        else -> "Your personalized plan"
                    }, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = DarkForestGreen,
                        fontSize = 17.sp, lineHeight = 23.sp)
                    Text(if (plan != null) "Saved from your assessment. This draft is not a diagnosis or prescription."
                        else "Your saved AI plan will appear here after assessment synchronization.",
                        color = DietMuted, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
            if (state is AgenticPlanUiState.Loading || (state as? AgenticPlanUiState.Success)?.refreshing == true) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = ForestGreen)
            }
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("Doctor status", plan?.doctorStatus ?: "Not available", true)
                StatusPill("Validation", plan?.validationStatus ?: "Not available", false)
            }
            (state as? AgenticPlanUiState.Success)?.notice?.let { Text(it, fontSize = 13.sp, color = DietMuted) }
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String, doctor: Boolean) {
    Column(Modifier.background(if (doctor && value != "APPROVED") DietCaution else Color.White, RoundedCornerShape(12.dp))
        .padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(label, fontSize = 11.sp, color = DietMuted)
        Text(value.replace('_', ' '), fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (doctor && value != "APPROVED") DietCautionText else DarkForestGreen)
    }
}

@Composable
fun DayTabs(selectedDay: Int, onSelect: (Int) -> Unit) {
    LazyRow(Modifier.widthIn(max = 840.dp).fillMaxWidth().testTag("diet-day-tabs"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items((1..7).toList()) { day ->
            val selected = day == selectedDay
            Surface(shape = RoundedCornerShape(50), color = if (selected) ForestGreen else Color.White,
                border = BorderStroke(1.dp, if (selected) ForestGreen else DietOutline)) {
                Box(Modifier.selectable(selected, onClick = { onSelect(day) }, role = Role.Tab)
                    .widthIn(min = 72.dp).heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("diet-day-tab-$day"), contentAlignment = Alignment.Center) {
                    Text("Day $day", color = if (selected) Color.White else DarkForestGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SavedDayCard(day: AiDietDay) {
    Card(Modifier.widthIn(max = 840.dp).fillMaxWidth().testTag("diet-selected-day"),
        shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, DietOutline),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Day ${day.day ?: "—"}" + (day.title?.takeIf(String::isNotBlank)?.let { " — $it" } ?: ""),
                fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
            savedDayDate(day.date)?.let { Text(it, color = DietMuted, fontSize = 13.sp) }
            day.description?.takeIf(String::isNotBlank)?.let { Text(it, color = DietMuted, fontSize = 14.sp, lineHeight = 20.sp) }
            val categories = listOf("Breakfast", "Lunch", "Evening Snack", "Dinner")
            categories.forEachIndexed { index, category ->
                val meals = day.meals.filter { mealCategory(it.name) == category }
                if (index != 0) HorizontalDivider(color = DietOutline)
                if (meals.isEmpty()) {
                    Text(category, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("No saved meal was supplied for this section.", color = DietMuted, fontSize = 13.sp)
                } else meals.forEach { SavedMealSection(it) }
            }
            day.meals.filter { mealCategory(it.name) !in categories }.forEach { SavedMealSection(it) }
            if (day.dailyTotal.isNotEmpty()) {
                HorizontalDivider(color = DietOutline)
                Text("Daily nutrition", fontWeight = FontWeight.Bold, color = DarkForestGreen, fontSize = 15.sp)
                NutrientLabels(day.dailyTotal)
            }
        }
    }
}

@Composable
private fun MealSymbol(category: String) {
    val background = when (category) {
        "Breakfast" -> Color(0xFFFFF0CE)
        "Lunch" -> Color(0xFFE1F2DA)
        "Evening Snack" -> Color(0xFFF0E5FC)
        else -> Color(0xFFE4F0FD)
    }
    Canvas(Modifier.size(44.dp).background(background, RoundedCornerShape(14.dp)).padding(8.dp)
        .semantics { contentDescription = "Generic meal symbol" }) {
        drawCircle(ForestGreen, radius = size.minDimension * .34f, style = Stroke(1.6.dp.toPx()))
        drawCircle(ForestGreen.copy(alpha = .4f), radius = size.minDimension * .23f, style = Stroke(1.dp.toPx()))
        drawLine(ForestGreen, Offset(1f, 0f), Offset(1f, size.height), strokeWidth = 1.5.dp.toPx())
        drawLine(ForestGreen, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), strokeWidth = 1.5.dp.toPx())
    }
}

@Composable
private fun SavedMealSection(meal: AiMeal) {
    val category = mealCategory(meal.name)
    val nutrients = mealNutrients(meal)
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("diet-meal-$category")) {
        val wide = maxWidth >= 500.dp && LocalDensity.current.fontScale < 1.3f
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            MealSymbol(category)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(category, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = DarkForestGreen, fontSize = 19.sp)
                if (meal.foods.isEmpty()) Text("Food details were not included.", color = DietMuted, fontSize = 13.sp)
                meal.foods.forEach { food ->
                    Text(food.name + (food.portion?.let { " · $it" } ?: ""),
                        color = DarkForestGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
                    food.description?.takeIf(String::isNotBlank)?.let { Text(it, color = DietMuted, fontSize = 13.sp, lineHeight = 18.sp) }
                }
                meal.description?.takeIf(String::isNotBlank)?.let { Text(it, color = DietMuted, fontSize = 13.sp, lineHeight = 19.sp) }
                if (!wide) NutritionBox(nutrients)
                var expanded by remember(meal) { mutableStateOf(false) }
                if (meal.foods.any { it.nutrition.isNotEmpty() || it.rankingReasons.isNotEmpty() } || meal.targetCalories != null) {
                    TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (expanded) "Hide meal details" else "Meal details", color = ForestGreen, fontSize = 13.sp)
                    }
                    if (expanded) {
                        meal.targetCalories?.let { Text("Saved calorie target: $it", fontSize = 12.sp, color = DietMuted) }
                        meal.foods.forEach { food ->
                            Text(food.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (food.nutrition.isNotEmpty()) NutrientLabels(food.nutrition)
                            if (food.rankingReasons.isNotEmpty()) Text(food.rankingReasons.joinToString("; "), fontSize = 12.sp, color = DietMuted)
                        }
                    }
                }
            }
            if (wide) Box(Modifier.width(175.dp)) { NutritionBox(nutrients) }
        }
    }
}

@Composable
private fun NutritionBox(nutrients: List<NutritionMetric>) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFF3F8EF), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Nutritional info", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = DarkForestGreen, fontSize = 14.sp)
        if (nutrients.isEmpty()) Text("No meal totals were supplied.", color = DietMuted, fontSize = 12.sp)
        else NutrientLabels(nutrients)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NutrientLabels(nutrients: List<NutritionMetric>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        nutrients.filter { it.label != "Basis" }.forEach {
            val unit = if (it.label == "Calories") "kcal" else "g"
            Text("${it.label}: ${it.value} $unit", fontSize = 12.sp, lineHeight = 18.sp, color = DarkForestGreen)
        }
    }
}

@Composable
private fun FoodGuidance(data: PersonalizedDietData) {
    BoxWithConstraints(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
        val columns = maxWidth >= 300.dp && LocalDensity.current.fontScale <= 1.2f
        if (columns) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { GuidanceCard("Foods to Eat", data.foodsToEat, data.foodsToEatSource, true) }
            Box(Modifier.weight(1f)) { GuidanceCard("Foods to Avoid", data.foodsToAvoid, data.foodsToAvoidSource, false) }
        } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GuidanceCard("Foods to Eat", data.foodsToEat, data.foodsToEatSource, true)
            GuidanceCard("Foods to Avoid", data.foodsToAvoid, data.foodsToAvoidSource, false)
        }
    }
}

@Composable
private fun GuidanceCard(title: String, entries: List<String>, source: String, positive: Boolean) {
    var expanded by remember(entries) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (positive) Color(0xFFF1F7ED) else Color(0xFFFFF4E8))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = if (positive) DarkForestGreen else DietCautionText)
            Text(source, fontSize = 11.sp, lineHeight = 16.sp, color = DietMuted)
            if (entries.isEmpty()) Text("No separate food guidance was supplied.", fontSize = 13.sp, lineHeight = 19.sp, color = DietMuted)
            (if (expanded) entries else entries.take(4)).forEach { Text("• $it", fontSize = 13.sp, lineHeight = 19.sp, color = DarkForestGreen) }
            if (entries.size > 4) TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(0.dp)) { Text(if (expanded) "Show less" else "Show all ${entries.size}", color = ForestGreen) }
        }
    }
}

@Composable
private fun SavedRecommendations(plan: AiLifestylePlan) {
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().background(LightSage, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Personalized Recommendations", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            fontSize = 21.sp, lineHeight = 27.sp, color = DarkForestGreen)
        if (plan.recommendations.isEmpty()) Text("No lifestyle recommendations were included in the saved plan.", color = DietMuted)
        plan.recommendations.forEach { RecommendationCard(it) }
    }
}

@Composable
fun RecommendationCard(item: AiLifestyleRecommendation) {
    var expanded by remember(item) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item.category?.let { Text(it.replace('_', ' ').replaceFirstChar(Char::titlecase),
            fontWeight = FontWeight.Bold, color = DarkForestGreen, fontSize = 15.sp) }
        item.recommendation?.let { Text(it, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp, lineHeight = 21.sp, color = DarkForestGreen) }
        // Safety stays visible independently of the expanded technical/source explanation.
        item.safetyConsideration?.let { Text(it, color = DietCautionText, fontSize = 12.sp, lineHeight = 18.sp) }
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(0.dp)) {
            Text(if (expanded) "Show less" else "Read more & sources", color = ForestGreen, fontSize = 13.sp)
        }
        if (expanded) {
            item.reason?.let { Text(it, color = DietMuted, fontSize = 13.sp, lineHeight = 19.sp) }
            item.source?.let {
                Text("Retrieved source: ${it.source}" + (it.page?.let { page -> " · Page $page" } ?: ""),
                    color = DietMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Text("Retrieval does not clinically verify this recommendation.", color = DietMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyDietCard(title: String, message: String) {
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().background(Color(0xFFF7F9F6), RoundedCornerShape(18.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontFamily = FontFamily.Serif, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
        Text(message, color = DietMuted, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

