package com.example.vedaahar

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

private val SymptomsCardBg = Color(0xFFFCFAF5)
private val SymptomsBlueSoft = Color(0xFFEDF5FC)
private val SymptomsGreenSoft = Color(0xFFEBF5EA)
private val SymptomsAmberSoft = Color(0xFFFFF7E6)

data class SymptomEntry(
    val name: String,
    val classicalName: String,
    val doshaTag: String,
    val doshaColor: Color,
    val rootCause: String,
    val kitchenRemedy: String,
    val dietAdjustment: String,
    val lifestyleTip: String
)

data class SymptomCategory(
    val title: String,
    val subtitle: String,
    val symptoms: List<SymptomEntry>
)

private val symptomCategories = listOf(
    SymptomCategory(
        title = "Digestive & Gut",
        subtitle = "Agni & Koshta imbalances",
        symptoms = listOf(
            SymptomEntry(
                name = "Acidity & Sour Reflux",
                classicalName = "Amlapitta",
                doshaTag = "Pitta Aggravation",
                doshaColor = Color(0xFFD97736),
                rootCause = "Elevated Ushna (hot) and Tikshna (sharp) gunas in Pachaka Pitta, caused by spicy food, skipped meals, or stress.",
                kitchenRemedy = "Drink cooling fennel (saunf) and coriander seed infusion twice daily after meals.",
                dietAdjustment = "Favor cooling sweet fruits, cucumber, and coconut water. Strictly avoid excessive chilies, vinegar, and deep-fried items.",
                lifestyleTip = "Avoid lying down immediately after meals. Keep dinner light and at least 2 hours before sleep."
            ),
            SymptomEntry(
                name = "Bloating & Gas",
                classicalName = "Adhmana / Anaha",
                doshaTag = "Vata Aggravation",
                doshaColor = Color(0xFF5B8CA8),
                rootCause = "Disturbed Samana and Apana Vata causing irregular digestive fire (Vishama Agni) and air accumulation.",
                kitchenRemedy = "Warm water with a pinch of roasted cumin, asafoetida (hing), and rock salt (saindhava).",
                dietAdjustment = "Favor warm, freshly cooked soupy foods cooked with ghee. Avoid raw salads, dry snacks, and carbonated drinks.",
                lifestyleTip = "Eat in a calm, seated posture without multitasking. Chew each bite slowly to reduce swallowed air."
            ),
            SymptomEntry(
                name = "Sluggish Digestion & Heaviness",
                classicalName = "Alasaka / Mandagni",
                doshaTag = "Kapha Aggravation",
                doshaColor = Color(0xFF6B9B76),
                rootCause = "Sluggish digestive fire (Manda Agni) smothered by heavy, oily, cold food or prolonged inactivity.",
                kitchenRemedy = "Chew a thin slice of fresh ginger with a drop of lemon juice and rock salt 10 minutes before meals.",
                dietAdjustment = "Favor light grains like barley and millets, steamed greens, and warming spices (black pepper, cinnamon).",
                lifestyleTip = "Take a 100-step gentle walk (Shatapadi) after meals. Avoid sleeping during daytime."
            ),
            SymptomEntry(
                name = "Irregular Bowel Movement",
                classicalName = "Vibandha",
                doshaTag = "Apana Vata",
                doshaColor = Color(0xFF5B8CA8),
                rootCause = "Dryness (Ruksha guna) in the colon obstructing the natural downward movement of Apana Vata.",
                kitchenRemedy = "Drink a cup of warm milk or warm water with 1 teaspoon of pure cow's ghee at bedtime.",
                dietAdjustment = "Increase warm hydration, soaked raisins, prunes, and well-cooked fibrous vegetables.",
                lifestyleTip = "Establish a consistent morning toilet routine without forcing. Practice gentle morning pelvic stretches."
            )
        )
    ),
    SymptomCategory(
        title = "Mind & Nervous",
        subtitle = "Prana & Manas rhythms",
        symptoms = listOf(
            SymptomEntry(
                name = "Restlessness & Overthinking",
                classicalName = "Chitta Anavastha",
                doshaTag = "Prana Vata",
                doshaColor = Color(0xFF5B8CA8),
                rootCause = "Hyperactive Chala (mobile) guna of Vata agitating mental channels (Mano Vaha Srotas).",
                kitchenRemedy = "Warm spiced almond or oat milk with a pinch of nutmeg and crushed cardamom before bed.",
                dietAdjustment = "Avoid caffeine, energy drinks, and erratic fasting. Eat grounding, warm meals with healthy fats.",
                lifestyleTip = "Practice 10 minutes of Anulom Vilom (alternate nostril breathwork) and apply warm sesame oil to foot soles."
            ),
            SymptomEntry(
                name = "Irritability & Mental Burnout",
                classicalName = "Krodha / Pitta Manas",
                doshaTag = "Sadhaka Pitta",
                doshaColor = Color(0xFFD97736),
                rootCause = "Excess mental heat, intense goal-orientation, and lack of parasympathetic cooling time.",
                kitchenRemedy = "Rose water infused herbal tea or fresh coconut water with mint sprigs.",
                dietAdjustment = "Reduce pungent spices, fermented condiments, and salty snacks. Favor sweet, bitter, and astringent flavors.",
                lifestyleTip = "Schedule non-screen cooling walks outdoors. Practice Sheetali (cooling tongue) pranayama."
            ),
            SymptomEntry(
                name = "Morning Brain Fog & Lethargy",
                classicalName = "Tandra / Mano Manda",
                doshaTag = "Tamas & Kapha",
                doshaColor = Color(0xFF6B9B76),
                rootCause = "Excess Kapha and Tamas stagnating mental alertness, often linked with late heavy dinners.",
                kitchenRemedy = "Warm water with fresh tulsi leaves, crushed black pepper, and a drop of organic honey.",
                dietAdjustment = "Keep dinners extremely light (mung soup or roasted vegetables). Stop eating after 7:30 PM.",
                lifestyleTip = "Wake up before sunrise during the Vata time window (before 6:00 AM) and practice brisk morning walking."
            )
        )
    ),
    SymptomCategory(
        title = "Skin & Heat",
        subtitle = "Pitta & Rakta tissue signs",
        symptoms = listOf(
            SymptomEntry(
                name = "Flushed Skin & Heat Sensitivity",
                classicalName = "Daha / Raktapitta",
                doshaTag = "Bhrajaka Pitta",
                doshaColor = Color(0xFFD97736),
                rootCause = "Excess internal heat manifesting in the blood tissue (Rakta Dhatu) and surface micro-capillaries.",
                kitchenRemedy = "Fresh aloe vera pulp mixed with a pinch of turmeric applied topically or 15ml fresh juice on empty stomach.",
                dietAdjustment = "Favor bitter gourd, zucchini, leafy greens, pomegranate, and coriander. Avoid red chili and alcohol.",
                lifestyleTip = "Avoid direct mid-day sun exposure. Use sandalwood or vetiver (khus) mist on face and neck."
            ),
            SymptomEntry(
                name = "Dry & Flaky Skin",
                classicalName = "Rukshata",
                doshaTag = "Vata Dryness",
                doshaColor = Color(0xFF5B8CA8),
                rootCause = "Depletion of bodily unctuousness (Sneha) and moisture in Rasa and Rakta channels.",
                kitchenRemedy = "Daily intake of 1-2 teaspoons of A2 ghee in warm meals, plus warm sesame oil Abhyanga before bath.",
                dietAdjustment = "Incorporate soaked walnuts, chia seeds, avocados, and warm broths into the weekly routine.",
                lifestyleTip = "Take warm, not scalding hot, showers. Avoid harsh sulfate soaps that strip natural sebum."
            )
        )
    ),
    SymptomCategory(
        title = "Congestion & Heaviness",
        subtitle = "Kapha & Meda balance",
        symptoms = listOf(
            SymptomEntry(
                name = "Post-Meal Heaviness & Drowsiness",
                classicalName = "Gaurava",
                doshaTag = "Kledaka Kapha",
                doshaColor = Color(0xFF6B9B76),
                rootCause = "Excessive moisture and heaviness in upper stomach coating Agni and slowing metabolic exchange.",
                kitchenRemedy = "Sip warm cumin-ginger water throughout the afternoon instead of cold plain water.",
                dietAdjustment = "Reduce portion sizes by 25%. Leave one-third of the stomach empty for digestive air and fire.",
                lifestyleTip = "Avoid reclining or sitting slouched after meals. Maintain upright spinal posture."
            ),
            SymptomEntry(
                name = "Seasonal Nasal Congestion",
                classicalName = "Pratishyaya",
                doshaTag = "Shleshaka Kapha",
                doshaColor = Color(0xFF6B9B76),
                rootCause = "Mucus build-up in respiratory passages triggered by cold winds, ice, or excessive dairy products.",
                kitchenRemedy = "Steam inhalation with 2 drops of eucalyptus oil and a pinch of turmeric powder.",
                dietAdjustment = "Avoid yogurt, cheese, banana, and chilled beverages at night. Favor warm spiced rasam.",
                lifestyleTip = "Use a warm saline nasal rinse (Jala Neti) under proper guidance. Keep chest and neck warm."
            )
        )
    ),
    SymptomCategory(
        title = "Toxin Accumulation",
        subtitle = "Ama indicators & markers",
        symptoms = listOf(
            SymptomEntry(
                name = "Coated White Tongue & Bad Breath",
                classicalName = "Jihwa Lepa / Ama",
                doshaTag = "Metabolic Toxins (Ama)",
                doshaColor = Color(0xFF9E7740),
                rootCause = "Undigested food residues fermenting in the gut and circulating as sticky systemic Ama.",
                kitchenRemedy = "Daily copper or stainless steel tongue scraping followed by warm water gargle with a pinch of rock salt.",
                dietAdjustment = "Follow a 24-hour digestive reset: light mung bean soup (kitchari) and boiled water with ginger slices.",
                lifestyleTip = "Never eat when previous meal is still undigested. Wait for genuine hunger before consuming food."
            ),
            SymptomEntry(
                name = "Unexplained Fatigue with Stiffness",
                classicalName = "Aalasya / Sandhi Stambha",
                doshaTag = "Ama in Tissues",
                doshaColor = Color(0xFF9E7740),
                rootCause = "Ama settling in joints and muscular channels, creating a feeling of heaviness without physical labor.",
                kitchenRemedy = "Drink Trikatu powder (equal parts dry ginger, black pepper, long pepper) with warm water before meals.",
                dietAdjustment = "Avoid heavy sweets, deep-fried snacks, and nightshades until digestive fire normalizes.",
                lifestyleTip = "Engage in dry heat fomentation or warm herbal baths. Perform gentle joint-freeing exercises (Sukshma Vyayama)."
            )
        )
    )
)

@Composable
fun SymptomsAnalysisScreen(
    onBack: () -> Unit,
    onConsultDoctor: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val activeCategory = symptomCategories[selectedCategoryIndex]

    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF9EE), Cream, LightSage.copy(alpha = 0.55f))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack, text = "Dashboard")
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SymptomsBlueSoft)
                            .border(BorderStroke(1.dp, Color(0xFFC7DDF0)), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Clinical Guide",
                            color = Color(0xFF1E527D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.1.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Card
                    SymptomsHeroCard()

                    // Category Tabs
                    Text(
                        text = "SYMPTOM CATEGORIES",
                        color = SageGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    ScrollableTabRow(
                        selectedTabIndex = selectedCategoryIndex,
                        containerColor = Color.Transparent,
                        contentColor = ForestGreen,
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        symptomCategories.forEachIndexed { index, cat ->
                            Tab(
                                selected = selectedCategoryIndex == index,
                                onClick = { selectedCategoryIndex = index },
                                text = {
                                    Text(
                                        text = cat.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }

                    // Category Subtitle
                    Text(
                        text = "${activeCategory.title} (${activeCategory.subtitle})",
                        color = DarkForestGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    // Symptoms in Selected Category
                    activeCategory.symptoms.forEach { symptom ->
                        SymptomDetailCard(symptom = symptom)
                    }

                    // Doctor Consultation Advisory
                    DoctorAdvisoryCard(onConsultDoctor = onConsultDoctor)

                    Spacer(modifier = Modifier.height(32.dp).navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun SymptomsHeroCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = ForestGreen.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SymptomsCardBg),
        border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(LightSage)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AYURVEDIC SYMPTOM GUIDE",
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.3.sp
                )
            }

            Text(
                text = "Understand Your Bodily Signals with Ayurveda",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                color = DarkForestGreen
            )

            Text(
                text = "In Ayurvedic medicine, discomfort and symptoms are meaningful signals indicating Dosha elevation (Vikriti) or digestive toxin (Ama) accumulation. Explore balanced clinical insights, root causes, and kitchen remedies below.",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = SoftBlueGray
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroStatBadge("Doshas", "Vata • Pitta • Kapha", Modifier.weight(1f))
                HeroStatBadge("Root Cause", "Agni & Ama", Modifier.weight(1f))
                HeroStatBadge("Remedies", "Kitchen Herbs", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStatBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.7f)), RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        Text(label.uppercase(), color = SageGreen, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomDetailCard(symptom: SymptomEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title and Dosha Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = symptom.name,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkForestGreen
                    )
                    Text(
                        text = "Classical: ${symptom.classicalName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SageGreen
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(symptom.doshaColor.copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, symptom.doshaColor.copy(alpha = 0.35f)), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(symptom.doshaColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = symptom.doshaTag,
                        color = symptom.doshaColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }
            }

            // Root Cause Section
            SymptomSectionBlock(
                title = "Root Cause (Nidana)",
                content = symptom.rootCause,
                accent = symptom.doshaColor
            )

            // Kitchen Remedy
            SymptomSectionBlock(
                title = "Kitchen Herbal Remedy (Upachara)",
                content = symptom.kitchenRemedy,
                accent = ForestGreen
            )

            // Dietary Guidance
            SymptomSectionBlock(
                title = "Dietary Adjustment (Pathya)",
                content = symptom.dietAdjustment,
                accent = SageGreen
            )

            // Lifestyle Advice
            SymptomSectionBlock(
                title = "Lifestyle Rhythm (Vihara)",
                content = symptom.lifestyleTip,
                accent = Color(0xFF5B8CA8)
            )
        }
    }
}

@Composable
private fun SymptomSectionBlock(
    title: String,
    content: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF9FBF7))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.6f)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title.uppercase(),
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp
            )
            Text(
                text = content,
                color = Color(0xFF334438),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun DoctorAdvisoryCard(onConsultDoctor: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F7F2)),
        border = BorderStroke(1.dp, LightSage)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Clinical Consultation Guidance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkForestGreen
                )
            }

            Text(
                text = "Home remedies provide gentle symptomatic support for mild imbalances. If you experience persistent pain, severe indigestion, unexplained weight loss, or high fever, please consult a qualified Ayurvedic physician or medical doctor.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = SoftBlueGray
            )

            Button(
                onClick = onConsultDoctor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestGreen,
                    contentColor = PureWhite
                )
            ) {
                Text("Find an Ayurvedic Doctor in Community", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
