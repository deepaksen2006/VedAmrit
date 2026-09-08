package com.example.vedaahar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.MutedCharcoal
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SoftOliveGreen
import com.example.vedaahar.ui.theme.VedAmritGreen
import com.example.vedaahar.ui.theme.WarmIvory

private data class FeatureItemData(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val DoshaAnalysisIcon: ImageVector = ImageVector.Builder(
    name = "DoshaAnalysisIcon",
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
        moveTo(5.5f, 13.2f)
        curveTo(5.5f, 8.2f, 10.5f, 4.8f, 18.5f, 5.5f)
        curveTo(18.8f, 13.3f, 14.4f, 18.6f, 9.4f, 18.6f)
        curveTo(6.9f, 18.6f, 5.5f, 16.4f, 5.5f, 13.2f)
        moveTo(8.5f, 16.2f)
        lineTo(15.8f, 8.8f)
        moveTo(9.8f, 12.4f)
        lineTo(9.8f, 15.3f)
        moveTo(12.4f, 9.8f)
        lineTo(15.3f, 9.8f)
    }
}.build()

private val DietPlansIcon: ImageVector = ImageVector.Builder(
    name = "DietPlansIcon",
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
        moveTo(7f, 4.8f)
        lineTo(7f, 10.2f)
        moveTo(10f, 4.8f)
        lineTo(10f, 10.2f)
        moveTo(13f, 4.8f)
        lineTo(13f, 10.2f)
        moveTo(7f, 10.2f)
        curveTo(7f, 12.2f, 8.4f, 13.6f, 10f, 13.6f)
        curveTo(11.6f, 13.6f, 13f, 12.2f, 13f, 10.2f)
        moveTo(10f, 13.6f)
        lineTo(10f, 19.2f)
        moveTo(17f, 4.8f)
        lineTo(17f, 19.2f)
        moveTo(17f, 4.8f)
        curveTo(15.2f, 6.7f, 15f, 9.6f, 17f, 12f)
    }
}.build()

private val HealthTrackerIcon: ImageVector = ImageVector.Builder(
    name = "HealthTrackerIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 12f)
        lineTo(8f, 12f)
        lineTo(10.2f, 7f)
        lineTo(13.4f, 17f)
        lineTo(15.8f, 12f)
        lineTo(20f, 12f)
    }
}.build()

private val ConsultDoctorIcon: ImageVector = ImageVector.Builder(
    name = "ConsultDoctorIcon",
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
        moveTo(6f, 5f)
        lineTo(6f, 9.2f)
        curveTo(6f, 12.2f, 8.1f, 14.2f, 10.8f, 14.2f)
        curveTo(13.5f, 14.2f, 15.6f, 12.2f, 15.6f, 9.2f)
        lineTo(15.6f, 5f)
        moveTo(10.8f, 14.2f)
        lineTo(10.8f, 15.4f)
        curveTo(10.8f, 18.2f, 13f, 20f, 15.8f, 20f)
        curveTo(18.4f, 20f, 20f, 18.4f, 20f, 16.3f)
        moveTo(18.3f, 16.3f)
        curveTo(18.3f, 15.4f, 19f, 14.7f, 20f, 14.7f)
        curveTo(21f, 14.7f, 21.7f, 15.4f, 21.7f, 16.3f)
        curveTo(21.7f, 17.3f, 21f, 18f, 20f, 18f)
        curveTo(19f, 18f, 18.3f, 17.3f, 18.3f, 16.3f)
    }
}.build()

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onStartAssessment: () -> Unit = {},
    onJoinAsDoctor: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = WarmIvory
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. BRANDING / LOGO (prominent, top-center, no card, no bordered container)
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vedamrit_logo),
                    contentDescription = "VedAmrit logo",
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. HERO SECTION
            // Kicker
            Text(
                text = "AYURVEDA + AI",
                color = SoftOliveGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Heading
            Text(
                text = "Personalized\nAyurvedic Care\nPowered by AI",
                color = VedAmritGreen,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. DESCRIPTION
            Text(
                text = "A calm, intelligent wellness companion\nthat helps you understand your body,\nimprove daily habits, and access holistic\ncare with confidence.",
                color = MutedCharcoal,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 4. FEATURES SECTION
            Text(
                text = "FEATURES",
                color = SoftOliveGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Everyday care, designed to feel simple",
                color = VedAmritGreen,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature List (clean, minimal, calm aesthetic)
            val features = listOf(
                FeatureItemData(
                    icon = DoshaAnalysisIcon,
                    title = "Dosha Analysis",
                    description = "Understand your Ayurvedic constitution with guided, personalized insights."
                ),
                FeatureItemData(
                    icon = DietPlansIcon,
                    title = "Diet Plans",
                    description = "Receive balanced meal suggestions aligned with your body type and goals."
                ),
                FeatureItemData(
                    icon = HealthTrackerIcon,
                    title = "Health Tracker",
                    description = "Monitor routines, wellness scores, and gradual lifestyle improvements over time."
                ),
                FeatureItemData(
                    icon = ConsultDoctorIcon,
                    title = "Consult Doctor",
                    description = "Connect with Ayurvedic experts for deeper guidance when you need support."
                )
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 600.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        features.forEach { item ->
                            CleanFeatureCard(item = item, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        features.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { item ->
                                    CleanFeatureCard(item = item, modifier = Modifier.weight(1f))
                                }
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // 5. CALL TO ACTION BUTTONS
            val createAccountSource = remember { MutableInteractionSource() }
            val isCreatePressed by createAccountSource.collectIsPressedAsState()
            val createScale by animateFloatAsState(
                targetValue = if (isCreatePressed) 0.98f else 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "cta-scale"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onStartAssessment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .scale(createScale),
                    interactionSource = createAccountSource,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VedAmritGreen,
                        contentColor = PureWhite
                    )
                ) {
                    Text(
                        text = "Create Patient Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }

                OutlinedButton(
                    onClick = onJoinAsDoctor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.2.dp, VedAmritGreen.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = VedAmritGreen
                    )
                ) {
                    Text(
                        text = "Join as Doctor",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VedAmritGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CleanFeatureCard(item: FeatureItemData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE8E4D9)),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = Color(0xFFF0F5EE), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = VedAmritGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.title,
                color = VedAmritGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.description,
                color = MutedCharcoal,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen()
}
