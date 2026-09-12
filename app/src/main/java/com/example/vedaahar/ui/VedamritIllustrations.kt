package com.example.vedaahar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.PureWhite

/**
 * Vedamrit Mortar & Pestle Logo:
 * Forest green ceramic bowl, slanted wooden pestle, and fresh green medicinal leaf.
 */
@Composable
fun VedamritMortarLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp, 36.dp)) {
        val w = size.width
        val h = size.height

        // Mortar rim
        val rimTop = h * 0.38f
        val rimH = h * 0.12f
        val rimW = w * 0.72f
        val rimX = (w - rimW) / 2f

        // Slanted Wooden Pestle (leaning right)
        val pestlePath = Path().apply {
            moveTo(w * 0.44f, h * 0.08f)
            lineTo(w * 0.58f, h * 0.02f)
            lineTo(w * 0.48f, h * 0.44f)
            lineTo(w * 0.36f, h * 0.44f)
            close()
        }
        drawPath(pestlePath, color = Color(0xFFA26B38))

        // Pestle rounded top tip
        drawCircle(
            color = Color(0xFFBD8249),
            radius = w * 0.065f,
            center = Offset(w * 0.51f, h * 0.05f)
        )

        // Mortar bowl body
        val bowlPath = Path().apply {
            moveTo(rimX, rimTop + rimH / 2f)
            cubicTo(
                rimX + w * 0.04f, h * 0.85f,
                rimX + rimW - w * 0.04f, h * 0.85f,
                rimX + rimW, rimTop + rimH / 2f
            )
            close()
        }
        drawPath(bowlPath, color = Color(0xFF134828))

        // Mortar base foot
        drawRoundRect(
            color = Color(0xFF0F3A20),
            topLeft = Offset(w * 0.30f, h * 0.86f),
            size = Size(w * 0.40f, h * 0.08f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Mortar top lip/rim
        drawOval(
            color = Color(0xFF1D5C35),
            topLeft = Offset(rimX, rimTop),
            size = Size(rimW, rimH)
        )
        // Mortar inner depth
        drawOval(
            color = Color(0xFF0E331B),
            topLeft = Offset(rimX + w * 0.04f, rimTop + h * 0.02f),
            size = Size(rimW - w * 0.08f, rimH * 0.7f)
        )

        // Herbal green leaf sprouting out of mortar
        val leafPath = Path().apply {
            moveTo(w * 0.34f, rimTop + h * 0.04f)
            cubicTo(w * 0.22f, h * 0.20f, w * 0.24f, h * 0.08f, w * 0.36f, h * 0.12f)
            cubicTo(w * 0.40f, h * 0.22f, w * 0.36f, rimTop, w * 0.34f, rimTop + h * 0.04f)
            close()
        }
        drawPath(leafPath, color = Color(0xFF4CAF50))
    }
}

/**
 * 1. Upload Your Document Illustration:
 * Clean white document page with folded corner, text lines, and a dark green circle with white upward arrow.
 */
@Composable
fun UploadDocIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(68.dp, 68.dp)) {
        val w = size.width
        val h = size.height

        val docW = w * 0.62f
        val docH = h * 0.78f
        val docX = w * 0.18f
        val docY = h * 0.06f
        val foldSize = docW * 0.32f

        // Document shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.06f),
            topLeft = Offset(docX + 2.dp.toPx(), docY + 3.dp.toPx()),
            size = Size(docW, docH),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        // Main Document body with cut top-right corner
        val sheetPath = Path().apply {
            moveTo(docX + 6.dp.toPx(), docY)
            lineTo(docX + docW - foldSize, docY)
            lineTo(docX + docW, docY + foldSize)
            lineTo(docX + docW, docY + docH - 6.dp.toPx())
            arcTo(
                rect = Rect(docX + docW - 12.dp.toPx(), docY + docH - 12.dp.toPx(), docX + docW, docY + docH),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(docX + 6.dp.toPx(), docY + docH)
            arcTo(
                rect = Rect(docX, docY + docH - 12.dp.toPx(), docX + 12.dp.toPx(), docY + docH),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(docX, docY + 6.dp.toPx())
            arcTo(
                rect = Rect(docX, docY, docX + 12.dp.toPx(), docY + 12.dp.toPx()),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(sheetPath, color = PureWhite)
        drawPath(
            sheetPath,
            color = Color(0xFFC7DEC9),
            style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round)
        )

        // Folded dog-ear corner
        val foldPath = Path().apply {
            moveTo(docX + docW - foldSize, docY)
            lineTo(docX + docW - foldSize, docY + foldSize)
            lineTo(docX + docW, docY + foldSize)
            close()
        }
        drawPath(foldPath, color = Color(0xFFE2EFE4))
        drawPath(
            foldPath,
            color = Color(0xFFB5D4BA),
            style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round)
        )

        // Document text lines
        val lineX = docX + docW * 0.16f
        val lineMaxW = docW * 0.68f
        val lineSpacing = docH * 0.13f
        val startLineY = docY + docH * 0.32f

        repeat(3) { i ->
            val curW = if (i == 2) lineMaxW * 0.55f else lineMaxW
            drawRoundRect(
                color = Color(0xFF88A892),
                topLeft = Offset(lineX, startLineY + i * lineSpacing),
                size = Size(curW, 2.8.dp.toPx()),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }

        // Circular Green Upload Badge
        val badgeRadius = w * 0.20f
        val badgeCenter = Offset(docX + docW * 0.88f, docY + docH * 0.76f)

        // Badge shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.12f),
            radius = badgeRadius,
            center = badgeCenter + Offset(0f, 2.dp.toPx())
        )
        // Badge background
        drawCircle(
            color = Color(0xFF124B28),
            radius = badgeRadius,
            center = badgeCenter
        )

        // White Upward Arrow inside badge
        val arrowH = badgeRadius * 1.05f
        val arrowW = badgeRadius * 0.85f
        val arrowTopY = badgeCenter.y - arrowH * 0.5f
        val arrowBottomY = badgeCenter.y + arrowH * 0.5f

        // Arrow vertical stem
        drawLine(
            color = PureWhite,
            start = Offset(badgeCenter.x, arrowBottomY),
            end = Offset(badgeCenter.x, arrowTopY),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Arrow head chevron
        val arrowHeadPath = Path().apply {
            moveTo(badgeCenter.x - arrowW * 0.45f, arrowTopY + arrowH * 0.42f)
            lineTo(badgeCenter.x, arrowTopY)
            lineTo(badgeCenter.x + arrowW * 0.45f, arrowTopY + arrowH * 0.42f)
        }
        drawPath(
            arrowHeadPath,
            color = PureWhite,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * 2. Dosha Test Illustration:
 * Prominent Ayurveda Tri-Dosha Graphic:
 * VATA (sky blue with swirl), PITTA (orange with flame), KAPHA (green with leaf)
 * Surrounding a meditating human silhouette with 3 chakra points.
 */
@Composable
fun DoshaTestIllustration(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.size(100.dp, 84.dp)) {
        val w = size.width
        val h = size.height

        val centerX = w * 0.50f
        val centerY = h * 0.56f

        // Connecting circular orbit ring
        drawCircle(
            color = Color(0xFFE8C8AE).copy(alpha = 0.45f),
            radius = w * 0.34f,
            center = Offset(centerX, centerY - h * 0.04f),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Meditating Human Silhouette (Lotus Posture)
        val figureColor = Color(0xFF2C221D).copy(alpha = 0.82f)
        val headY = centerY - h * 0.22f

        // Head
        drawCircle(
            color = figureColor,
            radius = w * 0.06f,
            center = Offset(centerX, headY)
        )
        // Torso / shoulders
        val bodyPath = Path().apply {
            moveTo(centerX, headY + w * 0.06f)
            lineTo(centerX - w * 0.12f, centerY + h * 0.06f)
            // Crossed legs
            lineTo(centerX - w * 0.19f, centerY + h * 0.24f)
            lineTo(centerX + w * 0.19f, centerY + h * 0.24f)
            lineTo(centerX + w * 0.12f, centerY + h * 0.06f)
            close()
        }
        drawPath(bodyPath, color = figureColor, style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round))

        // Three Chakra Points on spine
        // Crown/Throat (Light Blue)
        drawCircle(color = Color(0xFF4FC3F7), radius = 2.4.dp.toPx(), center = Offset(centerX, centerY - h * 0.06f))
        // Heart (Green)
        drawCircle(color = Color(0xFF66BB6A), radius = 2.4.dp.toPx(), center = Offset(centerX, centerY + h * 0.04f))
        // Solar / Sacral (Orange)
        drawCircle(color = Color(0xFFFF9800), radius = 2.4.dp.toPx(), center = Offset(centerX, centerY + h * 0.14f))

        // Badge 1: VATA (Top)
        val vataCenter = Offset(centerX, h * 0.15f)
        val badgeRadius = w * 0.125f

        drawCircle(color = Color(0xFFE1F0F8), radius = badgeRadius, center = vataCenter)
        drawCircle(color = Color(0xFFB3E0F2), radius = badgeRadius, center = vataCenter, style = Stroke(width = 1.dp.toPx()))

        // Wind swirl icon inside Vata
        val swirlPath = Path().apply {
            moveTo(vataCenter.x - badgeRadius * 0.5f, vataCenter.y - badgeRadius * 0.2f)
            cubicTo(
                vataCenter.x, vataCenter.y - badgeRadius * 0.5f,
                vataCenter.x + badgeRadius * 0.5f, vataCenter.y - badgeRadius * 0.1f,
                vataCenter.x + badgeRadius * 0.3f, vataCenter.y + badgeRadius * 0.2f
            )
            cubicTo(
                vataCenter.x + badgeRadius * 0.1f, vataCenter.y + badgeRadius * 0.4f,
                vataCenter.x - badgeRadius * 0.3f, vataCenter.y + badgeRadius * 0.2f,
                vataCenter.x - badgeRadius * 0.1f, vataCenter.y
            )
        }
        drawPath(swirlPath, color = Color(0xFF0288D1), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))

        val vataMeasure = textMeasurer.measure("VATA", style = TextStyle(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD)))
        drawText(vataMeasure, topLeft = Offset(vataCenter.x - vataMeasure.size.width / 2f, vataCenter.y + badgeRadius + 1.dp.toPx()))

        // Badge 2: PITTA (Left)
        val pittaCenter = Offset(w * 0.18f, h * 0.48f)
        drawCircle(color = Color(0xFFFEE7D6), radius = badgeRadius, center = pittaCenter)
        drawCircle(color = Color(0xFFF8CFB0), radius = badgeRadius, center = pittaCenter, style = Stroke(width = 1.dp.toPx()))

        // Flame icon inside Pitta
        val flamePath = Path().apply {
            moveTo(pittaCenter.x, pittaCenter.y - badgeRadius * 0.55f)
            cubicTo(
                pittaCenter.x + badgeRadius * 0.55f, pittaCenter.y - badgeRadius * 0.1f,
                pittaCenter.x + badgeRadius * 0.45f, pittaCenter.y + badgeRadius * 0.45f,
                pittaCenter.x, pittaCenter.y + badgeRadius * 0.5f
            )
            cubicTo(
                pittaCenter.x - badgeRadius * 0.45f, pittaCenter.y + badgeRadius * 0.45f,
                pittaCenter.x - badgeRadius * 0.55f, pittaCenter.y - badgeRadius * 0.1f,
                pittaCenter.x, pittaCenter.y - badgeRadius * 0.55f
            )
            close()
        }
        drawPath(flamePath, color = Color(0xFFE65100))

        val pittaMeasure = textMeasurer.measure("PITTA", style = TextStyle(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD84315)))
        drawText(pittaMeasure, topLeft = Offset(pittaCenter.x - pittaMeasure.size.width / 2f, pittaCenter.y + badgeRadius + 1.dp.toPx()))

        // Badge 3: KAPHA (Right)
        val kaphaCenter = Offset(w * 0.82f, h * 0.48f)
        drawCircle(color = Color(0xFFE4F3DB), radius = badgeRadius, center = kaphaCenter)
        drawCircle(color = Color(0xFFC7E4B7), radius = badgeRadius, center = kaphaCenter, style = Stroke(width = 1.dp.toPx()))

        // Leaf icon inside Kapha
        val kaphaLeaf = Path().apply {
            moveTo(kaphaCenter.x, kaphaCenter.y - badgeRadius * 0.55f)
            cubicTo(
                kaphaCenter.x + badgeRadius * 0.5f, kaphaCenter.y - badgeRadius * 0.2f,
                kaphaCenter.x + badgeRadius * 0.35f, kaphaCenter.y + badgeRadius * 0.45f,
                kaphaCenter.x, kaphaCenter.y + badgeRadius * 0.5f
            )
            cubicTo(
                kaphaCenter.x - badgeRadius * 0.35f, kaphaCenter.y + badgeRadius * 0.45f,
                kaphaCenter.x - badgeRadius * 0.5f, kaphaCenter.y - badgeRadius * 0.2f,
                kaphaCenter.x, kaphaCenter.y - badgeRadius * 0.55f
            )
            close()
        }
        drawPath(kaphaLeaf, color = Color(0xFF2E7D32))

        val kaphaMeasure = textMeasurer.measure("KAPHA", style = TextStyle(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)))
        drawText(kaphaMeasure, topLeft = Offset(kaphaCenter.x - kaphaMeasure.size.width / 2f, kaphaCenter.y + badgeRadius + 1.dp.toPx()))
    }
}

/**
 * 3. Personalised Diet Illustration:
 * Realistic healthy Indian/Ayurvedic meal bowl with grains, broccoli florets, spiced paneer/tofu cubes,
 * cucumbers, and cherry tomatoes in a clean white bowl.
 */
@Composable
fun HealthyMealBowlIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(118.dp, 108.dp)) {
        val w = size.width
        val h = size.height

        val bowlCenterX = w * 0.52f
        val bowlCenterY = h * 0.56f
        val bowlRadius = w * 0.44f

        // Soft ambient bowl shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.10f),
            topLeft = Offset(bowlCenterX - bowlRadius * 1.08f, bowlCenterY - bowlRadius * 0.82f + 8.dp.toPx()),
            size = Size(bowlRadius * 2.16f, bowlRadius * 2.05f)
        )

        // Outer white ceramic bowl rim
        drawCircle(
            color = Color(0xFFFBFBFA),
            radius = bowlRadius,
            center = Offset(bowlCenterX, bowlCenterY)
        )
        drawCircle(
            color = Color(0xFFD6DFD9),
            radius = bowlRadius,
            center = Offset(bowlCenterX, bowlCenterY),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Inner bowl depth gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFEDEAE1), Color(0xFFDFDACF)),
                center = Offset(bowlCenterX, bowlCenterY),
                radius = bowlRadius * 0.90f
            ),
            radius = bowlRadius * 0.90f,
            center = Offset(bowlCenterX, bowlCenterY)
        )

        // Food Section 1: Brown Rice / Quinoa Grains (Left area)
        val riceLeft = bowlCenterX - bowlRadius * 0.75f
        val riceTop = bowlCenterY - bowlRadius * 0.55f
        repeat(16) { i ->
            val gx = riceLeft + (i % 4) * (bowlRadius * 0.18f) + (i / 4) * 2f
            val gy = riceTop + (i / 4) * (bowlRadius * 0.22f)
            drawOval(
                color = if (i % 2 == 0) Color(0xFFD8B986) else Color(0xFFC79E65),
                topLeft = Offset(gx, gy),
                size = Size(7.dp.toPx(), 4.dp.toPx())
            )
        }

        // Food Section 2: Fresh Steamed Broccoli Florets (Top / Center-Right area)
        val broccoliColor1 = Color(0xFF2E7D32)
        val broccoliColor2 = Color(0xFF43A047)
        val brocCenters = listOf(
            Offset(bowlCenterX + bowlRadius * 0.05f, bowlCenterY - bowlRadius * 0.45f),
            Offset(bowlCenterX + bowlRadius * 0.26f, bowlCenterY - bowlRadius * 0.40f),
            Offset(bowlCenterX + bowlRadius * 0.15f, bowlCenterY - bowlRadius * 0.18f),
            Offset(bowlCenterX + bowlRadius * 0.38f, bowlCenterY - bowlRadius * 0.15f)
        )
        brocCenters.forEachIndexed { idx, pt ->
            drawCircle(color = if (idx % 2 == 0) broccoliColor1 else broccoliColor2, radius = bowlRadius * 0.16f, center = pt)
            // Floret textures
            drawCircle(color = Color(0xFF66BB6A), radius = bowlRadius * 0.06f, center = pt + Offset(-2f, -3f))
        }

        // Food Section 3: Sliced Fresh Cucumbers (Far Right area)
        val cucumberCenters = listOf(
            Offset(bowlCenterX + bowlRadius * 0.58f, bowlCenterY - bowlRadius * 0.05f),
            Offset(bowlCenterX + bowlRadius * 0.62f, bowlCenterY + bowlRadius * 0.22f),
            Offset(bowlCenterX + bowlRadius * 0.42f, bowlCenterY + bowlRadius * 0.38f)
        )
        cucumberCenters.forEach { pt ->
            // Cucumber skin
            drawCircle(color = Color(0xFF2E7D32), radius = bowlRadius * 0.14f, center = pt)
            // Cucumber flesh
            drawCircle(color = Color(0xFFC8E6C9), radius = bowlRadius * 0.11f, center = pt)
            // Seeds center
            drawCircle(color = Color(0xFFA5D6A7), radius = bowlRadius * 0.05f, center = pt)
        }

        // Food Section 4: Golden Spiced Paneer / Tofu Cubes (Bottom / Center-Left area)
        val paneerColor = Color(0xFFE89A3C)
        val paneerShadow = Color(0xFFC87820)
        val paneerPoints = listOf(
            Offset(bowlCenterX - bowlRadius * 0.38f, bowlCenterY + bowlRadius * 0.05f),
            Offset(bowlCenterX - bowlRadius * 0.15f, bowlCenterY + bowlRadius * 0.18f),
            Offset(bowlCenterX - bowlRadius * 0.40f, bowlCenterY + bowlRadius * 0.35f),
            Offset(bowlCenterX - bowlRadius * 0.12f, bowlCenterY + bowlRadius * 0.42f)
        )
        paneerPoints.forEach { pt ->
            drawRoundRect(
                color = paneerColor,
                topLeft = pt,
                size = Size(bowlRadius * 0.24f, bowlRadius * 0.20f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            // Grill mark / spice highlight
            drawLine(
                color = paneerShadow,
                start = pt + Offset(2f, 4f),
                end = pt + Offset(bowlRadius * 0.20f, 4f),
                strokeWidth = 1.4.dp.toPx()
            )
        }

        // Food Section 5: Bright Cherry Tomatoes (Center Nestled)
        val tomatoCenters = listOf(
            Offset(bowlCenterX - bowlRadius * 0.08f, bowlCenterY - bowlRadius * 0.22f),
            Offset(bowlCenterX + bowlRadius * 0.08f, bowlCenterY - bowlRadius * 0.08f),
            Offset(bowlCenterX + bowlRadius * 0.22f, bowlCenterY + bowlRadius * 0.10f)
        )
        tomatoCenters.forEach { pt ->
            drawCircle(color = Color(0xFFD32F2F), radius = bowlRadius * 0.11f, center = pt)
            // White glossy reflection
            drawCircle(color = PureWhite.copy(alpha = 0.65f), radius = bowlRadius * 0.035f, center = pt + Offset(-2f, -2.5f))
        }

        // Fresh coriander herb sprig on top
        drawCircle(color = Color(0xFF4CAF50), radius = 2.dp.toPx(), center = Offset(bowlCenterX, bowlCenterY - 2.dp.toPx()))
    }
}

/**
 * 4. Symptoms Analysis Illustration:
 * Healthcare clipboard with ECG heartbeat pulse line and 3D red heart.
 */
@Composable
fun SymptomsClipboardIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(68.dp, 68.dp)) {
        val w = size.width
        val h = size.height

        val boardW = w * 0.62f
        val boardH = h * 0.74f
        val boardX = w * 0.14f
        val boardY = h * 0.12f

        // Clipboard shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(boardX + 2.dp.toPx(), boardY + 3.dp.toPx()),
            size = Size(boardW, boardH),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        // Clipboard body (Blue backing)
        drawRoundRect(
            color = Color(0xFF2980B9),
            topLeft = Offset(boardX, boardY),
            size = Size(boardW, boardH),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        // Clipboard Clip at top
        val clipW = boardW * 0.44f
        val clipH = boardH * 0.14f
        drawRoundRect(
            color = Color(0xFF1F618D),
            topLeft = Offset(boardX + (boardW - clipW) / 2f, boardY - clipH * 0.35f),
            size = Size(clipW, clipH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawCircle(
            color = PureWhite,
            radius = 2.dp.toPx(),
            center = Offset(boardX + boardW / 2f, boardY + clipH * 0.15f)
        )

        // White Paper Sheet inside clipboard
        val paperMargin = boardW * 0.09f
        val paperX = boardX + paperMargin
        val paperY = boardY + boardH * 0.15f
        val paperW = boardW - paperMargin * 2f
        val paperH = boardH * 0.78f

        drawRoundRect(
            color = PureWhite,
            topLeft = Offset(paperX, paperY),
            size = Size(paperW, paperH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Blue Heartbeat ECG Waveform line across the paper
        val ecgY = paperY + paperH * 0.42f
        val ecgPath = Path().apply {
            moveTo(paperX + paperW * 0.08f, ecgY)
            lineTo(paperX + paperW * 0.28f, ecgY)
            lineTo(paperX + paperW * 0.36f, ecgY - paperH * 0.28f)
            lineTo(paperX + paperW * 0.48f, ecgY + paperH * 0.24f)
            lineTo(paperX + paperW * 0.58f, ecgY - paperH * 0.14f)
            lineTo(paperX + paperW * 0.68f, ecgY)
            lineTo(paperX + paperW * 0.92f, ecgY)
        }
        drawPath(
            ecgPath,
            color = Color(0xFF1976D2),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Subtle bottom paper text lines
        drawLine(
            color = Color(0xFFCFD8DC),
            start = Offset(paperX + paperW * 0.12f, paperY + paperH * 0.72f),
            end = Offset(paperX + paperW * 0.55f, paperY + paperH * 0.72f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 3D Red Heart overlapping bottom right of clipboard
        val heartCenterX = boardX + boardW * 0.86f
        val heartCenterY = boardY + boardH * 0.74f
        val hr = w * 0.18f

        // Heart shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = hr * 0.9f,
            center = Offset(heartCenterX + 1.dp.toPx(), heartCenterY + 2.dp.toPx())
        )

        // Heart shape
        val heartPath = Path().apply {
            moveTo(heartCenterX, heartCenterY + hr * 0.75f)
            cubicTo(
                heartCenterX - hr * 1.1f, heartCenterY + hr * 0.2f,
                heartCenterX - hr * 1.1f, heartCenterY - hr * 0.75f,
                heartCenterX - hr * 0.35f, heartCenterY - hr * 0.75f
            )
            cubicTo(
                heartCenterX - hr * 0.05f, heartCenterY - hr * 0.75f,
                heartCenterX, heartCenterY - hr * 0.35f,
                heartCenterX, heartCenterY - hr * 0.35f
            )
            cubicTo(
                heartCenterX, heartCenterY - hr * 0.35f,
                heartCenterX + hr * 0.05f, heartCenterY - hr * 0.75f,
                heartCenterX + hr * 0.35f, heartCenterY - hr * 0.75f
            )
            cubicTo(
                heartCenterX + hr * 1.1f, heartCenterY - hr * 0.75f,
                heartCenterX + hr * 1.1f, heartCenterY + hr * 0.2f,
                heartCenterX, heartCenterY + hr * 0.75f
            )
            close()
        }
        drawPath(
            heartPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F), Color(0xFFB71C1C)),
                center = Offset(heartCenterX - hr * 0.2f, heartCenterY - hr * 0.2f),
                radius = hr * 1.2f
            )
        )
        // White highlight gloss
        drawCircle(
            color = PureWhite.copy(alpha = 0.6f),
            radius = hr * 0.18f,
            center = Offset(heartCenterX - hr * 0.32f, heartCenterY - hr * 0.36f)
        )
    }
}

/**
 * 5. Ingredient Book Illustration:
 * Illustrated open book with purple/violet hardcover, white pages, text lines, and a purple ribbon bookmark.
 */
@Composable
fun IngredientBookIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(68.dp, 68.dp)) {
        val w = size.width
        val h = size.height

        val bookW = w * 0.74f
        val bookH = h * 0.62f
        val bookX = (w - bookW) / 2f
        val bookY = (h - bookH) / 2f
        val spineX = bookX + bookW / 2f

        // Hardcover book background (Purple edges)
        val coverPath = Path().apply {
            moveTo(bookX, bookY + 4.dp.toPx())
            lineTo(spineX, bookY + 8.dp.toPx())
            lineTo(bookX + bookW, bookY + 4.dp.toPx())
            lineTo(bookX + bookW, bookY + bookH + 4.dp.toPx())
            lineTo(spineX, bookY + bookH + 8.dp.toPx())
            lineTo(bookX, bookY + bookH + 4.dp.toPx())
            close()
        }
        drawPath(coverPath, color = Color(0xFF6A3EA1))

        // Open Pages Left (White Sheet)
        val leftPage = Path().apply {
            moveTo(bookX + 2.dp.toPx(), bookY + 2.dp.toPx())
            cubicTo(
                bookX + bookW * 0.15f, bookY,
                spineX - bookW * 0.10f, bookY + 4.dp.toPx(),
                spineX, bookY + 6.dp.toPx()
            )
            lineTo(spineX, bookY + bookH + 4.dp.toPx())
            cubicTo(
                spineX - bookW * 0.10f, bookY + bookH + 2.dp.toPx(),
                bookX + bookW * 0.15f, bookY + bookH,
                bookX + 2.dp.toPx(), bookY + bookH + 2.dp.toPx()
            )
            close()
        }
        drawPath(leftPage, color = PureWhite)
        drawPath(leftPage, color = Color(0xFFD6C8E8), style = Stroke(width = 1.dp.toPx()))

        // Open Pages Right (White Sheet)
        val rightPage = Path().apply {
            moveTo(spineX, bookY + 6.dp.toPx())
            cubicTo(
                spineX + bookW * 0.10f, bookY + 4.dp.toPx(),
                bookX + bookW - bookW * 0.15f, bookY,
                bookX + bookW - 2.dp.toPx(), bookY + 2.dp.toPx()
            )
            lineTo(bookX + bookW - 2.dp.toPx(), bookY + bookH + 2.dp.toPx())
            cubicTo(
                bookX + bookW - bookW * 0.15f, bookY + bookH,
                spineX + bookW * 0.10f, bookY + bookH + 2.dp.toPx(),
                spineX, bookY + bookH + 4.dp.toPx()
            )
            close()
        }
        drawPath(rightPage, color = PureWhite)
        drawPath(rightPage, color = Color(0xFFD6C8E8), style = Stroke(width = 1.dp.toPx()))

        // Text lines on Left Page
        val leftStartX = bookX + bookW * 0.10f
        val leftLineW = bookW * 0.30f
        val lineSpacing = bookH * 0.16f
        val textStartY = bookY + bookH * 0.25f

        repeat(4) { i ->
            drawLine(
                color = Color(0xFFB39DDB),
                start = Offset(leftStartX, textStartY + i * lineSpacing),
                end = Offset(leftStartX + if (i == 3) leftLineW * 0.6f else leftLineW, textStartY + i * lineSpacing),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Text lines on Right Page
        val rightStartX = spineX + bookW * 0.10f
        repeat(4) { i ->
            drawLine(
                color = Color(0xFFB39DDB),
                start = Offset(rightStartX, textStartY + i * lineSpacing),
                end = Offset(rightStartX + if (i == 3) leftLineW * 0.5f else leftLineW, textStartY + i * lineSpacing),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Purple Ribbon Bookmark draping from spine
        val bookmarkW = bookW * 0.12f
        val bookmarkH = bookH * 0.68f
        val bookmarkPath = Path().apply {
            moveTo(spineX - bookmarkW / 2f, bookY + 6.dp.toPx())
            lineTo(spineX + bookmarkW / 2f, bookY + 6.dp.toPx())
            lineTo(spineX + bookmarkW / 2f, bookY + bookmarkH)
            lineTo(spineX, bookY + bookmarkH - 4.dp.toPx())
            lineTo(spineX - bookmarkW / 2f, bookY + bookmarkH)
            close()
        }
        drawPath(bookmarkPath, color = Color(0xFF7E57C2))
    }
}

/**
 * 6. Life Style Calculator Illustration:
 * Dynamic running athletic figure in green shirt and dark shorts with speed motion lines.
 */
@Composable
fun RunnerFitnessIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(76.dp, 64.dp)) {
        val w = size.width
        val h = size.height

        val runnerX = w * 0.46f
        val runnerY = h * 0.48f

        // Horizontal speed / motion lines behind runner (on left)
        val speedColor = Color(0xFF81C784)
        drawLine(
            color = speedColor,
            start = Offset(w * 0.08f, h * 0.36f),
            end = Offset(w * 0.32f, h * 0.36f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = speedColor,
            start = Offset(w * 0.04f, h * 0.52f),
            end = Offset(w * 0.28f, h * 0.52f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFFB74D),
            start = Offset(w * 0.12f, h * 0.66f),
            end = Offset(w * 0.24f, h * 0.66f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Head (leaning forward)
        val headCenter = Offset(runnerX + w * 0.12f, h * 0.18f)
        val headRadius = w * 0.085f
        // Hair
        drawCircle(color = Color(0xFF2C221D), radius = headRadius * 1.05f, center = headCenter)
        // Face skin
        drawCircle(color = Color(0xFFF0B384), radius = headRadius * 0.85f, center = headCenter + Offset(1.5f, 1f))

        // Torso / Green Athletic Shirt
        val torsoPath = Path().apply {
            moveTo(runnerX + w * 0.08f, h * 0.26f)
            lineTo(runnerX + w * 0.15f, h * 0.36f)
            lineTo(runnerX + w * 0.04f, h * 0.56f)
            lineTo(runnerX - w * 0.06f, h * 0.52f)
            close()
        }
        drawPath(torsoPath, color = Color(0xFF2E7D32))

        // Skin neck
        drawLine(
            color = Color(0xFFF0B384),
            start = headCenter,
            end = Offset(runnerX + w * 0.06f, h * 0.28f),
            strokeWidth = 3.dp.toPx()
        )

        // Running Shorts (Charcoal)
        val shortsPath = Path().apply {
            moveTo(runnerX - w * 0.06f, h * 0.52f)
            lineTo(runnerX + w * 0.04f, h * 0.56f)
            lineTo(runnerX + w * 0.08f, h * 0.68f)
            lineTo(runnerX - w * 0.10f, h * 0.65f)
            close()
        }
        drawPath(shortsPath, color = Color(0xFF263238))

        // Arms in sprint stride
        // Forward Arm
        drawLine(
            color = Color(0xFF2E7D32),
            start = Offset(runnerX + w * 0.10f, h * 0.32f),
            end = Offset(runnerX + w * 0.22f, h * 0.40f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFF0B384),
            start = Offset(runnerX + w * 0.22f, h * 0.40f),
            end = Offset(runnerX + w * 0.28f, h * 0.32f),
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Back Arm
        drawLine(
            color = Color(0xFFF0B384),
            start = Offset(runnerX - w * 0.02f, h * 0.34f),
            end = Offset(runnerX - w * 0.14f, h * 0.42f),
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Legs in athletic stride
        // Front Right Leg (Forward knee bent)
        drawLine(
            color = Color(0xFFF0B384),
            start = Offset(runnerX + w * 0.04f, h * 0.64f),
            end = Offset(runnerX + w * 0.16f, h * 0.74f),
            strokeWidth = 3.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFF0B384),
            start = Offset(runnerX + w * 0.16f, h * 0.74f),
            end = Offset(runnerX + w * 0.12f, h * 0.90f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Front Sneaker (Orange/dark)
        drawOval(
            color = Color(0xFFE65100),
            topLeft = Offset(runnerX + w * 0.06f, h * 0.88f),
            size = Size(10.dp.toPx(), 4.5.dp.toPx())
        )

        // Back Left Leg (Extending backward)
        drawLine(
            color = Color(0xFFF0B384),
            start = Offset(runnerX - w * 0.08f, h * 0.62f),
            end = Offset(runnerX - w * 0.18f, h * 0.72f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFF0B384),
            start = Offset(runnerX - w * 0.18f, h * 0.72f),
            end = Offset(runnerX - w * 0.20f, h * 0.86f),
            strokeWidth = 2.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Back Sneaker
        drawOval(
            color = Color(0xFF263238),
            topLeft = Offset(runnerX - w * 0.26f, h * 0.84f),
            size = Size(8.dp.toPx(), 4.dp.toPx())
        )
    }
}

/**
 * Circular Action Button containing white right arrow (matching the reference CTAs).
 */
@Composable
fun CircularArrowCta(
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Int = 38,
    contentDescription: String = "Proceed"
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(4.dp, CircleShape, spotColor = background.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = PureWhite),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = contentDescription,
            tint = PureWhite,
            modifier = Modifier.size((sizeDp * 0.46f).dp)
        )
    }
}
