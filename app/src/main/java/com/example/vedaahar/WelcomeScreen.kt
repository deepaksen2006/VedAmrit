package com.example.vedaahar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.vedaahar.ui.theme.VedAmritCtaGreen
import com.example.vedaahar.ui.theme.VedAmritGreen
import com.example.vedaahar.ui.theme.WarmIvory

private data class ValuePropData(
    val icon: ImageVector,
    val title: String
)

// 1. Value Proposition Icons
private val PersonalizedGuidanceIcon: ImageVector = ImageVector.Builder(
    name = "PersonalizedGuidanceIcon",
    defaultWidth = 28.dp,
    defaultHeight = 28.dp,
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
        moveTo(6f, 19f)
        curveTo(9f, 17f, 14f, 12f, 17f, 6f)
    }
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(17f, 6f)
        curveTo(17f, 12f, 12.5f, 13.5f, 9.5f, 13f)
        curveTo(11f, 8.5f, 14f, 6f, 17f, 6f)
        close()
        moveTo(17f, 6f)
        lineTo(12f, 10.5f)
    }
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(11.5f, 13.5f)
        curveTo(10f, 17.5f, 6.5f, 18.5f, 4.5f, 17.5f)
        curveTo(5.5f, 14.5f, 8.5f, 13f, 11.5f, 13.5f)
        close()
    }
}.build()

private val AiInsightsIcon: ImageVector = ImageVector.Builder(
    name = "AiInsightsIcon",
    defaultWidth = 28.dp,
    defaultHeight = 28.dp,
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
        moveTo(14f, 3f)
        lineTo(6.5f, 3f)
        curveTo(5.4f, 3f, 4.5f, 3.9f, 4.5f, 5f)
        lineTo(4.5f, 19f)
        curveTo(4.5f, 20.1f, 5.4f, 21f, 6.5f, 21f)
        lineTo(17.5f, 21f)
        curveTo(18.6f, 21f, 19.5f, 20.1f, 19.5f, 19f)
        lineTo(19.5f, 8.5f)
        lineTo(14f, 3f)
        close()
        moveTo(14f, 3f)
        lineTo(14f, 8.5f)
        lineTo(19.5f, 8.5f)
        moveTo(8f, 12f)
        lineTo(16f, 12f)
        moveTo(8f, 15f)
        lineTo(14f, 15f)
        moveTo(8f, 18f)
        lineTo(12f, 18f)
    }
}.build()

private val HolisticWellnessIcon: ImageVector = ImageVector.Builder(
    name = "HolisticWellnessIcon",
    defaultWidth = 28.dp,
    defaultHeight = 28.dp,
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
        moveTo(4f, 13f)
        lineTo(20f, 13f)
        curveTo(20f, 18f, 16.5f, 20f, 12f, 20f)
        curveTo(7.5f, 20f, 4f, 18f, 4f, 13f)
        close()
        moveTo(8.5f, 20f)
        lineTo(15.5f, 20f)
        moveTo(12f, 13f)
        curveTo(10f, 9.5f, 7f, 8.5f, 5f, 9f)
        curveTo(6f, 11.5f, 9f, 12.5f, 12f, 13f)
        moveTo(12f, 13f)
        curveTo(14f, 9f, 17.5f, 8f, 19.5f, 8.5f)
        curveTo(18.5f, 11.5f, 15f, 12.5f, 12f, 13f)
    }
}.build()

private val TrustedCareIcon: ImageVector = ImageVector.Builder(
    name = "TrustedCareIcon",
    defaultWidth = 28.dp,
    defaultHeight = 28.dp,
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
        moveTo(10f, 9f)
        curveTo(11.4f, 9f, 12.5f, 7.9f, 12.5f, 6.5f)
        curveTo(12.5f, 5.1f, 11.4f, 4f, 10f, 4f)
        curveTo(8.6f, 4f, 7.5f, 5.1f, 7.5f, 6.5f)
        curveTo(7.5f, 7.9f, 8.6f, 9f, 10f, 9f)
        close()
        moveTo(4.5f, 17f)
        curveTo(4.5f, 14f, 7f, 12f, 10f, 12f)
        curveTo(11.8f, 12f, 13.5f, 12.8f, 14.5f, 14f)
        moveTo(15.5f, 7.5f)
        curveTo(16.6f, 7.5f, 17.5f, 6.6f, 17.5f, 5.5f)
        curveTo(17.5f, 4.4f, 16.6f, 3.5f, 15.5f, 3.5f)
        curveTo(14.7f, 3.5f, 14f, 4f, 13.7f, 4.7f)
        moveTo(18f, 13.5f)
        curveTo(15.5f, 13.5f, 13.5f, 15.5f, 13.5f, 18f)
        curveTo(13.5f, 20.5f, 15.5f, 22.5f, 18f, 22.5f)
        curveTo(20.5f, 22.5f, 22.5f, 20.5f, 22.5f, 18f)
        curveTo(22.5f, 15.5f, 20.5f, 13.5f, 18f, 13.5f)
        close()
        moveTo(18f, 15.5f)
        lineTo(18f, 20.5f)
        moveTo(15.5f, 18f)
        lineTo(20.5f, 18f)
    }
}.build()

// 2. Authentication CTA Icons
private val PatientProfileOutlineIcon: ImageVector = ImageVector.Builder(
    name = "PatientProfileOutlineIcon",
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
        moveTo(12f, 12f)
        curveTo(14.2f, 12f, 16f, 10.2f, 16f, 8f)
        curveTo(16f, 5.8f, 14.2f, 4f, 12f, 4f)
        curveTo(9.8f, 4f, 8f, 5.8f, 8f, 8f)
        curveTo(8f, 10.2f, 9.8f, 12f, 12f, 12f)
        close()
        moveTo(5.5f, 20f)
        curveTo(5.5f, 16.5f, 8.4f, 14f, 12f, 14f)
        curveTo(15.6f, 14f, 18.5f, 16.5f, 18.5f, 20f)
    }
}.build()

private val DoctorStethoscopeIcon: ImageVector = ImageVector.Builder(
    name = "DoctorStethoscopeIcon",
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
        moveTo(6f, 4f)
        lineTo(6f, 9.5f)
        curveTo(6f, 12.5f, 8.5f, 14.5f, 11.5f, 14.5f)
        curveTo(14.5f, 14.5f, 17f, 12.5f, 17f, 9.5f)
        lineTo(17f, 4f)
        moveTo(11.5f, 14.5f)
        lineTo(11.5f, 16.5f)
        curveTo(11.5f, 19f, 13.5f, 20.5f, 16f, 20.5f)
        curveTo(18.5f, 20.5f, 20.5f, 18.8f, 20.5f, 16.5f)
        moveTo(19f, 16.5f)
        curveTo(19f, 15.7f, 19.7f, 15f, 20.5f, 15f)
        curveTo(21.3f, 15f, 22f, 15.7f, 22f, 16.5f)
        curveTo(22f, 17.3f, 21.3f, 18f, 20.5f, 18f)
        curveTo(19.7f, 18f, 19f, 17.3f, 19f, 16.5f)
        close()
    }
}.build()

private val ArrowForwardIcon: ImageVector = ImageVector.Builder(
    name = "ArrowForwardIcon",
    defaultWidth = 20.dp,
    defaultHeight = 20.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(5f, 12f)
        lineTo(19f, 12f)
        moveTo(13f, 6f)
        lineTo(19f, 12f)
        lineTo(13f, 18f)
    }
}.build()

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onStartAssessment: () -> Unit = {},
    onJoinAsPatient: () -> Unit = onStartAssessment,
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
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. BRAND / LOGO
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vedamrit_logo),
                    contentDescription = "VedAmrit logo",
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .sizeIn(maxWidth = 230.dp, maxHeight = 230.dp)
                        .aspectRatio(1024f / 956f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. HERO SECTION
            Text(
                text = "AYURVEDA + AI",
                color = SoftOliveGreen,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Personalized\nAyurvedic Care\nPowered by AI",
                color = VedAmritGreen,
                fontSize = 29.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "A calm, intelligent wellness companion that helps you understand your body, improve daily habits, and access holistic care with confidence.",
                color = MutedCharcoal,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. VALUE PROPOSITION CARDS (2 x 2 Grid)
            val valueProps = listOf(
                ValuePropData(PersonalizedGuidanceIcon, "Personalized\nGuidance"),
                ValuePropData(AiInsightsIcon, "AI-Powered\nInsights"),
                ValuePropData(HolisticWellnessIcon, "Holistic\nWellness"),
                ValuePropData(TrustedCareIcon, "Trusted\nAyurvedic Care")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                valueProps.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            ValuePropCard(item = item, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 4. EVERYDAY CARE SECTION
            Text(
                text = "Everyday care, designed to feel\nsimple",
                color = VedAmritGreen,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Ayurvedic Illustration (Directly from uploaded asset, aspect ratio preserved)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ayurvedic_visual),
                    contentDescription = "Ancient Wisdom, Modern Intelligence, A Healthier You",
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .sizeIn(maxWidth = 360.dp, maxHeight = 230.dp)
                        .aspectRatio(1024f / 602f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 5. AUTHENTICATION CTAs (Stacked Large Cards)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AuthCtaCard(
                    title = "Join as a Patient",
                    subtitle = "Start your wellness journey",
                    icon = PatientProfileOutlineIcon,
                    isPrimary = true,
                    onClick = onJoinAsPatient
                )

                AuthCtaCard(
                    title = "Join as a Doctor",
                    subtitle = "Share your expertise",
                    icon = DoctorStethoscopeIcon,
                    isPrimary = false,
                    onClick = onJoinAsDoctor
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 6. FOOTER / BRAND MESSAGE
            Text(
                text = "Heal Naturally  |  Live Better  |  With VedAmrit",
                color = MutedCharcoal.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Image(
                painter = painterResource(id = R.drawable.bottom_leaves),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                contentScale = ContentScale.FillWidth
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ValuePropCard(item: ValuePropData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(106.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE8E0D2).copy(alpha = 0.85f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = VedAmritGreen,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                color = VedAmritGreen,
                fontSize = 13.sp,
                lineHeight = 16.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthCtaCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "auth-cta-scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        border = if (isPrimary) null else BorderStroke(1.dp, Color(0xFFE8E0D2)),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) VedAmritCtaGreen else Color(0xFFFFFDF8)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPrimary) 2.5.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (isPrimary) Color.White.copy(alpha = 0.15f) else Color(0xFFEFF5ED),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) PureWhite else VedAmritGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isPrimary) PureWhite else VedAmritGreen,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (isPrimary) PureWhite.copy(alpha = 0.85f) else MutedCharcoal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Icon(
                imageVector = ArrowForwardIcon,
                contentDescription = null,
                tint = if (isPrimary) PureWhite else VedAmritGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen()
}
