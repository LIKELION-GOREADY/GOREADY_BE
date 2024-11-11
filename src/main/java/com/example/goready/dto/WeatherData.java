package com.example.goready.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@Data
public class WeatherData {
    private int maxTemp;
    private int minTemp;
    private int rainPer;
    private int currentTemp;
    private int yesterdayTemp;

    @JsonCreator
    public WeatherData(@JsonProperty("maxTemp") int maxTemp,
                       @JsonProperty("minTemp") int minTemp,
                       @JsonProperty("rainPer") int rainPer,
                       @JsonProperty("currentTemp") int currentTemp,
                       @JsonProperty("yesterdayTemp") int yesterdayTemp) {
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.rainPer = rainPer;
        this.currentTemp = currentTemp;
        this.yesterdayTemp = yesterdayTemp;
    }
}