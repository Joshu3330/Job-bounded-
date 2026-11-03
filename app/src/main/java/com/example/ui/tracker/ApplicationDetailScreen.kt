package com.example.ui.tracker

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.JobApplication
import com.example.data.repository.FirestoreRepository
import com.example.ui.tailor.DocumentViewer
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    initialApp: JobApplication,
    repository: FirestoreRepository,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = Firebase.auth.currentUser?.uid ?: ""

    var app by remember { mutableStateOf(initialApp) }
    var currentStatus by remember { mutableStateOf(app.status) }
    var notes by remember { mutableStateOf(app.notes) }
    var deadline by remember { mutableStateOf(app.deadline) }
    var tailoredResume by remember { mutableStateOf(app.tailoredResume) }
    var tailoredCoverLetter by remember { mutableStateOf(app.tailoredCoverLetter) }

    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val statusOptions = listOf("Saved", "Applied", "Interviewing", "Offer", "Rejected")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = app.company.ifBlank { "Application Details" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Application",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header Info
                Text(
                    text = app.jobTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${app.company} • ${app.location.ifBlank { "Location not specified" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (app.salary.isNotBlank()) {
                    Text(
                        text = "Salary: ${app.salary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Selector Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Application Status",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            statusOptions.forEach { status ->
                                FilterChip(
                                    selected = currentStatus == status,
                                    onClick = { currentStatus = status },
                                    label = { Text(status) },
                                    leadingIcon = if (currentStatus == status) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes & Deadlines Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Notes & Deadlines",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = deadline,
                            onValueChange = { deadline = it },
                            label = { Text("Next Milestone / Deadline") },
                            placeholder = { Text("e.g. Technical Interview on Nov 15th at 2 PM") },
                            modifier = Modifier.fillMaxWidth().testTag("app_deadline_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Application Notes / Contacts") },
                            placeholder = { Text("Recruiter name, interview feedback, questions prepared...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .testTag("app_notes_input"),
                            maxLines = 5
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save Changes Button
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            val updated = app.copy(
                                status = currentStatus,
                                notes = notes,
                                deadline = deadline,
                                tailoredResume = tailoredResume,
                                tailoredCoverLetter = tailoredCoverLetter
                            )
                            val res = repository.updateApplication(updated)
                            isSaving = false
                            if (res.isSuccess) {
                                app = updated
                                Toast.makeText(context, "Changes saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Save failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_app_changes_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Status & Notes")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Tailored Documents & Follow-Up Email Tabs
                Text(
                    text = "Application Materials & Communications",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Resume") },
                        icon = { Icon(Icons.Default.Description, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Cover Letter") },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Follow-Up Email") },
                        icon = { Icon(Icons.Default.Email, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        DocumentViewer(
                            documentType = "Resume",
                            title = "${app.jobTitle} @ ${app.company}",
                            content = tailoredResume,
                            onContentChanged = { tailoredResume = it },
                            applicantName = Firebase.auth.currentUser?.displayName ?: "Candidate",
                            applicantContact = Firebase.auth.currentUser?.email ?: "",
                            testTagPrefix = "detail_resume"
                        )
                    }
                    1 -> {
                        DocumentViewer(
                            documentType = "Cover_Letter",
                            title = "${app.jobTitle} @ ${app.company}",
                            content = tailoredCoverLetter,
                            onContentChanged = { tailoredCoverLetter = it },
                            applicantName = Firebase.auth.currentUser?.displayName ?: "Candidate",
                            applicantContact = Firebase.auth.currentUser?.email ?: "",
                            testTagPrefix = "detail_cover_letter"
                        )
                    }
                    2 -> {
                        FollowUpEmailTool(
                            application = app.copy(status = currentStatus),
                            onAppendNote = { newNote ->
                                notes = if (notes.isBlank()) newNote.trim() else "$notes\n$newNote"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Application") },
            text = { Text("Are you sure you want to delete this job application? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            repository.deleteApplication(userId, app.id)
                            Toast.makeText(context, "Application deleted", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
