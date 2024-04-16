package com.example.wearosfirstapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class StopWatchViewModel: ViewModel() {

    private val _elpasedTime = MutableStateFlow(0L)
    private val _timerState = MutableStateFlow(TimerState.RESET)
    val timerState = _timerState.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")
    val stopWatchText = _elpasedTime
        .map {millis ->
            LocalTime.ofNanoOfDay(millis * 1_000_000).format(formatter)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "00:00:00:000"
        )

    init {
        _timerState
            .flatMapLatest { timerState->
                getTimerFlow(isRunning = timerState == TimerState.RUNNING)
            }
            .onEach {timeDiff->
                _elpasedTime.update { it + timeDiff  }
            }
            .launchIn(viewModelScope)
    }

    fun toggleIsRunning(){
        when(timerState.value){
            TimerState.RUNNING -> _timerState.update {   TimerState.PAUSED}
            TimerState.PAUSED,
            TimerState.RESET -> _timerState.update { TimerState.RUNNING}
        }
    }

    fun resetTimer(){
        _timerState.update {  TimerState.RESET }
        _elpasedTime.update {  0L }
    }

    private fun getTimerFlow(isRunning: Boolean) : Flow<Long>{
        return flow{
            var startMillis = System.currentTimeMillis()
            while(isRunning){
                val currentMils = System.currentTimeMillis()
                val timeDiff = if(currentMils > startMillis) {
                    currentMils - startMillis
                } else 0L
//                val elapsedMillis = System.currentTimeMillis() - startMillis
                emit(timeDiff)
                startMillis = System.currentTimeMillis()
                delay(10L)
            }
        }
    }
}