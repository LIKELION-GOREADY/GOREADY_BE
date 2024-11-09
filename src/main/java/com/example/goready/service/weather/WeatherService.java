package com.example.goready.service.weather;

import com.example.goready.dto.WeatherData;
import com.example.goready.dto.WeatherResponse;
import com.example.goready.utils.AddressUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherApiService weatherApiService;
    private final AddressUtil addressUtil;

    public WeatherResponse.WeatherDto getWeather(double lon, double lat) {
        // 1. 기상청 API에서 데이터 조회
        WeatherData weatherData = weatherApiService.getWeatherInfo(lon, lat);

        // 2. 우산 여부 판단: 강수 확률이 50 이상일 때 true
        boolean isUmbrella = weatherData.getRainPer() >= 50;

        // 3. 현재 온도와 어제 온도를 비교하여 상태 결정
        String status;
        int diffTemp = weatherData.getCurrentTemp() - weatherData.getYesterdayTemp();
        if (diffTemp > 0) {
            status = "hot";
        } else if (diffTemp < 0) {
            status = "cold";
        } else {
            status = "same";
        }

        // 4. WeatherDto 빌드하여 반환
        return WeatherResponse.WeatherDto.builder()
                .highTemp(weatherData.getMaxTemp())
                .lowTemp(weatherData.getMinTemp())
                .rainPer(weatherData.getRainPer())
                .status(status)
                .diffTemp(diffTemp)
                .currentTemp(weatherData.getCurrentTemp())
                .isUmbrella(isUmbrella)
                .build();
    }
}