package com.example.goready.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

public class WeatherResponse {

    @Builder
    @Data
    @AllArgsConstructor
    public static class WeatherDto {
        private int highTemp;
        private int lowTemp;
        private int rainPer;
        private String status; // hot/cold/same
        private int diffTemp;
        private int currentTemp;
        private boolean isUmbrella;
    }
}
