package com.example.goready.controller;

import com.example.goready.dto.response.WeatherResponseDto;
import com.example.goready.global.response.ApiResponse;
import com.example.goready.global.response.status.SuccessStatus;
import com.example.goready.service.weatherApi.WeatherApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class weatherController {
    private final WeatherApiService weatherApiService;

    public weatherController(WeatherApiService weatherApiService) {
        this.weatherApiService = weatherApiService;
    }

    @GetMapping("/weather")
    public ResponseEntity<ApiResponse<WeatherResponseDto>> convertTest2(@RequestParam double lon, @RequestParam double lat) {
        return ApiResponse.success(SuccessStatus.SUCCESS_GET_WEATHER, weatherApiService.getWeather(lon, lat));
    }

}
