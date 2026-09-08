package com.example.vedaahar.document.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SoftOliveGreen
import com.example.vedaahar.ui.theme.VedAmritCtaGreen
import com.example.vedaahar.ui.theme.VedAmritGreen
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MedicalDocumentScannerModal(
    onDismiss: () -> Unit,
    onDocumentScanned: (fileName: String, pageCount: Int, bytes: ByteArray) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val capturedPages = remember { mutableStateListOf<Bitmap>() }
        var isFlashOn by remember { mutableStateOf(false) }
        var isReviewingCurrentPage by remember { mutableStateOf(false) }
        var activePageIndex by remember { mutableStateOf(0) }

        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) {
                capturedPages.add(bitmap)
                activePageIndex = capturedPages.lastIndex
                isReviewingCurrentPage = true
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "scanBeam")
        val beamPosition by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scanBeamPosition"
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0C1710)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close scanner",
                            tint = PureWhite
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Scan Medical Document",
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Place your document inside the frame.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }

                    // Flash control
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFlashOn) Color(0xFFE8C97B).copy(alpha = 0.35f)
                                else Color.White.copy(alpha = 0.12f)
                            )
                    ) {
                        Text(
                            text = if (isFlashOn) "⚡" else "💡",
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Scanning Viewfinder / Scanned Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val currentBitmap = if (isReviewingCurrentPage && capturedPages.isNotEmpty()) {
                        capturedPages.getOrNull(activePageIndex)
                    } else null

                    if (currentBitmap != null) {
                        // Display review of current page
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(0.72f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(BorderStroke(2.dp, Color(0xFF90C987)), RoundedCornerShape(16.dp))
                                .background(Color(0xFF14241B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = "Scanned Page",
                                modifier = Modifier.fillMaxSize()
                            )

                            // Page Counter Pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Page ${activePageIndex + 1} of ${capturedPages.size}",
                                    color = PureWhite,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // Interactive Viewfinder Frame with corner brackets
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(0.72f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF132218).copy(alpha = 0.75f))
                        ) {
                            // Scanner Corner Indicators & animated scanning beam
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val bracketLen = 36.dp.toPx()
                                val strokeW = 4.dp.toPx()
                                val cornerColor = Color(0xFF90C987)

                                // Top-Left
                                drawLine(cornerColor, Offset(0f, 0f), Offset(bracketLen, 0f), strokeW)
                                drawLine(cornerColor, Offset(0f, 0f), Offset(0f, bracketLen), strokeW)

                                // Top-Right
                                drawLine(cornerColor, Offset(w, 0f), Offset(w - bracketLen, 0f), strokeW)
                                drawLine(cornerColor, Offset(w, 0f), Offset(w, bracketLen), strokeW)

                                // Bottom-Left
                                drawLine(cornerColor, Offset(0f, h), Offset(bracketLen, h), strokeW)
                                drawLine(cornerColor, Offset(0f, h), Offset(0f, h - bracketLen), strokeW)

                                // Bottom-Right
                                drawLine(cornerColor, Offset(w, h), Offset(w - bracketLen, h), strokeW)
                                drawLine(cornerColor, Offset(w, h), Offset(w, h - bracketLen), strokeW)

                                // Animated Laser Beam
                                val beamY = h * beamPosition
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color(0xFF90C987), Color.Transparent)
                                    ),
                                    start = Offset(0f, beamY),
                                    end = Offset(w, beamY),
                                    strokeWidth = 2.5.dp.toPx()
                                )
                            }

                            // Document Detection Guidance
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 18.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF90C987))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Document detected • Hold steady",
                                        color = PureWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Multi-page Thumbnails bar (if 2+ pages)
                if (capturedPages.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(capturedPages) { idx, bmp ->
                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 62.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        BorderStroke(
                                            if (idx == activePageIndex) 2.dp else 1.dp,
                                            if (idx == activePageIndex) Color(0xFF90C987) else Color.White.copy(alpha = 0.3f)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        activePageIndex = idx
                                        isReviewingCurrentPage = true
                                    }
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page ${idx + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Page count indicator
                if (capturedPages.isNotEmpty()) {
                    Text(
                        text = "${capturedPages.size} ${if (capturedPages.size == 1) "page" else "pages"} scanned",
                        color = Color(0xFFE8C97B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Bottom Action Controls
                if (isReviewingCurrentPage) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (capturedPages.isNotEmpty()) {
                                        capturedPages.removeAt(activePageIndex)
                                        if (capturedPages.isEmpty()) {
                                            isReviewingCurrentPage = false
                                        } else {
                                            activePageIndex = capturedPages.lastIndex
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retake", fontSize = 14.sp)
                            }

                            Button(
                                onClick = {
                                    isReviewingCurrentPage = false
                                    cameraLauncher.launch(null)
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E422C),
                                    contentColor = PureWhite
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add Another Page", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val timeStamp = SimpleDateFormat("ddMMM_HHmm", Locale.getDefault()).format(Date())
                                val fileName = "Scanned_Document_$timeStamp.jpg"
                                val outputStream = ByteArrayOutputStream()
                                val primaryBitmap = capturedPages.firstOrNull() ?: createSampleScannedBitmap()
                                primaryBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                                onDocumentScanned(fileName, capturedPages.size, outputStream.toByteArray())
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF90C987),
                                contentColor = Color(0xFF0F3826)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Use This Document",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Live Viewfinder Capture Actions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(4.dp, PureWhite), CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF90C987))
                                .clickable {
                                    cameraLauncher.launch(null)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F3826))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Tap circle to capture",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun createSampleScannedBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(600, 840, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 32f
        isAntiAlias = true
    }
    canvas.drawText("VedAmrit Medical Record", 60f, 100f, paint)
    paint.textSize = 22f
    canvas.drawText("Prescription & Assessment", 60f, 160f, paint)
    return bmp
}
