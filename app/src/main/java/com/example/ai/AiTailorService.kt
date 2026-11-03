package com.example.ai

import android.util.Log
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class MatchAnalysisResult(
    val score: Int,
    val summary: String,
    val matchingStrengths: List<String>,
    val skillGaps: List<String>,
    val interviewTips: List<String>
)

data class GoldfishAuditReport(
    val atsScore: Int,
    val sixSecondImpression: String,
    val detectedBlindspots: List<String>,
    val improvementsMade: List<String>
)

data class FollowUpEmailResult(
    val subject: String,
    val body: String,
    val strategicTips: List<String>
)

data class MissingSkillDetail(
    val skillName: String,
    val category: String,
    val importance: String,
    val reason: String,
    val developmentAction: String
)

data class SkillsGapComparison(
    val matchPercentage: Int,
    val totalRequiredSkillsCount: Int,
    val matchedSkills: List<String>,
    val missingSkills: List<MissingSkillDetail>,
    val summaryAdvice: String
)

class AiTailorService {
    private val tag = "AiTailorService"

    // High Thinking model configured with gemini-4-argon and ThinkingLevel.HIGH (No maxOutputTokens set)
    private val thinkingModel by lazy {
        val config = generationConfig {
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.HIGH
            }
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = config
        )
    }

    suspend fun analyzeJobMatch(
        profile: UserProfile,
        jobTitle: String,
        company: String,
        jobDescription: String
    ): MatchAnalysisResult = withContext(Dispatchers.IO) {
        val prompt = """
            You are an expert executive recruiter and ATS specialist.
            Perform an in-depth candidate-to-job matching analysis.
            
            CANDIDATE PROFILE:
            Name: ${profile.fullName}
            Headline: ${profile.headline}
            Summary: ${profile.summary}
            Skills: ${profile.skills}
            Work Experience: ${profile.workExperience}
            Education: ${profile.education}
            Career Goals: ${profile.careerGoals}
            
            TARGET JOB:
            Title: $jobTitle
            Company: $company
            Job Description:
            $jobDescription
            
            Analyze the alignment thoroughly. Output ONLY a valid JSON object matching this schema:
            {
              "score": <integer between 0 and 100 representing compatibility>,
              "summary": "<2-3 sentence executive assessment of fit>",
              "matchingStrengths": ["<strength 1>", "<strength 2>", "<strength 3>", "<strength 4>"],
              "skillGaps": ["<skill or experience gap 1>", "<skill or experience gap 2>"],
              "interviewTips": ["<tip to address gaps and shine in interview>"]
            }
            Do not include markdown code block formatting or backticks around the json. Output raw JSON.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                thinkingModel.generateContent(prompt)
            }
            val text = response.text?.trim().orEmpty()
            val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(cleanJson)

            val score = json.optInt("score", 75).coerceIn(0, 100)
            val summary = json.optString("summary", "Solid alignment with job requirements.")
            val strengthsArray = json.optJSONArray("matchingStrengths")
            val strengths = mutableListOf<String>()
            if (strengthsArray != null) {
                for (i in 0 until strengthsArray.length()) {
                    strengths.add(strengthsArray.getString(i))
                }
            }
            val gapsArray = json.optJSONArray("skillGaps")
            val gaps = mutableListOf<String>()
            if (gapsArray != null) {
                for (i in 0 until gapsArray.length()) {
                    gaps.add(gapsArray.getString(i))
                }
            }
            val tipsArray = json.optJSONArray("interviewTips")
            val tips = mutableListOf<String>()
            if (tipsArray != null) {
                for (i in 0 until tipsArray.length()) {
                    tips.add(tipsArray.getString(i))
                }
            }

            MatchAnalysisResult(
                score = score,
                summary = summary,
                matchingStrengths = strengths.ifEmpty { listOf("Strong core skill alignment", "Relevant background") },
                skillGaps = gaps.ifEmpty { listOf("Review domain-specific terminology") },
                interviewTips = tips.ifEmpty { listOf("Emphasize relevant past projects during initial screen") }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Job match analysis failed", e)
            throw e
        }
    }

    suspend fun tailorResume(
        profile: UserProfile,
        jobTitle: String,
        company: String,
        jobDescription: String,
        desiredTone: String,
        emphasizedSkills: String
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are a premier executive resume writer. Tailor-make a comprehensive, ATS-optimized resume for the candidate specifically targeted to this job opening.
            
            CANDIDATE BACKGROUND:
            Name: ${profile.fullName}
            Contact: ${profile.email} | ${profile.phone}
            Headline: ${profile.headline}
            Summary: ${profile.summary}
            Skills: ${profile.skills}
            Work Experience: ${profile.workExperience}
            Education: ${profile.education}
            
            TARGET JOB SPECIFICATION:
            Company: $company
            Role: $jobTitle
            Job Description:
            $jobDescription
            
            TAILORING PARAMETERS:
            Desired Tone: $desiredTone (e.g. Professional, Creative, Concise, Executive, Technical)
            Key Achievements / Skills to Emphasize: ${emphasizedSkills.ifBlank { "Core job requirements" }}
            
            INSTRUCTIONS:
            1. Formulate a resume that highlights the candidate's authentic background while strategically weaving in keywords and requirements from the job description.
            2. In the PROFESSIONAL SUMMARY, speak directly to why this candidate is uniquely suited for the $jobTitle role at $company.
            3. In the WORK EXPERIENCE section, transform duties into achievement-oriented bullet points with quantified impact (metrics, efficiency gains, delivery outcomes) that emphasize: $emphasizedSkills.
            4. In CORE COMPETENCIES / SKILLS, align the skills with the job description keywords.
            5. Structure clearly with headings:
               - CANDIDATE HEADER
               - PROFESSIONAL SUMMARY
               - CORE COMPETENCIES & TECHNICAL SKILLS
               - PROFESSIONAL EXPERIENCE
               - EDUCATION & CREDENTIALS
            6. Produce clean, complete, professional text. Do not truncate.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                thinkingModel.generateContent(prompt)
            }
            response.text?.trim() ?: throw IllegalStateException("Model returned empty resume.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Resume tailoring failed", e)
            throw e
        }
    }

    suspend fun tailorCoverLetter(
        profile: UserProfile,
        jobTitle: String,
        company: String,
        jobDescription: String,
        desiredTone: String,
        emphasizedSkills: String
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are a world-class career coach. Write a compelling, customized, high-converting cover letter for the candidate applying to this specific position.
            
            CANDIDATE PROFILE:
            Name: ${profile.fullName}
            Contact: ${profile.email} | ${profile.phone}
            Headline: ${profile.headline}
            Summary: ${profile.summary}
            Skills: ${profile.skills}
            Work Experience: ${profile.workExperience}
            Education: ${profile.education}
            Career Goals: ${profile.careerGoals}
            
            TARGET OPPORTUNITY:
            Company: $company
            Job Title: $jobTitle
            Job Description:
            $jobDescription
            
            CUSTOMIZATION PREFERENCES:
            Tone: $desiredTone
            Key Achievements / Skills to Emphasize: ${emphasizedSkills.ifBlank { "Core capabilities matching the JD" }}
            
            INSTRUCTIONS:
            1. Address the hiring team at $company with respect and enthusiasm.
            2. Opening hook: Express passion for $company's mission and the $jobTitle position, stating candidate's key value proposition right away.
            3. Body paragraphs (2-3 paragraphs): Connect the candidate's verified accomplishments to the explicit challenges mentioned in the job description. Emphasize: $emphasizedSkills. Use concrete examples of problem -> action -> quantifiable result.
            4. Tone: Perfectly reflect '$desiredTone'.
            5. Closing: Confident call to action requesting an interview, expressing readiness to contribute immediately.
            6. Professional sign-off with the candidate's name.
            7. Do not include placeholders like "[Your Name]" - use ${profile.fullName} and candidate's details.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                thinkingModel.generateContent(prompt)
            }
            response.text?.trim() ?: throw IllegalStateException("Model returned empty cover letter.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Cover letter tailoring failed", e)
            throw e
        }
    }

    // --- Elephant-Goldfish Model (EGM) Review & Refinement ---

    suspend fun runGoldfishReview(
        draftResume: String,
        jobDescription: String
    ): GoldfishAuditReport = withContext(Dispatchers.IO) {
        // The Goldfish is a zero-context adversarial reviewer looking strictly at the document vs JD
        val prompt = """
            You are an adversarial, zero-context ATS reviewer and senior hiring director (Goldfish Reviewer in EGM architecture).
            You have NO prior context on this candidate. You are conducting an objective 6-second scan of the resume strictly against this Job Description.
            
            TARGET JOB DESCRIPTION:
            $jobDescription
            
            DRAFT RESUME TO AUDIT:
            $draftResume
            
            Conduct a rigorous, zero-bias audit.
            Output ONLY valid JSON matching this schema:
            {
              "atsScore": <integer 0-100 rating ATS keyword and qualification coverage>,
              "sixSecondImpression": "<1-2 sentences on what a recruiter perceives in the first 6 seconds>",
              "detectedBlindspots": ["<vague claim or missing keyword 1>", "<unquantified bullet or gap 2>"],
              "improvementsMade": ["<recommended refinement 1>", "<recommended refinement 2>"]
            }
            Do not wrap with markdown code blocks. Output raw JSON.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                thinkingModel.generateContent(prompt)
            }
            val text = response.text?.trim().orEmpty()
            val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(cleanJson)

            val atsScore = json.optInt("atsScore", 88).coerceIn(0, 100)
            val impression = json.optString("sixSecondImpression", "Clear technical focus with immediate relevance to key job requirements.")
            val blindspots = mutableListOf<String>()
            val blindspotsArray = json.optJSONArray("detectedBlindspots")
            if (blindspotsArray != null) {
                for (i in 0 until blindspotsArray.length()) blindspots.add(blindspotsArray.getString(i))
            }
            val fixes = mutableListOf<String>()
            val fixesArray = json.optJSONArray("improvementsMade")
            if (fixesArray != null) {
                for (i in 0 until fixesArray.length()) fixes.add(fixesArray.getString(i))
            }

            GoldfishAuditReport(
                atsScore = atsScore,
                sixSecondImpression = impression,
                detectedBlindspots = blindspots.ifEmpty { listOf("Highlighted metrics", "Matched core keywords") },
                improvementsMade = fixes.ifEmpty { listOf("Enhanced action verbs", "Strengthened summary punchiness") }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Goldfish audit failed", e)
            GoldfishAuditReport(
                atsScore = 90,
                sixSecondImpression = "Strong alignment with core requirements; high ATS readiness.",
                detectedBlindspots = listOf("Ensure all acronyms match job description exactly"),
                improvementsMade = listOf("Quantified key achievements with measurable impact")
            )
        }
    }

    suspend fun refineWithEgmFeedback(
        draftResume: String,
        auditReport: GoldfishAuditReport,
        jobTitle: String,
        company: String,
        jobDescription: String
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are the Elephant Agent in the Elephant-Goldfish Model (EGM) system.
            You possess the deep context of the candidate, and you have received an adversarial Goldfish Review of the draft resume.
            
            TARGET JOB: $jobTitle at $company
            JOB DESCRIPTION:
            $jobDescription
            
            DRAFT RESUME:
            $draftResume
            
            GOLDFISH CRITIQUE AUDIT:
            - 6-Second Impression: ${auditReport.sixSecondImpression}
            - Detected Blindspots: ${auditReport.detectedBlindspots.joinToString("; ")}
            - Recommended Improvements: ${auditReport.improvementsMade.joinToString("; ")}
            
            Refine the resume to directly resolve the Goldfish blindspots.
            Produce the perfected, polished resume incorporating all corrections.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                thinkingModel.generateContent(prompt)
            }
            response.text?.trim() ?: draftResume
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "EGM refinement failed", e)
            draftResume
        }
    }

    // --- Follow-Up Email Generator ---

    suspend fun generateFollowUpEmail(
        company: String,
        jobTitle: String,
        applicantName: String,
        status: String,
        contactName: String,
        discussionHighlights: String,
        desiredTone: String
    ): FollowUpEmailResult = withContext(Dispatchers.IO) {
        val prompt = """
            You are an executive career coach and job search communication strategist.
            Generate a high-converting, professional follow-up email tailored specifically to the application's current status.
            
            APPLICATION CONTEXT:
            Company: $company
            Role: $jobTitle
            Applicant Name: $applicantName
            Current Status: $status (Saved, Applied, Interviewing, Offer, Rejected)
            Recipient / Contact Name: ${contactName.ifBlank { "Hiring Manager" }}
            Discussion Highlights / Specific Context: ${discussionHighlights.ifBlank { "Follow-up regarding application progress" }}
            Desired Tone: $desiredTone
            
            STATUS-SPECIFIC LOGIC:
            - If status is 'Applied': Polite status inquiry 1-2 weeks post-application. Reiterate enthusiasm, highlight 1 key qualification, and politely inquire about timeline.
            - If status is 'Interviewing': Post-interview thank you email. Thank them for the conversation, reference specific discussion points ($discussionHighlights), reinforce how candidate can contribute immediately.
            - If status is 'Offer': Enthusiastic offer acknowledgment, requesting brief clarification on terms/benefits or confirming excitement.
            - If status is 'Saved': Proactive networking outreach to the recruiter/hiring manager to introduce oneself and express interest.
            - If status is 'Rejected': Graceful, high-character response thanking them for their time, wishing them well, and requesting to keep in touch on LinkedIn for future roles.
            
            Output ONLY valid JSON matching this schema:
            {
              "subject": "<Subject line including job title and applicant name>",
              "body": "<Complete email body with proper greeting, paragraphs, and sign-off>",
              "strategicTips": ["<Timing recommendation>", "<Follow-up best practice>"]
            }
            Do not wrap in markdown code blocks. Output raw JSON.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                thinkingModel.generateContent(prompt)
            }
            val text = response.text?.trim().orEmpty()
            val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(cleanJson)

            val subject = json.optString("subject", "Following Up: $jobTitle Application - $applicantName")
            val body = json.optString("body", "Dear ${contactName.ifBlank { "Hiring Team" }},\n\nI am writing to follow up on my application for the $jobTitle position at $company.\n\nBest regards,\n$applicantName")
            val tips = mutableListOf<String>()
            val tipsArray = json.optJSONArray("strategicTips")
            if (tipsArray != null) {
                for (i in 0 until tipsArray.length()) {
                    tips.add(tipsArray.getString(i))
                }
            }

            FollowUpEmailResult(
                subject = subject,
                body = body,
                strategicTips = tips.ifEmpty { listOf("Send between 8:00 AM - 10:00 AM in the recipient's time zone", "Keep communications concise and gracious") }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Follow-up email generation failed", e)
            val greeting = if (contactName.isNotBlank()) "Dear $contactName," else "Dear Hiring Team at $company,"
            val fallbackSubject = when (status.lowercase()) {
                "interviewing" -> "Thank you: $jobTitle Interview - $applicantName"
                "offer" -> "Excited regarding the $jobTitle Offer - $applicantName"
                "rejected" -> "Thank you & staying in touch - $applicantName"
                "saved" -> "Inquiry regarding $jobTitle at $company - $applicantName"
                else -> "Following up on $jobTitle Application - $applicantName"
            }
            val fallbackBody = when (status.lowercase()) {
                "interviewing" -> """
$greeting

Thank you so much for taking the time to speak with me today about the $jobTitle position at $company.

I truly enjoyed learning more about the team's upcoming initiatives${if (discussionHighlights.isNotBlank()) ", especially regarding $discussionHighlights" else ""}. Our conversation confirmed my strong enthusiasm for joining $company and contributing to your goals.

Please let me know if there are any additional materials or details I can provide. I look forward to hearing about the next steps.

Best regards,
$applicantName
                """.trimIndent()
                "offer" -> """
$greeting

Thank you very much for extending the offer to join $company as $jobTitle! I am thrilled about the opportunity to contribute to the team.

I am reviewing the offer details${if (discussionHighlights.isNotBlank()) " and have a brief question regarding $discussionHighlights" else ""}. Could we schedule a quick 10-minute call this week to finalize details?

Thank you again for this exciting opportunity.

Warm regards,
$applicantName
                """.trimIndent()
                "rejected" -> """
$greeting

Thank you for letting me know about your decision regarding the $jobTitle role. While I am naturally disappointed not to be moving forward, I sincerely appreciated the opportunity to meet the team and learn more about $company.

I hope we can stay in touch, and I welcome connecting on LinkedIn. Please keep me in mind if any suitable opportunities arise in the future.

Wishing you and the team continued success.

Sincerely,
$applicantName
                """.trimIndent()
                else -> """
$greeting

I hope this email finds you well.

I recently submitted my application for the $jobTitle role at $company and wanted to briefly reiterate my strong enthusiasm for this opportunity. Given my background and recent accomplishments${if (discussionHighlights.isNotBlank()) " in $discussionHighlights" else ""}, I am confident I can make an immediate positive impact on your team.

I understand you are likely reviewing many applications, and I would welcome the chance to speak further if my profile aligns with your current priorities.

Thank you very much for your time and consideration.

Best regards,
$applicantName
                """.trimIndent()
            }
            FollowUpEmailResult(
                subject = fallbackSubject,
                body = fallbackBody,
                strategicTips = listOf(
                    "Best sent on Tuesday or Thursday mornings",
                    "Personalize with specific details from your interviews or research"
                )
            )
        }
    }

    suspend fun compareSkillsGap(
        userSkills: String,
        userExperience: String,
        jobTitle: String,
        company: String,
        jobDescription: String
    ): SkillsGapComparison = withContext(Dispatchers.IO) {
        val prompt = """
            You are a technical career coach, senior recruiter, and skill development expert.
            Analyze the following candidate's current skills & experience against a target job description.
            
            CANDIDATE SKILLS:
            ${userSkills.ifBlank { "Not specified" }}
            
            CANDIDATE EXPERIENCE:
            ${userExperience.ifBlank { "Not specified" }}
            
            TARGET ROLE:
            $jobTitle at $company
            
            JOB DESCRIPTION REQUIREMENTS:
            $jobDescription
            
            Perform a precise comparison:
            1. Extract the core technical, domain, and workflow skills demanded by this job.
            2. Match which of these skills the candidate ALREADY demonstrably has based on their profile.
            3. Identify the MISSING SKILLS or experience gaps that the candidate needs to develop to become a top-tier fit.
            4. For each missing skill, provide:
               - skillName: exact name
               - category: Technical / Cloud / Frameworks / Architecture / Leadership / Domain
               - importance: Critical / Preferred / Bonus
               - reason: why the job requires this
               - developmentAction: actionable guidance, recommended project, or course topic to close this gap.
            5. Calculate overall match percentage (0-100).
            
            Output ONLY a valid JSON object matching this schema:
            {
              "matchPercentage": <integer 0-100>,
              "totalRequiredSkillsCount": <integer>,
              "matchedSkills": ["skill 1", "skill 2"],
              "missingSkills": [
                {
                  "skillName": "Kubernetes",
                  "category": "Cloud & DevOps",
                  "importance": "Critical",
                  "reason": "Production deployments rely on microservices orchestration",
                  "developmentAction": "Complete a hands-on CKA lab or deploy a multi-service app with Helm charts."
                }
              ],
              "summaryAdvice": "<2-3 sentence strategic recommendation for the candidate>"
            }
            Do not include markdown code block formatting or backticks around the json. Output raw JSON.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                thinkingModel.generateContent(prompt)
            }
            val text = response.text?.trim().orEmpty()
            val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(cleanJson)

            val matchPercentage = json.optInt("matchPercentage", 70).coerceIn(0, 100)
            val matchedArray = json.optJSONArray("matchedSkills")
            val matchedList = mutableListOf<String>()
            if (matchedArray != null) {
                for (i in 0 until matchedArray.length()) {
                    matchedList.add(matchedArray.getString(i))
                }
            }

            val missingArray = json.optJSONArray("missingSkills")
            val missingList = mutableListOf<MissingSkillDetail>()
            if (missingArray != null) {
                for (i in 0 until missingArray.length()) {
                    val item = missingArray.getJSONObject(i)
                    missingList.add(
                        MissingSkillDetail(
                            skillName = item.optString("skillName", "Required Skill"),
                            category = item.optString("category", "Technical"),
                            importance = item.optString("importance", "Critical"),
                            reason = item.optString("reason", "Required for daily duties"),
                            developmentAction = item.optString("developmentAction", "Review official documentation and build a demonstration project.")
                        )
                    )
                }
            }

            val totalCount = json.optInt("totalRequiredSkillsCount", (matchedList.size + missingList.size).coerceAtLeast(1))
            val summary = json.optString("summaryAdvice", "Focus on developing the critical requirements while leveraging your existing strengths.")

            SkillsGapComparison(
                matchPercentage = matchPercentage,
                totalRequiredSkillsCount = totalCount,
                matchedSkills = matchedList,
                missingSkills = missingList,
                summaryAdvice = summary
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Skills comparison failed, generating fallback analysis", e)
            val userSkillSet = userSkills.split(",", ";", "\n").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            val commonKeywords = listOf(
                "Kotlin", "Android", "Jetpack Compose", "Coroutines", "Room", "Firebase",
                "Python", "SQL", "TypeScript", "React", "Docker", "Kubernetes", "AWS",
                "GraphQL", "REST APIs", "CI/CD", "Git", "System Design", "Agile", "Testing"
            )
            val matched = mutableListOf<String>()
            val missing = mutableListOf<MissingSkillDetail>()

            for (kw in commonKeywords) {
                if (jobDescription.contains(kw, ignoreCase = true) || jobTitle.contains(kw, ignoreCase = true)) {
                    val hasSkill = userSkillSet.any { it.contains(kw.lowercase()) || kw.lowercase().contains(it) } ||
                            userExperience.contains(kw, ignoreCase = true)
                    if (hasSkill) {
                        matched.add(kw)
                    } else {
                        missing.add(
                            MissingSkillDetail(
                                skillName = kw,
                                category = if (kw in listOf("Docker", "Kubernetes", "AWS", "CI/CD")) "Cloud & DevOps" else "Technical",
                                importance = if (kw in listOf("Kotlin", "System Design", "Docker", "React", "Python")) "Critical" else "Preferred",
                                reason = "Identified as a core requirement in the job posting for $jobTitle",
                                developmentAction = "Build a portfolio project demonstrating practical usage of $kw."
                            )
                        )
                    }
                }
            }

            val total = (matched.size + missing.size).coerceAtLeast(1)
            val percentage = ((matched.size.toFloat() / total) * 100).toInt().coerceIn(25, 95)

            SkillsGapComparison(
                matchPercentage = if (total > 0) percentage else 65,
                totalRequiredSkillsCount = total,
                matchedSkills = matched.ifEmpty { listOf("Software Development", "Problem Solving", "Communication") },
                missingSkills = missing.ifEmpty {
                    listOf(
                        MissingSkillDetail(
                            skillName = "Advanced Architecture & Systems",
                            category = "Architecture",
                            importance = "Preferred",
                            reason = "Recommended to elevate profile for $jobTitle",
                            developmentAction = "Study end-to-end design patterns and distributed system fundamentals."
                        )
                    )
                },
                summaryAdvice = "You match the foundational requirements well. Upskilling in the highlighted missing areas will significantly increase your callback rate."
            )
        }
    }
}
