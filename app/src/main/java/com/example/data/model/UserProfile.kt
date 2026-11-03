package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "main",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val headline: String = "",
    val summary: String = "",
    val skills: String = "",
    val workExperience: String = "",
    val education: String = "",
    val careerGoals: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
