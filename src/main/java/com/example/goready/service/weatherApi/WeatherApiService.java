package com.example.goready.service.weatherApi;

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
}

