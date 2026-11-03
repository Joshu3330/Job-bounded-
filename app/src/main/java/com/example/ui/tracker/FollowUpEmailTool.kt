package com.example.ui.tracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ai.AiTailorService
import com.example.ai.FollowUpEmailResult
import com.example.data.model.JobApplication
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun FollowUpEmailTool(
    application: JobApplication,
    aiService: AiTailorService = remember { AiTailorService() },
    onAppendNote: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val applicantName = Firebase.auth.currentUser?.displayName ?: "Candidate"

    var selectedStatus by remember { mutableStateOf(application.status) }
    var contactName by remember { mutableStateOf("") }
    var discussionHighlights by remember { mutableStateOf("") }
    val tones = listOf("Professional", "Warm & Enthusiastic", "Concise", "Executive")
    var selectedTone by remember { mutableStateOf("Professional") }

    var isGenerating by remember { mutableStateOf(false) }
    var emailResult by remember { mutableStateOf<FollowUpEmailResult?>(null) }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }

    val statusOptions = listOf("Applied", "Interviewing", "Offer", "Saved", "Rejected")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("follow_up_email_tool_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Follow-Up Email Generator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AI-tailored communication tailored to your application milestone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Application Status Selector
            Text(
                text = "Application Stage / Scenario:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { status ->
                    FilterChip(
                        selected = selectedStatus.equals(status, ignoreCase = true),
                        onClick = { selectedStatus = status },
                        label = {
                            Text(
                                when (status) {
                                    "Applied" -> "Status Check (Applied)"
                                    "Interviewing" -> "Post-Interview Thank You"
                                    "Offer" -> "Offer Discussion"
                                    "Saved" -> "Networking Outreach"
                                    "Rejected" -> "Stay in Touch"
                                    else -> status
                                }
                            )
                        },
                        leadingIcon = if (selectedStatus.equals(status, ignoreCase = true)) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inputs: Recruiter Name & Highlights
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Recruiter / Manager Name") },
                    placeholder = { Text("e.g. Sarah Jenkins") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("follow_up_contact_input"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = discussionHighlights,
                onValueChange = { discussionHighlights = it },
                label = { Text("Conversation Highlights or Topics to Mention") },
                placeholder = {
                    Text(
                        when (selectedStatus) {
                            "Interviewing" -> "e.g. Discussed mobile CI/CD automation & team scaling goals"
                            "Offer" -> "e.g. Reviewing benefits and confirmation of Q1 start date"
                            "Rejected" -> "e.g. Thanking for thoughtful feedback on technical challenge"
                            else -> "e.g. Reiterate relevant background in Jetpack Compose and cloud sync"
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("follow_up_highlights_input"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tone selector
            Text(
                text = "Tone:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tones.forEach { tone ->
                    FilterChip(
                        selected = selectedTone == tone,
                        onClick = { selectedTone = tone },
                        label = { Text(tone) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Generate Button
            Button(
                onClick = {
                    isGenerating = true
                    scope.launch {
                        val result = aiService.generateFollowUpEmail(
                            company = application.company,
                            jobTitle = application.jobTitle,
                            applicantName = applicantName,
                            status = selectedStatus,
                            contactName = contactName,
                            discussionHighlights = discussionHighlights,
                            desiredTone = selectedTone
                        )
                        emailResult = result
                        emailSubject = result.subject
                        emailBody = result.body
                        isGenerating = false
                        Toast.makeText(context, "Follow-up email generated!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_follow_up_email_button"),
                shape = RoundedCornerShape(10.dp),
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crafting Email with AI...")
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Tailored Email",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Results Display
            AnimatedVisibility(visible = emailSubject.isNotBlank() || emailBody.isNotBlank()) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Generated Email Template:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subject line field
                    OutlinedTextField(
                        value = emailSubject,
                        onValueChange = { emailSubject = it },
                        label = { Text("Subject Line") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("follow_up_subject_field"),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Subject", emailSubject))
                                Toast.makeText(context, "Subject copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Subject", modifier = Modifier.size(18.dp))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Email body field
                    OutlinedTextField(
                        value = emailBody,
                        onValueChange = { emailBody = it },
                        label = { Text("Email Message") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .testTag("follow_up_body_field"),
                        maxLines = 20
                    )

                    // Strategic Tips
                    if (emailResult?.strategicTips?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Strategic Delivery Advice:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                emailResult?.strategicTips?.forEach { tip ->
                                    Text(
                                        text = "• $tip",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Open in Email App, Copy All, Append to Notes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Open in Email Client
                        Button(
                            onClick = {
                                try {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                                        putExtra(Intent.EXTRA_TEXT, emailBody)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(emailIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_email_client_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open in Email", style = MaterialTheme.typography.labelSmall)
                        }

                        // Copy Entire Email
                        OutlinedButton(
                            onClick = {
                                val fullEmail = "Subject: $emailSubject\n\n$emailBody"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Follow Up Email", fullEmail))
                                Toast.makeText(context, "Copied full email to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_full_email_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy All", style = MaterialTheme.typography.labelSmall)
                        }

                        // Append to Application Notes
                        OutlinedButton(
                            onClick = {
                                val noteSnippet = "\n[Follow-up Email - $selectedStatus (${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())})]\nSubject: $emailSubject\n\n$emailBody\n"
                                onAppendNote(noteSnippet)
                                Toast.makeText(context, "Appended to Application Notes!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_email_to_notes_button")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
