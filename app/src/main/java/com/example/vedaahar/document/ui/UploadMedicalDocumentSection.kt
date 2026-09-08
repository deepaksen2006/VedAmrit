package com.example.vedaahar.document.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vedaahar.document.MedicalDocument
import com.example.vedaahar.document.MedicalDocumentStore
import com.example.vedaahar.ui.theme.MutedCharcoal
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SoftOliveGreen
import com.example.vedaahar.ui.theme.VedAmritCtaGreen
import com.example.vedaahar.ui.theme.VedAmritGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale

private sealed class UploadUiState {
    object Idle : UploadUiState()
    data class Ready(
        val title: String,
        val fileName: String,
        val fileType: String,
        val sizeBytes: Long,
        val formattedSize: String,
        val pageCount: Int,
        val bytes: ByteArray?
    ) : UploadUiState()
    data class Uploading(val progress: Float) : UploadUiState()
    data class Success(val document: MedicalDocument) : UploadUiState()
    data class Error(val title: String, val message: String, val canRetry: Boolean = false) : UploadUiState()
}

// Custom crisp vector icons
private val MedicalDocHeaderIcon: ImageVector = ImageVector.Builder(
    name = "MedicalDocHeaderIcon",
    defaultWidth = 28.dp,
    defaultHeight = 28.dp,
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
        moveTo(14f, 2f)
        lineTo(6f, 2f)
        curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
        lineTo(4f, 20f)
        curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
        lineTo(18f, 22f)
        curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
        lineTo(20f, 8f)
        lineTo(14f, 2f)
        close()
        moveTo(14f, 2f)
        lineTo(14f, 8f)
        lineTo(20f, 8f)
        moveTo(12f, 11f)
        lineTo(12f, 17f)
        moveTo(9f, 14f)
        lineTo(15f, 14f)
    }
}.build()

private val CameraScanIcon: ImageVector = ImageVector.Builder(
    name = "CameraScanIcon",
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
        moveTo(23f, 19f)
        curveTo(23f, 20.1f, 22.1f, 21f, 21f, 21f)
        lineTo(3f, 21f)
        curveTo(1.9f, 21f, 1f, 20.1f, 1f, 19f)
        lineTo(1f, 8f)
        curveTo(1f, 6.9f, 1.9f, 6f, 3f, 6f)
        lineTo(7f, 6f)
        lineTo(9f, 3f)
        lineTo(15f, 3f)
        lineTo(17f, 6f)
        lineTo(21f, 6f)
        curveTo(22.1f, 6f, 23f, 6.9f, 23f, 8f)
        lineTo(23f, 19f)
        close()
        moveTo(12f, 17f)
        curveTo(14.2f, 17f, 16f, 15.2f, 16f, 13f)
        curveTo(16f, 10.8f, 14.2f, 9f, 12f, 9f)
        curveTo(9.8f, 9f, 8f, 10.8f, 8f, 13f)
        curveTo(8f, 15.2f, 9.8f, 17f, 12f, 17f)
        close()
    }
}.build()

private val FolderUploadIcon: ImageVector = ImageVector.Builder(
    name = "FolderUploadIcon",
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
        moveTo(22f, 19f)
        curveTo(22f, 20.1f, 21.1f, 21f, 20f, 21f)
        lineTo(4f, 21f)
        curveTo(2.9f, 21f, 2f, 20.1f, 2f, 19f)
        lineTo(2f, 5f)
        curveTo(2f, 3.9f, 2.9f, 3f, 4f, 3f)
        lineTo(9f, 3f)
        lineTo(11f, 6f)
        lineTo(20f, 6f)
        curveTo(21.1f, 6f, 22f, 6.9f, 22f, 8f)
        lineTo(22f, 19f)
        close()
        moveTo(12f, 11f)
        lineTo(12f, 17f)
        moveTo(9f, 14f)
        lineTo(12f, 11f)
        lineTo(15f, 14f)
    }
}.build()

private val LockTrustIcon: ImageVector = ImageVector.Builder(
    name = "LockTrustIcon",
    defaultWidth = 16.dp,
    defaultHeight = 16.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(7f, 11f)
        lineTo(7f, 7f)
        curveTo(7f, 4.2f, 9.2f, 2f, 12f, 2f)
        curveTo(14.8f, 2f, 17f, 4.2f, 17f, 7f)
        lineTo(17f, 11f)
        moveTo(5f, 11f)
        lineTo(19f, 11f)
        curveTo(20.1f, 11f, 21f, 11.9f, 21f, 13f)
        lineTo(21f, 20f)
        curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
        lineTo(5f, 22f)
        curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
        lineTo(3f, 13f)
        curveTo(3f, 11.9f, 3.9f, 11f, 5f, 11f)
        close()
    }
}.build()

@Composable
fun UploadMedicalDocumentSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<UploadUiState>(UploadUiState.Idle) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showAllDocumentsDialog by remember { mutableStateOf(false) }
    var recentDocuments by remember { mutableStateOf(MedicalDocumentStore.getDocuments(context)) }

    // Native file picker launcher for PDF & Images
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handlePickedFile(context, uri) { state ->
                uiState = state
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Dedicated Upload Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0xFFE8E0D2)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = uiState) {
                    is UploadUiState.Idle -> {
                        // Top Heading & Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF5ED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = MedicalDocHeaderIcon,
                                    contentDescription = null,
                                    tint = VedAmritGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Upload Medical Document",
                                    color = VedAmritGreen,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Keep your medical records organized in one secure place.",
                                    color = MutedCharcoal,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Middle: Two Primary Action Cards
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isWide = maxWidth >= 500.dp
                            if (isWide) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    UploadOptionCard(
                                        title = "Scan with Camera",
                                        subtitle = "Take a photo of your document",
                                        icon = CameraScanIcon,
                                        modifier = Modifier.weight(1f),
                                        onClick = { showCameraScanner = true }
                                    )
                                    UploadOptionCard(
                                        title = "Upload from Phone",
                                        subtitle = "Choose a PDF or image",
                                        icon = FolderUploadIcon,
                                        modifier = Modifier.weight(1f),
                                        onClick = { filePickerLauncher.launch("*/*") }
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UploadOptionCard(
                                        title = "Scan with Camera",
                                        subtitle = "Take a photo of your document",
                                        icon = CameraScanIcon,
                                        onClick = { showCameraScanner = true }
                                    )
                                    UploadOptionCard(
                                        title = "Upload from Phone",
                                        subtitle = "Choose a PDF or image",
                                        icon = FolderUploadIcon,
                                        onClick = { filePickerLauncher.launch("*/*") }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom: Helper text
                        Text(
                            text = "Supported: PDF, JPG, PNG  •  Maximum size: 10 MB",
                            color = MutedCharcoal.copy(alpha = 0.8f),
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Trust Messaging
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = LockTrustIcon,
                                contentDescription = null,
                                tint = SoftOliveGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Your medical documents are securely stored",
                                color = SoftOliveGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    is UploadUiState.Ready -> {
                        // Document Ready Preview State
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFE8F3E8))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Document Ready",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // File Preview Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE8E0D2)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (state.fileType == "PDF") Color(0xFFFFEBEE)
                                                else Color(0xFFE8F5E9)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.fileType,
                                            color = if (state.fileType == "PDF") Color(0xFFC62828) else Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.fileName,
                                            color = VedAmritGreen,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${state.fileType} • ${state.formattedSize}" +
                                                    if (state.pageCount > 1) " • ${state.pageCount} pages" else "",
                                            color = MutedCharcoal,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Actions: Remove and Upload Document
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { uiState = UploadUiState.Idle },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, Color(0xFFD2C7B8)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedCharcoal)
                                ) {
                                    Text("Remove", fontSize = 14.sp)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            uiState = UploadUiState.Uploading(0.15f)
                                            delay(350)
                                            uiState = UploadUiState.Uploading(0.48f)
                                            delay(400)
                                            uiState = UploadUiState.Uploading(0.85f)
                                            delay(350)
                                            uiState = UploadUiState.Uploading(1f)
                                            delay(200)

                                            // Persist into store
                                            val saved = MedicalDocumentStore.addDocument(
                                                context = context,
                                                title = state.title,
                                                fileName = state.fileName,
                                                fileType = state.fileType,
                                                sizeBytes = state.sizeBytes,
                                                pageCount = state.pageCount,
                                                sourceBytes = state.bytes
                                            )
                                            recentDocuments = MedicalDocumentStore.getDocuments(context)
                                            uiState = UploadUiState.Success(saved)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VedAmritCtaGreen,
                                        contentColor = PureWhite
                                    )
                                ) {
                                    Text("Upload Document", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is UploadUiState.Uploading -> {
                        // Non-freezing Upload Progress State
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Uploading your document...",
                                color = VedAmritGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = VedAmritCtaGreen,
                                trackColor = Color(0xFFE8E0D2)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                color = SoftOliveGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    is UploadUiState.Success -> {
                        // Upload Complete State
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Document uploaded successfully",
                                color = VedAmritGreen,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Your medical document has been added to your records.",
                                color = MutedCharcoal,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { uiState = UploadUiState.Idle },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, Color(0xFFD2C7B8)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedCharcoal)
                                ) {
                                    Text("Upload Another", fontSize = 13.5.sp)
                                }

                                Button(
                                    onClick = {
                                        showAllDocumentsDialog = true
                                        uiState = UploadUiState.Idle
                                    },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VedAmritCtaGreen,
                                        contentColor = PureWhite
                                    )
                                ) {
                                    Text("View Medical Records", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is UploadUiState.Error -> {
                        // Error State Handling
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = state.title,
                                color = Color(0xFFC62828),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = state.message,
                                color = MutedCharcoal,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (state.canRetry) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { uiState = UploadUiState.Idle },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text("Cancel", fontSize = 14.sp)
                                    }

                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(containerColor = VedAmritCtaGreen)
                                    ) {
                                        Text("Try Again", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = VedAmritCtaGreen)
                                ) {
                                    Text("Choose Another File", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Medical Documents Section
        if (recentDocuments.isNotEmpty()) {
            RecentMedicalDocumentsCard(
                documents = recentDocuments.take(3),
                onViewAllClick = { showAllDocumentsDialog = true }
            )
        } else {
            // Empty state display
            EmptyRecordsCard(
                onScanCamera = { showCameraScanner = true },
                onUploadPhone = { filePickerLauncher.launch("*/*") }
            )
        }
    }

    // Camera Document Scanner Fullscreen Modal
    if (showCameraScanner) {
        MedicalDocumentScannerModal(
            onDismiss = { showCameraScanner = false },
            onDocumentScanned = { fileName, pageCount, bytes ->
                val sizeBytes = bytes.size.toLong()
                uiState = UploadUiState.Ready(
                    title = "Scanned Prescription",
                    fileName = fileName,
                    fileType = "JPG",
                    sizeBytes = sizeBytes,
                    formattedSize = MedicalDocumentStore.formatFileSize(sizeBytes),
                    pageCount = pageCount,
                    bytes = bytes
                )
            }
        )
    }

    // View All Records Dialog
    if (showAllDocumentsDialog) {
        AllMedicalDocumentsModal(
            documents = recentDocuments,
            onDismiss = { showAllDocumentsDialog = false },
            onDelete = { docId ->
                MedicalDocumentStore.deleteDocument(context, docId)
                recentDocuments = MedicalDocumentStore.getDocuments(context)
            }
        )
    }
}

@Composable
private fun UploadOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        label = "uploadOptionScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE8E0D2)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF5ED)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VedAmritGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = VedAmritGreen,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MutedCharcoal,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun RecentMedicalDocumentsCard(
    documents: List<MedicalDocument>,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE8E0D2)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Medical Documents",
                    color = VedAmritGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "View All Documents →",
                    color = SoftOliveGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onViewAllClick)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                documents.forEach { doc ->
                    RecentDocumentItemRow(doc = doc)
                }
            }
        }
    }
}

@Composable
private fun RecentDocumentItemRow(doc: MedicalDocument) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFEFE8DA)), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (doc.fileType == "PDF") Color(0xFFFFEBEE)
                    else Color(0xFFE8F5E9)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = doc.fileType,
                color = if (doc.fileType == "PDF") Color(0xFFC62828) else Color(0xFF2E7D32),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.title,
                color = VedAmritGreen,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${doc.formattedDate} • ${doc.fileType}",
                color = MutedCharcoal,
                fontSize = 11.5.sp
            )
        }

        Text(
            text = doc.formattedSize,
            color = MutedCharcoal.copy(alpha = 0.85f),
            fontSize = 11.5.sp
        )
    }
}

@Composable
private fun EmptyRecordsCard(
    onScanCamera: () -> Unit,
    onUploadPhone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE8E0D2)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF5ED)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = MedicalDocHeaderIcon,
                    contentDescription = null,
                    tint = SoftOliveGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your medical records start here",
                color = VedAmritGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Upload prescriptions, reports, and other medical documents to keep everything organized.",
                color = MutedCharcoal,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onScanCamera,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFD2C7B8))
                ) {
                    Text("📷 Scan", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onUploadPhone,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFD2C7B8))
                ) {
                    Text("📁 Upload", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AllMedicalDocumentsModal(
    documents: List<MedicalDocument>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFFFFFDF8)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Medical Records",
                        color = VedAmritGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedCharcoal)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (documents.isEmpty()) {
                    Text(
                        text = "No medical records uploaded yet.",
                        color = MutedCharcoal,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(documents) { doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(BorderStroke(1.dp, Color(0xFFEFE8DA)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (doc.fileType == "PDF") Color(0xFFFFEBEE)
                                            else Color(0xFFE8F5E9)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = doc.fileType,
                                        color = if (doc.fileType == "PDF") Color(0xFFC62828) else Color(0xFF2E7D32),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.title,
                                        color = VedAmritGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = doc.fileName,
                                        color = MutedCharcoal,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${doc.formattedDate} • ${doc.formattedSize}",
                                        color = MutedCharcoal.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }

                                IconButton(
                                    onClick = { onDelete(doc.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete document",
                                        tint = Color(0xFFB71C1C).copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = VedAmritCtaGreen)
                ) {
                    Text("Close", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun handlePickedFile(
    context: Context,
    uri: Uri,
    onResult: (UploadUiState) -> Unit
) {
    val contentResolver = context.contentResolver
    var fileName = "Document_${System.currentTimeMillis()}"
    var fileSize = 0L

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
        }
    }

    val mimeType = contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

    val isPdf = mimeType == "application/pdf" || ext == "pdf"
    val isJpg = mimeType == "image/jpeg" || ext in listOf("jpg", "jpeg")
    val isPng = mimeType == "image/png" || ext == "png"

    if (!isPdf && !isJpg && !isPng) {
        onResult(
            UploadUiState.Error(
                title = "File type not supported",
                message = "Please upload a PDF, JPG, or PNG file.",
                canRetry = false
            )
        )
        return
    }

    val maxSizeBytes = 10L * 1024 * 1024
    if (fileSize > maxSizeBytes) {
        onResult(
            UploadUiState.Error(
                title = "File is too large",
                message = "Please choose a file smaller than 10 MB.",
                canRetry = false
            )
        )
        return
    }

    val fileType = when {
        isPdf -> "PDF"
        isPng -> "PNG"
        else -> "JPG"
    }

    val fileBytes = runCatching {
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()

    if (fileSize == 0L && fileBytes != null) {
        fileSize = fileBytes.size.toLong()
    }

    val formattedSize = MedicalDocumentStore.formatFileSize(fileSize)
    val title = fileName.substringBeforeLast('.').replace('_', ' ')

    onResult(
        UploadUiState.Ready(
            title = title,
            fileName = fileName,
            fileType = fileType,
            sizeBytes = fileSize,
            formattedSize = formattedSize,
            pageCount = 1,
            bytes = fileBytes
        )
    )
}
