package com.example.enkan_haban.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

/** The one Supabase client of the app. Only touch it when [SupabaseConfig.isConfigured]. */
object Supabase {
    val client: SupabaseClient by lazy {
        check(SupabaseConfig.isConfigured) { "Fill in SupabaseConfig.URL and ANON_KEY first" }
        createSupabaseClient(supabaseUrl = SupabaseConfig.URL, supabaseKey = SupabaseConfig.ANON_KEY) {
            // Tables have more columns than the app reads (e.g. the search index); skip them.
            defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
            install(Auth) {
                scheme = SupabaseConfig.AUTH_SCHEME
                host = SupabaseConfig.AUTH_HOST
            }
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}
