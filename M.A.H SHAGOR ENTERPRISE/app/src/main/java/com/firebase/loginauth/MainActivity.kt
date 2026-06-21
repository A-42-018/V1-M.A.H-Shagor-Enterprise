package com.firebase.loginauth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.FirebaseLoginAuthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirebaseLoginAuthTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FirebaseAuthApp()
                }
            }
        }
    }
}

@Composable
fun FirebaseAuthApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    
    when (authState) {
        is AuthState.Loading -> {
            LoadingScreen()
        }
        is AuthState.Unauthenticated -> {
            LoginScreen(
                onSignIn = { email, password ->
                    authViewModel.signIn(email, password)
                },
                onSignUp = { email, password ->
                    authViewModel.signUp(email, password)
                },
                isLoading = isLoading,
                errorMessage = null,
                onErrorDismiss = { authViewModel.clearError() },
                showSuccessMessage = false
            )
        }
        is AuthState.Authenticated -> {
            DashboardScreen(
                user = (authState as AuthState.Authenticated).user,
                onSignOut = { authViewModel.signOut() }
            )
        }
        is AuthState.Error -> {
            LoginScreen(
                onSignIn = { email, password ->
                    authViewModel.signIn(email, password)
                },
                onSignUp = { email, password ->
                    authViewModel.signUp(email, password)
                },
                isLoading = isLoading,
                errorMessage = (authState as AuthState.Error).message,
                onErrorDismiss = { authViewModel.clearError() },
                showSuccessMessage = false
            )
        }
        is AuthState.LoginSuccess -> {
            LoginScreen(
                onSignIn = { email, password ->
                    authViewModel.signIn(email, password)
                },
                onSignUp = { email, password ->
                    authViewModel.signUp(email, password)
                },
                isLoading = isLoading,
                errorMessage = null,
                onErrorDismiss = { authViewModel.clearError() },
                showSuccessMessage = true
            )
        }
    }
}