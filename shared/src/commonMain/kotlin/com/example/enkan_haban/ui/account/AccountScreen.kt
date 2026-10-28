package com.example.enkan_haban.ui.account

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.enkan_haban.ui.components.CurrencyToggle
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.util.CurrencySettings

@Composable
fun AccountScreen() {
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = "Account")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Welcome to EnkanHaban", style = MaterialTheme.typography.titleMedium)
                    Text("Sign in to track orders, manage your ads and chat with sellers.")
                    Button(onClick = { /* TODO: authentication */ }, enabled = false) { Text("Sign in (coming soon)") }
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
                listOf("📦  My orders", "📢  My ads", "❤️  Saved items", "⚙️  Settings").forEachIndexed { index, label ->
                    if (index > 0) HorizontalDivider()
                    ListItem(headlineContent = { Text(label) })
                }
            }
        }
    }
}
