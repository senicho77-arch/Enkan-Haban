package com.example.enkan_haban.data.supabase

/**
 * Where the app finds your Supabase project. Paste both values from
 * Supabase → Project Settings → API (or the "Connect" button):
 *  - [URL]: "Project URL", e.g. "https://abcd1234.supabase.co"
 *  - [ANON_KEY]: the "anon public" / "publishable" key (a long text starting with "eyJ" or "sb_publishable_").
 *
 * Both are safe inside the app: Row Level Security in the database decides what each user may do.
 * NEVER put the "service_role" / secret key here.
 *
 * While either is empty the app runs on demo data (InMemory repositories).
 */
object SupabaseConfig {
    const val URL = "https://tcubokttkfwrsowgznse.supabase.co"
    const val ANON_KEY = "sb_publishable_LoWZi6eq2wGypYsJGb6yXw_IovHb_Lf"

    /** Google sign-in returns to the app through enkanhaban://login-callback (add it in Supabase → Auth → URL Configuration). */
    const val AUTH_SCHEME = "enkanhaban"
    const val AUTH_HOST = "login-callback"

    const val IMAGES_BUCKET = "listing-images"

    val isConfigured: Boolean get() = URL.startsWith("https://") && ANON_KEY.isNotBlank()
}
