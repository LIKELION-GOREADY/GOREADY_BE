package com.example.goready.controller;

import com.example.goready.dto.MaskResponse;
import com.example.goready.dto.response.WeatherResponse;
import com.example.goready.global.response.ApiResponse;
import com.example.goready.global.response.status.SuccessStatus;
import com.example.goready.service.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<WeatherResponse.WeatherDto>>> getWeather(
            @RequestParam(name = "lat") double lat,
            @RequestParam(name = "lon") double lon
    ) {
        return weatherService.getWeather(lat, lon)
                .map(weatherDto -> ApiResponse.success(SuccessStatus.SUCCESS_GET_DUST, weatherDto));
    }


}
