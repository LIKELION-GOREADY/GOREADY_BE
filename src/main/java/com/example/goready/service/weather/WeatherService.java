package com.example.goready.service.weather;

import com.example.goready.dto.Address;
import com.example.goready.dto.WeatherData;
import com.example.goready.dto.response.WeatherResponseDto;
import com.example.goready.utils.AddressUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherApiService weatherApiService;
    private final AddressUtil addressUtil;

    public WeatherResponseDto getWeather(double lon, double lat) {
        // 1. 기상청 API에서 데이터 조회
        WeatherData weatherData = weatherApiService.getWeatherData(lon, lat);

        // 2. 강수 확률에 따라 우산 필요 여부 결정
        boolean isUmbrella = weatherData.getRainPer() >= 50;

        // 3. 현재 기온과 어제 기온 비교
        String status = compareTemperature(weatherData.getCurrentTemp(), weatherData.getYesterdayTemp());

        // 4. 어제와 기온 차
        Integer diffTemp = Math.abs(weatherData.getCurrentTemp() - weatherData.getYesterdayTemp());

        // 5. 주소 정보 조회
        Address address = addressUtil.getAddress(lat, lon);

        // 6. WeatherResponseDto 생성 및 반환
        return WeatherResponseDto.builder()
                .status(status)
                .isUmbrella(isUmbrella)
                .highTemp(weatherData.getHighTemp())
                .lowTemp(weatherData.getLowTemp())
                .currentTemp(weatherData.getCurrentTemp())
                .diffTemp(diffTemp)
                .rainPer(weatherData.getRainPer())
                .address(formatAddress(address))
                .build();
    }

    private String compareTemperature(int currentTemp, int yesterdayTemp) {
        if (currentTemp > yesterdayTemp) {
            return "hot";
        } else if (currentTemp < yesterdayTemp) {
            return "cold";
        } else {
            return "same";
        }
    }

    private String formatAddress(Address address) {
        return String.format("%s %s %s", address.sidoName(), address.cityName(), address.dongName());
    }
}

