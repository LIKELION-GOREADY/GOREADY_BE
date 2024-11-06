package com.example.goready.converter;

import com.example.goready.dto.Address;
import com.example.goready.dto.WeatherResponse;

public class WeatherConverter {
    public static WeatherResponse.RainDto toRainDto(Integer maxTemp, Integer minTemp, Integer rainPer) {
        return WeatherResponse.RainDto.builder()
                .maxTemp(maxTemp)
                .minTemp(minTemp)
                .rainPer(rainPer)
                .build();
    }
}
