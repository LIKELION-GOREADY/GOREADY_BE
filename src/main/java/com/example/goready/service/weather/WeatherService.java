package com.example.goready.service.weather;

import com.example.goready.converter.WeatherConverter;
import com.example.goready.dto.WeatherResponse;
import com.example.goready.utils.AddressUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherApiService weatherApiService;
    private final AddressUtil addressUtil;

    public Mono<WeatherResponse.WeatherDto> getWeather(double lon, double lat) {
        return weatherApiService.getWeatherInfo(lon, lat)
                .map(weatherData -> {
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

                    // WeatherDto로 변환하여 반환
                    return WeatherConverter.toWeatherDto(weatherData, status, diffTemp, isUmbrella);
                });
    }
}