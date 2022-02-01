package com.corrot.kwiatonomousapp.presentation.device_details

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corrot.kwiatonomousapp.common.Constants
import com.corrot.kwiatonomousapp.common.Result
import com.corrot.kwiatonomousapp.common.components.LineChartDataType
import com.corrot.kwiatonomousapp.common.components.LineChartDateType
import com.corrot.kwiatonomousapp.common.toLong
import com.corrot.kwiatonomousapp.domain.repository.PreferencesRepository
import com.corrot.kwiatonomousapp.domain.usecase.GetDeviceConfigurationUseCase
import com.corrot.kwiatonomousapp.domain.usecase.GetDeviceUpdatesByDateUseCase
import com.corrot.kwiatonomousapp.domain.usecase.GetDeviceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appPreferencesRepository: PreferencesRepository,
    private val getDeviceUseCase: GetDeviceUseCase,
    private val getDeviceUpdatesByDateUseCase: GetDeviceUpdatesByDateUseCase,
    private val getDeviceConfigurationUseCase: GetDeviceConfigurationUseCase,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceId = savedStateHandle.get<String>(Constants.NAV_ARG_DEVICE_ID)
    val state = mutableStateOf(DeviceDetailsState())
    val currentAppTheme = appPreferencesRepository.getAppTheme()

    // Keep track of jobs to cancel it first when refreshData() is triggered
    // otherwise there will be multiple coroutines in background
    // TODO: is that a good approach
    private var getDeviceJob: Job? = null
    private var getDeviceUpdatesJob: Job? = null
    private var getDeviceConfigurationJob: Job? = null

    val isLoading: Boolean
        get() = state.value.isDeviceLoading
                || state.value.isDeviceUpdatesLoading
                || state.value.isDeviceConfigurationLoading

    init {
        state.value = state.value.copy(
            selectedDateRange = calculateDateRange(LineChartDateType.DAY)
        )
        refreshData()
    }

    fun refreshData() {
        deviceId?.let {
            getDevice(it)
            getDeviceConfiguration(it)
            getDeviceUpdates(
                id = it,
                from = state.value.selectedDateRange.first,
                to = state.value.selectedDateRange.second
            )
        }
    }

    fun confirmError() {
        state.value = state.value.copy(error = null)
    }

    private fun getDevice(id: String) {
        viewModelScope.launch(ioDispatcher) {
            getDeviceJob?.cancelAndJoin()
            getDeviceJob = viewModelScope.launch(ioDispatcher) {
                getDeviceUseCase.execute(id).collect { ret ->
                    withContext(Dispatchers.Main) {
                        when (ret) {
                            is Result.Loading -> state.value = state.value.copy(
                                isDeviceLoading = true,
                                device = ret.data
                            )
                            is Result.Success -> state.value = state.value.copy(
                                isDeviceLoading = false,
                                device = ret.data/*, error = null*/
                            )
                            is Result.Error -> state.value = state.value.copy(
                                isDeviceLoading = false,
                                error = ret.throwable.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getDeviceUpdates(id: String, from: Long, to: Long) {
        viewModelScope.launch(ioDispatcher) {
            getDeviceUpdatesJob?.cancelAndJoin()
            getDeviceUpdatesJob = viewModelScope.launch(ioDispatcher) {
                getDeviceUpdatesByDateUseCase.execute(id, from, to).collect { ret ->
                    withContext(Dispatchers.Main) {
                        when (ret) {
                            is Result.Loading -> state.value = state.value.copy(
                                isDeviceUpdatesLoading = true,
                                deviceUpdates = ret.data
                            )
                            is Result.Success -> state.value = state.value.copy(
                                isDeviceUpdatesLoading = false,
                                deviceUpdates = ret.data/*, error = null*/
                            )
                            is Result.Error -> state.value = state.value.copy(
                                isDeviceUpdatesLoading = false,
                                error = ret.throwable.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getDeviceConfiguration(id: String) {
        viewModelScope.launch(ioDispatcher) {
            getDeviceConfigurationJob?.cancelAndJoin()
            getDeviceConfigurationJob = viewModelScope.launch(ioDispatcher) {
                getDeviceConfigurationUseCase.execute(id).collect { ret ->
                    withContext(Dispatchers.Main) {
                        when (ret) {
                            is Result.Loading -> state.value = state.value.copy(
                                isDeviceConfigurationLoading = true,
                                deviceConfiguration = ret.data
                            )
                            is Result.Success -> state.value = state.value.copy(
                                isDeviceConfigurationLoading = false,
                                deviceConfiguration = ret.data/*, error = null*/
                            )
                            is Result.Error -> state.value = state.value.copy(
                                isDeviceConfigurationLoading = false,
                                error = ret.throwable.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun onChartDateTypeSelected(dateType: LineChartDateType) {
        viewModelScope.launch {
            when {
                dateType.ordinal < state.value.selectedChartDateType.ordinal -> {
                    state.value.deviceUpdates?.let { oldDeviceUpdates ->
                        val newDataRange = calculateDateRange(dateType)
                        val newDeviceUpdates = oldDeviceUpdates.filter {
                            it.updateTime.toLong() in newDataRange.first..newDataRange.second
                        }
                        state.value = state.value.copy(
                            deviceUpdates = newDeviceUpdates,
                            selectedChartDateType = dateType,
                            selectedDateRange = newDataRange
                        )
                    }
                }
                dateType.ordinal > state.value.selectedChartDateType.ordinal -> {
                    val newDataRange = calculateDateRange(dateType)
                    deviceId?.let {
                        getDeviceUpdates(
                            id = it,
                            from = newDataRange.first,
                            to = newDataRange.second
                        )
                    }
                    state.value = state.value.copy(
                        selectedChartDateType = dateType,
                        selectedDateRange = newDataRange
                    )
                }
                dateType.ordinal == state.value.selectedChartDateType.ordinal -> {}
            }
        }
    }

    fun onChartDataTypeSelected(dataType: LineChartDataType) {
        state.value = state.value.copy(selectedChartDataType = dataType)
    }

    private fun calculateDateRange(dateType: LineChartDateType): Pair<Long, Long> {
        val from: Long
        val to: Long
        val currentDateTime = LocalDateTime.now()
        val midnightTomorrow = currentDateTime
            .withHour(0)
            .withMinute(0)
            .plusDays(1L)
            .toLong()

        when (dateType) {
            LineChartDateType.DAY -> {
                from = midnightTomorrow - (24 * 3600)
                to = midnightTomorrow
            }
            LineChartDateType.WEEK -> {
                from = midnightTomorrow - (6 * 24 * 3600)
                to = midnightTomorrow
            }
            LineChartDateType.MONTH -> {
                val daysInMonth = currentDateTime.month.maxLength()
                from = midnightTomorrow - (daysInMonth * 24 * 3600)
                to = midnightTomorrow
            }
        }
        return Pair(from, to)
    }
}
