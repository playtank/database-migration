package com.playtank.databasemigration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playtank.databasemigration.data.UserEntityV2
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
fun MigrationDemoScreen(
    modifier: Modifier = Modifier,
    vm: MigrationViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Room Migration Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "v1 → v2",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(24.dp))

        // Name + Insert
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.nameInput,
                onValueChange = vm::onNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = vm::seedV1Database,
                enabled = !state.isLoading && !state.seeded,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Insert")
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Trigger Migration
        Button(
            onClick = vm::triggerMigration,
            enabled = !state.isLoading && state.seeded,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Trigger Migration", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Reset
        OutlinedButton(
            onClick = vm::reset,
            enabled = !state.isLoading && (state.seeded || state.result != DemoResult.Idle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset")
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        when (val result = state.result) {
            is DemoResult.Idle    -> Unit
            is DemoResult.Crash   -> CrashCard(result.error)
            is DemoResult.Success -> SuccessCard(result.users)
        }
    }
}

@Composable
private fun CrashCard(error: Throwable) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Migration Failed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${error.javaClass.simpleName}:\n${error.message ?: "no message"}",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFF1E1E1E), shape = MaterialTheme.shapes.small)
                    .padding(12.dp),
                color = Color(0xFFFF6B6B),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SuccessCard(users: List<UserEntityV2>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Migration Successful",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${users.size} row(s) found",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32)
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.height((users.size * 90).coerceAtMost(360).dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { UserRow(it) }
            }
        }
    }
}

@Composable
private fun UserRow(user: UserEntityV2) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "id: ${user.id}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "name: ${user.name}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "email: \"${user.email}\"" +
                        if (user.email.isEmpty()) "  ← DEFAULT '' backfilled" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (user.email.isEmpty()) Color(0xFF795548)
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
