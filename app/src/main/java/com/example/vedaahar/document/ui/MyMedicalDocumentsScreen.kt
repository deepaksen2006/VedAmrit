package com.example.vedaahar.document.ui

import android.content.Context
import android.widget.Toast
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

private val CardBorderColor = Color(0xFFE8E0D2)
private val CardBgWarm = Color(0xFFFFFDF8)
private val DestructiveRed = Color(0xFFC62828)

// Custom crisp vector icons
private val EmptyDocFolderIcon: ImageVector = ImageVector.Builder(
    name = "EmptyDocFolderIcon",
    defaultWidth = 52.dp,
    defaultHeight = 52.dp,
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
        moveTo(9f, 13f)
        lineTo(15f, 13f)
    }
}.build()

private val DownloadArrowIcon: ImageVector = ImageVector.Builder(
    name = "DownloadArrowIcon",
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
        moveTo(12f, 3f)
        lineTo(12f, 15f)
        moveTo(7f, 10f)
        lineTo(12f, 15f)
        lineTo(17f, 10f)
        moveTo(4f, 20f)
        lineTo(20f, 20f)
    }
}.build()

private val LockShieldSmall: ImageVector = ImageVector.Builder(
    name = "LockShieldSmall",
    defaultWidth = 14.dp,
    defaultHeight = 14.dp,
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
        close()
    }
}.build()

@Composable
fun MyMedicalDocumentsScreen(
    onBack: () -> Unit,
    onUploadClick: () -> Unit,
    initialViewDocId: String? = null
) {
    val context = LocalContext.current
    var documents by remember { mutableStateOf(MedicalDocumentStore.getDocuments(context)) }

    // Search, Filter, Sort state
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf("Newest First") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Active modals
    var viewingDocument by remember {
        mutableStateOf(
            if (initialViewDocId != null) documents.find { it.id == initialViewDocId } else null
        )
    }
    var documentToDelete by remember { mutableStateOf<MedicalDocument?>(null) }
    var documentToRename by remember { mutableStateOf<MedicalDocument?>(null) }
    var documentToShare by remember { mutableStateOf<MedicalDocument?>(null) }

    // Filter and Sort logic
    val filteredDocuments = remember(documents, searchQuery, selectedFilter, sortOrder) {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        var list = documents.filter { doc ->
            val matchesQuery = query.isEmpty() ||
                doc.title.lowercase(Locale.ROOT).contains(query) ||
                doc.fileName.lowercase(Locale.ROOT).contains(query) ||
                doc.doctorOrHospital.lowercase(Locale.ROOT).contains(query) ||
                doc.notes.lowercase(Locale.ROOT).contains(query) ||
                doc.documentType.lowercase(Locale.ROOT).contains(query)

            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Lab Reports" -> doc.documentType.equals("Lab Report", ignoreCase = true)
                "Prescriptions" -> doc.documentType.equals("Prescription", ignoreCase = true)
                "Reports" -> doc.documentType.contains("Report", ignoreCase = true) && !doc.documentType.equals("Lab Report", ignoreCase = true)
                "X-Ray / Scan" -> doc.documentType.contains("X-Ray", ignoreCase = true) || doc.documentType.contains("Scan", ignoreCase = true)
                "Other" -> doc.documentType !in listOf("Lab Report", "Prescription", "Doctor's Report", "X-Ray / Scan")
                else -> true
            }

            matchesQuery && matchesFilter
        }

        list = if (sortOrder == "Newest First") {
            list.sortedByDescending { it.uploadTimestamp }
        } else {
            list.sortedBy { it.uploadTimestamp }
        }

        list
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
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkForestGreen
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Medical Documents",
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = "View and manage your uploaded medical records.",
                        color = MutedCharcoal,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onUploadClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VedAmritCtaGreen,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("+ Upload", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color(0xFFE8DFD0), thickness = 1.dp)

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search documents...", color = MutedCharcoal.copy(alpha = 0.65f), fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MutedCharcoal,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MutedCharcoal, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VedAmritGreen,
                        unfocusedBorderColor = Color(0xFFD8CFBE),
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    )
                )
            }

            // Filter Tabs & Sort Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter chips horizontally scrollable
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf("All", "Lab Reports", "Prescriptions", "Reports", "X-Ray / Scan", "Other")
                    filterOptions.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) VedAmritGreen else Color(0xFFEFE8DA))
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) PureWhite else DarkForestGreen,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Order Selector
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(PureWhite)
                            .border(BorderStroke(1.dp, Color(0xFFD8CFBE)), RoundedCornerShape(50))
                            .clickable { sortMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sortOrder,
                            color = DarkForestGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = DarkForestGreen, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest First", fontSize = 13.sp) },
                            onClick = {
                                sortOrder = "Newest First"
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First", fontSize = 13.sp) },
                            onClick = {
                                sortOrder = "Oldest First"
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Document Cards List or Empty State
            if (filteredDocuments.isEmpty()) {
                EmptyDocumentsState(
                    hasQuery = searchQuery.isNotEmpty() || selectedFilter != "All",
                    onUploadClick = onUploadClick,
                    onClearFilters = {
                        searchQuery = ""
                        selectedFilter = "All"
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDocuments, key = { it.id }) { doc ->
                        DocumentItemCard(
                            doc = doc,
                            onView = { viewingDocument = doc },
                            onDownload = {
                                Toast.makeText(context, "Downloading “${doc.title}”...", Toast.LENGTH_SHORT).show()
                            },
                            onRename = { documentToRename = doc },
                            onShare = { documentToShare = doc },
                            onDelete = { documentToDelete = doc }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp).navigationBarsPadding())
                    }
                }
            }
        }
    }

    // Modal: Document Viewer
    if (viewingDocument != null) {
        DocumentViewerModal(
            document = viewingDocument!!,
            onDismiss = { viewingDocument = null },
            onShare = {
                val doc = viewingDocument!!
                viewingDocument = null
                documentToShare = doc
            },
            onDelete = {
                val doc = viewingDocument!!
                viewingDocument = null
                documentToDelete = doc
            },
            onDownload = {
                Toast.makeText(context, "Downloading “${viewingDocument!!.title}”...", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Delete Document Confirmation
    if (documentToDelete != null) {
        DeleteDocumentConfirmationDialog(
            document = documentToDelete!!,
            onDismiss = { documentToDelete = null },
            onConfirmDelete = {
                MedicalDocumentStore.deleteDocument(context, documentToDelete!!.id)
                documents = MedicalDocumentStore.getDocuments(context)
                Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                documentToDelete = null
            }
        )
    }

    // Modal: Rename Document
    if (documentToRename != null) {
        RenameDocumentDialog(
            document = documentToRename!!,
            onDismiss = { documentToRename = null },
            onConfirmRename = { newTitle ->
                MedicalDocumentStore.renameDocument(context, documentToRename!!.id, newTitle)
                documents = MedicalDocumentStore.getDocuments(context)
                Toast.makeText(context, "Document renamed", Toast.LENGTH_SHORT).show()
                documentToRename = null
            }
        )
    }

    // Modal: Share Document Securely
    if (documentToShare != null) {
        ShareMedicalDocumentModal(
            document = documentToShare!!,
            onDismiss = { documentToShare = null },
            onConfirmShare = { recipient, purpose, duration ->
                MedicalDocumentStore.shareDocument(context, documentToShare!!.id, recipient, purpose, duration)
                documents = MedicalDocumentStore.getDocuments(context)
                Toast.makeText(context, "Shared securely with $recipient", Toast.LENGTH_SHORT).show()
                documentToShare = null
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// DOCUMENT ITEM CARD
// -------------------------------------------------------------------------------------------------
@Composable
private fun DocumentItemCard(
    doc: MedicalDocument,
    onView: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFEBE3D5)),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (doc.fileType.uppercase(Locale.ROOT)) {
                                "PDF" -> Color(0xFFFFEBEE)
                                "JPG", "JPEG" -> Color(0xFFE8F5E9)
                                else -> Color(0xFFFFF8E1)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = doc.fileType,
                        color = when (doc.fileType.uppercase(Locale.ROOT)) {
                            "PDF" -> Color(0xFFC62828)
                            "JPG", "JPEG" -> Color(0xFF2E7D32)
                            else -> Color(0xFFF57F17)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.title,
                        color = DarkForestGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (doc.subTitle.isNotBlank() || doc.doctorOrHospital.isNotBlank()) {
                        val subText = buildString {
                            if (doc.subTitle.isNotBlank()) append(doc.subTitle)
                            if (doc.subTitle.isNotBlank() && doc.doctorOrHospital.isNotBlank()) append(" · ")
                            if (doc.doctorOrHospital.isNotBlank()) append(doc.doctorOrHospital)
                        }
                        Text(
                            text = subText,
                            color = MutedCharcoal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${doc.documentDate} · ${doc.fileType} · ${doc.formattedSize}",
                        color = MutedCharcoal.copy(alpha = 0.8f),
                        fontSize = 11.5.sp
                    )
                }

                // Actions: View button & 3-dot overflow menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onView,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEFF5ED),
                            contentColor = DarkForestGreen
                        ),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions",
                                tint = DarkForestGreen
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View", fontSize = 13.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onView()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Download", fontSize = 13.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onDownload()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename", fontSize = 13.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onRename()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share", fontSize = 13.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = DestructiveRed, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Trust badge at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = LockShieldSmall,
                    contentDescription = null,
                    tint = SoftOliveGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Private Record · ${doc.documentType}",
                    color = SoftOliveGreen,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )

                if (doc.sharedWith.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• Shared with ${doc.sharedWith.size}",
                        color = Color(0xFF1565C0),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// EMPTY DOCUMENTS STATE
// -------------------------------------------------------------------------------------------------
@Composable
private fun EmptyDocumentsState(
    hasQuery: Boolean,
    onUploadClick: () -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        colors = CardDefaults.cardColors(containerColor = CardBgWarm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
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
                    imageVector = EmptyDocFolderIcon,
                    contentDescription = null,
                    tint = SoftOliveGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (hasQuery) "No matching documents found" else "No medical documents yet",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hasQuery)
                    "Try checking your spelling or adjusting your category filters."
                else
                    "Upload your medical reports, prescriptions, and health records to keep everything organized in one secure place.",
                color = MutedCharcoal,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (hasQuery) {
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFD2C7B8))
                ) {
                    Text("Clear Search & Filters", fontSize = 13.sp, color = DarkForestGreen)
                }
            } else {
                Button(
                    onClick = onUploadClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VedAmritCtaGreen,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("+ Upload Your First Document", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// DOCUMENT VIEWER MODAL
// -------------------------------------------------------------------------------------------------
@Composable
private fun DocumentViewerModal(
    document: MedicalDocument,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Color(0xFFF7F4EC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PureWhite)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = DarkForestGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back to Documents", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = DownloadArrowIcon, contentDescription = "Download", tint = DarkForestGreen, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = DarkForestGreen, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DestructiveRed, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = Color(0xFFE8DFD0), thickness = 1.dp)

                // Header Details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PureWhite)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = document.title,
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${document.documentType} · ${document.documentDate} · ${document.fileType} · ${document.formattedSize}",
                        color = MutedCharcoal,
                        fontSize = 12.sp
                    )

                    if (document.doctorOrHospital.isNotBlank()) {
                        Text(
                            text = "Provider: ${document.doctorOrHospital}",
                            color = SoftOliveGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE8DFD0), thickness = 1.dp)

                // Main Viewer Body: Render authentic clinical document sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ClinicalDocumentSheet(document = document)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CLINICAL DOCUMENT SHEET PREVIEW (Simulated authentic clinical medical viewer)
// -------------------------------------------------------------------------------------------------
@Composable
private fun ClinicalDocumentSheet(document: MedicalDocument) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Color(0xFFD4CBBB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Clinic / Lab Letterhead
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = if (document.doctorOrHospital.isNotBlank()) document.doctorOrHospital else "VEDAMRIT AYURVEDIC HEALTHCARE",
                        color = DarkForestGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Clinical Diagnostic & Health Record System",
                        color = MutedCharcoal,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Accreditation: NABL & ISO 15189 Certified",
                        color = MutedCharcoal,
                        fontSize = 9.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE8F3E8))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("VERIFIED RECORD", color = VedAmritGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE0D8C8))

            // Patient & Sample Metadata Table
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("PATIENT NAME: Deepak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                    Text("AGE / GENDER: 28 Yrs / Male", fontSize = 10.sp, color = MutedCharcoal)
                    Text("REF. BY: ${if (document.doctorOrHospital.isNotBlank()) document.doctorOrHospital else "Self Refer"}", fontSize = 10.sp, color = MutedCharcoal)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DATE: ${document.documentDate}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                    Text("REPORT ID: VA-${document.id.take(8).uppercase()}", fontSize = 10.sp, color = MutedCharcoal)
                    Text("STATUS: Final Complete", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE0D8C8))

            // Specific Content based on Type
            when (document.documentType) {
                "Prescription" -> {
                    // Prescription View
                    Text("Rx (Medical Prescription)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Triphala Churna (Fine Powder) — 1 tsp with warm water before bedtime.", fontSize = 11.5.sp, color = DarkForestGreen)
                    Text("2. Ashwagandha Capsules (500mg) — 1 capsule twice daily after meals.", fontSize = 11.5.sp, color = DarkForestGreen)
                    Text("3. Brahmi Herbal Tea — Once daily in morning for Vata calm.", fontSize = 11.5.sp, color = DarkForestGreen)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Dietary Instructions: Light freshly prepared food. Avoid excess spicy and cold dairy.", fontSize = 11.sp, color = MutedCharcoal)
                }

                "X-Ray / Scan" -> {
                    // Diagnostic Imaging View
                    Text("RADIOLOGY INVESTIGATION: CHEST PA VIEW", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Both lung fields appear clear without focal consolidation or pneumothorax.", fontSize = 11.sp, color = DarkForestGreen)
                    Text("• Cardiothoracic ratio is within normal physiological limits.", fontSize = 11.sp, color = DarkForestGreen)
                    Text("• Costophrenic and cardiophrenic angles are well visualized.", fontSize = 11.sp, color = DarkForestGreen)
                    Text("• Bony thorax and soft tissues unremarkable.", fontSize = 11.sp, color = DarkForestGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("IMPRESSION: Normal radiograph of the chest.", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                }

                else -> {
                    // Lab Report Table (CBC / Blood Panel)
                    Text("INVESTIGATION: COMPLETE BLOOD COUNT (CBC)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF6F2E9)).padding(6.dp)) {
                        Text("Test Parameter", modifier = Modifier.weight(1.5f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                        Text("Observed", modifier = Modifier.weight(1f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                        Text("Reference", modifier = Modifier.weight(1.2f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                        Text("Status", modifier = Modifier.weight(0.8f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                    }

                    LabTableRow("Hemoglobin", "14.2 g/dL", "13.0 - 17.0", "Normal")
                    LabTableRow("Total Leucocyte Count (WBC)", "7,400 /mcL", "4,000 - 11,000", "Normal")
                    LabTableRow("Platelet Count", "260,000 /mcL", "150,000 - 450,000", "Normal")
                    LabTableRow("RBC Count", "4.95 M/mcL", "4.5 - 5.9", "Normal")
                    LabTableRow("Fasting Blood Sugar", "92 mg/dL", "70 - 100", "Normal")
                    LabTableRow("Serum Creatinine", "0.9 mg/dL", "0.7 - 1.3", "Normal")
                }
            }

            if (document.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("Doctor / Patient Notes:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                Text(document.notes, fontSize = 11.sp, color = MutedCharcoal)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Signature & Seal Stamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.dp, Color(0xFF2E7D32)), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("DIGITALLY SIGNED & ENCRYPTED", fontSize = 8.5.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Dr. VedaMrit Diagnostic Authority", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                    Text("Reg. No: AYUSH-MCI-2026-9812", fontSize = 9.sp, color = MutedCharcoal)
                }
            }
        }
    }
}

@Composable
private fun LabTableRow(name: String, value: String, ref: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Text(name, modifier = Modifier.weight(1.5f), fontSize = 10.sp, color = DarkForestGreen)
        Text(value, modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DarkForestGreen)
        Text(ref, modifier = Modifier.weight(1.2f), fontSize = 9.5.sp, color = MutedCharcoal)
        Text(status, modifier = Modifier.weight(0.8f), fontSize = 9.5.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color(0xFFF0EBE0), thickness = 0.5.dp)
}

// -------------------------------------------------------------------------------------------------
// DELETE DOCUMENT CONFIRMATION MODAL
// -------------------------------------------------------------------------------------------------
@Composable
private fun DeleteDocumentConfirmationDialog(
    document: MedicalDocument,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = DestructiveRed, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete this document?", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Text(
                text = "Are you sure you want to permanently delete “${document.title}”? This action cannot be undone.",
                color = MutedCharcoal,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = PureWhite),
                shape = RoundedCornerShape(50)
            ) {
                Text("Delete Document", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFD2C7B8))
            ) {
                Text("Cancel", fontSize = 13.sp, color = MutedCharcoal)
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(20.dp)
    )
}

// -------------------------------------------------------------------------------------------------
// RENAME DOCUMENT MODAL
// -------------------------------------------------------------------------------------------------
@Composable
private fun RenameDocumentDialog(
    document: MedicalDocument,
    onDismiss: () -> Unit,
    onConfirmRename: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(document.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rename Document", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column {
                Text(
                    text = "Enter a new name for this medical record.",
                    color = MutedCharcoal,
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VedAmritGreen,
                        unfocusedBorderColor = Color(0xFFD2C7B8),
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newTitle.isNotBlank()) onConfirmRename(newTitle) },
                enabled = newTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VedAmritCtaGreen, contentColor = PureWhite),
                shape = RoundedCornerShape(50)
            ) {
                Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFD2C7B8))
            ) {
                Text("Cancel", fontSize = 13.sp, color = MutedCharcoal)
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(20.dp)
    )
}

// -------------------------------------------------------------------------------------------------
// SHARE MEDICAL DOCUMENT MODAL
// -------------------------------------------------------------------------------------------------
@Composable
private fun ShareMedicalDocumentModal(
    document: MedicalDocument,
    onDismiss: () -> Unit,
    onConfirmShare: (recipient: String, purpose: String, duration: String) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Consultation") }
    var accessDuration by remember { mutableStateOf("7 Days") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share Medical Document",
                        color = DarkForestGreen,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.5.sp
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedCharcoal)
                    }
                }

                // Summary of Document Being Shared
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("DOCUMENT BEING SHARED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SoftOliveGreen)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(document.title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                        Text("${document.documentType} · ${document.formattedDate} · ${document.formattedSize}", fontSize = 11.sp, color = MutedCharcoal)
                    }
                }

                // Recipient Input
                Column {
                    Text("Shared With *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkForestGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        placeholder = { Text("e.g. Dr. Arvind Vaidya or doctor@vedamrit.com", fontSize = 12.5.sp, color = MutedCharcoal.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VedAmritGreen,
                            unfocusedBorderColor = Color(0xFFD2C7B8)
                        )
                    )
                }

                // Purpose
                Column {
                    Text("Purpose (Optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkForestGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Consultation", "Second Opinion", "Insurance Claim", "Personal").forEach { option ->
                            val isSelected = purpose == option
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) VedAmritGreen else Color(0xFFEFE8DA))
                                    .clickable { purpose = option }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 11.5.sp,
                                    color = if (isSelected) PureWhite else DarkForestGreen,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Duration
                Column {
                    Text("Access Duration", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkForestGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("24 Hours", "7 Days", "30 Days").forEach { duration ->
                            val isSelected = accessDuration == duration
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) VedAmritGreen else Color(0xFFEFE8DA))
                                    .clickable { accessDuration = duration }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = duration,
                                    fontSize = 11.5.sp,
                                    color = if (isSelected) PureWhite else DarkForestGreen,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Security Note
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LockShieldSmall, contentDescription = null, tint = SoftOliveGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Access is revocable at any time. Links expire automatically.",
                        fontSize = 10.5.sp,
                        color = SoftOliveGreen
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFFD2C7B8))
                    ) {
                        Text("Cancel", fontSize = 13.sp, color = MutedCharcoal)
                    }

                    Button(
                        onClick = { if (recipient.isNotBlank()) onConfirmShare(recipient, purpose, accessDuration) },
                        enabled = recipient.isNotBlank(),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = VedAmritCtaGreen, contentColor = PureWhite)
                    ) {
                        Text("Share Securely", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
