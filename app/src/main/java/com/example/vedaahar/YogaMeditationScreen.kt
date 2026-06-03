package com.example.vedaahar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val YogaGold = Color(0xFFD8AE2E)
private val YogaCard = Color(0xFFFFFBF2)
private val YogaDeep = Color(0xFF014331)
private val YogaBorder = Color(0xFFE3D3A4)
private val YogaInk = Color(0xFF1B3325)
private val Cinzel = FontFamily(Font(R.font.cinzel_decorative_regular))
private val YogaHeaderHeight = 72.dp

@Composable
fun YogaMeditationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF8EC), Color(0xFFFFFCF5), Cream)))
        ) {
            ParchmentPattern()
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                YogaTopBar()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    YogaHero()
                    DailySadhanaSection()
                    PranayamaSection()
                    YogaAsanasSection()
                    CalmMindSection()
                    ReminderButton()
                    Text(
                        text = "\"Yoga is the journey of the self, through the self, to the self.\"",
                        modifier = Modifier.fillMaxWidth(),
                        color = SoftBlueGray,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun YogaTopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(YogaHeaderHeight)
            .shadow(6.dp, ambientColor = DarkForestGreen.copy(alpha = 0.08f), spotColor = YogaGold.copy(alpha = 0.08f))
            .background(YogaCard.copy(alpha = 0.96f))
            .border(BorderStroke(1.dp, YogaBorder.copy(alpha = 0.55f)))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.vedaahaar_logo),
            contentDescription = null,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PureWhite),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "VedaAahar",
            modifier = Modifier.weight(1f),
            color = DarkForestGreen,
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5ECD2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = YogaGold, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun YogaHero() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Yoga & Meditation",
            color = DarkForestGreen,
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "Practice traditional Indian yoga, pranayama and mindful meditation.",
            color = SoftBlueGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DailySadhanaSection() {
    SectionHeading(title = "Daily Sadhana", subtitle = "Nitya Abhyasa")
    FeaturedPractice()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        MiniPracticeCard(title = "Morning Pranayama", icon = "Pr", modifier = Modifier.weight(1f))
        MiniPracticeCard(title = "Om Chanting", icon = "Om", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FeaturedPractice() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .shadow(8.dp, RoundedCornerShape(8.dp), ambientColor = YogaDeep.copy(alpha = 0.16f), spotColor = YogaGold.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, YogaBorder),
        colors = CardDefaults.cardColors(containerColor = YogaDeep)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.yoga_meditation_banner),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text("Featured Practice", color = YogaGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Surya Namaskar", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text("12 Poses of Salutation", color = PureWhite.copy(alpha = 0.82f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MiniPracticeCard(title: String, icon: String, modifier: Modifier = Modifier) {
    TapCard(
        modifier = modifier.height(64.dp),
        background = YogaCard,
        border = YogaBorder
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Text(icon, color = YogaGold, fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Text(title, color = DarkForestGreen, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PranayamaSection() {
    SectionHeading(title = "Pranayama", subtitle = "Prana Vidya")
    val practices = listOf(
        Triple("Anulom Vilom", "10 min", "Beginner"),
        Triple("Kapalbhati", "7 min", "Intermediate"),
        Triple("Bhramari", "5 min", "Beginner")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        practices.forEach { (name, duration, level) ->
            PranayamaRow(name = name, meta = "$duration  -  $level")
        }
    }
}

@Composable
private fun PranayamaRow(name: String, meta: String) {
    TapCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        background = YogaCard,
        border = YogaBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(YogaGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = YogaGold, modifier = Modifier.size(15.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(meta, color = SoftBlueGray, fontSize = 10.sp)
            }
            StartPill()
        }
    }
}

@Composable
private fun YogaAsanasSection() {
    SectionHeading(title = "Yoga Asanas", subtitle = "Asana")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        AsanaCard("Tadasana", 0, Modifier.weight(1f))
        AsanaCard("Vajrasana", 1, Modifier.weight(1f))
        AsanaCard("Bhujangasana", 2, Modifier.weight(1f))
    }
}

@Composable
private fun AsanaCard(label: String, pose: Int, modifier: Modifier = Modifier) {
    TapCard(modifier = modifier.height(116.dp), background = YogaCard, border = YogaBorder) {
        Column(modifier = Modifier.padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF173F31), Color(0xFFEFE2BE)))),
                contentAlignment = Alignment.Center
            ) {
                AsanaPose(pose = pose)
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(label, color = DarkForestGreen, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CalmMindSection() {
    SectionHeading(title = "Calm Mind", subtitle = "Dhyana")
    TapCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp),
        background = YogaDeep,
        border = Color(0xFF0B6049)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Om Meditation", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(5.dp))
                Text("Connect with the primordial sound", color = PureWhite.copy(alpha = 0.74f), fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(YogaGold.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Om", color = YogaGold, fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        MiniPracticeCard(title = "Chakra Balance", icon = "O", modifier = Modifier.weight(1f))
        MiniPracticeCard(title = "Deep Sleep", icon = "C", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ReminderButton() {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "reminder-scale")
    Button(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .scale(scale)
            .shadow(6.dp, RoundedCornerShape(9.dp), ambientColor = YogaGold.copy(alpha = 0.28f), spotColor = YogaGold.copy(alpha = 0.2f)),
        interactionSource = interaction,
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(containerColor = YogaGold, contentColor = DarkForestGreen)
    ) {
        Text("Set Daily Yoga Reminder", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.width(7.dp))
        Text(subtitle, color = YogaGold, fontSize = 10.sp, fontFamily = Cinzel)
    }
}

@Composable
private fun StartPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(YogaGold)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Start", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun TapCard(
    modifier: Modifier = Modifier,
    background: Color,
    border: Color,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "tap-card")
    Card(
        modifier = modifier
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(8.dp), ambientColor = ForestGreen.copy(alpha = 0.08f), spotColor = YogaGold.copy(alpha = 0.08f))
            .clickable(interactionSource = interaction, indication = null, onClick = {}),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, border.copy(alpha = 0.82f))
    ) {
        content()
    }
}

@Composable
private fun AsanaPose(pose: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.5f, size.height * 0.52f)
        drawLine(YogaGold.copy(alpha = 0.72f), Offset(size.width * 0.2f, size.height * 0.8f), Offset(size.width * 0.8f, size.height * 0.8f), 4f, StrokeCap.Round)
        when (pose) {
            0 -> {
                drawCircle(PureWhite, 5f, Offset(center.x, center.y - 28f))
                drawLine(PureWhite, Offset(center.x, center.y - 22f), Offset(center.x, center.y + 18f), 4f, StrokeCap.Round)
                drawLine(PureWhite, Offset(center.x, center.y - 10f), Offset(center.x - 18f, center.y - 26f), 3f, StrokeCap.Round)
                drawLine(PureWhite, Offset(center.x, center.y - 10f), Offset(center.x + 18f, center.y - 26f), 3f, StrokeCap.Round)
                drawLine(PureWhite, Offset(center.x, center.y + 18f), Offset(center.x - 10f, center.y + 42f), 4f, StrokeCap.Round)
                drawLine(PureWhite, Offset(center.x, center.y + 18f), Offset(center.x + 10f, center.y + 42f), 4f, StrokeCap.Round)
            }
            1 -> {
                drawCircle(PureWhite, 5f, Offset(center.x, center.y - 18f))
                drawLine(PureWhite, Offset(center.x, center.y - 12f), Offset(center.x, center.y + 14f), 4f, StrokeCap.Round)
                drawArc(
                    color = PureWhite,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(center.x - 24f, center.y + 8f),
                    size = Size(48f, 34f),
                    style = Stroke(4f, cap = StrokeCap.Round)
                )
                drawLine(PureWhite, Offset(center.x - 12f, center.y + 4f), Offset(center.x - 24f, center.y + 24f), 3f, StrokeCap.Round)
                drawLine(PureWhite, Offset(center.x + 12f, center.y + 4f), Offset(center.x + 24f, center.y + 24f), 3f, StrokeCap.Round)
            }
            else -> {
                drawCircle(PureWhite, 5f, Offset(center.x + 24f, center.y - 10f))
                drawLine(PureWhite, Offset(center.x - 28f, center.y + 22f), Offset(center.x + 20f, center.y - 4f), 4f, StrokeCap.Round)
                drawLine(PureWhite, Offset(center.x - 30f, center.y + 22f), Offset(center.x - 46f, center.y + 38f), 4f, StrokeCap.Round)
                drawLine(PureWhite, Offset(center.x + 2f, center.y + 6f), Offset(center.x + 42f, center.y + 34f), 4f, StrokeCap.Round)
                drawLine(PureWhite, Offset(center.x + 18f, center.y + 2f), Offset(center.x + 42f, center.y + 12f), 3f, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun ParchmentPattern() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(listOf(YogaGold.copy(alpha = 0.12f), Color.Transparent)),
            radius = size.width * 0.75f,
            center = Offset(size.width * 0.08f, size.height * 0.12f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(LightSage.copy(alpha = 0.42f), Color.Transparent)),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.94f, size.height * 0.45f)
        )
        val step = 34f
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                drawCircle(YogaGold.copy(alpha = 0.055f), radius = 1.2f, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}
