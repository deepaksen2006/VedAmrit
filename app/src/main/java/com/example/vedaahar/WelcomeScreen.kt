package com.example.vedaahar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray

private data class FeatureCardData(
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
    val onCreatePatientAccount: () -> Unit = onStartAssessment

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(260.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Cream),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.vedaahaar_logo),
                            contentDescription = "VedaAhaar logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AYURVEDA + AI",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SageGreen,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Personalized Ayurvedic Care Powered by AI",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = DarkForestGreen,
                    lineHeight = 40.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "A calm, intelligent wellness companion that helps you understand your body, improve daily habits, and access holistic care with confidence.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = SoftBlueGray,
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FEATURES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ForestGreen,
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Everyday care, designed to feel simple",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = DarkForestGreen,
                        lineHeight = 28.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val features = listOf(
                FeatureCardData(
                    icon = DoshaAnalysisIcon,
                    title = "Dosha Analysis",
                    description = "Understand your Ayurvedic constitution with guided, personalized insights."
                ),
                FeatureCardData(
                    icon = DietPlansIcon,
                    title = "Diet Plans",
                    description = "Receive balanced meal suggestions aligned with your body type and goals."
                ),
                FeatureCardData(
                    icon = HealthTrackerIcon,
                    title = "Health Tracker",
                    description = "Monitor routines, wellness scores, and gradual lifestyle improvements over time."
                ),
                FeatureCardData(
                    icon = ConsultDoctorIcon,
                    title = "Consult Doctor",
                    description = "Connect with Ayurvedic experts for deeper guidance when you need support."
                )
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth < 600.dp) 2 else 4
                if (columns == 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        features.forEach { item ->
                            FeatureCard(item = item, modifier = Modifier.weight(1f))
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
                                    FeatureCard(item = item, modifier = Modifier.weight(1f))
                                }
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 700.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StartTodayCard(
                            modifier = Modifier.weight(1f),
                            onCreatePatientAccount = onCreatePatientAccount,
                            onJoinAsDoctor = onJoinAsDoctor
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StartTodayCard(
                            modifier = Modifier.fillMaxWidth(),
                            onCreatePatientAccount = onCreatePatientAccount,
                            onJoinAsDoctor = onJoinAsDoctor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
@UiComposable
private fun FeatureCard(item: FeatureCardData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BeigeBorder),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = LightSage, shape = RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = ForestGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = DarkForestGreen,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SoftBlueGray,
                    lineHeight = 22.sp
                )
            )
        }
    }
}

@Composable
@UiComposable
private fun StartTodayCard(
    modifier: Modifier = Modifier,
    onCreatePatientAccount: () -> Unit = {},
    onJoinAsDoctor: () -> Unit = {}
) {
    val createAccountInteractionSource = remember { MutableInteractionSource() }
    val createAccountPressed by createAccountInteractionSource.collectIsPressedAsState()
    val createAccountScale by animateFloatAsState(
        targetValue = if (createAccountPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "create-account-scale"
    )
    val createAccountGlow by animateDpAsState(
        targetValue = if (createAccountPressed) 12.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 360f),
        label = "create-account-glow"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ForestGreen, DarkForestGreen)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "START TODAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = PureWhite,
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Start your health journey today",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = PureWhite,
                        lineHeight = 28.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Join VedaHaar to begin with your profile, discover personalized recommendations, and build a wellness routine that fits your life.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = PureWhite,
                        lineHeight = 22.sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onCreatePatientAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(createAccountScale)
                        .shadow(
                            elevation = createAccountGlow,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = PureWhite.copy(alpha = 0.25f),
                            spotColor = PureWhite.copy(alpha = 0.2f)
                        ),
                    interactionSource = createAccountInteractionSource,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite)
                ) {
                    Text(
                        text = "Create Patient Account",
                        color = ForestGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onJoinAsDoctor,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, PureWhite),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    )
                ) {
                    Text(text = "Join as Doctor")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen()
}
