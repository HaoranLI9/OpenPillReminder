package io.github.anshireminder.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.anshireminder.app.data.PillLogRepository
import io.github.anshireminder.app.model.PillLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PillLogRepository(application)

    val pillLogs: StateFlow<Map<LocalDate, PillLog>> = repository.pillLogsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun saveLog(log: PillLog) {
        viewModelScope.launch {
            repository.saveLog(log)
        }
    }
}