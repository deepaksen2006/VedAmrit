package com.example.vedaahar.document.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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

    // Step state: 1: Select, 2: Details, 3: Review, 4: Uploading, 5: Success
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedFile by remember { mutableStateOf<SelectedFileDetails?>(null) }
    var fileValidationError by remember { mutableStateOf<String?>(null) }
    var showCameraScanner by remember { mutableStateOf(false) }

    // Form inputs for Step 2
    var documentType by remember { mutableStateOf("Lab Report") }
    var documentName by remember { mutableStateOf("") }
    var documentDate by remember {
        mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()))
    }
    var doctorOrHospital by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Step 3 Confirmation
    var privacyConfirmed by remember { mutableStateOf(false) }

    // Uploading & Success state
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadedDocument by remember { mutableStateOf<MedicalDocument?>(null) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val details = parseUriToFileDetails(context, uri)
            selectedFile = details
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

    // Intercept back button when uploading
    BackHandler(enabled = isUploading) {
        // Prevent accidental exit during upload
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (!isUploading) {
                            if (currentStep > 1 && currentStep < 4) {
                                currentStep--
                            } else {
                                onBack()
                            }
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
                        fontSize = 19.sp
                    )
                    Text(
                        text = "Add your medical documents securely to your VedaMrit health records.",
                        color = MutedCharcoal,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE8DFD0), thickness = 1.dp)

            // Step Progress Indicator (Visible in steps 1, 2, 3)
            if (currentStep in 1..3) {
                StepProgressHeader(
                    currentStep = currentStep,
                    onStepClick = { targetStep ->
                        if (targetStep < currentStep) {
                            currentStep = targetStep
                        } else if (targetStep == 2 && selectedFile != null && fileValidationError == null) {
                            currentStep = 2
                        }
                    }
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        // ==================== STEP 1: SELECT DOCUMENT ====================
                        Step1SelectDocumentView(
                            selectedFile = selectedFile,
                            validationError = fileValidationError,
                            onChooseFile = { filePickerLauncher.launch("*/*") },
                            onTakePhoto = { showCameraScanner = true },
                            onRemoveFile = {
                                selectedFile = null
                                fileValidationError = null
                            },
                            onContinue = {
                                if (selectedFile != null && fileValidationError == null) {
                                    if (documentName.isBlank()) {
                                        documentName = selectedFile!!.fileName.substringBeforeLast('.').replace('_', ' ')
                                    }
                                    currentStep = 2
                                }
                            }
                        )
                    }

                    2 -> {
                        // ==================== STEP 2: DOCUMENT DETAILS ====================
                        Step2DocumentDetailsView(
                            documentType = documentType,
                            onDocumentTypeChange = { documentType = it },
                            documentName = documentName,
                            onDocumentNameChange = { documentName = it },
                            documentDate = documentDate,
                            onDocumentDateChange = { documentDate = it },
                            doctorOrHospital = doctorOrHospital,
                            onDoctorChange = { doctorOrHospital = it },
                            notes = notes,
                            onNotesChange = { notes = it },
                            onBack = { currentStep = 1 },
                            onContinue = {
                                if (documentName.isNotBlank() && documentDate.isNotBlank()) {
                                    currentStep = 3
                                }
                            }
                        )
                    }

                    3 -> {
                        // ==================== STEP 3: REVIEW & UPLOAD ====================
                        Step3ReviewAndUploadView(
                            selectedFile = selectedFile,
                            documentType = documentType,
                            documentName = documentName,
                            documentDate = documentDate,
                            doctorOrHospital = doctorOrHospital,
                            notes = notes,
                            privacyConfirmed = privacyConfirmed,
                            onPrivacyConfirmedChange = { privacyConfirmed = it },
                            onEditDocument = { currentStep = 1 },
                            onEditDetails = { currentStep = 2 },
                            onBack = { currentStep = 2 },
                            onUploadAndSave = {
                                coroutineScope.launch {
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

                                    val saved = MedicalDocumentStore.addDocument(
                                        context = context,
                                        title = documentName.ifBlank { selectedFile?.fileName ?: "Medical Document" },
                                        fileName = selectedFile?.fileName ?: "Document.pdf",
                                        fileType = selectedFile?.fileType ?: "PDF",
                                        sizeBytes = selectedFile?.sizeBytes ?: 1024L,
                                        pageCount = 1,
                                        sourceBytes = selectedFile?.bytes,
                                        subTitle = documentType,
                                        documentType = documentType,
                                        documentDate = documentDate,
                                        doctorOrHospital = doctorOrHospital,
                                        notes = notes
                                    )

                                    uploadedDocument = saved
                                    isUploading = false
                                    currentStep = 5
                                }
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
                                privacyConfirmed = false
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

    // Camera Document Scanner Modal
    if (showCameraScanner) {
        MedicalDocumentScannerModal(
            onDismiss = { showCameraScanner = false },
            onDocumentScanned = { fileName, _, bytes ->
                val sizeBytes = bytes.size.toLong()
                val details = SelectedFileDetails(
                    fileName = fileName,
                    fileType = "JPG",
                    sizeBytes = sizeBytes,
                    formattedSize = MedicalDocumentStore.formatFileSize(sizeBytes),
                    bytes = bytes,
                    isTooLarge = sizeBytes > 10L * 1024 * 1024,
                    isUnsupported = false
                )
                selectedFile = details
                fileValidationError = null
                if (documentName.isBlank()) {
                    documentName = "Scanned Medical Prescription"
                }
                documentType = "Prescription"
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// STEP PROGRESS INDICATOR
// -------------------------------------------------------------------------------------------------
@Composable
private fun StepProgressHeader(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    Surface(
        color = Color(0xFFFAF6EE),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepPillItem(
                stepNumber = 1,
                label = "Select Document",
                isActive = currentStep == 1,
                isCompleted = currentStep > 1,
                onClick = { onStepClick(1) }
            )

            HorizontalDivider(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(horizontal = 6.dp),
                color = if (currentStep > 1) VedAmritGreen else Color(0xFFD6CEBF),
                thickness = 1.5.dp
            )

            StepPillItem(
                stepNumber = 2,
                label = "Details",
                isActive = currentStep == 2,
                isCompleted = currentStep > 2,
                onClick = { onStepClick(2) }
            )

            HorizontalDivider(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(horizontal = 6.dp),
                color = if (currentStep > 2) VedAmritGreen else Color(0xFFD6CEBF),
                thickness = 1.5.dp
            )

            StepPillItem(
                stepNumber = 3,
                label = "Review & Upload",
                isActive = currentStep == 3,
                isCompleted = currentStep > 3,
                onClick = { onStepClick(3) }
            )
        }
    }
}

@Composable
private fun StepPillItem(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> VedAmritGreen
                        isActive -> VedAmritGreen
                        else -> Color(0xFFE2DAD0)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = "$stepNumber",
                    color = if (isActive) PureWhite else MutedCharcoal,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) DarkForestGreen else MutedCharcoal
        )
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 1: SELECT DOCUMENT VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step1SelectDocumentView(
    selectedFile: SelectedFileDetails?,
    validationError: String?,
    onChooseFile: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveFile: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 1: Choose Your Document",
            color = DarkForestGreen,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        // Drag & Drop / Tap Upload Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChooseFile),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, if (selectedFile != null) VedAmritGreen else Color(0xFFD8CFBE)),
            colors = CardDefaults.cardColors(containerColor = CardBgWarm)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF5ED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = DocUploadAreaIcon,
                        contentDescription = null,
                        tint = VedAmritGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Upload your medical document",
                    color = DarkForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Drag & drop your file here",
                    color = MutedCharcoal,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onChooseFile,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VedAmritGreen,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Choose File", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Supported formats: PDF, JPG, JPEG, PNG",
                    color = MutedCharcoal.copy(alpha = 0.85f),
                    fontSize = 11.5.sp
                )
                Text(
                    text = "Maximum file size: 10 MB",
                    color = MutedCharcoal.copy(alpha = 0.85f),
                    fontSize = 11.5.sp
                )
            }
        }

        // Secondary Option: Take a Photo
        OutlinedButton(
            onClick = onTakePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFD2C7B8)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkForestGreen)
        ) {
            Icon(
                imageVector = CameraCaptureIcon,
                contentDescription = null,
                tint = VedAmritGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("Take a Photo of Document", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        // Inline Validation Error Display
        if (validationError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = validationError,
                        color = ErrorRed,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Selected File Preview Card
        if (selectedFile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFD8CFBE)),
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
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedFile.fileType == "PDF") Color(0xFFFFEBEE)
                                else Color(0xFFE8F5E9)
                            ),
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
                            text = "${selectedFile.formattedSize} · ${selectedFile.fileType}",
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
                                contentDescription = "Remove",
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Continue Button
        Button(
            onClick = onContinue,
            enabled = selectedFile != null && validationError == null,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = VedAmritCtaGreen,
                contentColor = PureWhite
            )
        ) {
            Text("Continue to Details", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 2: DOCUMENT DETAILS VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step2DocumentDetailsView(
    documentType: String,
    onDocumentTypeChange: (String) -> Unit,
    documentName: String,
    onDocumentNameChange: (String) -> Unit,
    documentDate: String,
    onDocumentDateChange: (String) -> Unit,
    doctorOrHospital: String,
    onDoctorChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Document Details",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Add some information to help you find this document later.",
                color = MutedCharcoal,
                fontSize = 12.5.sp
            )
        }

        // Document Type Dropdown
        Column {
            Text(
                text = "Document Type *",
                color = DarkForestGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = documentType,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { typeDropdownExpanded = !typeDropdownExpanded }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select Type")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { typeDropdownExpanded = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VedAmritGreen,
                        unfocusedBorderColor = Color(0xFFD2C7B8),
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    )
                )

                DropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    DocumentTypeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontSize = 13.5.sp) },
                            onClick = {
                                onDocumentTypeChange(option)
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Document Name
        Column {
            Text(
                text = "Document Name *",
                color = DarkForestGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = documentName,
                onValueChange = onDocumentNameChange,
                placeholder = { Text("e.g. Complete Blood Count Report", color = MutedCharcoal.copy(alpha = 0.6f), fontSize = 13.5.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VedAmritGreen,
                    unfocusedBorderColor = Color(0xFFD2C7B8),
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                )
            )
        }

        // Document Date
        Column {
            Text(
                text = "Document Date *",
                color = DarkForestGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = documentDate,
                onValueChange = onDocumentDateChange,
                placeholder = { Text("DD / MM / YYYY", color = MutedCharcoal.copy(alpha = 0.6f), fontSize = 13.5.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VedAmritGreen,
                    unfocusedBorderColor = Color(0xFFD2C7B8),
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                )
            )
        }

        // Doctor / Hospital (Optional)
        Column {
            Text(
                text = "Doctor / Hospital (Optional)",
                color = DarkForestGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = doctorOrHospital,
                onValueChange = onDoctorChange,
                placeholder = { Text("Enter doctor or hospital name", color = MutedCharcoal.copy(alpha = 0.6f), fontSize = 13.5.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VedAmritGreen,
                    unfocusedBorderColor = Color(0xFFD2C7B8),
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                )
            )
        }

        // Notes (Optional)
        Column {
            Text(
                text = "Notes (Optional)",
                color = DarkForestGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                placeholder = { Text("Add any additional information about this document", color = MutedCharcoal.copy(alpha = 0.6f), fontSize = 13.5.sp) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VedAmritGreen,
                    unfocusedBorderColor = Color(0xFFD2C7B8),
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFD2C7B8))
            ) {
                Text("Back", fontSize = 14.sp, color = MutedCharcoal)
            }

            Button(
                onClick = onContinue,
                enabled = documentName.isNotBlank() && documentDate.isNotBlank(),
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VedAmritCtaGreen,
                    contentColor = PureWhite
                )
            ) {
                Text("Continue to Review", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 3: REVIEW & UPLOAD VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step3ReviewAndUploadView(
    selectedFile: SelectedFileDetails?,
    documentType: String,
    documentName: String,
    documentDate: String,
    doctorOrHospital: String,
    notes: String,
    privacyConfirmed: Boolean,
    onPrivacyConfirmedChange: (Boolean) -> Unit,
    onEditDocument: () -> Unit,
    onEditDetails: () -> Unit,
    onBack: () -> Unit,
    onUploadAndSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Review Document",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Please verify the information before securely uploading.",
                color = MutedCharcoal,
                fontSize = 12.5.sp
            )
        }

        // Review Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CardBorderColor),
            colors = CardDefaults.cardColors(containerColor = CardBgWarm)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ReviewRowItem(
                    label = "Document",
                    value = selectedFile?.fileName ?: "Selected document",
                    secondaryValue = "${selectedFile?.formattedSize ?: ""} · ${selectedFile?.fileType ?: ""}",
                    onEdit = onEditDocument
                )

                HorizontalDivider(color = Color(0xFFEDE5D8), thickness = 1.dp)

                ReviewRowItem(
                    label = "Type",
                    value = documentType,
                    onEdit = onEditDetails
                )

                HorizontalDivider(color = Color(0xFFEDE5D8), thickness = 1.dp)

                ReviewRowItem(
                    label = "Document Name",
                    value = documentName,
                    onEdit = onEditDetails
                )

                HorizontalDivider(color = Color(0xFFEDE5D8), thickness = 1.dp)

                ReviewRowItem(
                    label = "Date",
                    value = documentDate,
                    onEdit = onEditDetails
                )

                if (doctorOrHospital.isNotBlank()) {
                    HorizontalDivider(color = Color(0xFFEDE5D8), thickness = 1.dp)
                    ReviewRowItem(
                        label = "Doctor / Hospital",
                        value = doctorOrHospital,
                        onEdit = onEditDetails
                    )
                }

                if (notes.isNotBlank()) {
                    HorizontalDivider(color = Color(0xFFEDE5D8), thickness = 1.dp)
                    ReviewRowItem(
                        label = "Notes",
                        value = notes,
                        onEdit = onEditDetails
                    )
                }
            }
        }

        // Privacy & Security Confirmation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFFD6E2D5)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F9F4))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = LockShieldIcon,
                        contentDescription = null,
                        tint = SoftOliveGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your document is private and secure",
                        color = DarkForestGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "This document will be stored in your VedaMrit health records and will only be accessible according to your account permissions and consent settings.",
                    color = MutedCharcoal,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPrivacyConfirmedChange(!privacyConfirmed) },
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = privacyConfirmed,
                        onCheckedChange = onPrivacyConfirmedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = VedAmritGreen,
                            uncheckedColor = MutedCharcoal
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "I confirm that this document belongs to me and I want to securely store it in my VedaMrit health records.",
                        color = DarkForestGreen,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Navigation Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFD2C7B8))
            ) {
                Text("Back", fontSize = 14.sp, color = MutedCharcoal)
            }

            Button(
                onClick = onUploadAndSave,
                enabled = privacyConfirmed,
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VedAmritCtaGreen,
                    contentColor = PureWhite
                )
            ) {
                Text("Upload & Save", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReviewRowItem(
    label: String,
    value: String,
    secondaryValue: String? = null,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = SoftOliveGreen,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = DarkForestGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (secondaryValue != null) {
                Text(
                    text = secondaryValue,
                    color = MutedCharcoal,
                    fontSize = 11.5.sp
                )
            }
        }

        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit $label",
                tint = VedAmritGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 4: UPLOAD PROGRESS VIEW
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
