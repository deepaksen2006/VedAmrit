package com.example.vedaahar.document.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.R
import com.example.vedaahar.document.MedicalDocument
import com.example.vedaahar.ui.theme.PureWhite

private val BrandGold = Color(0xFFF4CD75)
private val DarkHerbalDeep = Color(0xFF123722)
private val ForestDarkGreen = Color(0xFF1B452B)

@Composable
fun UploadMedicalDocumentSection(
    modifier: Modifier = Modifier,
    onUploadClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {},
    onDocumentClick: (MedicalDocument) -> Unit = {}
) {
    // Dedicated Hero "Upload Medical Document" Card (Card No. 1)
    // The Recent Documents section has been removed so following sections flow seamlessly upward
    UploadDocumentHeroBanner(
        modifier = modifier,
        onUploadClick = onUploadClick
    )
}

// -------------------------------------------------------------------------------------------------
// HERO BANNER
// -------------------------------------------------------------------------------------------------
@Composable
private fun UploadDocumentHeroBanner(
    modifier: Modifier = Modifier,
    onUploadClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        label = "heroScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0xFF0D2517).copy(alpha = 0.25f),
                spotColor = Color(0xFF0A1F13).copy(alpha = 0.20f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        DarkHerbalDeep,
                        ForestDarkGreen,
                        Color(0xFF163E27)
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onUploadClick
            )
            .padding(18.dp)
    ) {
        // Subtle background decorative circles
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 35.dp, y = (-35).dp)
                .clip(CircleShape)
                .background(Color(0xFF265638).copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 25.dp, y = 30.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E482D).copy(alpha = 0.4f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Tag, Heading, Supporting Text, CTA Button
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .padding(end = 6.dp)
            ) {
                // "No. 1" Pill Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0F2C1B).copy(alpha = 0.8f))
                        .border(BorderStroke(1.dp, Color(0xFFD4AF57).copy(alpha = 0.65f)), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "No. 1",
                        color = Color(0xFFE8C97B),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Heading
                Text(
                    text = "Upload\nMedical Document",
                    color = PureWhite,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Supporting text
                Text(
                    text = "Securely upload and manage your medical reports, prescriptions, and health records in one place.",
                    color = Color(0xFFD3E4D6),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Primary CTA Button
                Button(
                    onClick = onUploadClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGold,
                        contentColor = DarkHerbalDeep
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = "Upload Document",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Right Column: Illustration Graphic
            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .height(140.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_upload_medical_doc),
                    contentDescription = "Medical Document Illustration",
                    modifier = Modifier
                        .size(132.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}
