package com.example.goready.converter;

import com.example.goready.dto.WeatherData;

public class WeatherConverter {
    public static WeatherData toWeatherData(Integer maxTemp, Integer minTemp, Integer rainPer, Integer currnetTemp) {
        return WeatherData.builder()
                .maxTemp(maxTemp)
                .minTemp(minTemp)
                .rainPer(rainPer)
                .currentTemp(currnetTemp)
                .build();
    }
}
