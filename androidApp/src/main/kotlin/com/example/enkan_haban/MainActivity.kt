package com.example.enkan_haban

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Let the in-app navigator handle back first; at the root, fall through to the system.
        onBackPressedDispatcher.addCallback(this) {
            if (!AppContainer.navigator.goBack()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }

        // Google sign-in comes back here (enkanhaban://login-callback).
        handleAuthDeeplink(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthDeeplink(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
