package com.example.vedaahar.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.VedamritBrandBrown
import com.example.vedaahar.ui.theme.VedamritBrownSoft
import com.example.vedaahar.ui.theme.VedamritGreenSoft
import java.util.Locale

private const val VoicePrefsName = "vedamrit_voice_assistant"
private const val VoiceEnabledKey = "voice_enabled"
private const val ReadQuestionsKey = "read_assessment_questions"

data class VoiceGuide(
    val title: String,
    val description: String,
    val sensitive: Boolean = false
)

class VoiceAssistantController(
    private val context: Context
) {
    private val preferences = context.getSharedPreferences(VoicePrefsName, Context.MODE_PRIVATE)
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    var enabled by mutableStateOf(preferences.getBoolean(VoiceEnabledKey, true))
        private set

    var readAssessmentQuestions by mutableStateOf(preferences.getBoolean(ReadQuestionsKey, false))
        private set

    var isSpeaking by mutableStateOf(false)
        private set

    fun attach(engine: TextToSpeech) {
        tts = engine
        ttsReady = true
        configureLanguage()
    }

    fun dispose() {
        stopSpeaking()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }

    fun toggleEnabled() {
        updateEnabled(!enabled)
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        preferences.edit().putBoolean(VoiceEnabledKey, value).apply()
        if (!value) stopSpeaking()
    }

    fun toggleReadQuestions() {
        readAssessmentQuestions = !readAssessmentQuestions
        preferences.edit().putBoolean(ReadQuestionsKey, readAssessmentQuestions).apply()
        if (!readAssessmentQuestions) stopSpeaking()
    }

    fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
    }

    fun speakScreenGuide(route: String?) {
        if (!enabled) return
        val guide = ScreenVoiceGuides.forRoute(route) ?: return
        speak("${guide.title}. ${guide.description}")
    }

    fun speakAssessmentQuestion(
        currentQuestion: Int,
        totalQuestions: Int,
        question: String,
        options: List<String>
    ) {
        if (!enabled || !readAssessmentQuestions) return
        val optionText = options.mapIndexed { index, option ->
            "Option ${('A'.code + index).toChar()}: $option."
        }.joinToString(" ")
        speak("Question $currentQuestion of $totalQuestions. $question $optionText")
    }

    fun speak(message: String) {
        if (!enabled || message.isBlank()) return
        configureLanguage()
        stopSpeaking()
        val result = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "vedamrit_voice_${System.currentTimeMillis()}")
        isSpeaking = result == TextToSpeech.SUCCESS
    }

    private fun configureLanguage() {
        if (!ttsReady) return
        val availability = tts?.setLanguage(Locale.US)
        if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.ENGLISH)
        }
    }
}

val LocalVoiceAssistantController = compositionLocalOf<VoiceAssistantController?> { null }

@Composable
fun VoiceAssistantProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val controller = remember(context) {
        VoiceAssistantController(context.applicationContext)
    }

    DisposableEffect(controller) {
        val engineRef = arrayOfNulls<TextToSpeech>(1)
        val engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engineRef[0]?.let(controller::attach)
            }
        }
        engineRef[0] = engine
        onDispose {
            controller.dispose()
        }
    }

    CompositionLocalProvider(LocalVoiceAssistantController provides controller) {
        content()
    }
}

@Composable
fun GlobalVoiceAssistantButton(
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val controller = LocalVoiceAssistantController.current ?: return
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute, controller.enabled) {
        controller.stopSpeaking()
        controller.speakScreenGuide(currentRoute)
    }

    DisposableEffect(currentRoute) {
        onDispose { controller.stopSpeaking() }
    }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .semantics {
                    contentDescription = if (controller.enabled) "Voice Assistant On" else "Voice Assistant Off"
                    role = Role.Button
                }
                .clip(RoundedCornerShape(50))
                .clickable { expanded = true },
            color = if (controller.enabled) VedamritGreenSoft else PureWhite,
            shape = RoundedCornerShape(50),
            shadowElevation = 6.dp,
            border = BorderStroke(
                1.dp,
                if (controller.enabled) ForestGreen.copy(alpha = 0.55f) else VedamritBrandBrown.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VoiceGlyph(active = controller.enabled)
                Text(
                    text = if (controller.enabled) "ON" else "OFF",
                    color = if (controller.enabled) ForestGreen else VedamritBrandBrown,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = if (controller.enabled) "Voice Assistant ON" else "Voice Assistant OFF",
                            color = if (controller.enabled) ForestGreen else VedamritBrandBrown,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                onClick = {}
            )
            DropdownMenuItem(
                text = { Text("Repeat Screen Guide") },
                onClick = {
                    expanded = false
                    controller.speakScreenGuide(currentRoute)
                }
            )
            DropdownMenuItem(
                text = { Text("Read Screen") },
                onClick = {
                    expanded = false
                    controller.speakScreenGuide(currentRoute)
                }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Read Assessment Questions", modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(checked = controller.readAssessmentQuestions, onCheckedChange = null)
                    }
                },
                onClick = { controller.toggleReadQuestions() }
            )
            DropdownMenuItem(
                text = { Text("Stop Speaking") },
                onClick = {
                    expanded = false
                    controller.stopSpeaking()
                }
            )
            DropdownMenuItem(
                text = { Text(if (controller.enabled) "Voice Assistant OFF" else "Voice Assistant ON") },
                onClick = {
                    expanded = false
                    controller.toggleEnabled()
                }
            )
        }
    }
}

@Composable
fun AssessmentQuestionVoiceEffect(
    screenKey: String,
    currentQuestion: Int,
    totalQuestions: Int,
    question: String,
    options: List<String>
) {
    val controller = LocalVoiceAssistantController.current ?: return
    LaunchedEffect(
        screenKey,
        currentQuestion,
        totalQuestions,
        question,
        options,
        controller.enabled,
        controller.readAssessmentQuestions
    ) {
        controller.stopSpeaking()
        controller.speakAssessmentQuestion(currentQuestion, totalQuestions, question, options)
    }
}

@Composable
private fun VoiceGlyph(active: Boolean) {
    val color = if (active) ForestGreen else VedamritBrandBrown
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (active) PureWhite else VedamritBrownSoft)
            .border(1.dp, color.copy(alpha = 0.22f), CircleShape)
            .padding(4.dp)
    ) {
        val micWidth = size.width * 0.28f
        val micHeight = size.height * 0.46f
        val left = (size.width - micWidth) / 2f
        val top = size.height * 0.18f
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(micWidth, micHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(micWidth / 2f, micWidth / 2f)
        )
        drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(size.width * 0.24f, size.height * 0.38f),
            size = Size(size.width * 0.52f, size.height * 0.36f),
            style = Stroke(width = 2.2f, cap = StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.5f, size.height * 0.72f),
            end = Offset(size.width * 0.5f, size.height * 0.86f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
        if (!active) {
            drawLine(
                color = DarkForestGreen,
                start = Offset(size.width * 0.25f, size.height * 0.24f),
                end = Offset(size.width * 0.75f, size.height * 0.78f),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
        }
    }
}

object ScreenVoiceGuides {
    fun forRoute(route: String?): VoiceGuide? {
        val normalized = route?.substringBefore("?") ?: return null
        return guides[normalized]
    }

    private val guides = mapOf(
        "welcome" to VoiceGuide(
            "Welcome to VedAmrit",
            "You can log in or create an account to continue."
        ),
        "login" to VoiceGuide(
            "Login",
            "Enter your login details to access your VedAmrit account."
        ),
        "consent_privacy" to VoiceGuide(
            "Consent and Privacy",
            "Review the consent information and continue only if you agree."
        ),
        "patient_profile" to VoiceGuide(
            "Profile",
            "Here you can view and update your profile information.",
            sensitive = true
        ),
        "dosha_assessment" to VoiceGuide(
            "Dosha Test",
            "This section contains your Ayurvedic health assessments. Complete all required assessments to continue."
        ),
        "dosha_retake" to VoiceGuide(
            "Assessment Results",
            "Your assessment results are available on this screen. You can review wellness information and continue."
        ),
        "diet_assessment" to VoiceGuide(
            "Personalized Diet",
            "Here you can complete Prakriti, Vikriti, Agni, and symptom assessments, then view personalized dietary guidance."
        ),
        "patient_dashboard" to VoiceGuide(
            "Dashboard",
            "From here you can access your assessments, personalized diet, medical records, and other health features."
        ),
        "shopping" to VoiceGuide(
            "Shopping",
            "Browse Ayurvedic products and add useful items to your cart."
        ),
        "yoga_meditation" to VoiceGuide(
            "Yoga and Meditation",
            "Explore guided wellness practices for breathing, movement, and relaxation."
        ),
        "health_reminder" to VoiceGuide(
            "Health Reminders",
            "Here you can review wellness reminders and routine prompts."
        ),
        "ingredient_book" to VoiceGuide(
            "Ingredient Book",
            "Explore Ayurvedic ingredients, uses, and simple wellness notes."
        ),
        "symptoms_analysis" to VoiceGuide(
            "Symptoms Analysis",
            "Tell us about the symptoms you are currently experiencing. Select the symptoms that apply to you."
        ),
        "community_care" to VoiceGuide(
            "Community Care",
            "Find nearby care options and support resources."
        ),
        "doctor_module" to VoiceGuide(
            "Doctor Consultation",
            "You can review doctor information and consultation options."
        ),
        "upload_medical_document" to VoiceGuide(
            "Upload Document",
            "Upload your medical document here. You can select a document from your device or use the camera.",
            sensitive = true
        ),
        "my_medical_documents" to VoiceGuide(
            "Medical Records",
            "Your medical records are available on this screen.",
            sensitive = true
        )
    )
}

