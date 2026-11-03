package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.ai.AiTailorService
import com.example.data.repository.FirestoreRepository
import com.example.ui.MainAppContainer
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.ResumeCraftTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY: Configure App Check with launch intent before rendering Compose UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            ResumeCraftTheme {
                val scope = rememberCoroutineScope()
                var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        currentUser = auth.currentUser
                    }
                    Firebase.auth.addAuthStateListener(listener)
                    onDispose {
                        Firebase.auth.removeAuthStateListener(listener)
                    }
                }

                if (currentUser == null) {
                    AuthScreen(
                        onAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                        }
                    )
                } else {
                    val repository = remember { FirestoreRepository(applicationContext) }
                    val aiService = remember { AiTailorService() }

                    MainAppContainer(
                        repository = repository,
                        aiService = aiService,
                        scope = scope,
                        onSignOutComplete = {
                            currentUser = null
                        }
                    )
                }
            }
        }
    }
}
