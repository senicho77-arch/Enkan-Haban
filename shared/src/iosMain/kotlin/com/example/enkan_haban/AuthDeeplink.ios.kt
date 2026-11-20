package com.example.enkan_haban

import com.example.enkan_haban.data.supabase.Supabase
import com.example.enkan_haban.data.supabase.SupabaseConfig
import io.github.jan.supabase.auth.handleDeeplinks
import platform.Foundation.NSURL

/** Called from Swift (.onOpenURL) to finish Google sign-in: enkanhaban://login-callback. */
fun handleAuthDeeplink(url: NSURL) {
    if (!SupabaseConfig.isConfigured) return
    Supabase.client.handleDeeplinks(url)
}
