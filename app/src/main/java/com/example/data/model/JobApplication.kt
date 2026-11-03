package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class JobApplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val jobDescription: String = "",
    val applicationDate: String = "",
    val status: String = "Saved", // Saved, Applied, Interviewing, Offer, Rejected
    val notes: String = "",
    val deadline: String = "",
    val salary: String = "",
    val location: String = "",
    val tailoredResume: String = "",
    val tailoredCoverLetter: String = "",
    val matchScore: Long = 0L,
    val matchAnalysis: String = "",
    val desiredTone: String = "Professional", // Professional, Creative, Concise, Executive, Technical
    val emphasizedSkills: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
