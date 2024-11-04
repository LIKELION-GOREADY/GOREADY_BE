package com.example.goready.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeatherData {
    private Integer highTemp;       // 최고 기온
    private Integer lowTemp;        // 최저 기온
    private Integer rainPer;        // 강수 확률
    private Integer currentTemp;    // 현재 기온
    private Integer yesterdayTemp;  // 어제 기온
}
