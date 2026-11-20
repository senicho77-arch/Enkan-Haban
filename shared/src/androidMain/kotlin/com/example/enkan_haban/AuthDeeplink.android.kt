package com.example.enkan_haban

import android.content.Intent
import com.example.enkan_haban.data.supabase.Supabase
import com.example.enkan_haban.data.supabase.SupabaseConfig
import io.github.jan.supabase.auth.handleDeeplinks

/** Finishes Google sign-in when the browser sends the user back via enkanhaban://login-callback. */
fun handleAuthDeeplink(intent: Intent?) {
    if (intent == null || !SupabaseConfig.isConfigured) return
    Supabase.client.handleDeeplinks(intent)
}
