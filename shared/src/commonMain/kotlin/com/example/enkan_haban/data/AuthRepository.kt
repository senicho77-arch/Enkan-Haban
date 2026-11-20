package com.example.enkan_haban.data

import com.example.enkan_haban.domain.Seller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Who is using the app. Sign-in is with Google only. */
interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val currentUser: StateFlow<Seller?>

    /** False in demo mode (no Supabase project configured). */
    val isSignInAvailable: Boolean

    /** Opens Google sign-in in the browser; the app gets the session back through a deep link. */
    suspend fun signInWithGoogle()

    suspend fun signOut()
}

/** Demo mode: always the same local user, no real sign-in. */
class DemoAuthRepository : AuthRepository {
    override val currentUser: StateFlow<Seller?> =
        MutableStateFlow(Seller(id = "me", name = "You", phone = "")).asStateFlow()
    override val isSignInAvailable: Boolean = false
    override suspend fun signInWithGoogle() = Unit
    override suspend fun signOut() = Unit
}
