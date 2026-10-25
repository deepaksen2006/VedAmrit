package com.example.vedaahar.assessment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.vedaahar.network.ApiClient
import com.example.vedaahar.network.AuthTokenStore
import com.example.vedaahar.network.PatientSession
import com.example.vedaahar.ui.theme.ForestGreen
import kotlinx.coroutines.launch

data class AssessmentSyncUi(val record: AssessmentSyncRecord?, val retry: () -> Unit)

@Composable
fun rememberAssessmentSync(onRestored: () -> Unit = {}): AssessmentSyncUi {
    val context = LocalContext.current.applicationContext
    val owner = remember { PatientSession.currentKey(context) }
    val store = remember(owner) { AssessmentSyncStore(context, owner) }
    val coordinator = remember(owner) {
        AssessmentSyncCoordinator(owner, { PatientSession.currentKey(context) }, store,
            AssessmentRepository(ApiClient.agenticApi(context), { AuthTokenStore(context).getAccessToken() }))
    }
    var record by remember(owner) { mutableStateOf(store.read()) }
    val latestOnRestored by rememberUpdatedState(onRestored)
    val scope = rememberCoroutineScope()
    DisposableEffect(store) {
        val unsubscribe = store.observe {
            val updated = store.read()
            record = updated
            if (updated?.status == AssessmentSyncStatus.SYNCED && PatientSession.currentKey(context) == owner) latestOnRestored()
        }
        onDispose { unsubscribe() }
    }
    LaunchedEffect(owner) {
        val saved = store.read()
        if (saved?.ready != true && saved?.editingComponents.isNullOrEmpty()) {
            AssessmentProfileStore.stageIfComplete(context, owner)
        }
        coordinator.retry()
    }
    LaunchedEffect(record?.submissionId, record?.ready) {
        if (record?.ready == true && record?.status == AssessmentSyncStatus.PENDING) coordinator.sync()
    }
    return AssessmentSyncUi(record) { scope.launch { coordinator.retry() } }
}

@Composable
fun AssessmentSyncSection(sync: AssessmentSyncUi) {
    Column(Modifier.fillMaxWidth()) {
        Text("Assessment sync: ${sync.record?.status?.name ?: "PENDING"}", color = ForestGreen, fontSize = 12.sp)
        Text(sync.record?.message ?: "Complete all three assessments, or restore your latest saved assessment.", fontSize = 11.sp)
        OutlinedButton(onClick = sync.retry, enabled = sync.record?.status != AssessmentSyncStatus.SYNCING) {
            Text(if (sync.record?.status == AssessmentSyncStatus.SYNCED) "Restore latest assessment" else "Retry sync / restore")
        }
    }
}
