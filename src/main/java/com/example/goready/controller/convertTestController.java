package com.example.goready.controller;

import com.example.goready.service.weatherApi.WeatherApiService;
import com.example.goready.utils.LonXLatY;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class convertTestController {

    private final WeatherApiService weatherApiService;

    public convertTestController(WeatherApiService weatherApiService) {
        this.weatherApiService = weatherApiService;
    }

    @GetMapping("/convert")
    public LonXLatY convertTest(@RequestParam double lon, @RequestParam double lat) {
        return weatherApiService.convertTest(lon, lat);
    }
    // lat: 36.5, lon:127.5 -> x: 69, y: 104 (성공)
}
