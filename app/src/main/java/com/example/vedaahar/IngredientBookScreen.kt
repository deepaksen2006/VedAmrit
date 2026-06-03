package com.example.vedaahar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private val IngredientBackground = Color(0xFFFAF7EF)
private val IngredientCardSurface = Color(0xFFFFFCF5)
private val IngredientMint = Color(0xFFDDEED5)
private val IngredientFresh = Color(0xFF78A96A)
private val IngredientDeep = Color(0xFF1F3A24)
private val IngredientLine = Color(0xFFCFE0C8)
private val IngredientGold = Color(0xFFE5C66D)

private data class IngredientBookItem(
    val classic: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val accent: Color,
    val uses: List<String>
)

private val ingredientBookItems = listOf(
    IngredientBookItem(
        classic = "Golden root",
        name = "Turmeric",
        description = "A warming household staple used in face packs, soothing drinks, and daily cooking.",
        tags = listOf("Skin", "Hair", "Health"),
        accent = Color(0xFFA9CF77),
        uses = listOf("Add a pinch to warm milk", "Mix with gram flour for a simple face mask", "Use in daily cooking for gentle support")
    ),
    IngredientBookItem(
        classic = "Bitter botanical",
        name = "Neem",
        description = "Purifying leaf for scalp care, skin rituals, and traditional cleansing routines.",
        tags = listOf("Skin", "Hair"),
        accent = Color(0xFF8EC394),
        uses = listOf("Use neem water as a scalp rinse", "Apply diluted paste on oily skin", "Add to cleansing bath water")
    ),
    IngredientBookItem(
        classic = "Cooling gel",
        name = "Aloe Vera",
        description = "Cooling gel for skin calm, soft hair, and soothing daily care rituals.",
        tags = listOf("Skin", "Hair", "Health"),
        accent = Color(0xFF8FD6B3),
        uses = listOf("Apply fresh gel after sun exposure", "Use as a hair mask base", "Mix into simple hydrating face packs")
    ),
    IngredientBookItem(
        classic = "Radiance fruit",
        name = "Amla",
        description = "Vitamin-rich fruit for radiance, scalp rituals, and everyday vitality.",
        tags = listOf("Skin", "Hair", "Health"),
        accent = Color(0xFFB8D978),
        uses = listOf("Use amla oil for scalp massage", "Add powder to hair packs", "Take with guidance for daily wellness")
    ),
    IngredientBookItem(
        classic = "Sacred leaf",
        name = "Tulsi",
        description = "Aromatic household herb used in tea, steam rituals, and simple topical blends.",
        tags = listOf("Health"),
        accent = Color(0xFF80B76B),
        uses = listOf("Brew as a gentle herbal tea", "Use steam for seasonal comfort", "Add to simple home rituals")
    ),
    IngredientBookItem(
        classic = "Pantry seed",
        name = "Fenugreek",
        description = "Common kitchen seed for hair masks, warm meals, and soaked wellness routines.",
        tags = listOf("Hair", "Health"),
        accent = Color(0xFFC7D989),
        uses = listOf("Soak seeds overnight", "Use paste as hair mask base", "Add lightly to warm recipes")
    )
)

@Composable
fun IngredientBookScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    var selectedIngredient by remember { mutableStateOf(ingredientBookItems.first()) }

    Surface(modifier = modifier.fillMaxSize(), color = IngredientBackground) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(IngredientBackground, Color(0xFFF4FAF1), Cream)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BackButton(onClick = onBack, text = "Dashboard")
                IngredientBookHero(selectedIngredient = selectedIngredient)
                IngredientFilterRow()
                Text(
                    text = "INGREDIENT CHAPTERS",
                    color = SageGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp
                )
                IngredientGrid(
                    selectedIngredient = selectedIngredient,
                    onSelectIngredient = { selectedIngredient = it }
                )
                IngredientDetailPanel(ingredient = selectedIngredient)
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun IngredientBookHero(selectedIngredient: IngredientBookItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = ForestGreen.copy(alpha = 0.08f), spotColor = IngredientFresh.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = IngredientCardSurface),
        border = BorderStroke(1.dp, IngredientLine)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "AYURVEDIC INGREDIENT BOOK",
                color = SageGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.3.sp
            )
            Text(
                text = "A digital home apothecary for skin, hair, and health rituals",
                color = IngredientDeep,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 29.sp,
                lineHeight = 32.sp
            )
            Text(
                text = "Explore Ayurvedic kitchen staples with benefits, safe-use notes, ritual logs, and one-tap wellness reminders.",
                color = SoftBlueGray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                IngredientStat("Chapters", "13", Modifier.weight(1f))
                IngredientStat("Saved", "0", Modifier.weight(1f))
                IngredientStat("Used today", "0", Modifier.weight(1f))
            }
            FeaturedIngredientShelf(ingredient = selectedIngredient)
        }
    }
}

@Composable
private fun IngredientStat(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FBF4))
            .border(BorderStroke(1.dp, IngredientLine), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(IngredientMint),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = SoftBlueGray, fontSize = 9.sp, maxLines = 1)
            Text(value, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun FeaturedIngredientShelf(ingredient: IngredientBookItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF0F8E8), Color(0xFFFFFCF5))))
            .border(BorderStroke(1.dp, IngredientLine), RoundedCornerShape(22.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IngredientIllustration(accent = ingredient.accent, modifier = Modifier.size(96.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Book cover", color = SageGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(ingredient.name, color = IngredientDeep, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(ingredient.description, color = SoftBlueGray, fontSize = 11.sp, lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun IngredientFilterRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Filter", color = SoftBlueGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        listOf("All", "Skin", "Hair", "Health").forEachIndexed { index, label ->
            Text(
                text = label,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (index == 0) ForestGreen else Color(0xFFF8FBF4))
                    .border(BorderStroke(1.dp, IngredientLine), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                color = if (index == 0) PureWhite else ForestGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun IngredientGrid(
    selectedIngredient: IngredientBookItem,
    onSelectIngredient: (IngredientBookItem) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val twoColumns = maxWidth > 430.dp
        if (twoColumns) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ingredientBookItems.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { ingredient ->
                            IngredientChapterCard(
                                ingredient = ingredient,
                                selected = selectedIngredient.name == ingredient.name,
                                onClick = { onSelectIngredient(ingredient) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ingredientBookItems.forEach { ingredient ->
                    IngredientChapterCard(
                        ingredient = ingredient,
                        selected = selectedIngredient.name == ingredient.name,
                        onClick = { onSelectIngredient(ingredient) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientChapterCard(
    ingredient: IngredientBookItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFF4FBF0) else IngredientCardSurface),
        border = BorderStroke(1.dp, if (selected) ingredient.accent else IngredientLine)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            IngredientIllustration(accent = ingredient.accent, modifier = Modifier.fillMaxWidth().height(96.dp))
            Text(ingredient.classic.uppercase(), color = SageGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(ingredient.name, color = IngredientDeep, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp)
            Text(ingredient.description, color = SoftBlueGray, fontSize = 11.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ingredient.tags.take(3).forEach { tag ->
                    IngredientTag(tag)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text("Read chapter", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, IngredientLine),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Use today", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun IngredientTag(tag: String) {
    Text(
        text = tag,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF8FBF4))
            .border(BorderStroke(1.dp, IngredientLine), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        color = ForestGreen,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun IngredientDetailPanel(ingredient: IngredientBookItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = IngredientCardSurface),
        border = BorderStroke(1.dp, IngredientLine)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("INGREDIENT CHAPTER", color = SageGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                    Text(ingredient.name, color = IngredientDeep, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ingredient.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                }
            }
            Text(ingredient.description, color = SoftBlueGray, fontSize = 12.sp, lineHeight = 18.sp)
            ingredient.uses.forEachIndexed { index, use ->
                ChapterNote(index = index + 1, text = use, accent = ingredient.accent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)
                ) {
                    Text("Save ingredient", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, IngredientLine),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
                ) {
                    Text("Set reminder", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun ChapterNote(index: Int, text: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FBF4))
            .border(BorderStroke(1.dp, IngredientLine), RoundedCornerShape(14.dp))
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(index.toString().padStart(2, '0'), color = ForestGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = DarkForestGreen, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun IngredientIllustration(accent: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF2FAE9), Color(0xFFFFF8E8))))
            .border(BorderStroke(1.dp, IngredientLine), RoundedCornerShape(18.dp))
    ) {
        drawCircle(accent.copy(alpha = 0.18f), radius = size.minDimension * 0.34f, center = Offset(size.width * 0.34f, size.height * 0.48f))
        drawCircle(accent.copy(alpha = 0.34f), radius = size.minDimension * 0.25f, center = Offset(size.width * 0.72f, size.height * 0.42f))
        drawCircle(IngredientGold.copy(alpha = 0.18f), radius = size.minDimension * 0.24f, center = Offset(size.width * 0.22f, size.height * 0.25f))
        drawLine(
            color = ForestGreen.copy(alpha = 0.32f),
            start = Offset(size.width * 0.72f, size.height * 0.64f),
            end = Offset(size.width * 0.82f, size.height * 0.24f),
            strokeWidth = 4f
        )
        drawLine(
            color = PureWhite.copy(alpha = 0.58f),
            start = Offset(size.width * 0.34f, size.height * 0.32f),
            end = Offset(size.width * 0.38f, size.height * 0.7f),
            strokeWidth = 3f
        )
    }
}
