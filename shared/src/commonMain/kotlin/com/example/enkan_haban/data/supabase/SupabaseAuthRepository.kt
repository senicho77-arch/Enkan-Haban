package com.example.enkan_haban.data.supabase

import com.example.enkan_haban.data.AuthRepository
import com.example.enkan_haban.domain.Seller
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Google sign-in through Supabase Auth. The profile row is created by the database on first sign-in. */
class SupabaseAuthRepository(
    private val client: SupabaseClient,
    scope: CoroutineScope,
) : AuthRepository {
    private val _currentUser = MutableStateFlow<Seller?>(null)
    override val currentUser: StateFlow<Seller?> = _currentUser.asStateFlow()
    override val isSignInAvailable: Boolean = true

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                val userId = (status as? SessionStatus.Authenticated)?.session?.user?.id
                _currentUser.value = if (userId == null) null else loadMe(userId)
            }
        }
    }

    private suspend fun loadMe(userId: String): Seller {
        val profile = runCatching { client.postgrest.rpc("get_my_profile").decodeAs<MyProfileRow>() }.getOrNull()
        val metaName = (client.auth.currentUserOrNull()?.userMetadata?.get("full_name") as? JsonPrimitive)?.contentOrNull
        return Seller(
            id = userId,
            name = profile?.fullName?.takeIf { it.isNotBlank() } ?: metaName ?: "",
            phone = profile?.phone.orEmpty(),
        )
    }

    override suspend fun signInWithGoogle() {
        client.auth.signInWith(Google)
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }
}
