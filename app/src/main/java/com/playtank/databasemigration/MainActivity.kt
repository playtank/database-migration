package com.playtank.databasemigration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playtank.databasemigration.ui.theme.DatabaseMigrationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DatabaseMigrationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MigrationDemoScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MigrationDemoScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Room Migration Demo",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = "Database v3 – users(id, name, email, age)",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "Run the instrumented tests to see the\nmigration bug and its fix.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
