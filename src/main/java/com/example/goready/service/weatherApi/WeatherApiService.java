package com.example.goready.service.weatherApi;

import com.example.goready.dto.response.WeatherResponseDto;
import com.example.goready.utils.GridUtils;
import com.example.goready.utils.LonXLatY;
import org.springframework.stereotype.Service;

@Service
public class WeatherApiService {

    public LonXLatY convertTest(double lon, double lat) {
        // 위경도 -> 격자 좌표로 변환
        LonXLatY xy = GridUtils.convertGRID_GPS(0, lon, lat);
        return xy;
    }

    public WeatherResponseDto getWeather(double lon, double lat) {
        // 기상청 api 조회
        // convertGRID_GPS 호출
        // 단기 예보 api : 오늘 최고/최저 기온, 강수확률
        // 초단기 실황 api : 현재 기온, 어제 기온
    }



