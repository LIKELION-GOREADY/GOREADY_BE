package com.example.goready.controller;

import com.example.goready.dto.response.WeatherResponseDto;
import com.example.goready.global.response.ApiResponse;
import com.example.goready.global.response.status.SuccessStatus;
import com.example.goready.service.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/weather")
    public ResponseEntity<ApiResponse<WeatherResponseDto>> getWeather(@RequestParam double lon, @RequestParam double lat) {
        WeatherResponseDto weatherResponse = weatherService.getWeather(lon, lat);
        return ApiResponse.success(SuccessStatus.SUCCESS_GET_WEATHER, weatherResponse);
    }

}
