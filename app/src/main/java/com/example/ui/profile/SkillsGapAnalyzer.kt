package com.example.ui.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AiTailorService
import com.example.ai.MissingSkillDetail
import com.example.ai.SkillsGapComparison
import com.example.data.model.JobApplication
import com.example.data.repository.FirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsGapAnalyzer(
    userSkills: String,
    userExperience: String,
    repository: FirestoreRepository,
    onAddSkillToProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = Firebase.auth.currentUser?.uid ?: ""
    val aiService = remember { AiTailorService() }

    val applications by repository.observeApplications(userId).collectAsState(initial = emptyList())

    var isExpanded by remember { mutableStateOf(true) }
    var inputMode by remember { mutableIntStateOf(0) } // 0 = Tracked App, 1 = Custom JD
    var selectedApp by remember { mutableStateOf<JobApplication?>(null) }

    var customJobTitle by remember { mutableStateOf("") }
    var customCompany by remember { mutableStateOf("") }
    var customJobDescription by remember { mutableStateOf("") }

    var isAnalyzing by remember { mutableStateOf(false) }
    var comparisonResult by remember { mutableStateOf<SkillsGapComparison?>(null) }
    var lastAnalyzedJobLabel by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("skills_gap_analyzer_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row with Expand/Collapse
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Skills Gap & Requirements Analyzer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Compare current skills vs target job & plan upskilling",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.testTag("toggle_skills_gap_expand")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Job Source Mode Toggle
                    Text(
                        text = "Target Job Description Source:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = inputMode == 0,
                            onClick = { inputMode = 0 },
                            label = { Text("From Tracked Jobs (${applications.size})") },
                            leadingIcon = if (inputMode == 0) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = inputMode == 1,
                            onClick = { inputMode = 1 },
                            label = { Text("Paste Custom JD") },
                            leadingIcon = if (inputMode == 1) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Content based on source
                    if (inputMode == 0) {
                        if (applications.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "No tracked job applications yet. Switch to 'Paste Custom JD' or search jobs in the Search tab to track applications.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Select an Application to Compare:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                applications.forEach { appItem ->
                                    val isSelected = selectedApp?.id == appItem.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedApp = appItem },
                                        label = { Text("${appItem.jobTitle} • ${appItem.company}") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Work,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Custom JD inputs
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customJobTitle,
                                onValueChange = { customJobTitle = it },
                                label = { Text("Job Title") },
                                placeholder = { Text("e.g. Senior Mobile Engineer") },
                                modifier = Modifier.weight(1f).testTag("custom_jd_title_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customCompany,
                                onValueChange = { customCompany = it },
                                label = { Text("Company") },
                                placeholder = { Text("e.g. TechCorp") },
                                modifier = Modifier.weight(1f).testTag("custom_jd_company_input"),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customJobDescription,
                            onValueChange = { customJobDescription = it },
                            label = { Text("Job Description & Requirements") },
                            placeholder = { Text("Paste the target role description, key responsibilities, and required qualifications...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("custom_jd_text_input"),
                            maxLines = 8
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Button: Run Comparison
                    Button(
                        onClick = {
                            val targetTitle: String
                            val targetCompany: String
                            val targetJd: String

                            if (inputMode == 0) {
                                if (selectedApp == null) {
                                    Toast.makeText(context, "Please select an application to compare", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                targetTitle = selectedApp?.jobTitle ?: ""
                                targetCompany = selectedApp?.company ?: ""
                                targetJd = selectedApp?.jobDescription ?: ""
                            } else {
                                if (customJobDescription.isBlank()) {
                                    Toast.makeText(context, "Please enter the job description", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                targetTitle = customJobTitle.ifBlank { "Target Role" }
                                targetCompany = customCompany.ifBlank { "Hiring Company" }
                                targetJd = customJobDescription
                            }

                            if (userSkills.isBlank() && userExperience.isBlank()) {
                                Toast.makeText(context, "Please enter your skills in your profile above first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isAnalyzing = true
                            lastAnalyzedJobLabel = "$targetTitle at $targetCompany"

                            scope.launch {
                                try {
                                    val result = aiService.compareSkillsGap(
                                        userSkills = userSkills,
                                        userExperience = userExperience,
                                        jobTitle = targetTitle,
                                        company = targetCompany,
                                        jobDescription = targetJd
                                    )
                                    comparisonResult = result
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Analysis error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isAnalyzing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_skills_gap_comparison_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAnalyzing
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing Skills Match (High Thinking)...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compare Skills Against Job Requirements")
                        }
                    }

                    // Comparison Results Display
                    comparisonResult?.let { result ->
                        Spacer(modifier = Modifier.height(18.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(14.dp))

                        // Match Score Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("skills_gap_result_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Skills Alignment Overview",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = lastAnalyzedJobLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Score badge
                                    val scoreColor = when {
                                        result.matchPercentage >= 75 -> Color(0xFF2E7D32)
                                        result.matchPercentage >= 50 -> Color(0xFFE65100)
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = scoreColor.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "${result.matchPercentage}% Match",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = scoreColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Progress bar
                                LinearProgressIndicator(
                                    progress = { result.matchPercentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    strokeCap = StrokeCap.Round
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${result.matchedSkills.size} Matched Skills",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = "${result.missingSkills.size} Skills Need Development",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = result.summaryAdvice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECTION 1: Matched Skills
                        Text(
                            text = "Matching Current Strengths (${result.matchedSkills.size}):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            result.matchedSkills.forEach { skill ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = skill,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // SECTION 2: Highlighted Missing Skills That Need Development
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Missing Skills That Need Development (${result.missingSkills.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "Requirements from the job posting missing from your profile. Follow the targeted upskilling actions to close each gap:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        result.missingSkills.forEachIndexed { index, missing ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("missing_skill_item_$index"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (missing.importance.equals("Critical", ignoreCase = true))
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                    else
                                        MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Skill Name and Badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = missing.skillName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // Category Chip
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = missing.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Importance Badge
                                            val badgeColor = when (missing.importance.lowercase()) {
                                                "critical" -> MaterialTheme.colorScheme.error
                                                "preferred" -> Color(0xFFE65100)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = badgeColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = missing.importance,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Why required: ${missing.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Actionable Development Pathway
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                            .padding(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "Development Action:",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = missing.developmentAction,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Quick Add to Profile button if user already has it
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                onAddSkillToProfile(missing.skillName)
                                                Toast.makeText(context, "Added '${missing.skillName}' to Profile Skills!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("add_skill_${index}_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Already have this? Add to Profile", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
