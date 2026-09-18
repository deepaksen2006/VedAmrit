package com.example.vedaahar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import com.example.vedaahar.i18n.AppLanguage
import com.example.vedaahar.i18n.Text
import com.example.vedaahar.i18n.nativeLanguageName
import com.example.vedaahar.i18n.supportedAppLanguages
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray
import com.example.vedaahar.ui.theme.VedamritBrandBrown
import com.example.vedaahar.ui.theme.VedamritBrownSoft
import com.example.vedaahar.ui.theme.VedamritGreenSoft

private data class LanguageCardVisual(
    val code: String,
    val background: Color,
    val accent: Color
)

private fun languageVisualFor(language: AppLanguage): LanguageCardVisual {
    val greenCodes = setOf("as", "brx", "hi", "kn", "mai", "mr", "or", "sa", "ta", "ur")
    return if (language.code in greenCodes) {
        LanguageCardVisual(language.code, VedamritGreenSoft, ForestGreen)
    } else {
        LanguageCardVisual(language.code, VedamritBrownSoft, VedamritBrandBrown)
    }
}

@Composable
fun LanguageSelectionScreen(
    onContinue: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf<AppLanguage?>(null) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFFFFFF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vedamrit_logo),
                    contentDescription = "VedAmrit logo",
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .sizeIn(maxWidth = 172.dp)
                        .aspectRatio(1024f / 956f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Select Your Language",
                color = DarkForestGreen,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose your preferred language to continue",
                color = SoftBlueGray,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            supportedAppLanguages.chunked(2).forEach { rowLanguages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowLanguages.forEach { language ->
                        LanguageOptionCard(
                            language = language,
                            selected = selectedLanguage?.code == language.code,
                            onClick = { selectedLanguage = language },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { selectedLanguage?.let(onContinue) },
                enabled = selectedLanguage != null,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(62.dp)
                    .shadow(12.dp, RoundedCornerShape(50), ambientColor = ForestGreen.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestGreen,
                    disabledContainerColor = SageGreen.copy(alpha = 0.45f),
                    contentColor = PureWhite,
                    disabledContentColor = PureWhite.copy(alpha = 0.68f)
                )
            ) {
                Text(
                    text = "Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = PureWhite
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.44f)
                    .height(1.dp)
                .background(VedamritBrandBrown.copy(alpha = 0.72f))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "TRADITIONAL WISDOM FOR A HEALTHIER TOMORROW",
                color = ForestGreen,
                fontSize = 11.sp,
                letterSpacing = 2.6.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LanguageOptionCard(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = languageVisualFor(language)
    val borderColor by animateColorAsState(
        targetValue = if (selected) DarkForestGreen else PureWhite.copy(alpha = 0.92f),
        label = "language-border"
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 7.dp else 3.dp,
        label = "language-elevation"
    )

    Card(
        modifier = modifier
            .height(104.dp)
            .shadow(elevation, RoundedCornerShape(28.dp))
            .border(BorderStroke(if (selected) 2.5.dp else 1.2.dp, borderColor), RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = visual.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                .background(visual.background)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageCardMark(language = language, visual = visual)
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = nativeLanguageName(language),
                        color = DarkForestGreen,
                        fontSize = if (language.code == "ml") 18.sp else 23.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(DarkForestGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageCardMark(
    language: AppLanguage,
    visual: LanguageCardVisual
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(PureWhite.copy(alpha = 0.72f))
            .border(1.dp, visual.accent.copy(alpha = 0.26f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = nativeLanguageName(language).take(1),
            color = visual.accent,
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

