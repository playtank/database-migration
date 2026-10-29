package com.playtank.databasemigration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playtank.databasemigration.data.AppDatabase
import com.playtank.databasemigration.data.UserEntity
import com.playtank.databasemigration.ui.theme.DatabaseMigrationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Sealed state for the three possible outcomes of a DB operation.
private sealed interface DemoState {
    data object Idle : DemoState
    data object Loading : DemoState
    data class Success(val users: List<UserEntity>) : DemoState
    data class Crash(val error: Throwable) : DemoState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.getInstance(this)
        setContent {
            DatabaseMigrationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MigrationDemoScreen(
                        db = db,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MigrationDemoScreen(
    db: AppDatabase,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<DemoState>(DemoState.Idle) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Text(
            text = "Room Migration Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "STEP 1 (crashDemo)  →  STEP 2 (fixedDemo)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(28.dp))

        // ── Action button ────────────────────────────────────────────────────
        Button(
            onClick = {
                state = DemoState.Loading
                scope.launch {
                    state = withContext(Dispatchers.IO) {
                        runCatching {
                            val dao = db.userDao()

                            // Seed: Alice simulates a "v1 user" (no email at insert time).
                            // Bob was added after the migration with a real email.
                            dao.insert(UserEntity(id = 1, name = "Alice", email = ""))
                            dao.insert(UserEntity(id = 2, name = "Bob",   email = "bob@example.com"))
                            dao.getAll()
                        }.fold(
                            onSuccess = { DemoState.Success(it) },
                            onFailure = { DemoState.Crash(it) }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is DemoState.Loading
        ) {
            Text(if (state is DemoState.Loading) "Running…" else "Upgrade & Query Database")
        }

        Spacer(Modifier.height(20.dp))

        // ── Result area ──────────────────────────────────────────────────────
        when (val s = state) {

            DemoState.Idle -> {
                Text(
                    text = "Tap the button to start the demo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DemoState.Loading -> {
                Text("Accessing database…", style = MaterialTheme.typography.bodyMedium)
            }

            // ── STEP 1 outcome: crash panel ───────────────────────────────
            is DemoState.Crash -> {
                CrashPanel(error = s.error)
            }

            // ── STEP 2 outcome: success panel ─────────────────────────────
            is DemoState.Success -> {
                SuccessPanel(users = s.users)
            }
        }
    }
}

// ── Crash panel (shown in Step 1 / crashDemo) ────────────────────────────────

@Composable
private fun CrashPanel(error: Throwable) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "❌  CRASH – Schema mismatch detected",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Room found that the entity's compiled schema (which includes " +
                    "the new `email` column) does not match the on-disk schema, and " +
                    "the version number is still 1 so no migration can run.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        // Show the raw exception message in a monospaced box – exactly what
        // you'd see in Logcat before the app crashes.
        Text(
            text = error.javaClass.simpleName + ":\n" + (error.message ?: "no message"),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), shape = MaterialTheme.shapes.small)
                .padding(12.dp),
            color = Color(0xFFFF6B6B),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Fix: bump @Database(version = 2) and register MIGRATION_1_2_FIXED.\n" +
                    "Then install the fixedDemo variant without clearing app data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// ── Success panel (shown in Step 2 / fixedDemo) ──────────────────────────────

@Composable
private fun SuccessPanel(users: List<UserEntity>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "✅  Migration successful – zero data loss",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${users.size} user(s) loaded. " +
                    "Alice's row survived with email defaulted to \"\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users) { user -> UserCard(user) }
        }
    }
}

@Composable
private fun UserCard(user: UserEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "id=${user.id}   name=${user.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("email = \"${user.email}\"")
                    if (user.email.isEmpty()) append("  ← DEFAULT '' (v1 row, no data lost)")
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (user.email.isEmpty())
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
