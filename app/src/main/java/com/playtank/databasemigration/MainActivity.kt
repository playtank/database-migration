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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.playtank.databasemigration.data.AppDatabaseV1
import com.playtank.databasemigration.data.AppDatabaseV2
import com.playtank.databasemigration.data.UserEntity
import com.playtank.databasemigration.data.UserEntityV1
import com.playtank.databasemigration.migration.MIGRATION_1_2_FIXED
import com.playtank.databasemigration.ui.theme.DatabaseMigrationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DB_NAME = "app_database"

private enum class Step { NONE, SEEDED, CRASHED, FIXED }

private sealed interface StepResult {
    data object Idle : StepResult
    data class Info(val message: String) : StepResult
    data class Crash(val error: Throwable) : StepResult
    data class Success(val users: List<UserEntity>) : StepResult
}

// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DatabaseMigrationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    MigrationDemoScreen(modifier = Modifier.padding(inner))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MigrationDemoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var step   by remember { mutableStateOf(Step.NONE) }
    var result by remember { mutableStateOf<StepResult>(StepResult.Idle) }

    Column(
        modifier = modifier
            .fillMaxSize()
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
            text = "Follow the three steps in order",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(24.dp))

        // ── Step 1 ── Seed a real v1 database via Room (AppDatabaseV1) ──────
        StepButton(
            number   = "1",
            label    = "Seed v1 Data",
            subtitle = "Opens app_database at version 1 (id + name only) and inserts Alice.\nThis is the \"before\" state on an old user's device.",
            enabled  = step == Step.NONE,
            color    = MaterialTheme.colorScheme.secondary
        ) {
            scope.launch {
                result = withContext(Dispatchers.IO) {
                    runCatching {
                        // Delete any leftover file so the demo starts clean.
                        context.deleteDatabase(DB_NAME)

                        // Use AppDatabaseV1 (version=1, entity has only id+name).
                        val db = Room.databaseBuilder(
                            context, AppDatabaseV1::class.java, DB_NAME
                        ).build()
                        db.userDao().insert(UserEntityV1(id = 1, name = "Alice"))
                        db.close()
                    }.fold(
                        onSuccess = {
                            StepResult.Info(
                                "✔ app_database created at v1.\n" +
                                "Alice (id=1, name='Alice') inserted — no email column yet.\n\n" +
                                "Now tap Step 2 to reproduce the crash."
                            )
                        },
                        onFailure = { StepResult.Crash(it) }
                    )
                }
                if (result is StepResult.Info) step = Step.SEEDED
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Step 2 ── Open the v1 file with AppDatabaseV2 — no migration ────
        StepButton(
            number   = "2",
            label    = "Open with Room v2 — BROKEN",
            subtitle = "AppDatabaseV2 has version=2 and `email` in the entity, but no migration is registered.\nRoom cannot reconcile the schema jump → crash.",
            enabled  = step == Step.SEEDED,
            color    = MaterialTheme.colorScheme.error
        ) {
            scope.launch {
                result = withContext(Dispatchers.IO) {
                    runCatching {
                        // AppDatabaseV2 targets version 2 (entity has email).
                        // No migration registered → Room throws on first access.
                        val db = Room.databaseBuilder(
                            context, AppDatabaseV2::class.java, DB_NAME
                        ).build()
                        // Trigger the actual DB open by accessing the DAO.
                        db.userDao().getAll().also { db.close() }
                    }.fold(
                        onSuccess = { StepResult.Success(it) }, // unexpected path
                        onFailure = { StepResult.Crash(it) }
                    )
                }
                if (result is StepResult.Crash) step = Step.CRASHED
                if (result is StepResult.Success) step = Step.FIXED
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Step 3 ── Open with AppDatabaseV2 + MIGRATION_1_2_FIXED ─────────
        StepButton(
            number   = "3",
            label    = "Open with Room v2 — FIXED",
            subtitle = "Same AppDatabaseV2, but now MIGRATION_1_2_FIXED is registered.\nRoom runs ALTER TABLE … DEFAULT '' and Alice survives.",
            enabled  = step == Step.CRASHED,
            color    = Color(0xFF2E7D32)
        ) {
            scope.launch {
                result = withContext(Dispatchers.IO) {
                    runCatching {
                        val db = Room.databaseBuilder(
                            context, AppDatabaseV2::class.java, DB_NAME
                        )
                            .addMigrations(MIGRATION_1_2_FIXED)
                            .build()
                        db.userDao().getAll().also { db.close() }
                    }.fold(
                        onSuccess = { StepResult.Success(it) },
                        onFailure = { StepResult.Crash(it) }
                    )
                }
                if (result is StepResult.Success) step = Step.FIXED
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        when (val r = result) {
            is StepResult.Idle    -> Unit
            is StepResult.Info    -> InfoPanel(r.message)
            is StepResult.Crash   -> CrashPanel(r.error)
            is StepResult.Success -> SuccessPanel(r.users)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepButton(
    number: String,
    label: String,
    subtitle: String,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors   = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text       = "Step $number — $label",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp
            )
            Text(
                text      = subtitle,
                fontSize  = 11.sp,
                lineHeight = 15.sp,
                color     = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun InfoPanel(message: String) {
    Text(
        text  = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CrashPanel(error: Throwable) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = "CRASH – Schema mismatch detected",
            style      = MaterialTheme.typography.titleMedium,
            color      = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text  = "AppDatabaseV2 compiled entity includes `email`, but the on-disk " +
                    "file is v1 and has no email column. Version jumped 1→2 with no " +
                    "migration path registered.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text     = "${error.javaClass.simpleName}:\n${error.message ?: "no message"}",
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), shape = MaterialTheme.shapes.small)
                .padding(12.dp),
            color      = Color(0xFFFF6B6B),
            fontFamily = FontFamily.Monospace,
            fontSize   = 11.sp,
            lineHeight = 15.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text  = "Tap Step 3 to apply the fix.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun SuccessPanel(users: List<UserEntity>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = "✅  Migration successful – zero data loss",
            style      = MaterialTheme.typography.titleMedium,
            color      = Color(0xFF2E7D32),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "${users.size} user(s) loaded. " +
                    "Alice survived with email defaulted to \"\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users) { UserCard(it) }
        }
    }
}

@Composable
private fun UserCard(user: UserEntity) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = "id=${user.id}   name=${user.name}",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = buildString {
                    append("email = \"${user.email}\"")
                    if (user.email.isEmpty()) append("  ← DEFAULT '' (v1 row, backfilled)")
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
