package com.example.goready.service.weatherApi;

import com.example.goready.utils.GridUtils;
import com.example.goready.utils.LatXLonY;
import org.springframework.stereotype.Service;

@Service
public class WeatherApiService {

    public LatXLonY convertTest(double lat, double lon) {
        // 위경도 -> 격자 좌표로 변환
        LatXLonY xy = GridUtils.convertGRID_GPS(0, lat, lon);
        return xy;
    }
}

