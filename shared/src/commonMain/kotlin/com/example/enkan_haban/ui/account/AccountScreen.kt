package com.example.enkan_haban.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.ui.components.CurrencyToggle
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.util.CurrencySettings
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(onOpenChats: () -> Unit, onAdminOrderClick: () -> Unit = {}) {
    val isAdmin by AppContainer.adminRepository.isAdmin.collectAsStateWithLifecycle()
    val auth = AppContainer.authRepository
    val signedInUser by auth.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var signInError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = "ኣካውንት")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val user = signedInUser
                    when {
                        !auth.isSignInAvailable -> {
                            Text("እንቋዕ ናብ እንካን ሃባን ብደሓን መጻእኩም", style = MaterialTheme.typography.titleMedium)
                            Text("እዚ ናይ ፈተነ (demo) ስሪት እዩ — ምእታው ምስ ዳታበይዝ ምስ ተራኸበ ይሰርሕ።")
                        }
                        user == null -> {
                            Text("እንቋዕ ናብ እንካን ሃባን ብደሓን መጻእኩም", style = MaterialTheme.typography.titleMedium)
                            Text("ትእዛዛትካ ንምክትታል፡ ዝሸጥካዮም ኣቕሑ ንምምሕዳርን ምስ ሸየጥቲ ንምዝራብን እቶ።")
                            Button(onClick = {
                                signInError = null
                                scope.launch {
                                    runCatching { auth.signInWithGoogle() }
                                        .onFailure { signInError = "ምእታው ኣይተኻእለን — ደጊምካ ፈትን" }
                                }
                            }) { Text("እቶ ብ Google") }
                        }
                        else -> {
                            Text("ሰላም ${user.name.ifBlank { "👋" }}", style = MaterialTheme.typography.titleMedium)
                            Text("ብ Google ኣቲኻ ኣለኻ።", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { scope.launch { runCatching { auth.signOut() } } }) { Text("ውጻእ") }
                        }
                    }
                    signInError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("💱  ገንዘብ · Currency") },
                    supportingContent = { Text(CurrencySettings.current.localName) },
                    trailingContent = { CurrencyToggle() },
                )
            }
            Card(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("💬  መልእኽታተይ") },
                    modifier = Modifier.clickable(onClick = onOpenChats),
                )
                listOf("📦  ትእዛዛተይ", "🏷️  ኣቕሑተይ", "❤️  ዝዓቀብክዎም", "⚙️  ምድላዋት").forEach { label ->
                    HorizontalDivider()
                    ListItem(headlineContent = { Text(label) })
                }
            }
            if (isAdmin) {
                Card(onClick = onAdminOrderClick, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("🛠️  ምሕደራ መስርዕ") },
                        supportingContent = { Text("ኣብ ነፍሲ ወከፍ ዓይነት መስርዕ ፖስትታት ወስን") },
                    )
                }
            }
        }
    }
}
