package com.example.goready.service.weather;

import com.example.goready.dto.Address;
import com.example.goready.dto.WeatherData;
import com.example.goready.dto.WeatherResponse;
import com.example.goready.dto.response.WeatherResponse;
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
        // 1. 기상청 API에서 데이터 조회
        Mono<WeatherData> weatherData = weatherApiService.getWeatherInfo(lon, lat);

        // weatherData 가져옴
        // 어제날씨와 비교 로직
        // isUmbrella 로직


        // 3. 여러 비동기 데이터 병합
        return
    }

        // 3. WeatherDto 반환


//        // 2. 강수 확률에 따라 우산 필요 여부 결정
//        boolean isUmbrella = weatherData.getRainPer() >= 50;
//
//        // 3. 현재 기온과 어제 기온 비교
//        String status = compareTemperature(weatherData.getCurrentTemp(), weatherData.getYesterdayTemp());
//
//        // 4. 어제와 기온 차
//        Integer diffTemp = Math.abs(weatherData.getCurrentTemp() - weatherData.getYesterdayTemp());
//
//        // 5. WeatherResponseDto 생성 및 반환
//        return WeatherResponse.WeatherDto.builder()
//                .status(status)
//                .isUmbrella(isUmbrella)
//                .highTemp(weatherData.getHighTemp())
//                .lowTemp(weatherData.getLowTemp())
//                .currentTemp(weatherData.getCurrentTemp())
//                .diffTemp(diffTemp)
//                .rainPer(weatherData.getRainPer())
//                .build();
//    }

//    private String compareTemperature(int currentTemp, int yesterdayTemp) {
//        if (currentTemp > yesterdayTemp) {
//            return "hot";
//        } else if (currentTemp < yesterdayTemp) {
//            return "cold";
//        } else {
//            return "same";
//        }
//    }
//
//    private String formatAddress(Address address) {
//        return String.format("%s %s %s", address.sidoName(), address.cityName(), address.dongName());
//    }
//}

