package com.example.weatherdashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherdashboard.data.WeatherData
import com.example.weatherdashboard.data.WeatherRepository
import kotlinx.coroutines.Job  // ← добавить импорт
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository()
    private var loadJob: Job? = null

    private val _weatherState = MutableStateFlow(WeatherData())
    val weatherState: StateFlow<WeatherData> = _weatherState.asStateFlow()

    init {
        loadWeatherData()
        startAutoRefresh()
    }

    fun toggleErrorSimulation() {
        repository.toggleErrorSimulation()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            flow {
                while (true) {
                    delay(10000)
                    emit(Unit)
                }
            }.collect {
                loadWeatherData()
            }
        }
    }

    fun loadWeatherData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(
                isLoading = true,
                error = null,
                loadingProgress = "Запуск загрузки..."
            )

            try {
                coroutineScope {
                    _weatherState.value = _weatherState.value.copy(
                        loadingProgress = "Загружаем температуру, влажность, скорость ветра..."
                    )

                    val temperatureDeferred = async { repository.fetchTemperature() }
                    val humidityDeferred = async { repository.fetchHumidity() }
                    val windSpeedDeferred = async { repository.fetchWindSpeed() }

                    val temperature = temperatureDeferred.await()
                    val humidity = humidityDeferred.await()
                    val windSpeed = windSpeedDeferred.await()

                    _weatherState.value = _weatherState.value.copy(
                        loadingProgress = "Вычисление индекса погоды..."
                    )
                    val weatherIndex = repository.calculateWeatherIndex(
                        temperature,
                        humidity,
                        windSpeed
                    )

                    _weatherState.value = WeatherData(
                        temperature = temperature,
                        humidity = humidity,
                        windSpeed = windSpeed,
                        weatherIndex = weatherIndex,
                        isLoading = false,
                        error = null,
                        loadingProgress = "Загрузка завершена!"
                    )
                }
            } catch (e: Exception) {
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}",
                    loadingProgress = ""
                )
            }
        }
    }
}