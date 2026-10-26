package com.example.vedaahar.agentic

import android.app.Activity
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.BuildConfig
import com.example.vedaahar.network.*
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive

@Composable
fun PatientDraftPdfActions(plan: WellnessPlanResponse?, downloadLabel: String = "Download Personalized Diet PDF", compact: Boolean = false) {
    if (!BuildConfig.AGENTIC_DRAFT_PREVIEW) return
    val context = LocalContext.current
    val owner = PatientSession.currentKey(context) ?: return
    val store = remember(owner) { PatientPdfStore(context.applicationContext, owner) }
    val downloads = remember(owner) { PublicPdfDownloads(context.applicationContext, owner) }
    val repository = remember(owner) {
        val tokens = AuthTokenStore(context.applicationContext)
        DraftPdfRepository(ApiClient.createAuthenticatedApi(BuildConfig.AGENTIC_API_BASE_URL, tokens,
            DraftPdfApi::class.java, owner)) { tokens.getAccessToken() }
    }
    var downloaded by remember(owner) { mutableStateOf<DownloadedDraft?>(null) }
    var exported by remember(owner) { mutableStateOf<PublicPdfExport?>(null) }
    var busy by remember(owner) { mutableStateOf(false) }
    var message by remember(owner) { mutableStateOf<String?>(null) }
    var pendingPicker by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val pending = runCatching { Gson().fromJson(pendingPicker, PendingPdfPicker::class.java) }.getOrNull()
        pendingPicker = null
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) {
            busy = false
            message = "Public save cancelled. The private PDF remains available in VedAmrit."
        } else {
            busy = true
            scope.launch {
                try {
                    check(pending != null && pending.ownerHash == reportOwnerHash(owner) && PatientSession.currentKey(context) == owner) {
                        "Patient changed or save request expired. Download again from the intended account."
                    }
                    val saved = withContext(Dispatchers.IO) {
                        downloads.saveToDocument(pending.record, store.readPdf(pending.record), pending.displayName, uri) {
                            coroutineContext.ensureActive()
                            store.assertCurrent(pending.record)
                        }
                    }
                    exported = saved
                    downloaded = pending.record
                    message = "Saved to the folder selected in the system picker."
                } catch (error: Exception) {
                    withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                        if (uri.scheme == "content" && DocumentsContract.isDocumentUri(context, uri)) {
                            runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                        }
                    }
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    message = "Public PDF could not be saved. ${error.message.orEmpty()} The private copy remains available."
                } finally { busy = false }
            }
        }
    }
    LaunchedEffect(owner, plan?.id) {
        val saved = withContext(Dispatchers.IO) { runCatching { store.latest() }.getOrNull() }
        val public = withContext(Dispatchers.IO) { runCatching { downloads.latest() }.getOrNull() }
        if (PatientSession.currentKey(context) == owner) { downloaded = saved; exported = public }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!compact) {
            Text("Personalized AI Draft PDF", style = MaterialTheme.typography.titleSmall)
            Text("AI-GENERATED DRAFT — NOT DOCTOR REVIEWED. This document is separate from the example PDFs and the approved final report.",
                style = MaterialTheme.typography.bodySmall)
        }
        Text("Exported PDFs in public Downloads are accessible outside VedAmrit and are no longer protected by app-level account isolation.",
            style = MaterialTheme.typography.bodySmall)
        Button(onClick = {
            if (busy) return@Button
            val selected = plan ?: return@Button
            busy = true
            message = "Downloading personalized PDF..."
            scope.launch {
                var waitingForPicker = false
                try {
                    when (val result = repository.download(selected.id)) {
                        is AgenticApiResult.Success -> {
                            val record = withContext(Dispatchers.IO) { store.save(selected, result.value) }
                            check(PatientSession.currentKey(context) == owner) { "Patient changed. Download again from the intended account." }
                            downloaded = record
                            if (publicPdfRoute(Build.VERSION.SDK_INT) == PublicPdfRoute.MediaStoreDownloads) {
                                exported = withContext(Dispatchers.IO) {
                                    downloads.saveToDownloads(record, result.value) {
                                        coroutineContext.ensureActive()
                                        store.assertCurrent(record)
                                    }
                                }
                                message = "Saved to Downloads/VedAmrit"
                            } else {
                                val name = newPdfExportName(record)
                                pendingPicker = Gson().toJson(PendingPdfPicker(reportOwnerHash(owner), record, name))
                                message = "Choose Downloads/VedAmrit in the system Save As picker."
                                picker.launch(createPdfDocumentIntent(name))
                                waitingForPicker = true
                            }
                        }
                        is AgenticApiResult.Error -> message = result.message
                    }
                } catch (error: Exception) {
                    pendingPicker = null
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    message = "Public PDF could not be saved. ${error.message.orEmpty()} Any saved private copy remains available."
                } finally { if (!waitingForPicker) busy = false }
            }
        }, enabled = !busy && plan?.draftPdfAvailable == true,
            colors = ButtonDefaults.buttonColors(containerColor = DarkForestGreen, contentColor = PureWhite),
            shape = RoundedCornerShape(if (compact) 16.dp else 50.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 56.dp else 48.dp).testTag("personalized-diet-download")) {
            Text(if (busy) "Saving PDF..." else downloadLabel)
        }
        if (plan != null && !plan.draftPdfAvailable) {
            Text(plan.draftPdfBlockReasons?.joinToString("\n")
                ?: "Refresh the plan to check PDF validation and development eligibility.",
                style = MaterialTheme.typography.bodySmall)
        }
        downloaded?.let { record ->
            val public = exported?.takeIf { it.record == record }
            Text("Saved draft: plan #${record.planId}, generated ${record.generatedAt ?: "date unavailable"}. A saved document is a dated snapshot; review current validation before following it.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                if (busy) return@OutlinedButton
                busy = true
                scope.launch {
                    try {
                        message = withContext(Dispatchers.IO) {
                            if (public != null) downloads.open(public) { store.assertCurrent(record) }
                            else store.open(record)
                        }
                    }
                    catch (error: Exception) {
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        message = error.message ?: "The saved PDF could not be opened."
                    } finally { busy = false }
                }
            }, enabled = !busy && !(plan?.id == record.planId && !plan.draftPdfAvailable),
                modifier = Modifier.fillMaxWidth()) { Text("Open PDF") }
            OutlinedButton(onClick = {
                if (busy || public == null) return@OutlinedButton
                busy = true
                scope.launch {
                    try { message = withContext(Dispatchers.IO) { downloads.share(public) { store.assertCurrent(record) } } }
                    catch (error: Exception) {
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        message = error.message ?: "The PDF could not be shared."
                    } finally { busy = false }
                }
            }, enabled = !busy && public != null && !(plan?.id == record.planId && !plan.draftPdfAvailable),
                modifier = Modifier.fillMaxWidth()) { Text("Share PDF") }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

