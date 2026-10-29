package com.playtank.databasemigration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.playtank.databasemigration.data.AppDatabaseV1
import com.playtank.databasemigration.data.AppDatabaseV2
import com.playtank.databasemigration.data.UserEntityV1
import com.playtank.databasemigration.data.UserEntityV2
import com.playtank.databasemigration.migration.MIGRATION_1_2_FIXED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DemoResult {
    data object Idle : DemoResult
    data class Crash(val error: Throwable) : DemoResult
    data class Success(val users: List<UserEntityV2>) : DemoResult
}

data class MigrationUiState(
    val nameInput: String  = "Alice",
    val seeded: Boolean    = false,
    val result: DemoResult = DemoResult.Idle,
    val isLoading: Boolean = false
)

private const val DB_NAME = "app_database"

class MigrationViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(MigrationUiState())
    val uiState: StateFlow<MigrationUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(nameInput = value) }

    // Deletes any existing DB, seeds a fresh v1 database with the typed name
    fun seedV1Database() {
        val ctx  = getApplication<Application>()
        val name = _uiState.value.nameInput.ifBlank { "Alice" }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                ctx.deleteDatabase(DB_NAME)
                val db = Room.databaseBuilder(ctx, AppDatabaseV1::class.java, DB_NAME).build()
                db.userDao().insert(UserEntityV1(id = 1, name = name))
                db.close()
            }
            _uiState.update { it.copy(isLoading = false, seeded = ok.isSuccess, result = DemoResult.Idle) }
        }
    }

    // Opens the seeded v1 DB via AppDatabaseV2.
    // Result is Success or Crash depending on which migration code is active below.
    fun triggerMigration() {
        val ctx = getApplication<Application>()

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val outcome = runCatching {
                 val db = Room.databaseBuilder(ctx, AppDatabaseV2::class.java, DB_NAME).build()

                db.userDao().getAll().also { db.close() }
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    result = outcome.fold(
                        onSuccess = { DemoResult.Success(it) },
                        onFailure = { DemoResult.Crash(it) }
                    )
                )
            }
        }
    }

    // Clears the result so the demo can be run again
    fun reset() {
        _uiState.update { MigrationUiState() }
    }
}
