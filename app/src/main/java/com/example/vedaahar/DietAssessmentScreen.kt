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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.agni.AgniOption
import com.example.vedaahar.agni.AgniResult
import com.example.vedaahar.agni.AgniResultStore
import com.example.vedaahar.agni.AgniType
import com.example.vedaahar.agni.agniQuestions
import com.example.vedaahar.agni.generateAgniResult
import com.example.vedaahar.assessment.AssessmentProfileStore
import com.example.vedaahar.dosha.DoshaResultStore
import com.example.vedaahar.prakriti.PrakritiDosha
import com.example.vedaahar.prakriti.PrakritiOption
import com.example.vedaahar.prakriti.PrakritiResult
import com.example.vedaahar.prakriti.generatePrakritiResult
import com.example.vedaahar.prakriti.prakritiQuestions
import com.example.vedaahar.symptoms.SymptomFeature
import com.example.vedaahar.symptoms.SymptomsAnalysisResult
import com.example.vedaahar.symptoms.SymptomsAnalysisStore
import com.example.vedaahar.symptoms.generateSymptomsAnalysisResult
import com.example.vedaahar.symptoms.symptomFeatures
import com.example.vedaahar.vikriti.VikritiDosha
import com.example.vedaahar.vikriti.VikritiAnswerOption
import com.example.vedaahar.vikriti.VikritiResult
import com.example.vedaahar.vikriti.VikritiResultStore
import com.example.vedaahar.vikriti.generateVikritiResult
import com.example.vedaahar.vikriti.vikritiAnswerOptions
import com.example.vedaahar.vikriti.vikritiQuestions
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

private val DietWarmCard = Color(0xFFFFFBF4)
private val DietGreenGlow = Color(0xFF90C987)
private val DietMint = Color(0xFFDDEFE2)
private val DietSky = Color(0xFFDDEBFF)
private val DietDangerSoft = Color(0xFFFFEFE9)
private val DietDanger = Color(0xFFB85A45)
private val DietPitta = Color(0xFFE65100)
private val DietKapha = Color(0xFF2E7D32)

data class DietResult(
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

private val constitutionOptions = listOf(
    "Vata-Pitta",
    "Pitta-Vata",
    "Pitta-Kapha",
    "Kapha-Pitta",
    "Vata-Kapha",
    "Kapha-Vata",
    "Tridoshic",
    "Vata",
    "Pitta",
    "Kapha"
)

private fun normalizePrakritiForDietPlan(prakriti: String): String {
    return when (prakriti) {
        "Pitta-Vata" -> "Vata-Pitta"
        "Kapha-Pitta" -> "Pitta-Kapha"
        "Kapha-Vata" -> "Vata-Kapha"
        "Tridoshic" -> "Tri-Doshic"
        else -> prakriti
    }
}

private fun getDietPlanForConstitution(prakriti: String): DietResult {
    val normalizedPrakriti = normalizePrakritiForDietPlan(prakriti)
    return when (normalizedPrakriti) {
        "Pitta" -> DietResult(
            prakriti = "Pitta",
            vikriti = "Pitta Balanced",
            agni = "Tikshna Agni (High Metabolic Fire)",
            amaStatus = "Ama: Low",
            season = "Seasonal Cooling Adjusted",
            analysis = "Pitta represents fire and transformation. Your nutrition emphasizes cooling, sweet, bitter, and astringent foods that soothe digestive fire and prevent acid accumulation.",
            foodsToEat = listOf(
                "Moong dal khichdi cooked with cooling ghee",
                "Bottle gourd (lauki), zucchini, and cucumber",
                "Fresh tender coconut water and coconut milk",
                "Amla (Indian gooseberry) and pomegranate",
                "Sweet ripe fruits (melons, pears, sweet apples)",
                "Cooling herbs: fresh coriander, fennel, and mint"
            ),
            foodsToAvoid = listOf(
                "Excessive green chilies, cayenne, and hot mustard",
                "Fermented, sour pickles and vinegar condiments",
                "Deep-fried oily snacks and reheating oils",
                "Excess coffee, black tea, and energy drinks",
                "Curd / yogurt at night (increases bodily heat)",
                "Refined white sugar and artificial sweeteners"
            ),
            breakfast = "Cooling oatmeal with soaked sweet almonds and stewed pear, or sweet rice porridge.",
            lunch = "Main meal: Basmati rice, ghee-tempered yellow moong dal, steamed bottle gourd, and cooling mint raita.",
            dinner = "Light vegetable soup with barley or mung chilla before 7:30 PM.",
            snacks = "Fresh sweet coconut water, soaked black raisins, or a crisp sweet apple.",
            lifestyleTips = listOf(
                "Never skip meals; high Pitta acid burns stomach lining when food is delayed.",
                "Eat in a calm, cool environment away from work stress and screens.",
                "Sip room-temperature fennel-infused water throughout the afternoon.",
                "Take a calm evening stroll near greenery or water.",
                "Practice 5 minutes of cooling Sheetali breathwork after work."
            ),
            ayurvedicTips = listOf(
                "Use coriander and fennel seeds as daily digestive cooling spices.",
                "Take 1 teaspoon of pure cow's A2 ghee with warm meals for internal oleation.",
                "Amla churna or fresh juice supports liver health and skin clarity.",
                "Avoid intense hot yoga; prefer cooling, restorative postures.",
                "Opt for natural sweet rasas instead of synthetic sugars."
            )
        )
        "Kapha" -> DietResult(
            prakriti = "Kapha",
            vikriti = "Kapha Balanced",
            agni = "Manda Agni (Slow, Steady Digestion)",
            amaStatus = "Ama: Low",
            season = "Warming & Light Adjusted",
            analysis = "Kapha represents earth and water: stability and heaviness. Your dietary plan favors warm, light, stimulating, and pungent foods that invigorate digestion and prevent sluggishness.",
            foodsToEat = listOf(
                "Barley (jau) khichdi and roasted millet upma",
                "Steamed bitter greens (spinach, methi, moringa)",
                "Light split red lentil or whole moong soup",
                "Warming spices: ginger, black pepper, cinnamon, and pippali",
                "Astringent fruits: pomegranate, dry figs, and green apples",
                "Warm herbal teas with tulsi, ginger, and a touch of raw honey"
            ),
            foodsToAvoid = listOf(
                "Heavy sweets, ice creams, and chilled milk shakes",
                "Deep-fried snacks, samosas, and refined flour (maida)",
                "Late heavy dinners and night snacking",
                "Excessive cheese, paneer, and dense butter",
                "Daytime sleeping immediately after lunch",
                "Very cold, refrigerated food and iced water"
            ),
            breakfast = "Light roasted millet upma with steamed vegetables and dry ginger tea.",
            lunch = "Balanced meal: Barley roti or brown rice, spicy moong soup, and sautéed greens.",
            dinner = "Warm clear vegetable soup or thin mung soup before 7:00 PM.",
            snacks = "Roasted makhana (foxnuts) with black pepper or a tart green apple.",
            lifestyleTips = listOf(
                "Practice intermittent overnight fasting (12-14 hours) to allow complete digestive clearance.",
                "Engage in vigorous morning movement or brisk Surya Namaskar.",
                "Avoid reclining after meals; take a 100-step gentle walk (Shatapadi).",
                "Rise early before 6:00 AM to prevent lethargy.",
                "Dry skin brushing (Garshana) stimulates lymphatic circulation."
            ),
            ayurvedicTips = listOf(
                "Chew a slice of fresh ginger with rock salt 10 minutes before meals.",
                "Trikatu churna (dry ginger, black pepper, pippali) rekindles sluggish Agni.",
                "Honey should only be consumed raw and never heated or cooked.",
                "Practice Bhastrika or Kapalbhati pranayama in the morning.",
                "Favor bitter, pungent, and astringent rasas."
            )
        )
        "Vata" -> DietResult(
            prakriti = "Vata",
            vikriti = "Vata Balanced",
            agni = "Vishama Agni (Variable, Fluctuating Digestion)",
            amaStatus = "Ama: Low",
            season = "Grounding & Nourishing",
            analysis = "Vata represents air and ether: lightness, mobility, and coldness. Your diet focuses on warm, nourishing, unctuous (ghee/oil), and grounding meals to stabilize digestive rhythm.",
            foodsToEat = listOf(
                "Warm rice porridge, oatmeal, and soft cooked grains",
                "Well-cooked root vegetables (carrots, sweet potatoes, beets)",
                "Yellow moong dal tempered with ghee, cumin, and hing",
                "Sweet, ripe fruits: bananas, soaked dates, stewed apples",
                "Healthy unctuous fats: pure cow's ghee and sesame oil",
                "Warming spices: cardamom, cinnamon, ajwain, and ginger"
            ),
            foodsToAvoid = listOf(
                "Raw cold salads, dry crackers, and cold sandwiches",
                "Carbonated drinks, cold water, and iced beverages",
                "Excess caffeine, stimulating sodas, and energy drinks",
                "Irregular meal timings and skipped meals",
                "Hard-to-digest beans (raw rajma, chana) without digestive spices",
                "Eating on the go or while driving/walking"
            ),
            breakfast = "Warm cooked oats or suji porridge with soaked almonds, dates, and cinnamon.",
            lunch = "Warm basmati rice with ghee, yellow moong dal, soft cooked pumpkin/carrots.",
            dinner = "Comforting khichdi with ghee or nourishing vegetable soup before 8:00 PM.",
            snacks = "Warm spiced almond milk, soaked figs, or soft ripe banana.",
            lifestyleTips = listOf(
                "Maintain strict, regular meal times to anchor variable Vishama Agni.",
                "Eat in a quiet, warm, draft-free room with minimal sensory stimulation.",
                "Sip warm cumin-ajwain water throughout the day.",
                "Perform daily warm sesame oil body massage (Abhyanga).",
                "Protect sleep quality with a consistent 10:00 PM bedtime."
            ),
            ayurvedicTips = listOf(
                "Always add hing (asafoetida) and cumin to lentils to prevent gas and bloating.",
                "Ashwagandha supports nervous stability and restorative rest.",
                "Warm golden milk with a pinch of nutmeg promotes deep recovery.",
                "Practice Nadi Shodhana (calm alternate nostril breathwork).",
                "Favor sweet, sour, and salty rasas for grounding."
            )
        )
        "Vata-Pitta" -> DietResult(
            prakriti = "Vata-Pitta",
            vikriti = "Vata-Pitta Harmonized",
            agni = "Sama Agni (Balanced Digestive Fire)",
            amaStatus = "Ama: Low",
            season = "Seasonal Harmony",
            analysis = "Your dual constitution blends Vata's agility with Pitta's sharp focus. The plan prioritizes warm, unctuous, but moderately spiced meals that ground nervous energy while preventing internal heat.",
            foodsToEat = listOf(
                "Warm moong dal khichdi with cow's ghee",
                "Steamed bottle gourd, carrots, zucchini, and spinach",
                "Basmati rice, rolled oats, and quinoa",
                "Ripe sweet fruits: pomegranate, sweet grapes, soaked raisins",
                "A2 cow's milk, ghee, and fresh homemade paneer in moderation",
                "Gentle digestive spices: cumin, fennel, coriander, and turmeric"
            ),
            foodsToAvoid = listOf(
                "Excessive red or green chilies and pungent mustard",
                "Dry packaged snacks, chips, and cold raw salads",
                "Fermented foods, stale leftovers, and sour curd at night",
                "Very hot coffee and artificial energy drinks",
                "Irregular eating schedules and prolonged empty-stomach periods",
                "Deep-fried roadside food and hydrogenated fats"
            ),
            breakfast = "Warm moong dal chilla with mint chutney, or warm spiced oatmeal with dates.",
            lunch = "Main balanced meal: Steamed basmati rice, yellow moong dal, sautéed zucchini, and cucumber salad.",
            dinner = "Warm vegetable khichdi or comforting pumpkin soup before 8:00 PM.",
            snacks = "Tender coconut water, soaked almonds, or roasted makhana with ghee.",
            lifestyleTips = listOf(
                "Anchor your daily rhythm with consistent meal hours.",
                "Chew each morsel thoroughly in a relaxed, peaceful setting.",
                "Avoid screens and stressful reading during meals.",
                "Sip warm fennel-infused water between meals, not directly with meals.",
                "Dedicate 10 minutes to gentle mindfulness before bed."
            ),
            ayurvedicTips = listOf(
                "Coriander and fennel create the perfect balancing duo for Vata-Pitta.",
                "Use ghee as primary cooking fat to nourish Vata and soothe Pitta.",
                "Amla and soaked raisins gently nourish without overheating.",
                "Practice gentle yoga postures that ground the pelvis and cool the chest.",
                "Favor naturally sweet, bitter, and astringent flavors."
            )
        )
        "Pitta-Kapha" -> DietResult(
            prakriti = "Pitta-Kapha",
            vikriti = "Pitta-Kapha Harmonized",
            agni = "Sama Agni (Stable Metabolic Flame)",
            amaStatus = "Ama: Low",
            season = "Cooling & Light Adjusted",
            analysis = "Pitta-Kapha combines fiery metabolism with grounded endurance. Your dietary plan highlights cooling, light, and cleansing foods that prevent both acidity and heaviness.",
            foodsToEat = listOf(
                "Barley, millet, and aged basmati rice",
                "Moong dal, split peas, and sprouted legumes",
                "Leafy bitter greens, cucumber, bitter gourd, and cabbage",
                "Apples, pomegranates, cranberries, and berries",
                "Cooling herbs: mint, cilantro, fennel, and cardamom",
                "Light vegetable broths and herbal digestive infusions"
            ),
            foodsToAvoid = listOf(
                "Heavy deep-fried snacks, heavy cream, and rich cheeses",
                "Excessive hot peppers, garlic, and fermented condiments",
                "Salty fried chips and processed packaged foods",
                "Ice cold beverages that quench digestive fire",
                "Heavy late-night feasts and daytime naps after eating",
                "Refined sugar and dense oily bakery items"
            ),
            breakfast = "Light vegetable poha with mint, or stewed green apple with cinnamon.",
            lunch = "Whole grain roti, moong dal soup, steamed greens, and cucumber slices.",
            dinner = "Clear seasonal vegetable soup or light dal soup before 7:30 PM.",
            snacks = "Pomegranate seeds, roasted chana, or fresh coconut water.",
            lifestyleTips = listOf(
                "Maintain moderate portion sizes; avoid eating until uncomfortably full.",
                "Engage in steady, non-competitive cardiovascular movement daily.",
                "Take a 15-minute gentle walk after your principal midday meal.",
                "Avoid alcohol and heavy evening dining.",
                "Keep living and sleeping areas well-ventilated and cool."
            ),
            ayurvedicTips = listOf(
                "Use cumin and coriander to stimulate digestion without creating excess heat.",
                "Bitter vegetables like karela (bitter gourd) effectively balance Pitta and Kapha.",
                "Triphala water at bedtime supports colon cleansing and tissue clarity.",
                "Incorporate moderate morning sun salutations.",
                "Favor bitter, astringent, and mildly pungent flavors."
            )
        )
        "Vata-Kapha" -> DietResult(
            prakriti = "Vata-Kapha",
            vikriti = "Vata-Kapha Harmonized",
            agni = "Vishama-Manda Agni",
            amaStatus = "Ama: Low",
            season = "Warming & Digestive",
            analysis = "Vata-Kapha constitutions require warmth, light textures, and gentle stimulation. The plan focuses on freshly cooked, warm, easy-to-digest meals that activate circulation and prevent cold stagnation.",
            foodsToEat = listOf(
                "Warm cooked quinoa, basmati rice, and millet",
                "Cooked root vegetables, leafy greens, and zucchini",
                "Light yellow moong dal prepared with ginger and cumin",
                "Warm stewed fruits, soaked figs, and papayas",
                "Warming spices: dry ginger, black pepper, cumin, cloves, and ajwain",
                "Clear herbal broths and warm spiced digestive teas"
            ),
            foodsToAvoid = listOf(
                "Iced drinks, cold salads, and refrigerated raw food",
                "Heavy creamy sauces, cold yogurt, and processed cheese",
                "Excessive raw cabbage, cauliflower, or dry crackers",
                "Irregular eating intervals and skipped breakfasts",
                "Heavy oily sweets and dense fried foods",
                "Sleeping immediately following a heavy meal"
            ),
            breakfast = "Warm spiced rice porridge or hot millet upma with ginger.",
            lunch = "Warm basmati rice, yellow moong dal with cumin tempering, and steamed vegetables.",
            dinner = "Light vegetable soup with black pepper and toasted pumpkin seeds before 7:30 PM.",
            snacks = "Warm herbal ginger tea with a touch of honey, or a ripe sweet papaya.",
            lifestyleTips = listOf(
                "Keep meals consistently warm and freshly cooked.",
                "Avoid cold or iced drinks under all circumstances.",
                "Maintain an active morning routine with energizing movement.",
                "Practice regular meal schedules to guide digestive predictability.",
                "Wear warm layers to protect against cold drafts and chill."
            ),
            ayurvedicTips = listOf(
                "Ajwain and fresh ginger provide the ideal digestive stimulation for Vata-Kapha.",
                "A pinch of black pepper in meals aids assimilation and eliminates mucus.",
                "Trikatu supports metabolic rate and tissue lightness.",
                "Perform brisk morning walking and warm solar pranayama.",
                "Favor warm, pungent, and gently bitter rasas."
            )
        )
        else -> DietResult(
            prakriti = "Tri-Doshic",
            vikriti = "Tridosha Balanced",
            agni = "Sama Agni (Harmonious Metabolic Rhythm)",
            amaStatus = "Ama: Low",
            season = "Seasonal Equilibrium",
            analysis = "A balanced Tri-Doshic constitution benefits from fresh, seasonal, sattvic whole foods. Nutrition focuses on balance across all six rasas with seasonal adaptation.",
            foodsToEat = listOf(
                "Fresh seasonal vegetables and whole cooked grains",
                "Moong dal, red lentils, and light vegetable soups",
                "Fresh seasonal fruits consumed between meals",
                "Pure cow's A2 ghee used mindfully for cooking",
                "Balanced spices: turmeric, cumin, coriander, fennel, ginger",
                "Fresh buttermilk (Takra) seasoned with roasted cumin at lunch"
            ),
            foodsToAvoid = listOf(
                "Overly processed, canned, or microwave-reheated foods",
                "Extreme flavor excesses: overly spicy, excessively sour, or overly salty",
                "Mindless eating while multitasking or watching television",
                "Very cold or iced beverages directly with meals",
                "Eating past 8:30 PM",
                "Artificial additives and refined sugars"
            ),
            breakfast = "Warm cooked porridge, steamed idlis with fresh mint chutney, or seasonal fruit.",
            lunch = "Complete balanced thali: grain, dal, seasonal vegetable subzi, and fresh buttermilk.",
            dinner = "Comforting light khichdi or warm vegetable soup before 8:00 PM.",
            snacks = "Roasted makhana, seasonal fruit, or fresh coconut water.",
            lifestyleTips = listOf(
                "Follow the natural solar cycle for meal timing.",
                "Consume the largest meal when the sun is highest (12:00 PM - 1:30 PM).",
                "Practice 2 minutes of silent gratitude before beginning your meal.",
                "Stay active through balanced daily movement and yoga.",
                "Retire by 10:30 PM to honor natural biological repair."
            ),
            ayurvedicTips = listOf(
                "Incorporate all six tastes (sweet, sour, salty, pungent, bitter, astringent) daily.",
                "Adjust diet smoothly according to seasonal transitions (Ritucharya).",
                "Maintain healthy hydration with warm, boiled water throughout the day.",
                "Practice mindful eating (Ahara Vidhi) for supreme vitality (Ojas).",
                "Trust natural hunger cues and never force-feed."
            )
        )
    }
}

@Composable
fun DietAssessmentScreen(
    prakriti: String = "Vata-Pitta",
    onBack: () -> Unit,
    onEditPrakriti: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedDosha = remember { DoshaResultStore.current(context)?.profileName }
    val initialPrakriti = savedDosha?.takeIf { it.isNotBlank() } ?: prakriti
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var answers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var assessmentResult by remember { mutableStateOf<PrakritiResult?>(null) }
    var showVikritiIntro by remember { mutableStateOf(false) }
    var vikritiStarted by remember { mutableStateOf(false) }
    var vikritiQuestionIndex by remember { mutableIntStateOf(0) }
    var vikritiAnswers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var vikritiResult by remember { mutableStateOf<VikritiResult?>(null) }
    var showAgniIntro by remember { mutableStateOf(false) }
    var agniStarted by remember { mutableStateOf(false) }
    var agniQuestionIndex by remember { mutableIntStateOf(0) }
    var agniAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var agniResult by remember { mutableStateOf<AgniResult?>(null) }
    var symptomsQuestionIndex by remember { mutableIntStateOf(0) }
    var symptomsAnswers by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var symptomsResult by remember { mutableStateOf<SymptomsAnalysisResult?>(null) }
    var showAyurvedicProfile by remember { mutableStateOf(false) }
    var showDietPlan by remember { mutableStateOf(false) }
    var autoAdvanceLocked by remember { mutableStateOf(false) }

    if (!showDietPlan) {
        Surface(modifier = modifier.fillMaxSize(), color = Cream) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF9EE), Cream, LightSage.copy(alpha = 0.55f))))
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
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DietMint)
                                .border(BorderStroke(1.dp, ForestGreen.copy(alpha = 0.4f)), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    assessmentResult == null -> "Prakriti Assessment"
                                    vikritiResult == null -> "Vikriti Assessment"
                                    agniResult == null -> "Agni Assessment"
                                    symptomsResult == null -> "Symptoms Analysis"
                                    else -> "Final Results"
                                },
                                color = ForestGreen,
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
                        val completedResult = assessmentResult
                        val completedVikriti = vikritiResult
                        val completedAgni = agniResult
                        val completedSymptoms = symptomsResult
                        if (completedResult == null) {
                            PrakritiQuestionnaireContent(
                                currentQuestionIndex = currentQuestionIndex,
                                answers = answers,
                                onAnswerSelected = { questionId, optionId ->
                                    if (!autoAdvanceLocked) {
                                        autoAdvanceLocked = true
                                        val selectedIndex = currentQuestionIndex
                                        val updatedAnswers = answers + (questionId to optionId)
                                        answers = updatedAnswers
                                        coroutineScope.launch {
                                            delay(250)
                                            if (currentQuestionIndex == selectedIndex) {
                                                if (selectedIndex < prakritiQuestions.lastIndex) {
                                                    currentQuestionIndex = selectedIndex + 1
                                                } else if (updatedAnswers.size == prakritiQuestions.size) {
                                                    assessmentResult = generatePrakritiResult(updatedAnswers)
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
                        } else if (completedVikriti == null) {
                            VikritiQuestionnaireContent(
                                currentQuestionIndex = vikritiQuestionIndex,
                                answers = vikritiAnswers,
                                onAnswerSelected = { questionId, optionId ->
                                    if (!autoAdvanceLocked) {
                                        autoAdvanceLocked = true
                                        val selectedIndex = vikritiQuestionIndex
                                        val updatedAnswers = vikritiAnswers + (questionId to optionId)
                                        vikritiAnswers = updatedAnswers
                                        coroutineScope.launch {
                                            delay(250)
                                            if (vikritiQuestionIndex == selectedIndex) {
                                                if (selectedIndex < vikritiQuestions.lastIndex) {
                                                    vikritiQuestionIndex = selectedIndex + 1
                                                } else if (updatedAnswers.size == vikritiQuestions.size) {
                                                    val generatedVikriti = generateVikritiResult(
                                                        answers = updatedAnswers,
                                                        prakritiType = completedResult.prakritiType
                                                    )
                                                    VikritiResultStore.save(context, generatedVikriti)
                                                    vikritiResult = generatedVikriti
                                                }
                                            }
                                            autoAdvanceLocked = false
                                        }
                                    }
                                },
                                onPrevious = {
                                    vikritiQuestionIndex = (vikritiQuestionIndex - 1).coerceAtLeast(0)
                                }
                            )
                        } else if (completedAgni == null) {
                            AgniQuestionnaireContent(
                                currentQuestionIndex = agniQuestionIndex,
                                answers = agniAnswers,
                                onAnswerSelected = { questionId, optionId ->
                                    if (!autoAdvanceLocked) {
                                        autoAdvanceLocked = true
                                        val selectedIndex = agniQuestionIndex
                                        val updatedAnswers = agniAnswers + (questionId to optionId)
                                        agniAnswers = updatedAnswers
                                        coroutineScope.launch {
                                            delay(250)
                                            if (agniQuestionIndex == selectedIndex) {
                                                if (selectedIndex < agniQuestions.lastIndex) {
                                                    agniQuestionIndex = selectedIndex + 1
                                                } else if (updatedAnswers.size == agniQuestions.size) {
                                                    val generatedAgni = generateAgniResult(updatedAnswers)
                                                    AgniResultStore.save(context, generatedAgni)
                                                    agniResult = generatedAgni
                                                }
                                            }
                                            autoAdvanceLocked = false
                                        }
                                    }
                                },
                                onPrevious = {
                                    agniQuestionIndex = (agniQuestionIndex - 1).coerceAtLeast(0)
                                }
                            )
                        } else if (completedSymptoms == null) {
                            AssessmentSymptomsQuestionnaireContent(
                                currentQuestionIndex = symptomsQuestionIndex,
                                answers = symptomsAnswers,
                                onAnswerSelected = { key, value ->
                                    if (!autoAdvanceLocked) {
                                        autoAdvanceLocked = true
                                        val selectedIndex = symptomsQuestionIndex
                                        val updatedAnswers = symptomsAnswers + (key to value)
                                        symptomsAnswers = updatedAnswers
                                        coroutineScope.launch {
                                            delay(250)
                                            if (symptomsQuestionIndex == selectedIndex) {
                                                if (selectedIndex < symptomFeatures.lastIndex) {
                                                    symptomsQuestionIndex = selectedIndex + 1
                                                } else if (updatedAnswers.size == symptomFeatures.size) {
                                                    val completeInput = symptomFeatures.associate { feature ->
                                                        feature.key to updatedAnswers.getValue(feature.key)
                                                    }
                                                    val generatedSymptoms = generateSymptomsAnalysisResult(completeInput)
                                                    SymptomsAnalysisStore.save(context, generatedSymptoms)
                                                    AssessmentProfileStore.save(context, completedResult, completedVikriti, completedAgni, generatedSymptoms)
                                                    symptomsResult = generatedSymptoms
                                                }
                                            }
                                            autoAdvanceLocked = false
                                        }
                                    }
                                },
                                onPrevious = {
                                    symptomsQuestionIndex = (symptomsQuestionIndex - 1).coerceAtLeast(0)
                                }
                            )
                        } else {
                            AyurvedicProfileSummaryContent(
                                prakritiResult = completedResult,
                                vikritiResult = completedVikriti,
                                agniResult = completedAgni,
                                symptomsResult = completedSymptoms,
                                onViewDietPlan = { showDietPlan = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp).navigationBarsPadding())
                    }
                }
            }
        }
        return
    }

    val assessmentPrakriti = assessmentResult?.prakritiType ?: initialPrakriti
    var selectedPrakriti by remember(assessmentPrakriti) { mutableStateOf(assessmentPrakriti) }
    var activeTabIndex by remember { mutableIntStateOf(0) }
    val result = remember(selectedPrakriti) { getDietPlanForConstitution(selectedPrakriti) }
    val currentVikriti = vikritiResult?.dominantDosha ?: result.vikriti
    val currentAgni = agniResult?.displayResult ?: result.agni
    val tabs = listOf("Body Analysis", "Foods To Eat", "Foods To Avoid", "Meal Plan", "Lifestyle Tips", "Ayurvedic Dravya")

    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF9EE), Cream, LightSage.copy(alpha = 0.55f))))
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
                            .background(DietMint)
                            .border(BorderStroke(1.dp, ForestGreen.copy(alpha = 0.4f)), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pathya Apathya",
                            color = ForestGreen,
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = ForestGreen.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
                        border = BorderStroke(1.dp, BeigeBorder)
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
                                    text = "AYURVEDIC DIET DASHBOARD",
                                    color = ForestGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.3.sp
                                )
                            }

                            Text(
                                text = "Personalised Diet & Nutritional Guide",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                lineHeight = 31.sp,
                                color = DarkForestGreen
                            )

                            Text(
                                text = "Food is medicine (Maha Bheshaja) in Ayurveda. Your diet plan is customized for your constitution, digestive fire (Agni), and seasonal vitality.",
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                                color = SoftBlueGray
                            )

                            // Status Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DietStatusBadge("Prakriti", result.prakriti, Modifier.weight(1f))
                                DietStatusBadge("Vikriti", currentVikriti, Modifier.weight(1f))
                                DietStatusBadge("Agni", currentAgni, Modifier.weight(1f))
                                DietStatusBadge("Season", "Sharad Ritu", Modifier.weight(1f))
                            }
                        }
                    }

                    if (assessmentResult == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "SELECT CONSTITUTION FOR PLAN",
                                color = SageGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.8.sp
                            )

                            ScrollableTabRow(
                                selectedTabIndex = constitutionOptions.indexOf(selectedPrakriti).coerceAtLeast(0),
                                containerColor = Color.Transparent,
                                contentColor = ForestGreen,
                                edgePadding = 0.dp,
                                divider = {}
                            ) {
                                constitutionOptions.forEach { option ->
                                    val isSelected = option == selectedPrakriti
                                    Tab(
                                        selected = isSelected,
                                        onClick = { selectedPrakriti = option },
                                        text = {
                                            Text(
                                                text = option,
                                                color = if (isSelected) ForestGreen else SoftBlueGray,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        PersonalizedDietSourceCard(
                            prakriti = result.prakriti,
                            vikriti = currentVikriti,
                            agni = currentAgni
                        )
                    }

                    // 6 Comprehensive Feature Tabs
                    ScrollableTabRow(
                        selectedTabIndex = activeTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = ForestGreen,
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = activeTabIndex == index,
                                onClick = { activeTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }

                    // Tab Contents
                    when (activeTabIndex) {
                        0 -> BodyAnalysisContent(result, currentVikriti, currentAgni)
                        1 -> FoodsListContent(result.foodsToEat, positive = true)
                        2 -> FoodsListContent(result.foodsToAvoid, positive = false)
                        3 -> MealPlanContent(result)
                        4 -> LifestyleContent(result.lifestyleTips)
                        else -> AyurvedicDravyaContent(result.ayurvedicTips)
                    }

                    Spacer(modifier = Modifier.height(32.dp).navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun PrakritiQuestionnaireContent(
    currentQuestionIndex: Int,
    answers: Map<Int, String>,
    onAnswerSelected: (questionId: Int, optionId: String) -> Unit,
    onPrevious: () -> Unit
) {
    val question = prakritiQuestions[currentQuestionIndex]
    val selectedOption = answers[question.id]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = ForestGreen.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "PERSONALISED DIET STARTS HERE",
                color = SageGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Text(
                text = "21-Question Prakriti Assessment",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp,
                lineHeight = 32.sp,
                color = DarkForestGreen
            )
            Text(
                text = "Answer each question honestly. This wellness assessment estimates your Ayurvedic body constitution and then shapes your diet guidance.",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
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
                    text = "Question ${currentQuestionIndex + 1} of ${prakritiQuestions.size}",
                    color = ForestGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${answers.size}/${prakritiQuestions.size} answered",
                    color = SageGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = { answers.size.toFloat() / prakritiQuestions.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = ForestGreen,
                trackColor = LightSage
            )

            Text(
                text = question.text,
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp
            )

            question.options.forEach { option ->
                PrakritiAnswerCard(
                    option = option,
                    selected = selectedOption == option.id,
                    onClick = { onAnswerSelected(question.id, option.id) }
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
private fun PrakritiAnswerCard(
    option: PrakritiOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) ForestGreen else BeigeBorder.copy(alpha = 0.8f)
    val background = if (selected) DietMint else Color(0xFFFFFDF8)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(BorderStroke(if (selected) 1.6.dp else 1.dp, borderColor), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) ForestGreen else PureWhite)
                .border(BorderStroke(1.dp, borderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
            }
        }
        Text(
            text = option.text,
            color = DarkForestGreen,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PrakritiResultContent(
    result: PrakritiResult,
    onRetake: () -> Unit,
    primaryActionText: String = "View Diet Guide",
    onViewDietPlan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = ForestGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your Prakriti", color = SageGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text(
                text = result.prakritiType,
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 38.sp
            )
            Text(
                text = prakritiResultMessage(result),
                color = SoftBlueGray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            PrakritiPercentageBar("Vata", result.percentages.getValue(PrakritiDosha.Vata), DietSky)
            PrakritiPercentageBar("Pitta", result.percentages.getValue(PrakritiDosha.Pitta), DietDangerSoft)
            PrakritiPercentageBar("Kapha", result.percentages.getValue(PrakritiDosha.Kapha), DietMint)

            MetricCard("Primary Dosha", result.primaryDosha, if (result.prakritiType == "Tridoshic") "All three doshas are approximately balanced." else "The strongest constitution influence in this assessment.")
            result.secondaryDosha?.let {
                MetricCard("Secondary Dosha", it, "A meaningful supporting influence in your constitution.")
            }
            MetricCard("Prakriti Type", result.prakritiType, if (result.borderline) "Borderline result: your second dosha is close enough to consider during lifestyle planning." else "Used to shape the diet guidance that follows.")

            Text(
                text = "This is an Ayurvedic wellness body-constitution assessment, not a clinical diagnosis or replacement for professional medical care.",
                color = SoftBlueGray,
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BeigeBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
                ) {
                    Text("Retake", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onViewDietPlan,
                    modifier = Modifier
                        .weight(1.25f)
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
                ) {
                    Text(primaryActionText, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }
    }
}

@Composable
private fun VikritiIntroContent(onStart: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = ForestGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("CURRENT BALANCE", color = SageGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text(
                text = "Vikriti Assessment - Your Current Dosha Balance",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 29.sp,
                lineHeight = 34.sp
            )
            Text(
                text = "Your Prakriti represents your natural constitution, while Vikriti reflects your current state. Answer the following questions based on how you have been feeling recently.",
                color = SoftBlueGray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("Start Vikriti Assessment", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VikritiQuestionnaireContent(
    currentQuestionIndex: Int,
    answers: Map<String, String>,
    onAnswerSelected: (questionId: String, optionId: String) -> Unit,
    onPrevious: () -> Unit
) {
    val question = vikritiQuestions[currentQuestionIndex]
    val selectedOption = answers[question.id]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = ForestGreen.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Vikriti Assessment",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp,
                lineHeight = 32.sp,
                color = DarkForestGreen
            )
            Text(
                text = "Answer based on how you have been feeling recently.",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
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
                    text = "Question ${currentQuestionIndex + 1} of ${vikritiQuestions.size}",
                    color = ForestGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${answers.size}/${vikritiQuestions.size} answered",
                    color = SageGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = { answers.size.toFloat() / vikritiQuestions.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = ForestGreen,
                trackColor = LightSage
            )

            Text(
                text = question.text,
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp
            )

            vikritiAnswerOptions.forEach { option ->
                VikritiAnswerCard(
                    option = option,
                    selected = selectedOption == option.id,
                    onClick = { onAnswerSelected(question.id, option.id) }
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
private fun VikritiAnswerCard(
    option: VikritiAnswerOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) ForestGreen else BeigeBorder.copy(alpha = 0.8f)
    val background = if (selected) DietMint else Color(0xFFFFFDF8)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(BorderStroke(if (selected) 1.6.dp else 1.dp, borderColor), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) ForestGreen else PureWhite)
                .border(BorderStroke(1.dp, borderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
            }
        }
        Text(
            text = option.label,
            color = DarkForestGreen,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VikritiResultContent(
    result: VikritiResult,
    onRetake: () -> Unit,
    primaryActionText: String = "View Profile",
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = ForestGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your Current Vikriti", color = SageGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text(
                text = "Current Dominant Imbalance: ${result.dominantDosha}",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 29.sp,
                lineHeight = 34.sp
            )
            Text(
                text = vikritiResultMessage(result),
                color = SoftBlueGray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            VikritiDosha.entries.forEach { dosha ->
                VikritiScoreCard(
                    dosha = dosha.displayName,
                    score = result.scores.getValue(dosha),
                    severity = result.severities.getValue(dosha),
                    percentage = result.percentages.getValue(dosha),
                    highlighted = result.dominantDosha == dosha.displayName
                )
            }

            Text(
                text = "This is a rule-based Ayurvedic wellness assessment and should not be treated as a medical diagnosis.",
                color = SoftBlueGray,
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BeigeBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
                ) {
                    Text("Retake", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onViewProfile,
                    modifier = Modifier
                        .weight(1.35f)
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
                ) {
                    Text(primaryActionText, fontWeight = FontWeight.Bold, fontSize = 12.2.sp)
                }
            }
        }
    }
}

@Composable
private fun VikritiScoreCard(
    dosha: String,
    score: Int,
    severity: String,
    percentage: Double,
    highlighted: Boolean
) {
    val accent = when (dosha) {
        "Pitta" -> DietPitta
        "Kapha" -> DietKapha
        else -> ForestGreen
    }
    val background = if (highlighted) DietMint else PureWhite
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(BorderStroke(if (highlighted) 1.8.dp else 1.dp, if (highlighted) accent else BeigeBorder), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(dosha, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Score: $score / 35", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        }
        Text("Severity: $severity", color = SoftBlueGray, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(
            progress = { (percentage / 100.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = accent,
            trackColor = LightSage
        )
    }
}

@Composable
private fun AgniIntroContent(onStart: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = ForestGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Agni Assessment", color = SageGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text(
                text = "Understand Your Digestive Pattern",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 35.sp
            )
            Text(
                text = "Answer the following questions based on your usual digestion, appetite, eating habits, bowel habits, and how you generally feel after meals.",
                color = SoftBlueGray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("Start Agni Assessment", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AgniQuestionnaireContent(
    currentQuestionIndex: Int,
    answers: Map<Int, String>,
    onAnswerSelected: (questionId: Int, optionId: String) -> Unit,
    onPrevious: () -> Unit
) {
    val question = agniQuestions[currentQuestionIndex]
    val selectedOption = answers[question.id]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = ForestGreen.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Agni Assessment",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp,
                lineHeight = 32.sp,
                color = DarkForestGreen
            )
            Text(
                text = "Choose the option that best reflects your usual digestive pattern.",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
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
                    text = "Question ${currentQuestionIndex + 1} of ${agniQuestions.size}",
                    color = ForestGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${answers.size}/${agniQuestions.size} answered",
                    color = SageGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = { answers.size.toFloat() / agniQuestions.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = ForestGreen,
                trackColor = LightSage
            )

            Text(
                text = question.text,
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp
            )

            question.options.forEach { option ->
                AgniAnswerCard(
                    option = option,
                    selected = selectedOption == option.id,
                    onClick = { onAnswerSelected(question.id, option.id) }
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
private fun AgniAnswerCard(
    option: AgniOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) ForestGreen else BeigeBorder.copy(alpha = 0.8f)
    val background = if (selected) DietMint else Color(0xFFFFFDF8)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(BorderStroke(if (selected) 1.6.dp else 1.dp, borderColor), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) ForestGreen else PureWhite)
                .border(BorderStroke(1.dp, borderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
            }
        }
        Text(
            text = option.text,
            color = DarkForestGreen,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AgniResultContent(
    result: AgniResult,
    onRetake: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = ForestGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your Agni Type", color = SageGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text(
                text = result.displayResult,
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 31.sp,
                lineHeight = 36.sp
            )
            Text(
                text = agniResultMessage(result),
                color = SoftBlueGray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            AgniType.entries.forEach { type ->
                AgniScoreRow(
                    label = type.displayName,
                    score = result.scores.getValue(type),
                    highlighted = !result.isMixed && result.dominantAgni == type.displayName || result.mixedAgniTypes.contains(type.displayName)
                )
            }

            Text(
                text = "This is a rule-based Ayurvedic wellness assessment and should not be treated as a medical diagnosis.",
                color = SoftBlueGray,
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BeigeBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
                ) {
                    Text("Retake", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onViewProfile,
                    modifier = Modifier
                        .weight(1.35f)
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
                ) {
                    Text("View Profile", fontWeight = FontWeight.Bold, fontSize = 12.2.sp)
                }
            }
        }
    }
}

@Composable
private fun AssessmentSymptomsQuestionnaireContent(
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
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Symptoms Analysis",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp,
                lineHeight = 32.sp,
                color = DarkForestGreen
            )
            Text(
                text = "Select the symptoms you are currently experiencing.",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
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
                AssessmentSymptomAnswerCard(
                    label = "No",
                    selected = selectedValue == 0,
                    onClick = { onAnswerSelected(symptom.key, 0) },
                    modifier = Modifier.weight(1f)
                )
                AssessmentSymptomAnswerCard(
                    label = "Yes",
                    selected = selectedValue == 1,
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
private fun AssessmentSymptomAnswerCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) DietMint else Color(0xFFFFFDF8))
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = DarkForestGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AgniScoreRow(label: String, score: Int, highlighted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlighted) DietMint else PureWhite)
            .border(BorderStroke(if (highlighted) 1.6.dp else 1.dp, if (highlighted) ForestGreen else BeigeBorder), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = DarkForestGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("$score / ${agniQuestions.size}", color = ForestGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AyurvedicProfileSummaryContent(
    prakritiResult: PrakritiResult,
    vikritiResult: VikritiResult,
    agniResult: AgniResult,
    symptomsResult: SymptomsAnalysisResult,
    onViewDietPlan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = ForestGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DietWarmCard),
        border = BorderStroke(1.dp, BeigeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your Ayurvedic Profile", color = SageGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            ProfileSummaryCard("Prakriti", "Natural Constitution", prakritiResult.prakritiType)
            ProfileSummaryCard("Vikriti", "Current Balance", vikritiResult.dominantDosha)
            VikritiSummaryScores(vikritiResult)
            ProfileSummaryCard("Agni", "Digestive Pattern", agniResult.displayResult)
            SymptomsSummaryCard(symptomsResult.reportedSymptoms)
            Text(
                text = "Prakriti represents your natural constitution, Vikriti represents your current state, Agni reflects your digestive pattern, and Symptoms Analysis records what you are experiencing now. These independent results work together to shape your final profile.",
                color = SoftBlueGray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Button(
                onClick = onViewDietPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
            ) {
                Text("View My Personalized Diet", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SymptomsSummaryCard(reportedSymptoms: List<SymptomFeature>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Symptoms Analysis", color = DarkForestGreen, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Reported Symptoms", color = SageGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
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

@Composable
private fun VikritiSummaryScores(result: VikritiResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        VikritiDosha.entries.forEach { dosha ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dosha.displayName, color = DarkForestGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${result.scores.getValue(dosha)} / 35 - ${result.severities.getValue(dosha)}",
                    color = ForestGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(label: String, caption: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = DarkForestGreen, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(caption, color = SageGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Text(value, color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun PersonalizedDietSourceCard(prakriti: String, vikriti: String, agni: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricMini("Prakriti", prakriti, Modifier.weight(1f))
        MetricMini("Vikriti", vikriti, Modifier.weight(1f))
        MetricMini("Agni", agni, Modifier.weight(1f))
    }
}

@Composable
private fun MetricMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), color = SageGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = DarkForestGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
    }
}

@Composable
private fun PrakritiPercentageBar(label: String, percentage: Double, trackColor: Color) {
    val barColor = when (label) {
        "Pitta" -> DietPitta
        "Kapha" -> DietKapha
        else -> ForestGreen
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(String.format("%.1f%%", percentage), color = barColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        LinearProgressIndicator(
            progress = { (percentage / 100.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
            color = barColor,
            trackColor = trackColor
        )
    }
}

private fun prakritiResultMessage(result: PrakritiResult): String {
    if (result.prakritiType == "Tridoshic") {
        return "Your assessment indicates an approximately balanced Vata, Pitta, and Kapha pattern."
    }
    val secondary = result.secondaryDosha
    val base = if (secondary != null) {
        "Your assessment indicates that ${result.primaryDosha} is your dominant dosha, with $secondary as your secondary influence."
    } else {
        "Your assessment indicates that ${result.primaryDosha} is your dominant dosha."
    }
    return if (result.borderline) "$base Your score is borderline, so your next strongest dosha may still matter." else base
}

private fun vikritiResultMessage(result: VikritiResult): String {
    val others = listOf("Vata", "Pitta", "Kapha").filterNot { it == result.dominantDosha }.joinToString(" and ")
    return "Your Vikriti result suggests that ${result.dominantDosha}-related characteristics are currently more prominent compared with $others."
}

private fun agniResultMessage(result: AgniResult): String {
    if (result.isMixed) {
        return "Your answers suggest a mixed digestive pattern, with ${result.mixedAgniTypes.joinToString(" and ")} showing the strongest counts."
    }
    val subtitle = AgniType.entries.firstOrNull { it.displayName == result.dominantAgni }?.subtitle ?: "Digestive Pattern"
    return "Your answers suggest ${result.dominantAgni}, a $subtitle. Use this as wellness guidance rather than a diagnosis."
}

@Composable
private fun DietStatusBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.7f)), RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        Text(label.uppercase(), color = SageGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BodyAnalysisContent(result: DietResult, vikriti: String, agni: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard("Constitution (Prakriti)", result.prakriti, "Primary constitutional dosha blueprint.")
        MetricCard("Metabolic State (Vikriti)", vikriti, "Current balance of biological humors.")
        MetricCard("Digestive Fire (Agni)", agni, "Strength and stability of gut enzymes and assimilation.")
        MetricCard("Toxin Clearance (Ama)", result.amaStatus, "Indicator of undigested metabolic byproduct clearance.")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, BeigeBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CLINICAL DIETETIC RATIONALE",
                    color = SageGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = result.analysis,
                    color = Color(0xFF334438),
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, headline: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(LightSage),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title.uppercase(), color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, letterSpacing = 1.2.sp)
            Text(headline, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(body, color = SoftBlueGray, fontSize = 11.5.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun FoodsListContent(items: List<String>, positive: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val headerText = if (positive) "BALANCING FOODS TO FAVOR (PATHYA)" else "FOODS TO MINIMIZE OR AVOID (APATHYA)"
        val headerColor = if (positive) ForestGreen else DietDanger
        val icon = if (positive) Icons.Filled.Check else Icons.Filled.Close
        val containerColor = if (positive) PureWhite else DietDangerSoft.copy(alpha = 0.5f)
        val iconBg = if (positive) DietMint else Color(0xFFFFDAD4)

        Text(
            text = headerText,
            color = headerColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp
        )

        items.forEach { food ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(containerColor)
                    .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.7f)), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = headerColor, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = food,
                        color = DarkForestGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = if (positive) "Promotes digestion, cellular nourishment, and doshic equilibrium." else "May provoke ama formation or aggravate constitutional tendencies.",
                        color = SoftBlueGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MealPlanContent(result: DietResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "DAILY AYURVEDIC MEAL TIMELINE",
            color = SageGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp
        )

        MealTimelineCard("Early Morning (Ushapan)", "6:30 AM - 7:00 AM", "Warm boiled water with dry ginger or copper-vessel water to initiate peristalsis.")
        MealTimelineCard("Breakfast (Pratarasha)", "8:00 AM - 8:30 AM", result.breakfast)
        MealTimelineCard("Lunch (Madhyahna Ahara)", "12:30 PM - 1:30 PM", result.lunch)
        MealTimelineCard("Afternoon Refresh", "4:30 PM - 5:00 PM", result.snacks)
        MealTimelineCard("Dinner (Sayam Ahara)", "7:00 PM - 7:45 PM", result.dinner)
        MealTimelineCard("Bedtime Elixir", "9:45 PM", "Warm spiced almond milk with nutmeg or soothing chamomile infusion.")
    }
}

@Composable
private fun MealTimelineCard(mealName: String, timing: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DietMint),
            contentAlignment = Alignment.Center
        ) {
            Text("🍽", fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(mealName, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(timing, color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
            }
            Text(description, color = Color(0xFF334438), fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun LifestyleContent(tips: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "DINACHARYA & MINDFUL EATING GUIDELINES",
            color = SageGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp
        )

        tips.forEachIndexed { index, tip ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PureWhite)
                    .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(LightSage),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tip, color = DarkForestGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Daily routine practice to optimize nutrient absorption and gut vitality.", color = SoftBlueGray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AyurvedicDravyaContent(tips: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "AYURVEDIC DRAVYA & SPICE THERAPY",
            color = SageGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp
        )

        tips.forEach { tip ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PureWhite)
                    .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.75f)), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(DietWarmCard)
                        .border(BorderStroke(1.dp, BeigeBorder), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌿", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tip, color = DarkForestGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Classical herb and kitchen spice formulation for systemic harmony.", color = SoftBlueGray, fontSize = 11.sp)
                }
            }
        }
    }
}
