package com.example.vedaahar.document.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.document.MedicalDocument
import com.example.vedaahar.document.MedicalDocumentStore
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.MutedCharcoal
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftOliveGreen
import com.example.vedaahar.ui.theme.VedAmritCtaGreen
import com.example.vedaahar.ui.theme.VedAmritGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandGold = Color(0xFFE8C97B)
private val BrandGoldDark = Color(0xFF9B762B)
private val ErrorRed = Color(0xFFC62828)
private val SuccessGreen = Color(0xFF2E7D32)
private val CardBorderColor = Color(0xFFE8E0D2)
private val CardBgWarm = Color(0xFFFFFDF8)

// Custom crisp vector icons
private val DocUploadAreaIcon: ImageVector = ImageVector.Builder(
    name = "DocUploadAreaIcon",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.6f,
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
        moveTo(12f, 18f)
        lineTo(12f, 12f)
        moveTo(9f, 15f)
        lineTo(12f, 12f)
        lineTo(15f, 15f)
    }
}.build()

private val LockShieldIcon: ImageVector = ImageVector.Builder(
    name = "LockShieldIcon",
    defaultWidth = 22.dp,
    defaultHeight = 22.dp,
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
        moveTo(12f, 2f)
        lineTo(3f, 6f)
        verticalLineTo(11.5f)
        curveTo(3f, 16.5f, 6.8f, 21.2f, 12f, 22.5f)
        curveTo(17.2f, 21.2f, 21f, 16.5f, 21f, 11.5f)
        verticalLineTo(6f)
        lineTo(12f, 2f)
        close()
        moveTo(9f, 12f)
        verticalLineTo(10f)
        curveTo(9f, 8.3f, 10.3f, 7f, 12f, 7f)
        curveTo(13.7f, 7f, 15f, 8.3f, 15f, 10f)
        verticalLineTo(12f)
        moveTo(8.5f, 12f)
        horizontalLineTo(15.5f)
        curveTo(16.3f, 12f, 17f, 12.7f, 17f, 13.5f)
        verticalLineTo(16.5f)
        curveTo(17f, 17.3f, 16.3f, 18f, 15.5f, 18f)
        horizontalLineTo(8.5f)
        curveTo(7.7f, 18f, 7f, 17.3f, 7f, 16.5f)
        verticalLineTo(13.5f)
        curveTo(7f, 12.7f, 7.7f, 12f, 8.5f, 12f)
        close()
    }
}.build()

private val CameraCaptureIcon: ImageVector = ImageVector.Builder(
    name = "CameraCaptureIcon",
    defaultWidth = 20.dp,
    defaultHeight = 20.dp,
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

val DocumentTypeOptions = listOf(
    "Lab Report",
    "Prescription",
    "Doctor's Report",
    "X-Ray / Scan",
    "Discharge Summary",
    "Medical Certificate",
    "Vaccination Record",
    "Other"
)

data class SelectedFileDetails(
    val fileName: String,
    val fileType: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val bytes: ByteArray?,
    val isTooLarge: Boolean = false,
    val isUnsupported: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadMedicalDocumentScreen(
    onBack: () -> Unit,
    onNavigateToMyDocuments: () -> Unit,
    onViewDocument: (MedicalDocument) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Step state: 1: Select, 4: Uploading, 5: Success
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedFile by remember { mutableStateOf<SelectedFileDetails?>(null) }
    var fileValidationError by remember { mutableStateOf<String?>(null) }
    var pendingCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Form inputs for Step 2
    var documentType by remember { mutableStateOf("Lab Report") }
    var documentName by remember { mutableStateOf("") }
    var documentDate by remember {
        mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()))
    }
    var doctorOrHospital by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Uploading & Success state
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadedDocument by remember { mutableStateOf<MedicalDocument?>(null) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val details = parseUriToFileDetails(context, uri)
            selectedFile = details
            uploadErrorMessage = null
            if (details.isTooLarge) {
                fileValidationError = "File too large: This document exceeds the 10 MB limit. Please select a smaller file."
            } else if (details.isUnsupported) {
                fileValidationError = "Unsupported format: Please upload a PDF, JPG, JPEG, or PNG file."
            } else {
                fileValidationError = null
                if (documentName.isBlank()) {
                    documentName = details.fileName.substringBeforeLast('.').replace('_', ' ')
                }
            }
        }
    }

    val nativeCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        val imageUri = pendingCameraImageUri
        pendingCameraImageUri = null
        if (captured && imageUri != null) {
            val details = parseCapturedPhotoToFileDetails(context, imageUri)
            selectedFile = details
            uploadErrorMessage = null
            if (details.isTooLarge) {
                fileValidationError = "File too large: This photo exceeds the 10 MB limit. Please take a smaller photo."
            } else {
                fileValidationError = null
                if (documentName.isBlank()) {
                    documentName = details.fileName.substringBeforeLast('.').replace('_', ' ')
                }
            }
        }
    }

    fun openNativeCamera() {
        val imageUri = createCameraImageUri(context)
        pendingCameraImageUri = imageUri
        nativeCameraLauncher.launch(imageUri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openNativeCamera()
        } else {
            pendingCameraImageUri = null
            fileValidationError = "Camera permission is required to take a photo."
        }
    }

    // Intercept back button when uploading
    BackHandler(enabled = isUploading) {
        // Prevent accidental exit during upload
    }

    fun uploadSelectedDocument() {
        val file = selectedFile
        when {
            isUploading -> return
            file == null -> {
                fileValidationError = "Please choose a document or take a photo before uploading."
                return
            }
            file.isTooLarge -> {
                fileValidationError = "File too large: This document exceeds the 10 MB limit. Please select a smaller file."
                return
            }
            file.isUnsupported -> {
                fileValidationError = "Unsupported format: Please upload a PDF, JPG, JPEG, or PNG file."
                return
            }
        }

        fileValidationError = null
        val uploadTitle = file.fileName.substringBeforeLast('.').replace('_', ' ').ifBlank { "Medical Document" }
        coroutineScope.launch {
            runCatching {
                uploadErrorMessage = null
                currentStep = 4
                isUploading = true
                uploadProgress = 0.15f
                delay(400)
                uploadProgress = 0.45f
                delay(350)
                uploadProgress = 0.78f
                delay(400)
                uploadProgress = 0.95f
                delay(300)
                uploadProgress = 1.0f
                delay(200)

                MedicalDocumentStore.addDocument(
                    context = context,
                    title = uploadTitle,
                    fileName = file.fileName,
                    fileType = file.fileType,
                    sizeBytes = file.sizeBytes,
                    pageCount = 1,
                    sourceBytes = file.bytes,
                    subTitle = "Medical Document",
                    documentType = "Medical Document",
                    documentDate = documentDate,
                    doctorOrHospital = "",
                    notes = ""
                )
            }.onSuccess { saved ->
                uploadedDocument = saved
                isUploading = false
                currentStep = 5
            }.onFailure {
                isUploading = false
                currentStep = 1
                uploadErrorMessage = "Unable to upload this document. Please try again."
            }
        }
    }

    LaunchedEffect(currentStep, uploadedDocument) {
        if (currentStep == 5 && uploadedDocument != null) {
            delay(1200)
            onNavigateToMyDocuments()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            UploadMedicalDocumentBackground()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = {
                        if (!isUploading) {
                            onBack()
                        }
                    },
                    enabled = !isUploading
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkForestGreen
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Upload Medical Document",
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add your medical documents securely to your Veda health record.",
                        color = MutedCharcoal,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 150.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        // ==================== STEP 1: SELECT DOCUMENT ====================
                        UploadDocumentSelectionContent(
                            selectedFile = selectedFile,
                            validationError = fileValidationError,
                            uploadError = uploadErrorMessage,
                            onChooseFile = { filePickerLauncher.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
                            onTakePhoto = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    openNativeCamera()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            onRemoveFile = {
                                selectedFile = null
                                fileValidationError = null
                                uploadErrorMessage = null
                            },
                            onContinue = {
                                uploadSelectedDocument()
                            }
                        )
                    }

                    4 -> {
                        // ==================== STEP 4: UPLOAD PROGRESS ====================
                        UploadProgressView(
                            fileName = selectedFile?.fileName ?: "Document.pdf",
                            progress = uploadProgress
                        )
                    }

                    5 -> {
                        // ==================== STEP 5: SUCCESS CONFIRMATION ====================
                        Step5SuccessView(
                            document = uploadedDocument,
                            onViewDocument = {
                                uploadedDocument?.let { doc -> onViewDocument(doc) }
                            },
                            onUploadAnother = {
                                selectedFile = null
                                fileValidationError = null
                                documentName = ""
                                doctorOrHospital = ""
                                notes = ""
                                currentStep = 1
                            },
                            onBackToMyDocuments = onNavigateToMyDocuments
                        )
                    }
                }
                Spacer(modifier = Modifier.height(100.dp).navigationBarsPadding())
            }
        }
    }

}

@Composable
private fun UploadMedicalDocumentBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Cream)
        drawCircle(
            color = LightSage.copy(alpha = 0.42f),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.02f, -size.width * 0.12f)
        )
        drawOval(
            color = LightSage.copy(alpha = 0.34f),
            topLeft = Offset(-size.width * 0.12f, size.height * 0.82f),
            size = Size(size.width * 0.86f, size.height * 0.26f)
        )
        drawOval(
            color = Color(0xFFD8F0DF).copy(alpha = 0.46f),
            topLeft = Offset(size.width * 0.66f, size.height * 0.88f),
            size = Size(size.width * 0.5f, size.height * 0.18f)
        )
    }
}

@Composable
private fun UploadDocumentSelectionContent(
    selectedFile: SelectedFileDetails?,
    validationError: String?,
    uploadError: String?,
    onChooseFile: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveFile: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        UploadSourceCard(
            title = "Upload from Files",
            subtitle = "Select from your device",
            badge = "PDF, JPG, JPEG, PNG | Max 10 MB",
            icon = DocUploadAreaIcon,
            iconTint = VedAmritGreen,
            containerColor = Color(0xFFFBFFFB),
            accentColor = Color(0xFFDFF3E5),
            borderColor = Color(0xFFD5EADD),
            onClick = onChooseFile
        )

        UploadSourceCard(
            title = "Upload from Camera",
            subtitle = "Take a photo of your document",
            badge = null,
            icon = CameraCaptureIcon,
            iconTint = Color(0xFF1565A9),
            containerColor = Color(0xFFF1FAFF),
            accentColor = Color(0xFFDDEFFF),
            borderColor = Color(0xFFC9E6FA),
            onClick = onTakePhoto
        )

        if (validationError != null) {
            UploadMessageCard(message = validationError, error = true)
        }

        if (uploadError != null) {
            UploadMessageCard(message = uploadError, error = true)
        }

        if (selectedFile != null) {
            SelectedDocumentPreviewCard(
                selectedFile = selectedFile,
                onChooseFile = onChooseFile,
                onRemoveFile = onRemoveFile
            )

            Button(
                onClick = onContinue,
                enabled = validationError == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VedAmritCtaGreen,
                    contentColor = PureWhite
                )
            ) {
                Text(if (uploadError == null) "Upload Document" else "Retry Upload", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
            }
        }

        SecurityFooter()
    }
}

@Composable
private fun UploadSourceCard(
    title: String,
    subtitle: String,
    badge: String?,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = iconTint.copy(alpha = 0.08f), spotColor = iconTint.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            val compact = maxWidth < 380.dp
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 74.dp else 92.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(if (compact) 38.dp else 48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(VedAmritGreen)
                            .border(BorderStroke(3.dp, containerColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (badge == null) CameraCaptureIcon else DocUploadAreaIcon,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = DarkForestGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 20.sp else 23.sp,
                        lineHeight = if (compact) 25.sp else 28.sp
                    )
                    Text(
                        text = subtitle,
                        color = MutedCharcoal,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                    if (badge != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFE9F6EC))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(DocUploadAreaIcon, contentDescription = null, tint = DarkForestGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(badge, color = DarkForestGreen, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDocumentPreviewCard(
    selectedFile: SelectedFileDetails,
    onChooseFile: () -> Unit,
    onRemoveFile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = VedAmritGreen.copy(alpha = 0.08f), spotColor = VedAmritGreen.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFD8E8DB)),
        colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
        CapturedImagePreview(selectedFile = selectedFile)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedFile.fileType == "PDF") Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedFile.fileType,
                    color = if (selectedFile.fileType == "PDF") Color(0xFFC62828) else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedFile.fileName,
                    color = DarkForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${selectedFile.formattedSize} | ${selectedFile.fileType}",
                    color = MutedCharcoal,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onChooseFile,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(50),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text("Replace", fontSize = 11.5.sp)
                }

                IconButton(onClick = onRemoveFile, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove selected document",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadMessageCard(message: String, error: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (error) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
        border = BorderStroke(1.dp, if (error) Color(0xFFFFCDD2) else Color(0xFFCDE8D1))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (error) Icons.Default.Warning else Icons.Default.Check,
                contentDescription = null,
                tint = if (error) ErrorRed else SuccessGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = if (error) ErrorRed else SuccessGreen,
                fontSize = 12.5.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SecurityFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 38.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = LockShieldIcon,
            contentDescription = null,
            tint = VedAmritGreen,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(34.dp)
                .background(Color(0xFFD7E8D9))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Your data is safe and secure",
            color = VedAmritGreen,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CapturedImagePreview(selectedFile: SelectedFileDetails) {
    if (selectedFile.fileType == "PDF" || selectedFile.bytes == null) return
    val bitmap = remember(selectedFile.bytes) {
        BitmapFactory.decodeByteArray(selectedFile.bytes, 0, selectedFile.bytes.size)
    } ?: return

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Selected document preview",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(start = 14.dp, top = 14.dp, end = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, Color(0xFFD8CFBE)), RoundedCornerShape(14.dp))
    )
}

// -------------------------------------------------------------------------------------------------
// UPLOAD PROGRESS VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun UploadProgressView(
    fileName: String,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        colors = CardDefaults.cardColors(containerColor = CardBgWarm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF4E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DocUploadAreaIcon,
                    contentDescription = null,
                    tint = VedAmritGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Uploading your document...",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = fileName,
                color = MutedCharcoal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = VedAmritCtaGreen,
                trackColor = Color(0xFFE8E0D2)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${(progress * 100).toInt()}%",
                color = DarkForestGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = LockShieldIcon,
                    contentDescription = null,
                    tint = SoftOliveGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Encrypted & stored in your secure health vault",
                    color = SoftOliveGreen,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 5: SUCCESS CONFIRMATION VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step5SuccessView(
    document: MedicalDocument?,
    onViewDocument: () -> Unit,
    onUploadAnother: () -> Unit,
    onBackToMyDocuments: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        colors = CardDefaults.cardColors(containerColor = CardBgWarm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Document uploaded successfully",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your medical document has been securely added to your VedaMrit health records.",
                color = MutedCharcoal,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Uploaded Document Summary
            if (document != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE8E0D2)),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (document.fileType == "PDF") Color(0xFFFFEBEE)
                                    else Color(0xFFE8F5E9)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = document.fileType,
                                color = if (document.fileType == "PDF") Color(0xFFC62828) else Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = document.title,
                                color = DarkForestGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${document.formattedDate} · ${document.fileType} · ${document.formattedSize}",
                                color = MutedCharcoal,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onViewDocument,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VedAmritCtaGreen,
                    contentColor = PureWhite
                )
            ) {
                Text("View Document", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onUploadAnother,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFD2C7B8))
                ) {
                    Text("Upload Another", fontSize = 13.sp, color = DarkForestGreen)
                }

                OutlinedButton(
                    onClick = onBackToMyDocuments,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFD2C7B8))
                ) {
                    Text("Back to My Documents", fontSize = 13.sp, color = DarkForestGreen)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// UTILITY: PARSE URI INTO FILE DETAILS & VALIDATION
// -------------------------------------------------------------------------------------------------
private fun parseUriToFileDetails(context: Context, uri: Uri): SelectedFileDetails {
    val contentResolver = context.contentResolver
    var fileName = "Medical_Report_${System.currentTimeMillis()}"
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

    val isUnsupported = !isPdf && !isJpg && !isPng
    val maxBytes = 10L * 1024 * 1024
    val isTooLarge = fileSize > maxBytes

    val fileBytes = runCatching {
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()

    if (fileSize == 0L && fileBytes != null) {
        fileSize = fileBytes.size.toLong()
    }

    val fileType = when {
        isPdf -> "PDF"
        isPng -> "PNG"
        else -> "JPG"
    }

    return SelectedFileDetails(
        fileName = fileName,
        fileType = fileType,
        sizeBytes = fileSize,
        formattedSize = MedicalDocumentStore.formatFileSize(fileSize),
        bytes = fileBytes,
        isTooLarge = fileSize > maxBytes,
        isUnsupported = isUnsupported
    )
}

private fun createCameraImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "camera_documents").apply { mkdirs() }
    val imageFile = File(directory, "Medical_Document_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

private fun parseCapturedPhotoToFileDetails(context: Context, uri: Uri): SelectedFileDetails {
    val fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    val sizeBytes = fileBytes.size.toLong()
    val fileName = "Medical_Document_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
    return SelectedFileDetails(
        fileName = fileName,
        fileType = "JPG",
        sizeBytes = sizeBytes,
        formattedSize = MedicalDocumentStore.formatFileSize(sizeBytes),
        bytes = fileBytes,
        isTooLarge = sizeBytes > 10L * 1024 * 1024,
        isUnsupported = false
    )
}


