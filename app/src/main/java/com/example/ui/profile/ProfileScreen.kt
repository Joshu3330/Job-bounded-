package com.example.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: FirestoreRepository,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val userId = currentUser?.uid ?: ""

    var fullName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var phone by remember { mutableStateOf("") }
    var headline by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var workExperience by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var careerGoals by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Load initial profile from Firestore
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            val existing = repository.observeProfile(userId).firstOrNull()
            if (existing != null) {
                if (existing.fullName.isNotBlank()) fullName = existing.fullName
                if (existing.email.isNotBlank()) email = existing.email
                phone = existing.phone
                headline = existing.headline
                summary = existing.summary
                skills = existing.skills
                workExperience = existing.workExperience
                education = existing.education
                careerGoals = existing.careerGoals
            }
            isLoading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading candidate profile...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Screen Title & Pre-fill Shortcut
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Candidate Profile",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Your core experience used by AI to craft tailored applications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            fullName = if (fullName.isBlank()) "Alex Morgan" else fullName
                            email = if (email.isBlank()) "alex.morgan@example.com" else email
                            phone = "+1 (555) 389-2041"
                            headline = "Senior Full-Stack & Android Software Engineer"
                            summary = "Results-driven Software Engineer with 6+ years designing scalable cloud services and high-performance Android mobile apps. Proven track record reducing app latency by 35% and leading cross-functional teams to deliver enterprise products on time."
                            skills = "Kotlin, Jetpack Compose, Java, Coroutines & Flow, Android Architecture (MVVM/MVI), Room, Firebase (Firestore, Auth, AI Logic), REST APIs, GraphQL, Python, TypeScript, CI/CD, Unit Testing (JUnit, Robolectric), System Design, Agile Leadership"
                            workExperience = "• Senior Mobile Engineer @ TechCorp Solutions (2022 - Present):\n  - Architected modular Android app using Jetpack Compose, serving 2M+ monthly active users.\n  - Reduced cold start startup time by 42% through baseline profiles and lazy dependency loading.\n  - Mentored 5 junior engineers and standardized code review guidelines.\n\n• Software Engineer @ CloudApp Systems (2019 - 2022):\n  - Developed real-time telemetry syncing engine with offline caching and background WorkManager.\n  - Integrated third-party payment gateways and biometrics authentication, boosting checkout completion by 18%."
                            education = "• B.S. in Computer Science — University of Washington (2015 - 2019)\n• Google Associate Android Developer Certification (2021)"
                            careerGoals = "Seeking a Staff or Lead Mobile Engineering role in an innovative product company where I can architect high-scale mobile applications and drive AI-augmented user experiences."
                            Toast.makeText(context, "Loaded rich sample profile!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("load_sample_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pre-fill Sample", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Contact Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Contact Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name *") },
                            modifier = Modifier.fillMaxWidth().testTag("profile_full_name_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth().testTag("profile_email_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = headline,
                            onValueChange = { headline = it },
                            label = { Text("Professional Headline (e.g. Senior Mobile Engineer)") },
                            modifier = Modifier.fillMaxWidth().testTag("profile_headline_input"),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Summary & Career Goals
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Summary & Career Goals",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = summary,
                            onValueChange = { summary = it },
                            label = { Text("Professional Summary / Bio") },
                            modifier = Modifier.fillMaxWidth().height(110.dp).testTag("profile_summary_input"),
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = careerGoals,
                            onValueChange = { careerGoals = it },
                            label = { Text("Career Objectives & Target Roles") },
                            modifier = Modifier.fillMaxWidth().height(90.dp).testTag("profile_goals_input"),
                            maxLines = 4
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Skills & Competencies
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Skills & Expertise",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Comma-separated or bullet points of technical and soft skills",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = skills,
                            onValueChange = { skills = it },
                            label = { Text("Skills List") },
                            modifier = Modifier.fillMaxWidth().height(110.dp).testTag("profile_skills_input"),
                            maxLines = 5
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 4: Work Experience & Education
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Experience & Education",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = workExperience,
                            onValueChange = { workExperience = it },
                            label = { Text("Work Experience & Key Achievements") },
                            placeholder = { Text("Company, role, timeframe, bullet points of duties & wins") },
                            modifier = Modifier.fillMaxWidth().height(160.dp).testTag("profile_experience_input"),
                            maxLines = 8
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = education,
                            onValueChange = { education = it },
                            label = { Text("Education & Certifications") },
                            placeholder = { Text("Degrees, institutions, honors, graduation years") },
                            modifier = Modifier.fillMaxWidth().height(100.dp).testTag("profile_education_input"),
                            maxLines = 5
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 5: Skills Gap & Job Requirements Comparison Analyzer
                SkillsGapAnalyzer(
                    userSkills = skills,
                    userExperience = workExperience,
                    repository = repository,
                    onAddSkillToProfile = { newSkill ->
                        skills = if (skills.isBlank()) newSkill else "$skills, $newSkill"
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save Profile Button
                Button(
                    onClick = {
                        if (fullName.isBlank()) {
                            Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        scope.launch {
                            val profile = UserProfile(
                                id = "main",
                                userId = userId,
                                fullName = fullName,
                                email = email,
                                phone = phone,
                                headline = headline,
                                summary = summary,
                                skills = skills,
                                workExperience = workExperience,
                                education = education,
                                careerGoals = careerGoals
                            )
                            val res = repository.saveProfile(profile)
                            isSaving = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Profile saved to cloud!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to save: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
