package com.example.weatherdashboard.data

data class WeatherData(
    val temperature: Int? = null,
    val humidity: Int? = null,
    val windSpeed: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

)
