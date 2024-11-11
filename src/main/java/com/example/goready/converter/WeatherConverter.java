package com.example.goready.converter;

import com.example.goready.dto.WeatherData;
import com.example.goready.dto.WeatherResponse;

public class WeatherConverter {
    public static WeatherData toWeatherData(Integer maxTemp, Integer minTemp, Integer rainPer, Integer currnetTemp) {
        return WeatherData.builder()
                .maxTemp(maxTemp)
                .minTemp(minTemp)
                .rainPer(rainPer)
                .currentTemp(currnetTemp)
                .build();
    }

    public static WeatherResponse.WeatherDto toWeatherDto(WeatherData weatherData, String status, Integer diffTemp, boolean isUmbrella) {
        return WeatherResponse.WeatherDto.builder()
                .highTemp(weatherData.getMaxTemp())
                .lowTemp(weatherData.getMinTemp())
                .rainPer(weatherData.getRainPer())
                .status(status)
                .diffTemp(diffTemp)
                .currentTemp(weatherData.getCurrentTemp())
                .isUmbrella(isUmbrella)
                .build();
    }
}
