package com.example.goready.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class WeatherData {

    @Getter
    @Builder
    @Data
    @AllArgsConstructor
    public static class RainData{
        private int maxTemp;
        private int minTemp;
        private int rainPer;
    }
    @Getter
    @Builder
    @Data
    @AllArgsConstructor
    public static class TempData {
        private int currentTemp;
        private int yesterdayTemp;
    }
}
