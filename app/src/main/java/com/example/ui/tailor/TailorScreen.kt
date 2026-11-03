package com.example.ui.tailor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiTailorService
import com.example.ai.GoldfishAuditReport
import com.example.ai.MatchAnalysisResult
import com.example.data.model.JobApplication
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.JobPosting
import com.example.util.PdfExporter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun TailorScreen(
    repository: FirestoreRepository,
    aiService: AiTailorService,
    initialJobPosting: JobPosting? = null,
    onApplicationSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = Firebase.auth.currentUser?.uid ?: ""

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isProfileLoading by remember { mutableStateOf(true) }

    // Form inputs
    var companyName by remember { mutableStateOf(initialJobPosting?.company ?: "") }
    var jobTitle by remember { mutableStateOf(initialJobPosting?.title ?: "") }
    var location by remember { mutableStateOf(initialJobPosting?.location ?: "") }
    var salary by remember { mutableStateOf(initialJobPosting?.salary ?: "") }
    var jobDescription by remember { mutableStateOf(initialJobPosting?.description ?: "") }

    LaunchedEffect(initialJobPosting) {
        if (initialJobPosting != null) {
            companyName = initialJobPosting.company
            jobTitle = initialJobPosting.title
            location = initialJobPosting.location
            salary = initialJobPosting.salary
            jobDescription = initialJobPosting.description
        }
    }

    // Customization inputs
    val availableTones = listOf("Professional", "Creative", "Concise", "Executive", "Technical")
    var selectedTone by remember { mutableStateOf("Professional") }
    var emphasizedSkills by remember { mutableStateOf("") }

    // Generation states
    var isThinking by remember { mutableStateOf(false) }
    var thinkingStep by remember { mutableStateOf("") }
    var matchResult by remember { mutableStateOf<MatchAnalysisResult?>(null) }
    var generatedResume by remember { mutableStateOf("") }
    var generatedCoverLetter by remember { mutableStateOf("") }
    var generationError by remember { mutableStateOf<String?>(null) }
    var useEgmReview by remember { mutableStateOf(true) }
    var goldfishReport by remember { mutableStateOf<GoldfishAuditReport?>(null) }

    // Active result tab: 0 -> Resume, 1 -> Cover Letter, 2 -> Match Analysis, 3 -> EGM Audit
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSavingApp by remember { mutableStateOf(false) }

    // Load profile
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            profile = repository.observeProfile(userId).firstOrNull()
            isProfileLoading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tailor Resume & Cover Letter",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Paste a job description to generate targeted application materials with High-Thinking AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = {
                        companyName = "Apex Robotics"
                        jobTitle = "Senior Mobile Engineer"
                        location = "San Francisco, CA (Hybrid)"
                        salary = "$160,000 - $190,000"
                        jobDescription = """
We are seeking a Senior Mobile Engineer to lead development of our flagship robot command mobile app.

Responsibilities:
- Build modern, responsive UI with Jetpack Compose and reactive state management (Flow/Coroutines).
- Integrate low-latency real-time telemetry streaming and offline data caching.
- Optimize app launch performance and battery consumption across various Android devices.
- Collaborate with hardware and backend teams to implement secure IoT device communication.

Requirements:
- 5+ years of production Android development with Kotlin.
- Deep expertise in Jetpack Compose, Clean Architecture, and Room / Cloud databases.
- Demonstrated experience building high-performance or real-time connected mobile experiences.
- Strong communication and cross-functional leadership skills.
                        """.trimIndent()
                        emphasizedSkills = "Jetpack Compose architecture, telemetry caching, cold start optimization"
                        selectedTone = "Executive"
                        Toast.makeText(context, "Loaded sample job posting!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("load_sample_job_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sample Job", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Candidate Profile Check Warning
            if (!isProfileLoading && (profile == null || profile?.workExperience.isNullOrBlank())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tip: Fill out your Work Experience and Skills in the 'Profile' tab for the best tailored results!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Step 1: Job Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Target Job Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company Name *") },
                            modifier = Modifier.weight(1f).testTag("tailor_company_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = jobTitle,
                            onValueChange = { jobTitle = it },
                            label = { Text("Job Title *") },
                            modifier = Modifier.weight(1f).testTag("tailor_job_title_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location (e.g. Remote)") },
                            modifier = Modifier.weight(1f).testTag("tailor_location_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = salary,
                            onValueChange = { salary = it },
                            label = { Text("Salary / Rate") },
                            modifier = Modifier.weight(1f).testTag("tailor_salary_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        label = { Text("Job Description provided by hiring party *") },
                        placeholder = { Text("Paste the full job description or requirements...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("tailor_job_description_input"),
                        maxLines = 10
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Customization Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Tailoring Tone & Focus",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select tone:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Tone chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTones.forEach { tone ->
                            FilterChip(
                                selected = selectedTone == tone,
                                onClick = { selectedTone = tone },
                                label = { Text(tone) },
                                leadingIcon = if (selectedTone == tone) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = emphasizedSkills,
                        onValueChange = { emphasizedSkills = it },
                        label = { Text("Key Achievements or Skills to Emphasize") },
                        placeholder = { Text("e.g. Jetpack Compose UI, CI/CD pipelines, team mentorship") },
                        modifier = Modifier.fillMaxWidth().testTag("tailor_emphasize_input"),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // EGM Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Elephant-Goldfish Review (EGM)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Deep-context drafting (Elephant) + Zero-context 6s cold ATS review (Goldfish) + Stress-tested refinement",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = useEgmReview,
                            onCheckedChange = { useEgmReview = it },
                            modifier = Modifier.testTag("egm_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action: Generate with High Thinking AI
            Button(
                onClick = {
                    if (companyName.isBlank() || jobTitle.isBlank() || jobDescription.isBlank()) {
                        Toast.makeText(context, "Please fill in Company, Job Title, and Job Description", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isThinking = true
                    generationError = null
                    scope.launch {
                        try {
                            val candidateProfile = profile ?: UserProfile(
                                fullName = Firebase.auth.currentUser?.displayName ?: "Candidate",
                                email = Firebase.auth.currentUser?.email ?: ""
                            )

                            thinkingStep = "Pass 1 (Elephant): High-Thinking Reasoning & Job Match Analysis..."
                            val analysis = aiService.analyzeJobMatch(
                                profile = candidateProfile,
                                jobTitle = jobTitle,
                                company = companyName,
                                jobDescription = jobDescription
                            )
                            matchResult = analysis

                            thinkingStep = "Pass 1 (Elephant): Deep-context tailoring ($selectedTone tone)..."
                            val initialResume = aiService.tailorResume(
                                profile = candidateProfile,
                                jobTitle = jobTitle,
                                company = companyName,
                                jobDescription = jobDescription,
                                desiredTone = selectedTone,
                                emphasizedSkills = emphasizedSkills
                            )

                            if (useEgmReview) {
                                thinkingStep = "Pass 2 (Goldfish): Zero-context 6-second ATS stress test..."
                                val audit = aiService.runGoldfishReview(
                                    draftResume = initialResume,
                                    jobDescription = jobDescription
                                )
                                goldfishReport = audit

                                thinkingStep = "Pass 3 (Synthesis): Applying Goldfish audit improvements..."
                                val refined = aiService.refineWithEgmFeedback(
                                    draftResume = initialResume,
                                    auditReport = audit,
                                    jobTitle = jobTitle,
                                    company = companyName,
                                    jobDescription = jobDescription
                                )
                                generatedResume = refined
                            } else {
                                generatedResume = initialResume
                            }

                            thinkingStep = "Pass 1 (Elephant): Crafting tailored cover letter..."
                            val coverLetter = aiService.tailorCoverLetter(
                                profile = candidateProfile,
                                jobTitle = jobTitle,
                                company = companyName,
                                jobDescription = jobDescription,
                                desiredTone = selectedTone,
                                emphasizedSkills = emphasizedSkills
                            )
                            generatedCoverLetter = coverLetter

                            isThinking = false
                            thinkingStep = ""
                            selectedTab = 0
                            Toast.makeText(context, "Documents tailored with EGM successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            isThinking = false
                            generationError = e.localizedMessage ?: "Failed to tailor documents."
                            Toast.makeText(context, "Tailoring error: $generationError", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_tailored_docs_button"),
                shape = RoundedCornerShape(14.dp),
                enabled = !isThinking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isThinking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Thinking (Gemini 4 Argon)...", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tailor with High Thinking AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Thinking Status Banner
            AnimatedVisibility(visible = isThinking) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = thinkingStep,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (generationError != null) {
                Text(
                    text = "Error: $generationError",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Results Section
            if (matchResult != null || generatedResume.isNotBlank() || generatedCoverLetter.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Score Card
                if (matchResult != null) {
                    val score = matchResult?.score ?: 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            score >= 80 -> MaterialTheme.colorScheme.tertiary
                                            score >= 60 -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$score%",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Candidate Match Score",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = matchResult?.summary ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Tabs for Switch between Resume, Cover Letter, Match Analysis, EGM Audit
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    edgePadding = 0.dp
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
                        text = { Text("Match Analysis") },
                        icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) }
                    )
                    if (goldfishReport != null) {
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("EGM Audit") },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Tailored Resume Tab
                        DocumentViewer(
                            documentType = "Resume",
                            title = "$jobTitle @ $companyName",
                            content = generatedResume,
                            onContentChanged = { generatedResume = it },
                            applicantName = profile?.fullName ?: Firebase.auth.currentUser?.displayName ?: "Candidate",
                            applicantContact = "${profile?.email ?: ""} • ${profile?.phone ?: ""}",
                            testTagPrefix = "resume"
                        )
                    }
                    1 -> {
                        // Tailored Cover Letter Tab
                        DocumentViewer(
                            documentType = "Cover_Letter",
                            title = "$jobTitle @ $companyName",
                            content = generatedCoverLetter,
                            onContentChanged = { generatedCoverLetter = it },
                            applicantName = profile?.fullName ?: Firebase.auth.currentUser?.displayName ?: "Candidate",
                            applicantContact = "${profile?.email ?: ""} • ${profile?.phone ?: ""}",
                            testTagPrefix = "cover_letter"
                        )
                    }
                    2 -> {
                        // Match Analysis Details
                        MatchAnalysisDetails(result = matchResult)
                    }
                    3 -> {
                        // EGM Audit Report Details
                        EgmAuditCard(report = goldfishReport)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save to Job Application Tracker
                Button(
                    onClick = {
                        isSavingApp = true
                        scope.launch {
                            val newApp = JobApplication(
                                userId = userId,
                                company = companyName,
                                jobTitle = jobTitle,
                                jobDescription = jobDescription,
                                location = location,
                                salary = salary,
                                status = "Applied",
                                applicationDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                                desiredTone = selectedTone,
                                emphasizedSkills = emphasizedSkills,
                                matchScore = (matchResult?.score ?: 75).toLong(),
                                matchAnalysis = matchResult?.summary ?: "",
                                tailoredResume = generatedResume,
                                tailoredCoverLetter = generatedCoverLetter,
                                notes = "Tailored with High-Thinking AI ($selectedTone tone)."
                            )
                            val res = repository.saveApplication(newApp)
                            isSavingApp = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Saved to Application Tracker!", Toast.LENGTH_SHORT).show()
                                onApplicationSaved()
                            } else {
                                Toast.makeText(context, "Save failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_to_tracker_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    enabled = !isSavingApp
                ) {
                    if (isSavingApp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onTertiary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save to Application Tracker",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DocumentViewer(
    documentType: String,
    title: String,
    content: String,
    onContentChanged: (String) -> Unit,
    applicantName: String,
    applicantContact: String,
    testTagPrefix: String
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Action bar: Edit, Copy, Export PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = documentType.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Copy to clipboard
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(documentType, content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied $documentType to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("${testTagPrefix}_copy_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }

                    // Toggle Edit
                    OutlinedButton(
                        onClick = { isEditing = !isEditing },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("${testTagPrefix}_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditing) "Done" else "Edit", style = MaterialTheme.typography.labelSmall)
                    }

                    // Export to PDF Button (User prompt requirement)
                    Button(
                        onClick = {
                            PdfExporter.exportAndSharePdf(
                                context = context,
                                title = title,
                                documentType = documentType,
                                applicantName = applicantName,
                                applicantContact = applicantContact,
                                content = content
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("${testTagPrefix}_export_pdf_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export PDF", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export PDF", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .testTag("${testTagPrefix}_content_editor"),
                    maxLines = 50
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(14.dp)
                ) {
                    Text(
                        text = content.ifBlank { "No content generated yet." },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MatchAnalysisDetails(result: MatchAnalysisResult?) {
    if (result == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Key Strengths Matching the Role",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            result.matchingStrengths.forEach { strength ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strength, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Identified Skill & Experience Gaps",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            result.skillGaps.forEach { gap ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = gap, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Interview Strategy Recommendations",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            result.interviewTips.forEach { tip ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = tip, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun EgmAuditCard(report: GoldfishAuditReport?) {
    if (report == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Elephant-Goldfish (EGM) Audit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Zero-context adversarial ATS & 6-second recruiter critique",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${report.atsScore}% ATS Score",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Recruiter's 6-Second First Impression:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = report.sixSecondImpression,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            Divider()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Blindspots & Gaps Caught by Goldfish Review:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            report.detectedBlindspots.forEach { spot ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = spot, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Stress-Tested Refinements Applied in Final Synthesis:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            report.improvementsMade.forEach { fix ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = fix, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

