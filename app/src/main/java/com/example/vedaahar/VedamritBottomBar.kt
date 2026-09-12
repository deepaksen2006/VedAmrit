package com.example.vedaahar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.VedamritDarkGreen
import com.example.vedaahar.ui.theme.VedamritNavInactive

// =========================================================================
// 5 Tab Navigation Item Data Model
// =========================================================================

data class NavItemData(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

// =========================================================================
// Vector Icons (Active Filled + Inactive Outline)
// =========================================================================

val OutlineHomeIcon: ImageVector = ImageVector.Builder(
    name = "OutlineHome",
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
        moveTo(10f, 20f)
        lineTo(10f, 14f)
        lineTo(14f, 14f)
        lineTo(14f, 20f)
        lineTo(19f, 20f)
        lineTo(19f, 10.5f)
        lineTo(12f, 4f)
        lineTo(5f, 10.5f)
        lineTo(5f, 20f)
        close()
    }
}.build()

val OutlineHeartIcon: ImageVector = ImageVector.Builder(
    name = "OutlineHeart",
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
        moveTo(12f, 21.35f)
        lineTo(10.55f, 20.03f)
        curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
        curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
        curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
        curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
        curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
        curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
        close()
    }
}.build()

val FilledHeartIcon: ImageVector = ImageVector.Builder(
    name = "FilledHeart",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 21.35f)
        lineTo(10.55f, 20.03f)
        curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
        curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
        curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
        curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
        curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
        curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
        close()
    }
}.build()

val OutlineConsultIcon: ImageVector = ImageVector.Builder(
    name = "OutlineConsult",
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
        moveTo(20f, 2f)
        lineTo(4f, 2f)
        curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
        lineTo(2f, 18f)
        lineTo(6f, 14f)
        lineTo(20f, 14f)
        curveTo(21.1f, 14f, 22f, 13.1f, 22f, 12f)
        lineTo(22f, 4f)
        curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
        close()
        moveTo(7f, 8f)
        lineTo(7.01f, 8f)
        moveTo(12f, 8f)
        lineTo(12.01f, 8f)
        moveTo(17f, 8f)
        lineTo(17.01f, 8f)
    }
}.build()

val FilledConsultIcon: ImageVector = ImageVector.Builder(
    name = "FilledConsult",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.EvenOdd
    ) {
        moveTo(20f, 2f)
        lineTo(4f, 2f)
        curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
        lineTo(2f, 18f)
        lineTo(6f, 14f)
        lineTo(20f, 14f)
        curveTo(21.1f, 14f, 22f, 13.1f, 22f, 12f)
        lineTo(22f, 4f)
        curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
        close()
        moveTo(7f, 7.5f)
        curveTo(6.45f, 7.5f, 6f, 7.95f, 6f, 8.5f)
        curveTo(6f, 9.05f, 6.45f, 9.5f, 7f, 9.5f)
        curveTo(7.55f, 9.5f, 8f, 9.05f, 8f, 8.5f)
        curveTo(8f, 7.95f, 7.55f, 7.5f, 7f, 7.5f)
        close()
        moveTo(12f, 7.5f)
        curveTo(11.45f, 7.5f, 11f, 7.95f, 11f, 8.5f)
        curveTo(11f, 9.05f, 11.45f, 9.5f, 12f, 9.5f)
        curveTo(12.55f, 9.5f, 13f, 9.05f, 13f, 8.5f)
        curveTo(13f, 7.95f, 12.55f, 7.5f, 12f, 7.5f)
        close()
        moveTo(17f, 7.5f)
        curveTo(16.45f, 7.5f, 16f, 7.95f, 16f, 8.5f)
        curveTo(16f, 9.05f, 16.45f, 9.5f, 17f, 9.5f)
        curveTo(17.55f, 9.5f, 18f, 9.05f, 18f, 8.5f)
        curveTo(18f, 7.95f, 17.55f, 7.5f, 17f, 7.5f)
        close()
    }
}.build()

val OutlineShoppingIcon: ImageVector = ImageVector.Builder(
    name = "OutlineShopping",
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
        moveTo(1f, 1f)
        lineTo(4.27f, 1f)
        lineTo(6.68f, 13.39f)
        curveTo(6.8f, 14.02f, 7.35f, 14.49f, 8f, 14.49f)
        lineTo(19f, 14.49f)
        curveTo(19.65f, 14.49f, 20.2f, 14.02f, 20.32f, 13.39f)
        lineTo(22f, 4f)
        lineTo(5f, 4f)
        moveTo(8.5f, 20.5f)
        curveTo(9.33f, 20.5f, 10f, 19.83f, 10f, 19f)
        curveTo(10f, 18.17f, 9.33f, 17.5f, 8.5f, 17.5f)
        curveTo(7.67f, 17.5f, 7f, 18.17f, 7f, 19f)
        curveTo(7f, 19.83f, 7.67f, 20.5f, 8.5f, 20.5f)
        close()
        moveTo(17.5f, 20.5f)
        curveTo(18.33f, 20.5f, 19f, 19.83f, 19f, 19f)
        curveTo(19f, 18.17f, 18.33f, 17.5f, 17.5f, 17.5f)
        curveTo(16.67f, 17.5f, 16f, 18.17f, 16f, 19f)
        curveTo(16f, 19.83f, 16.67f, 20.5f, 17.5f, 20.5f)
        close()
    }
}.build()

val FilledShoppingIcon: ImageVector = ImageVector.Builder(
    name = "FilledShopping",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(7f, 18f)
        curveTo(5.9f, 18f, 5.01f, 18.9f, 5.01f, 20f)
        curveTo(5.01f, 21.1f, 5.9f, 22f, 7f, 22f)
        curveTo(8.1f, 22f, 9f, 21.1f, 9f, 20f)
        curveTo(9f, 18.9f, 8.1f, 18f, 7f, 18f)
        close()
        moveTo(1f, 2f)
        lineTo(1f, 4f)
        lineTo(3f, 4f)
        lineTo(6.6f, 11.59f)
        lineTo(5.25f, 14.04f)
        curveTo(5.09f, 14.32f, 5f, 14.65f, 5f, 15f)
        curveTo(5f, 16.1f, 5.9f, 17f, 7f, 17f)
        lineTo(19f, 17f)
        lineTo(19f, 15f)
        lineTo(7.42f, 15f)
        curveTo(7.28f, 15f, 7.17f, 14.89f, 7.17f, 14.75f)
        lineTo(7.2f, 14.63f)
        lineTo(8.1f, 13f)
        lineTo(15.55f, 13f)
        curveTo(16.3f, 13f, 16.96f, 12.59f, 17.3f, 11.97f)
        lineTo(20.88f, 5.48f)
        curveTo(21.25f, 4.82f, 20.77f, 4f, 20.01f, 4f)
        lineTo(5.21f, 4f)
        lineTo(4.27f, 2f)
        lineTo(1f, 2f)
        close()
        moveTo(17f, 18f)
        curveTo(15.9f, 18f, 15.01f, 18.9f, 15.01f, 20f)
        curveTo(15.01f, 21.1f, 15.9f, 22f, 17f, 22f)
        curveTo(18.1f, 22f, 19f, 21.1f, 19f, 20f)
        curveTo(19f, 18.9f, 18.1f, 18f, 17f, 18f)
        close()
    }
}.build()

val OutlineProfileIcon: ImageVector = ImageVector.Builder(
    name = "OutlineProfile",
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
        moveTo(12f, 12f)
        curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
        curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
        curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
        curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
        close()
        moveTo(4f, 20f)
        curveTo(4f, 16.69f, 7.58f, 14f, 12f, 14f)
        curveTo(16.42f, 14f, 20f, 16.69f, 20f, 20f)
    }
}.build()

val FilledProfileIcon: ImageVector = ImageVector.Builder(
    name = "FilledProfile",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 12f)
        curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
        curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
        curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
        curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
        close()
        moveTo(12f, 14f)
        curveTo(7.58f, 14f, 4f, 16.69f, 4f, 20f)
        lineTo(4f, 22f)
        lineTo(20f, 22f)
        lineTo(20f, 20f)
        curveTo(20f, 16.69f, 16.42f, 14f, 12f, 14f)
        close()
    }
}.build()

// =========================================================================
// Fixed / Sticky Bottom Navigation Bar Component
// =========================================================================

@Composable
fun VedamritBottomNavigationBar(
    selectedLabel: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            NavItemData("Home", Icons.Filled.Home, OutlineHomeIcon),
            NavItemData("Wellness", FilledHeartIcon, OutlineHeartIcon),
            NavItemData("Consult", FilledConsultIcon, OutlineConsultIcon),
            NavItemData("Shopping", FilledShoppingIcon, OutlineShoppingIcon),
            NavItemData("Profile", FilledProfileIcon, OutlineProfileIcon)
        )
    }

    val selectedIndex = remember(selectedLabel) {
        items.indexOfFirst { it.label == selectedLabel }.coerceAtLeast(0)
    }

    // Clean white rounded-top container anchored at viewport bottom
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = PureWhite,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Color(0xFFE8EDE8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Insets safe-area: pure white extends behind 3-button or gesture bar
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                val totalWidth = maxWidth
                val tabCount = items.size
                val tabWidth = totalWidth / tabCount
                val indicatorSize = 44.dp
                val targetIndicatorOffset = (tabWidth * selectedIndex) + (tabWidth - indicatorSize) / 2

                // Smooth horizontal spring slide transition
                val animatedIndicatorOffset by animateDpAsState(
                    targetValue = targetIndicatorOffset,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 380f
                    ),
                    label = "navIndicatorSpring"
                )

                // Single active dark-green circular indicator sliding horizontally
                Box(
                    modifier = Modifier
                        .offset(x = animatedIndicatorOffset, y = 3.dp)
                        .size(indicatorSize)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = VedamritDarkGreen.copy(alpha = 0.20f),
                            spotColor = VedamritDarkGreen.copy(alpha = 0.30f)
                        )
                        .clip(CircleShape)
                        .background(VedamritDarkGreen)
                )

                // 5 navigation items evenly distributed across the entire width
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        VedamritNavItem(
                            item = item,
                            isSelected = selectedIndex == index,
                            onClick = { onTabSelected(item.label) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// Interactive Navigation Item (Fully clickable across its entire area)
// =========================================================================

@Composable
private fun VedamritNavItem(
    item: NavItemData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessMediumLow),
        label = "navPressScale"
    )

    // Smooth active & inactive icon alpha transitions
    val activeAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "navActiveAlpha"
    )
    val activeScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.78f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "navActiveScale"
    )

    val inactiveAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "navInactiveAlpha"
    )
    val inactiveScale by animateFloatAsState(
        targetValue = if (isSelected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "navInactiveScale"
    )

    // Active label becomes bold/dark green, inactive remains regular weight
    val textColor by animateColorAsState(
        targetValue = if (isSelected) VedamritDarkGreen else VedamritNavInactive,
        animationSpec = tween(durationMillis = 240),
        label = "navTextColor"
    )

    // Full tab area is clickable
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.scale(pressScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon container box (fixed 44.dp to match the sliding background circle perfectly)
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                // Inactive icon: dark gray/green outline
                if (inactiveAlpha > 0.005f) {
                    Icon(
                        imageVector = item.inactiveIcon,
                        contentDescription = null,
                        tint = VedamritNavInactive.copy(alpha = inactiveAlpha),
                        modifier = Modifier
                            .size(24.dp)
                            .scale(inactiveScale)
                    )
                }

                // Active icon: solid white inside the sliding green circle
                if (activeAlpha > 0.005f) {
                    Icon(
                        imageVector = item.activeIcon,
                        contentDescription = item.label,
                        tint = PureWhite.copy(alpha = activeAlpha),
                        modifier = Modifier
                            .size(24.dp)
                            .scale(activeScale)
                    )
                }
            }

            // Active label is bold/dark green, inactive is regular weight
            Text(
                text = item.label,
                modifier = Modifier.padding(top = 2.dp),
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
