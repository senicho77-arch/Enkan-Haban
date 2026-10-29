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
        SimpleTopBar(title = "ኣካውንት")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("እንቋዕ ናብ እንካን ሃባን ብደሓን መጻእኩም", style = MaterialTheme.typography.titleMedium)
                    Text("ትእዛዛትካ ንምክትታል፡ መዋዓውዒታትካ ንምምሕዳርን ምስ ሸየጥቲ ንምዝራብን እቶ።")
                    Button(onClick = { /* TODO: authentication */ }, enabled = false) { Text("እቶ (ቀልጢፉ ይመጽእ)") }
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
                listOf("📦  ትእዛዛተይ", "📢  መዋዓውዒታተይ", "❤️  ዝዓቀብክዎም", "⚙️  ምድላዋት").forEachIndexed { index, label ->
                    if (index > 0) HorizontalDivider()
                    ListItem(headlineContent = { Text(label) })
                }
            }
        }
    }
}
