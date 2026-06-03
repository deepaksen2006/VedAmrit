package com.example.vedaahar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray

private val TermsGreen = Color(0xFF14783A)
private val TermsDeepGreen = Color(0xFF0F5D2C)
private val TermsLine = Color(0xFFC8DEC9)
private val TermsCard = Color(0xFFFCFFFB)
private val TermsMint = Color(0xFFF0F8F1)
private val TermsDisabled = Color(0xFF95A99A)
private val TermsText = Color(0xFF141B16)

@Composable
fun ConsentPrivacyScreen(
    onAgreeContinue: () -> Unit,
    onDecline: () -> Unit,
    onBack: () -> Unit = onDecline
) {
    var accepted by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(PureWhite, Color(0xFFFBFEFB), Color(0xFFF7FBF6))))
        ) {
            TermsBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                TermsTopBar(onBack = onBack)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeaderConsentIcon()
                    Spacer(modifier = Modifier.height(18.dp))
                    ConsentInfoCard()
                    Spacer(modifier = Modifier.height(18.dp))
                    TermsCheckboxCard(
                        checked = accepted,
                        onCheckedChange = { accepted = it }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    PrivacyCard()
                    Spacer(modifier = Modifier.height(24.dp))
                    AgreeButton(
                        enabled = accepted,
                        onClick = onAgreeContinue
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    BottomConsentNote()
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TermsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TermsDeepGreen,
                modifier = Modifier.size(31.dp)
            )
        }
        Text(
            text = "Terms & Conditions",
            color = TermsDeepGreen,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HeaderConsentIcon() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        DecorativeRule(reverse = true, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .size(82.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(LightSage, Color(0xFFE5F1E5))))
                .border(BorderStroke(1.dp, TermsLine.copy(alpha = 0.72f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(55.dp)) {
                val stroke = Stroke(width = 4.2f, cap = StrokeCap.Round)
                val clipLeft = size.width * 0.23f
                val clipTop = size.height * 0.12f
                drawRoundRect(
                    color = TermsGreen,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.16f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height * 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(9f, 9f),
                    style = stroke
                )
                drawLine(TermsGreen, Offset(clipLeft, clipTop + 5f), Offset(clipLeft + 22f, clipTop + 5f), strokeWidth = 4.2f, cap = StrokeCap.Round)
                drawLine(TermsGreen.copy(alpha = 0.72f), Offset(size.width * 0.32f, size.height * 0.36f), Offset(size.width * 0.56f, size.height * 0.36f), strokeWidth = 3.4f, cap = StrokeCap.Round)
                drawLine(TermsGreen.copy(alpha = 0.72f), Offset(size.width * 0.32f, size.height * 0.49f), Offset(size.width * 0.54f, size.height * 0.49f), strokeWidth = 3.4f, cap = StrokeCap.Round)
                drawLine(TermsGreen.copy(alpha = 0.72f), Offset(size.width * 0.32f, size.height * 0.62f), Offset(size.width * 0.49f, size.height * 0.62f), strokeWidth = 3.4f, cap = StrokeCap.Round)

                val shield = Path().apply {
                    moveTo(size.width * 0.64f, size.height * 0.47f)
                    cubicTo(size.width * 0.78f, size.height * 0.44f, size.width * 0.86f, size.height * 0.38f, size.width * 0.92f, size.height * 0.34f)
                    lineTo(size.width * 0.92f, size.height * 0.62f)
                    cubicTo(size.width * 0.9f, size.height * 0.78f, size.width * 0.78f, size.height * 0.88f, size.width * 0.66f, size.height * 0.93f)
                    cubicTo(size.width * 0.54f, size.height * 0.88f, size.width * 0.44f, size.height * 0.77f, size.width * 0.43f, size.height * 0.62f)
                    lineTo(size.width * 0.43f, size.height * 0.34f)
                    cubicTo(size.width * 0.51f, size.height * 0.39f, size.width * 0.56f, size.height * 0.44f, size.width * 0.64f, size.height * 0.47f)
                    close()
                }
                drawPath(shield, TermsGreen)
                drawLine(PureWhite, Offset(size.width * 0.57f, size.height * 0.62f), Offset(size.width * 0.64f, size.height * 0.7f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(PureWhite, Offset(size.width * 0.64f, size.height * 0.7f), Offset(size.width * 0.77f, size.height * 0.55f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
        }
        DecorativeRule(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DecorativeRule(reverse: Boolean = false, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(34.dp)) {
        val y = size.height / 2f
        val leafX = if (reverse) size.width * 0.72f else size.width * 0.28f
        drawLine(
            color = TermsLine,
            start = Offset(if (reverse) size.width else 0f, y),
            end = Offset(leafX, y),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
        drawCircle(TermsGreen.copy(alpha = 0.55f), radius = 4f, center = Offset(leafX, y))
        val direction = if (reverse) -1f else 1f
        val stemStart = Offset(leafX + 18f * direction, y)
        val stemEnd = Offset(leafX + 46f * direction, y - 7f)
        drawLine(TermsGreen.copy(alpha = 0.62f), stemStart, stemEnd, strokeWidth = 2.4f, cap = StrokeCap.Round)
        drawOval(
            color = TermsGreen.copy(alpha = 0.56f),
            topLeft = Offset(leafX + 22f * direction - 9f, y - 18f),
            size = androidx.compose.ui.geometry.Size(20f, 13f)
        )
        drawOval(
            color = TermsGreen.copy(alpha = 0.45f),
            topLeft = Offset(leafX + 34f * direction - 9f, y - 3f),
            size = androidx.compose.ui.geometry.Size(20f, 13f)
        )
    }
}

@Composable
private fun ConsentInfoCard() {
    TermsCardSurface {
        Box(modifier = Modifier.fillMaxWidth()) {
            DecorativeCornerLeaves(modifier = Modifier.align(Alignment.BottomEnd))
            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Your Consent Matters",
                    color = TermsDeepGreen,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 25.sp,
                    lineHeight = 30.sp
                )
                Text(
                    text = "By continuing, you voluntarily agree to provide your personal, lifestyle, and health-related information for Ayurvedic diet assessment and personalized healthcare recommendations.",
                    color = TermsText,
                    fontSize = 18.sp,
                    lineHeight = 34.sp
                )
            }
        }
    }
}

@Composable
private fun TermsCheckboxCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (checked) TermsGreen else TermsLine,
        label = "termsCheckboxBorder"
    )
    TermsCardSurface(borderColor = borderColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 22.dp, vertical = 26.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(42.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = TermsGreen,
                    uncheckedColor = TermsGreen,
                    checkmarkColor = PureWhite
                )
            )
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = buildAnnotatedString {
                    append("I have read and understood the ")
                    withStyle(SpanStyle(color = TermsDeepGreen, fontWeight = FontWeight.ExtraBold)) {
                        append("Terms & Conditions")
                    }
                    append(" and voluntarily consent to the collection, storage, and use of my health information for Ayurvedic diet and healthcare purposes.")
                },
                color = TermsText,
                fontSize = 18.sp,
                lineHeight = 32.sp
            )
        }
    }
}

@Composable
private fun PrivacyCard() {
    TermsCardSurface(
        containerColor = TermsMint,
        borderColor = Color(0xFFDDECE0),
        shadowAlpha = 0.06f
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(TermsGreen, TermsDeepGreen))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = PureWhite, modifier = Modifier.size(33.dp))
            }
            Spacer(modifier = Modifier.width(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "We value your privacy",
                    color = TermsDeepGreen,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 25.sp
                )
                Text(
                    text = "Your information is safe with us and will be used only for authorized healthcare purposes.",
                    color = TermsText,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )
            }
        }
    }
}

@Composable
private fun AgreeButton(enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed && enabled) 0.98f else 1f, label = "agreeButtonScale")

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .scale(scale)
            .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = TermsGreen.copy(alpha = 0.18f), spotColor = TermsGreen.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TermsGreen,
            contentColor = PureWhite,
            disabledContainerColor = TermsDisabled,
            disabledContentColor = PureWhite.copy(alpha = 0.86f)
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(PureWhite.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "I Agree & Continue",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun BottomConsentNote() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = SoftBlueGray.copy(alpha = 0.76f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = "You can withdraw your consent at any time.",
            color = SoftBlueGray.copy(alpha = 0.92f),
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TermsCardSurface(
    containerColor: Color = TermsCard,
    borderColor: Color = TermsLine,
    shadowAlpha: Float = 0.09f,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = TermsGreen.copy(alpha = shadowAlpha), spotColor = TermsGreen.copy(alpha = shadowAlpha)),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        content()
    }
}

@Composable
private fun DecorativeCornerLeaves(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(122.dp)) {
        val base = Offset(size.width * 0.72f, size.height * 0.78f)
        repeat(4) { index ->
            val offset = index * 18f
            drawOval(
                color = TermsGreen.copy(alpha = 0.08f + index * 0.025f),
                topLeft = Offset(base.x - offset - 18f, base.y - offset - 9f),
                size = androidx.compose.ui.geometry.Size(56f, 24f)
            )
        }
    }
}

@Composable
private fun TermsBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Cream.copy(alpha = 0.35f),
            radius = size.width * 0.48f,
            center = Offset(size.width * 1.02f, size.height * 0.06f)
        )
        drawCircle(
            color = SageGreen.copy(alpha = 0.06f),
            radius = size.width * 0.52f,
            center = Offset(-size.width * 0.1f, size.height * 0.58f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsentPrivacyScreenPreview() {
    ConsentPrivacyScreen(onAgreeContinue = {}, onDecline = {})
}
