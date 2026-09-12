package com.example.vedaahar

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.PathFillType
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.doctor.data.DoctorRepository
import com.example.vedaahar.doctor.ui.PatientVerifiedDoctorsSection
import com.example.vedaahar.dosha.DoshaResultStore
import com.example.vedaahar.dosha.SavedDoshaAssessment
import com.example.vedaahar.ui.CircularArrowCta
import com.example.vedaahar.ui.DoshaTestIllustration
import com.example.vedaahar.ui.HealthyMealBowlIllustration
import com.example.vedaahar.ui.IngredientBookIllustration
import com.example.vedaahar.ui.RunnerFitnessIllustration
import com.example.vedaahar.ui.SymptomsClipboardIllustration
import com.example.vedaahar.ui.UploadDocIllustration
import com.example.vedaahar.ui.VedamritMortarLogo
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.VedamritAvatarBg
import com.example.vedaahar.ui.theme.VedamritAvatarIcon
import com.example.vedaahar.ui.theme.VedamritBlueCard
import com.example.vedaahar.ui.theme.VedamritBlueCta
import com.example.vedaahar.ui.theme.VedamritBlueSubtext
import com.example.vedaahar.ui.theme.VedamritBlueText
import com.example.vedaahar.ui.theme.VedamritCanvas
import com.example.vedaahar.ui.theme.VedamritDarkGreen
import com.example.vedaahar.ui.theme.VedamritDietCard
import com.example.vedaahar.ui.theme.VedamritDietCta
import com.example.vedaahar.ui.theme.VedamritMintCard
import com.example.vedaahar.ui.theme.VedamritMintCta
import com.example.vedaahar.ui.theme.VedamritMintSubtext
import com.example.vedaahar.ui.theme.VedamritMintText
import com.example.vedaahar.ui.theme.VedamritNavInactive
import com.example.vedaahar.ui.theme.VedamritNavSurface
import com.example.vedaahar.ui.theme.VedamritNotificationRed
import com.example.vedaahar.ui.theme.VedamritPeachCard
import com.example.vedaahar.ui.theme.VedamritPeachCta
import com.example.vedaahar.ui.theme.VedamritPeachSubtext
import com.example.vedaahar.ui.theme.VedamritPeachText
import com.example.vedaahar.ui.theme.VedamritPurpleCard
import com.example.vedaahar.ui.theme.VedamritPurpleCta
import com.example.vedaahar.ui.theme.VedamritPurpleSubtext
import com.example.vedaahar.ui.theme.VedamritPurpleText
import com.example.vedaahar.ui.theme.VedamritTagline
import com.example.vedaahar.ui.theme.VedamritYellowCard
import com.example.vedaahar.ui.theme.VedamritYellowCta
import com.example.vedaahar.ui.theme.VedamritYellowSubtext
import com.example.vedaahar.ui.theme.VedamritYellowText

private const val DashboardPrefsName = "vedaahaar_onboarding"
private const val DashboardPatientFullNameKey = "patient_full_name"

private fun patientDisplayName(fullName: String): String {
    val clean = fullName.trim().split(Regex("\\s+")).firstOrNull { it.isNotBlank() }
    return if (!clean.isNullOrBlank()) clean.replaceFirstChar { it.titlecase() } else "Deepak"
}

@Composable
fun PatientDashboardScreen(
    modifier: Modifier = Modifier,
    selectedTab: String = "Home",
    onTabSelected: (String) -> Unit = {},
    onShoppingClick: () -> Unit = {},
    onYogaMeditationClick: () -> Unit = {},
    onHealthReminderClick: () -> Unit = {},
    onIngredientBookClick: () -> Unit = {},
    onCommunityCareClick: () -> Unit = {},
    onRetakeDoshaClick: () -> Unit = {},
    onDietPlanClick: () -> Unit = {},
    onSymptomsAnalysisClick: () -> Unit = {},
    onUploadDocumentClick: () -> Unit = {},
    onViewAllDocumentsClick: () -> Unit = {},
    onViewDocumentClick: (com.example.vedaahar.document.MedicalDocument) -> Unit = {}
) {
    var showNotificationDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentDoshaResult = DoshaResultStore.current(context)
    val patientFullName = remember {
        context.getSharedPreferences(DashboardPrefsName, Context.MODE_PRIVATE)
            .getString(DashboardPatientFullNameKey, null)
            ?.trim()
            .orEmpty()
    }
    val greetingName = remember(patientFullName) { patientDisplayName(patientFullName) }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = {
                Text(
                    text = "Health Notifications",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = VedamritDarkGreen,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "• Time for your warm herbal water ritual.",
                        color = Color(0xFF334438),
                        fontSize = 14.sp
                    )
                    Text(
                        "• Dosha rhythm: Afternoon Pitta digestion peak active.",
                        color = Color(0xFF334438),
                        fontSize = 14.sp
                    )
                    Text(
                        "• Document storage is secure and encrypted.",
                        color = Color(0xFF334438),
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Got it", color = VedamritMintCta, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFFFFFBF4),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = VedamritCanvas
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    "Wellness" -> WellnessTabContent(
                        onYogaMeditationClick = onYogaMeditationClick,
                        onHealthReminderClick = onHealthReminderClick,
                        onIngredientBookClick = onIngredientBookClick
                    )
                    "Consult" -> ConsultTabContent(onCommunityCareClick = onCommunityCareClick)
                    "Shopping" -> ShoppingTabContent(onOpenFullStoreClick = onShoppingClick)
                    "Profile" -> ProfileTabContent(
                        patientFullName = if (patientFullName.isNotBlank()) patientFullName else "Deepak Sharma",
                        doshaResult = currentDoshaResult,
                        onViewDocumentsClick = onViewAllDocumentsClick,
                        onUploadDocumentClick = onUploadDocumentClick,
                        onRetakeDoshaClick = onRetakeDoshaClick,
                        onDietPlanClick = onDietPlanClick
                    )
                    else -> VedamritHomeContent(
                        greetingName = greetingName,
                        onNotificationClick = { showNotificationDialog = true },
                        onProfileClick = { onTabSelected("Profile") },
                        onUploadDocumentClick = onUploadDocumentClick,
                        onRetakeDoshaClick = onRetakeDoshaClick,
                        onDietPlanClick = onDietPlanClick,
                        onSymptomsAnalysisClick = onSymptomsAnalysisClick,
                        onIngredientBookClick = onIngredientBookClick,
                        onHealthReminderClick = onHealthReminderClick
                    )
                }
                // Proper safe-area padding ensuring cards are 100% visible above bottom bar with zero overlap
                Spacer(modifier = Modifier.height(110.dp).navigationBarsPadding())
            }
        }
    }
}

// ==========================================
// HOME TAB CONTENT (Matches Reference Exactly)
// ==========================================

@Composable
private fun VedamritHomeContent(
    greetingName: String,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUploadDocumentClick: () -> Unit,
    onRetakeDoshaClick: () -> Unit,
    onDietPlanClick: () -> Unit,
    onSymptomsAnalysisClick: () -> Unit,
    onIngredientBookClick: () -> Unit,
    onHealthReminderClick: () -> Unit
) {
    // 1. HEADER
    VedamritHeader(
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick
    )

    // 2. GREETING
    VedamritGreeting(name = greetingName)

    // 3. TWO-COLUMN ROW 1: Upload Your Document | Dosha Test
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        UploadDocumentCard(
            onClick = onUploadDocumentClick,
            modifier = Modifier.weight(1f)
        )
        DoshaTestCard(
            onClick = onRetakeDoshaClick,
            modifier = Modifier.weight(1f)
        )
    }

    // 4. FULL-WIDTH ROW 2: Personalised Diet
    PersonalisedDietCard(
        onClick = onDietPlanClick,
        modifier = Modifier.fillMaxWidth()
    )

    // 5. TWO-COLUMN ROW 3: Symptoms Analysis | Ingredient Book
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SymptomsAnalysisCard(
            onClick = onSymptomsAnalysisClick,
            modifier = Modifier.weight(1f)
        )
        IngredientBookCard(
            onClick = onIngredientBookClick,
            modifier = Modifier.weight(1f)
        )
    }

    // 6. FULL-WIDTH ROW 4: Life Style Calculator
    LifestyleCalculatorCard(
        onClick = onHealthReminderClick,
        modifier = Modifier.fillMaxWidth()
    )
}

// ==========================================
// 1. HEADER COMPONENT
// ==========================================

@Composable
private fun VedamritHeader(
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Logo + Brand Wordmark + Tagline
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VedamritMortarLogo()

            Column {
                Text(
                    text = "vedamrit",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    lineHeight = 26.sp,
                    color = VedamritDarkGreen
                )
                Text(
                    text = "Ancient Wisdom • Healthier You",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = VedamritTagline
                )
            }
        }

        // Right: Notification Bell + Profile Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notification Bell with Red Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onNotificationClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = Color(0xFF2C3E30),
                    modifier = Modifier.size(23.dp)
                )
                // Red badge
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 7.dp)
                        .clip(CircleShape)
                        .background(VedamritNotificationRed)
                )
            }

            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VedamritAvatarBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onProfileClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = VedamritAvatarIcon,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ==========================================
// 2. GREETING COMPONENT
// ==========================================

@Composable
private fun VedamritGreeting(name: String) {
    Column(modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Hello, ",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color(0xFF1E2A22)
            )
            Text(
                text = name,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = VedamritDarkGreen
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Your health journey starts here",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF556559)
        )
    }
}

// ==========================================
// CARD 1: UPLOAD YOUR DOCUMENT
// ==========================================

@Composable
private fun UploadDocumentCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(208.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VedamritMintCard),
        border = BorderStroke(1.dp, Color(0xFFCFE3D5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UploadDocIllustration()
                }

                // Text Content
                Column(modifier = Modifier.padding(bottom = 2.dp)) {
                    Text(
                        text = "Upload Your\nDocument",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        color = VedamritMintText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Securely upload and\nmanage your reports",
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = VedamritMintSubtext
                    )
                }
            }

            // Bottom Right Action Arrow
            CircularArrowCta(
                background = VedamritMintCta,
                onClick = onClick,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

// ==========================================
// CARD 2: DOSHA TEST
// ==========================================

@Composable
private fun DoshaTestCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(208.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VedamritPeachCard),
        border = BorderStroke(1.dp, Color(0xFFF7D6B9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tri-Dosha Graphic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dosha_assessment_diagram),
                        contentDescription = "Dosha Assessment Diagram",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                }


                // Text Content
                Column(modifier = Modifier.padding(bottom = 2.dp)) {
                    Text(
                        text = "Dosha\nTest",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        color = VedamritPeachText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Know your Prakriti\nand Vikriti",
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = VedamritPeachSubtext
                    )
                }
            }

            // Bottom Right Action Arrow
            CircularArrowCta(
                background = VedamritPeachCta,
                onClick = onClick,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

// ==========================================
// CARD 3: PERSONALISED DIET (Full-Width)
// ==========================================

@Composable
private fun PersonalisedDietCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(166.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VedamritDietCard),
        border = BorderStroke(1.dp, Color(0xFFCCE2D4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left content column
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Personalised Diet",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF11291C)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Get AI-powered diet plans\nbased on your body type\nand health goals",
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF425648)
                    )
                }

                CircularArrowCta(
                    background = VedamritDietCta,
                    onClick = onClick,
                    sizeDp = 38
                )
            }

            // Right meal bowl image
            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Image(
                    painter = painterResource(id = R.drawable.healthy_diet_bowl),
                    contentDescription = "Personalised Diet Bowl",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(138.dp)
                        .offset(x = 6.dp)
                )
            }

        }
    }
}

// ==========================================
// CARD 4: SYMPTOMS ANALYSIS
// ==========================================

@Composable
private fun SymptomsAnalysisCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(208.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VedamritBlueCard),
        border = BorderStroke(1.dp, Color(0xFFC7DDF0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Clipboard Illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SymptomsClipboardIllustration()
                }

                // Text Content
                Column(modifier = Modifier.padding(bottom = 2.dp)) {
                    Text(
                        text = "Symptoms\nAnalysis",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        color = VedamritBlueText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Understand your symptoms\nwith AI insights",
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = VedamritBlueSubtext
                    )
                }
            }

            // Bottom Right Action Arrow
            CircularArrowCta(
                background = VedamritBlueCta,
                onClick = onClick,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

// ==========================================
// CARD 5: INGREDIENT BOOK
// ==========================================

@Composable
private fun IngredientBookCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(208.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VedamritPurpleCard),
        border = BorderStroke(1.dp, Color(0xFFDBCBEF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Book Illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IngredientBookIllustration()
                }

                // Text Content
                Column(modifier = Modifier.padding(bottom = 2.dp)) {
                    Text(
                        text = "Ingredient\nBook",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        color = VedamritPurpleText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Explore the healing power\nof natural ingredients",
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = VedamritPurpleSubtext
                    )
                }
            }

            // Bottom Right Action Arrow
            CircularArrowCta(
                background = VedamritPurpleCta,
                onClick = onClick,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

// ==========================================
// CARD 6: LIFE STYLE CALCULATOR (Full-Width)
// ==========================================

@Composable
private fun LifestyleCalculatorCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(124.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VedamritYellowCard),
        border = BorderStroke(1.dp, Color(0xFFF3E1A6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Runner illustration
            RunnerFitnessIllustration(modifier = Modifier.size(76.dp, 64.dp))

            Spacer(modifier = Modifier.width(10.dp))

            // Center: Title + Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Life Style Calculator",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.5.sp,
                    color = VedamritYellowText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Track and improve your daily habits\nfor a healthier you",
                    fontSize = 11.8.sp,
                    lineHeight = 16.sp,
                    color = VedamritYellowSubtext
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Golden Arrow CTA
            CircularArrowCta(
                background = VedamritYellowCta,
                onClick = onClick,
                sizeDp = 38
            )
        }
    }
}




// ==========================================
// PRESERVED WELLNESS TAB CONTENT
// ==========================================

@Composable
private fun WellnessTabContent(
    onYogaMeditationClick: () -> Unit,
    onHealthReminderClick: () -> Unit,
    onIngredientBookClick: () -> Unit
) {
    Text(
        text = "Wellness Practice",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = VedamritDarkGreen
    )
    Text(
        text = "Daily Ayurvedic rituals and calming movement.",
        fontSize = 14.sp,
        color = Color(0xFF5A665E)
    )
    Spacer(modifier = Modifier.height(4.dp))

    WellnessToolCard(
        title = "Yoga & Meditation",
        description = "Practice guided breathwork, pranayama, and calming movement.",
        icon = "🧘",
        accent = Color(0xFF5E8B52),
        onClick = onYogaMeditationClick
    )
    WellnessToolCard(
        title = "Ingredient Book",
        description = "Browse 240+ natural herbs, rituals, and safe usage guides.",
        icon = "🌿",
        accent = Color(0xFFA07850),
        onClick = onIngredientBookClick
    )
    WellnessToolCard(
        title = "Health Reminder",
        description = "Manage diet timings, medicine intake, hydration, and yoga alarms.",
        icon = "🔔",
        accent = Color(0xFF527B8B),
        onClick = onHealthReminderClick
    )
}

@Composable
private fun WellnessToolCard(
    title: String,
    description: String,
    icon: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 22.sp)
                }
                Column {
                    Text(
                        text = title,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1E2A22)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF5A665E),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = title,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// PRESERVED CONSULT TAB CONTENT
// ==========================================

@Composable
private fun ConsultTabContent(onCommunityCareClick: () -> Unit) {
    Text(
        text = "Consult & Care",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = VedamritDarkGreen
    )
    Text(
        text = "Connect with certified Ayurvedic Vaidyas and healthcare centers.",
        fontSize = 14.sp,
        color = Color(0xFF5A665E)
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Community Care Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCommunityCareClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VedamritMintCard),
        border = BorderStroke(1.dp, Color(0xFFC7DEC9))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Community Care Locator",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = VedamritDarkGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Find verified Ayurvedic clinics, botanical hospitals, and community centers nearby.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF3B5643)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    PatientVerifiedDoctorsSection(doctors = DoctorRepository().verifiedDoctors())
}

// ==========================================
// PRESERVED PROFILE TAB CONTENT
// ==========================================

@Composable
private fun ProfileTabContent(
    patientFullName: String,
    doshaResult: SavedDoshaAssessment?,
    onViewDocumentsClick: () -> Unit,
    onUploadDocumentClick: () -> Unit,
    onRetakeDoshaClick: () -> Unit,
    onDietPlanClick: () -> Unit
) {
    val dosha = doshaResult?.profileName ?: "Kapha"

    Text(
        text = "Patient Profile",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = VedamritDarkGreen
    )

    // User Profile Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
        border = BorderStroke(1.dp, Color(0xFFE2E9E2))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VedamritAvatarBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = VedamritAvatarIcon,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patientFullName,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1E2A22)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(VedamritMintCard)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Prakriti: $dosha",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = VedamritMintCta
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "QUICK ACTIONS",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF6B7B70),
        letterSpacing = 2.sp
    )

    ProfileActionButton(
        title = "My Medical Documents",
        subtitle = "View uploaded prescriptions and lab reports",
        icon = "📄",
        onClick = onViewDocumentsClick
    )
    ProfileActionButton(
        title = "Upload New Document",
        subtitle = "Add a fresh diagnostic report or prescription",
        icon = "📤",
        onClick = onUploadDocumentClick
    )
    ProfileActionButton(
        title = "Retake Dosha Assessment",
        subtitle = "Recalculate Prakriti and Vikriti imbalances",
        icon = "🌿",
        onClick = onRetakeDoshaClick
    )
    ProfileActionButton(
        title = "AI Diet Recommendations",
        subtitle = "Review food guidelines tailored to your dosha",
        icon = "🥗",
        onClick = onDietPlanClick
    )
}

@Composable
private fun ProfileActionButton(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
        border = BorderStroke(1.dp, Color(0xFFE8EDE8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(icon, fontSize = 20.sp)
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color(0xFF1E2A22)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF647568)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = VedamritDarkGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ==========================================
// PRESERVED SHOPPING TAB CONTENT
// ==========================================

@Composable
private fun ShoppingTabContent(
    onOpenFullStoreClick: () -> Unit
) {
    Text(
        text = "Ayurvedic Store",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = VedamritDarkGreen
    )
    Text(
        text = "Authentic herbal formulations, oils, and dosha teas.",
        fontSize = 14.sp,
        color = Color(0xFF5A665E)
    )
    Spacer(modifier = Modifier.height(4.dp))

    ShoppingCategoryCard(
        title = "Supplements & Tonics",
        description = "Pure organic Ashwagandha, Triphala, Brahmi, and Chyawanprash.",
        icon = "🌿",
        accent = Color(0xFF3F6A48),
        onClick = onOpenFullStoreClick
    )

    ShoppingCategoryCard(
        title = "Dosha Teas & Elixirs",
        description = "Blended herbal infusions crafted specifically for Vata, Pitta, and Kapha balance.",
        icon = "🍵",
        accent = Color(0xFFC5A866),
        onClick = onOpenFullStoreClick
    )

    ShoppingCategoryCard(
        title = "Medicinal Oils & Skincare",
        description = "Kumkumadi Tailam, Bhringraj hair nectar, and pure sesame massage oil.",
        icon = "✨",
        accent = Color(0xFF73181C),
        onClick = onOpenFullStoreClick
    )

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onOpenFullStoreClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VedamritDarkGreen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = FilledShoppingIcon,
                contentDescription = null,
                tint = PureWhite,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Explore Full Botanical Store",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ShoppingCategoryCard(
    title: String,
    description: String,
    icon: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 22.sp)
                }
                Column {
                    Text(
                        text = title,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1E2A22)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF5A665E),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = title,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
