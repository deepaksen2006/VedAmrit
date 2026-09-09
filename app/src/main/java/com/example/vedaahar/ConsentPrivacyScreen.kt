package com.example.vedaahar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.PureWhite

private val ConsentBackground = Color(0xFFFAF8F2)
private val ConsentCardBg = Color(0xFFFCFAF5)
private val ConsentBorder = Color(0xFFE2DCD0)
private val ConsentStripBg = Color(0xFFEDE9DE)
private val ConsentTextPrimary = Color(0xFF143324)
private val ConsentTextSecondary = Color(0xFF45554B)
private val ConsentTextMuted = Color(0xFF6F7C74)
private val ConsentButtonGreen = Color(0xFF183B2B)
private val CinzelDecorative = FontFamily(Font(R.font.cinzel_decorative_regular))

@Composable
fun ConsentPrivacyScreen(
    onAgreeContinue: () -> Unit,
    onDecline: () -> Unit,
    onBack: () -> Unit = onDecline,
    modifier: Modifier = Modifier
) {
    var abdmConsent by remember { mutableStateOf(true) }
    var dpdpConsent by remember { mutableStateOf(true) }
    val canContinue = abdmConsent && dpdpConsent

    Surface(modifier = modifier.fillMaxSize(), color = ConsentBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Back Arrow and Logo
            ConsentHeaderBar(onBack = onBack)

            Spacer(modifier = Modifier.height(16.dp))

            // Consent Title Section
            ConsentTitleSection()

            Spacer(modifier = Modifier.height(18.dp))

            // Card 1: ABDM Consent
            ConsentCard(
                badge = { AbdmLogoBadge() },
                title = "Ayushman Bharat Digital Mission (ABDM) Consent",
                description = "I allow Vedamrit to collect and use my health information (such as health records, consultation history, and Ayurvedic assessment data) in a secure and standardized format, as per ABDM guidelines, for better and continuous care.",
                infoIcon = { ShieldCheckIcon() },
                infoText = "This helps us connect with the national health ecosystem and ensures your data is safe, interoperable and used only for your healthcare benefit.",
                checkboxChecked = abdmConsent,
                onCheckboxChange = { abdmConsent = it },
                checkboxLabel = "I give my consent for ABDM integration."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: DPDP Consent
            ConsentCard(
                badge = { DpdpLogoBadge() },
                title = "Digital Personal Data Protection Act (DPDP) Consent",
                description = "I agree to the collection, storage, and processing of my personal data (such as name, age, contact details, lifestyle and health information) as per the DPDP Act, 2023. This data will be used only for the purposes mentioned in the Privacy Policy and will not be shared without my explicit consent.",
                infoIcon = { StripLockIcon() },
                infoText = "Your privacy matters. We follow strict data protection and security practices to keep your information safe.",
                checkboxChecked = dpdpConsent,
                onCheckboxChange = { dpdpConsent = it },
                checkboxLabel = "I give my consent for data collection and processing as per DPDP Act, 2023."
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Withdrawal note flanked by thin divider lines
            WithdrawalConsentNote()

            Spacer(modifier = Modifier.height(14.dp))

            // "I Agree & Continue →" Action Button
            Button(
                onClick = onAgreeContinue,
                enabled = canContinue,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConsentButtonGreen,
                    contentColor = PureWhite,
                    disabledContainerColor = ConsentButtonGreen.copy(alpha = 0.42f),
                    disabledContentColor = PureWhite.copy(alpha = 0.72f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = if (canContinue) 6.dp else 0.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = ConsentButtonGreen.copy(alpha = 0.2f),
                        spotColor = ConsentButtonGreen.copy(alpha = 0.15f)
                    )
            ) {
                Text(
                    text = "I Agree & Continue →",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // "Remind Me Later" Button
            Text(
                text = "Remind Me Later",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ConsentTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDecline
                    )
                    .padding(vertical = 6.dp, horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConsentHeaderBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
        ) {
            BackChevronIcon()
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VEDAMRIT",
                style = TextStyle(
                    fontFamily = CinzelDecorative,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = ConsentTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "Ancient Wisdom • Modern Health",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                color = ConsentTextPrimary
            )
        }
    }
}

@Composable
private fun ConsentTitleSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Consent",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = ConsentTextPrimary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your data. Your health. Our priority.",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ConsentTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(50))
                .background(ConsentTextPrimary)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "To provide you with a personalized Ayurvedic experience, we need your consent to collect and process certain health and personal information, in accordance with ABDM and DPDP guidelines.",
            fontSize = 13.5.sp,
            lineHeight = 18.5.sp,
            color = ConsentTextSecondary
        )
    }
}

@Composable
private fun ConsentCard(
    badge: @Composable () -> Unit,
    title: String,
    description: String,
    infoIcon: @Composable () -> Unit,
    infoText: String,
    checkboxChecked: Boolean,
    onCheckboxChange: (Boolean) -> Unit,
    checkboxLabel: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = ConsentTextPrimary.copy(alpha = 0.04f),
                spotColor = ConsentTextPrimary.copy(alpha = 0.03f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = ConsentCardBg,
        border = BorderStroke(1.dp, ConsentBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Emblem badge + Title & Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                badge()
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 19.sp,
                            color = ConsentTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        lineHeight = 16.5.sp,
                        color = ConsentTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Security/Privacy Information Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ConsentStripBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                infoIcon()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = infoText,
                    fontSize = 11.5.sp,
                    lineHeight = 15.5.sp,
                    color = ConsentTextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Checkbox Row
            ConsentCheckbox(
                checked = checkboxChecked,
                onCheckedChange = onCheckboxChange,
                label = checkboxLabel
            )
        }
    }
}

@Composable
private fun ConsentCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (checked) ConsentButtonGreen else PureWhite)
                .border(
                    BorderStroke(
                        if (checked) 0.dp else 1.6.dp,
                        if (checked) ConsentButtonGreen else Color(0xFF8E9C93)
                    ),
                    RoundedCornerShape(5.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = ConsentTextPrimary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AbdmLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(PureWhite)
            .border(BorderStroke(1.dp, Color(0xFFDFD9CC)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(width = 36.dp, height = 24.dp)) {
                val cx = size.width / 2f
                val strokeWidth = 2.4.dp.toPx()
                val headRadius = 2.7.dp.toPx()

                val blueColor = Color(0xFF1555A2)
                val tealColor = Color(0xFF009677)
                val orangeColor = Color(0xFFE86025)

                // Center Head & U-curve Body (Blue)
                drawCircle(color = blueColor, radius = headRadius, center = Offset(cx, 4.dp.toPx()))
                val bluePath = Path().apply {
                    moveTo(cx - 5.dp.toPx(), 9.dp.toPx())
                    cubicTo(
                        cx - 5.dp.toPx(), 18.dp.toPx(),
                        cx + 5.dp.toPx(), 18.dp.toPx(),
                        cx + 5.dp.toPx(), 9.dp.toPx()
                    )
                }
                drawPath(bluePath, color = blueColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

                // Left Head & Embracing Body (Teal)
                drawCircle(color = tealColor, radius = headRadius, center = Offset(cx - 9.dp.toPx(), 6.5.dp.toPx()))
                val tealPath = Path().apply {
                    moveTo(cx - 13.dp.toPx(), 11.dp.toPx())
                    cubicTo(
                        cx - 13.dp.toPx(), 19.5.dp.toPx(),
                        cx - 3.dp.toPx(), 22.dp.toPx(),
                        cx, 22.dp.toPx()
                    )
                }
                drawPath(tealPath, color = tealColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

                // Right Head & Embracing Body (Orange)
                drawCircle(color = orangeColor, radius = headRadius, center = Offset(cx + 9.dp.toPx(), 6.5.dp.toPx()))
                val orangePath = Path().apply {
                    moveTo(cx + 13.dp.toPx(), 11.dp.toPx())
                    cubicTo(
                        cx + 13.dp.toPx(), 19.5.dp.toPx(),
                        cx + 3.dp.toPx(), 22.dp.toPx(),
                        cx, 22.dp.toPx()
                    )
                }
                drawPath(orangePath, color = orangeColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ABDM",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1555A2),
                letterSpacing = 0.5.sp,
                lineHeight = 11.sp
            )
            Text(
                text = "Ayushman Bharat",
                fontSize = 5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A3E70),
                lineHeight = 6.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Digital Mission",
                fontSize = 5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A3E70),
                lineHeight = 6.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DpdpLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(PureWhite)
            .border(BorderStroke(1.dp, Color(0xFFDFD9CC)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(width = 24.dp, height = 24.dp)) {
                val lockColor = Color(0xFF1B4332)
                val shackleStroke = 2.3.dp.toPx()
                val cx = size.width / 2f

                // Shackle
                val shacklePath = Path().apply {
                    moveTo(cx - 5.5.dp.toPx(), 11.dp.toPx())
                    lineTo(cx - 5.5.dp.toPx(), 6.5.dp.toPx())
                    cubicTo(
                        cx - 5.5.dp.toPx(), 1.5.dp.toPx(),
                        cx + 5.5.dp.toPx(), 1.5.dp.toPx(),
                        cx + 5.5.dp.toPx(), 6.5.dp.toPx()
                    )
                    lineTo(cx + 5.5.dp.toPx(), 11.dp.toPx())
                }
                drawPath(shacklePath, color = lockColor, style = Stroke(width = shackleStroke, cap = StrokeCap.Round))

                // Lock Body
                val bodyWidth = 17.dp.toPx()
                val bodyHeight = 13.dp.toPx()
                val bodyTop = 10.5.dp.toPx()
                val bodyLeft = cx - (bodyWidth / 2f)
                drawRoundRect(
                    color = lockColor,
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
                )

                // Keyhole
                drawCircle(
                    color = PureWhite,
                    radius = 1.6.dp.toPx(),
                    center = Offset(cx, bodyTop + 4.8.dp.toPx())
                )
                val keyholeSlot = Path().apply {
                    moveTo(cx - 0.9.dp.toPx(), bodyTop + 5.2.dp.toPx())
                    lineTo(cx + 0.9.dp.toPx(), bodyTop + 5.2.dp.toPx())
                    lineTo(cx + 1.2.dp.toPx(), bodyTop + 8.8.dp.toPx())
                    lineTo(cx - 1.2.dp.toPx(), bodyTop + 8.8.dp.toPx())
                    close()
                }
                drawPath(keyholeSlot, color = PureWhite)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "DPDP",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1B4332),
                letterSpacing = 0.5.sp,
                lineHeight = 11.sp
            )
            Text(
                text = "Digital Personal Data",
                fontSize = 5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF384E42),
                lineHeight = 6.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Protection Act",
                fontSize = 5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF384E42),
                lineHeight = 6.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ShieldCheckIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeColor = Color(0xFF1B4332)
        val strokeWidth = 2.dp.toPx()

        // Shield contour
        val shieldPath = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.08f)
            cubicTo(
                size.width * 0.75f, size.height * 0.08f,
                size.width * 0.90f, size.height * 0.16f,
                size.width * 0.90f, size.height * 0.38f
            )
            cubicTo(
                size.width * 0.90f, size.height * 0.68f,
                size.width * 0.66f, size.height * 0.88f,
                size.width * 0.5f, size.height * 0.96f
            )
            cubicTo(
                size.width * 0.34f, size.height * 0.88f,
                size.width * 0.10f, size.height * 0.68f,
                size.width * 0.10f, size.height * 0.38f
            )
            cubicTo(
                size.width * 0.10f, size.height * 0.16f,
                size.width * 0.25f, size.height * 0.08f,
                size.width * 0.5f, size.height * 0.08f
            )
            close()
        }
        drawPath(shieldPath, color = strokeColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Checkmark inside shield
        val checkPath = Path().apply {
            moveTo(size.width * 0.33f, size.height * 0.50f)
            lineTo(size.width * 0.45f, size.height * 0.62f)
            lineTo(size.width * 0.68f, size.height * 0.38f)
        }
        drawPath(checkPath, color = strokeColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun StripLockIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val lockColor = Color(0xFF1B4332)
        val cx = size.width / 2f
        val shackleStroke = 2.dp.toPx()

        val shacklePath = Path().apply {
            moveTo(cx - 4.5.dp.toPx(), 9.dp.toPx())
            lineTo(cx - 4.5.dp.toPx(), 5.dp.toPx())
            cubicTo(
                cx - 4.5.dp.toPx(), 1.5.dp.toPx(),
                cx + 4.5.dp.toPx(), 1.5.dp.toPx(),
                cx + 4.5.dp.toPx(), 5.dp.toPx()
            )
            lineTo(cx + 4.5.dp.toPx(), 9.dp.toPx())
        }
        drawPath(shacklePath, color = lockColor, style = Stroke(width = shackleStroke, cap = StrokeCap.Round))

        val bodyW = 14.dp.toPx()
        val bodyH = 10.5.dp.toPx()
        val bodyT = 8.5.dp.toPx()
        drawRoundRect(
            color = lockColor,
            topLeft = Offset(cx - bodyW / 2f, bodyT),
            size = Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
        )

        drawCircle(
            color = ConsentStripBg,
            radius = 1.3.dp.toPx(),
            center = Offset(cx, bodyT + 3.8.dp.toPx())
        )
        drawLine(
            color = ConsentStripBg,
            start = Offset(cx, bodyT + 3.8.dp.toPx()),
            end = Offset(cx, bodyT + 7.2.dp.toPx()),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun BackChevronIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val color = ConsentTextPrimary
        val strokeWidth = 2.2.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.62f, size.height * 0.18f)
            lineTo(size.width * 0.30f, size.height * 0.50f)
            lineTo(size.width * 0.62f, size.height * 0.82f)
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun WithdrawalConsentNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFDDD7CB))
        )
        Text(
            text = "You can withdraw your consent anytime from your profile settings.",
            fontSize = 11.sp,
            color = ConsentTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFDDD7CB))
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsentPrivacyScreenPreview() {
    ConsentPrivacyScreen(onAgreeContinue = {}, onDecline = {}, onBack = {})
}
