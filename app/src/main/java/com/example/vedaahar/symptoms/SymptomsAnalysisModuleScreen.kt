package com.example.vedaahar.symptoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.BackButton
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SymptomsWarmCard = Color(0xFFFFFBF4)
private val SymptomsNoSoft = Color(0xFFF3F7F4)
private val SymptomsYesSoft = Color(0xFFE6F4EC)
private val SymptomsNoticeSoft = Color(0xFFFFF7E6)

@Composable
fun SymptomsAnalysisModuleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var answers by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var result by remember { mutableStateOf<SymptomsAnalysisResult?>(null) }
    var autoAdvanceLocked by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF9EE), Cream, LightSage.copy(alpha = 0.52f))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack, text = "Dashboard")
                    Text(
                        text = "Symptoms Analysis",
                        color = ForestGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val completedResult = result
                    if (completedResult == null) {
                        SymptomsInputContent(
                            currentQuestionIndex = currentQuestionIndex,
                            answers = answers,
                            onAnswerSelected = { key, value ->
                                if (!autoAdvanceLocked) {
                                    autoAdvanceLocked = true
                                    val selectedIndex = currentQuestionIndex
                                    val updatedAnswers = answers + (key to value)
                                    answers = updatedAnswers
                                    coroutineScope.launch {
                                        delay(250)
                                        if (currentQuestionIndex == selectedIndex) {
                                            if (selectedIndex < symptomFeatures.lastIndex) {
                                                currentQuestionIndex = selectedIndex + 1
                                            } else if (updatedAnswers.size == symptomFeatures.size) {
                                                val completeInput = symptomFeatures.associate { feature ->
                                                    feature.key to updatedAnswers.getValue(feature.key)
                                                }
                                                val analysis = generateSymptomsAnalysisResult(completeInput)
                                                SymptomsAnalysisStore.save(context, analysis)
                                                result = analysis
                                            }
                                        }
                                        autoAdvanceLocked = false
                                    }
                                }
                            },
                            onPrevious = {
                                currentQuestionIndex = (currentQuestionIndex - 1).coerceAtLeast(0)
                            }
                        )
                    } else {
                        SymptomsResultContent(
                            result = completedResult,
                            onRetake = {
                                answers = emptyMap()
                                currentQuestionIndex = 0
                                result = null
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp).navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun SymptomsInputContent(
    currentQuestionIndex: Int,
    answers: Map<String, Int>,
    onAnswerSelected: (key: String, value: Int) -> Unit,
    onPrevious: () -> Unit
) {
    val symptom = symptomFeatures[currentQuestionIndex]
    val selectedValue = answers[symptom.key]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = ForestGreen.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SymptomsWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Symptoms Analysis",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 35.sp,
                color = DarkForestGreen
            )
            Text(
                text = "Select the symptoms you are currently experiencing.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = SoftBlueGray
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Question ${currentQuestionIndex + 1} of ${symptomFeatures.size}",
                    color = ForestGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${answers.size}/${symptomFeatures.size} answered",
                    color = SageGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = { answers.size.toFloat() / symptomFeatures.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = ForestGreen,
                trackColor = LightSage
            )

            Text(
                text = "Do you experience ${symptom.label}?",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                lineHeight = 29.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SymptomYesNoCard(
                    label = "No",
                    selected = selectedValue == 0,
                    background = SymptomsNoSoft,
                    onClick = { onAnswerSelected(symptom.key, 0) },
                    modifier = Modifier.weight(1f)
                )
                SymptomYesNoCard(
                    label = "Yes",
                    selected = selectedValue == 1,
                    background = SymptomsYesSoft,
                    onClick = { onAnswerSelected(symptom.key, 1) },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = onPrevious,
                enabled = currentQuestionIndex > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, BeigeBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
            ) {
                Text("Previous", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SymptomYesNoCard(
    label: String,
    selected: Boolean,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) background else Color(0xFFFFFDF8))
            .border(BorderStroke(if (selected) 1.7.dp else 1.dp, if (selected) ForestGreen else BeigeBorder), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) ForestGreen else PureWhite)
                .border(BorderStroke(1.dp, if (selected) ForestGreen else BeigeBorder), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(label, color = DarkForestGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SymptomsResultContent(
    result: SymptomsAnalysisResult,
    onRetake: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = ForestGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SymptomsWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Symptoms Analysis Result",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 35.sp
            )
            ReportedSymptomsCard(result.reportedSymptoms)
            Text(
                text = "No validated prediction model is connected, so this page only summarizes the symptoms you selected. This symptom analysis is separate from Prakriti, Vikriti, and Agni.",
                color = SoftBlueGray,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, BeigeBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
            ) {
                Text("Analyze Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReportedSymptomsCard(reportedSymptoms: List<SymptomFeature>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.78f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reported Symptoms", color = SageGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            if (reportedSymptoms.isEmpty()) {
                Text("No symptoms were selected as Yes.", color = SoftBlueGray, fontSize = 13.sp, lineHeight = 19.sp)
            } else {
                reportedSymptoms.forEach { symptom ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ForestGreen)
                        )
                        Text(symptom.label, color = DarkForestGreen, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
