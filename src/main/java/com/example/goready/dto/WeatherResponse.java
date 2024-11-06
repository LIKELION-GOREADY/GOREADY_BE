package com.example.goready.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

public class WeatherResponse {
    @Builder
    @Data
    @AllArgsConstructor
    public static class RainDto {
        private int maxTemp;
        private int minTemp;
        private int rainPer;
    }
    @Builder
    @Data
    @AllArgsConstructor
    public static class TempDto {
        private int currentTemp;
        private int yesterdayTemp;
        private String status;
        private int diffTemp;
        // Getter, Setter, 생성자
    }
    @Builder
    @Data
    @AllArgsConstructor
    public class WeatherDto {
        private RainDto rainDto;
        private TempDto tempDto;
        private boolean isUmbrella;
    }
}
