package com.example.goready.service.weather;

import com.example.goready.dto.WeatherData;
import com.example.goready.dto.response.WeatherResponseDto;
import com.example.goready.utils.GridUtils;
import com.example.goready.utils.LonXLatY;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherApiService {

    @Value("${weather.api-key}")
    private String WEATHER_API_KEY;

    private final WebClient webClient;
    private final RedisTemplate<String, WeatherData> redisTemplate;

    public WeatherData getWeatherData(double lon, double lat) {
        // 1. 위치, 날짜
        LonXLatY grid = GridUtils.convertGRID_GPS(GridUtils.TO_GRID, lon, lat);
        String baseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 2. 단기 예보 데이터 조회
        WeatherData todayData = fetchToday(grid.x, grid.y, baseDate);

        // 3. 초단기 실황 데이터 조회
        WeatherData nowData = fetchNow(grid.x, grid.y, baseDate);

        // 4. 두 데이터 합쳐 반환
        return new WeatherData(
                todayData.getHighTemp(),
                todayData.getLowTemp(),
                todayData.getRainPer(),
                nowData.getCurrentTemp(),
                nowData.getYesterdayTemp()
        );
    }

    public WeatherData fetchToday(double x, double y, String baseDate) {

        // 1. 가장 가까운 baseTime 찾기 (3시간 단위)
        String baseTime = getTodayBaseTime();

        // 2. Redis 키 생성
        String redisKey = String.format("weather:today:%s:%s:%f:%f", baseDate, baseTime, x, y);

        // 3. 캐시 데이터 확인
        ValueOperations<String, WeatherData> valueOps = redisTemplate.opsForValue();
        WeatherData cachedData = valueOps.get(redisKey);

        if (cachedData != null) {
            // 캐시된 데이터가 있을 경우 바로 반환
            return cachedData;
        }

        // 4. 캐시 데이터 없을 경우, API 호출
        String url = String.format(
                "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst?pageNo=1&numOfRows=1000&dataType=JSON&base_date=%s&base_time=%s&nx=%d&ny=%d&authKey=%s",
                baseDate, baseTime, (int) x, (int) y, WEATHER_API_KEY
        );

        Map<String, Object> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 응답 출력
        System.out.println("API response: " + response);

        // 5. response 구조 확인 후 items 추출
        Map<String, Object> items = null;
        if (response != null && response.containsKey("response")) {
            Map<String, Object> responseMap = (Map<String, Object>) response.get("response");
            if (responseMap.containsKey("body")) {
                items = (Map<String, Object>) responseMap.get("body");
            }
        }

        // items가 null인 경우 처리
        if (items == null) {
            System.err.println("items가 null입니다. 응답 구조를 다시 확인하세요.");
            return null;  // 혹은 적절한 예외 처리
        }
//        Map<String, Object> items = extractItems(response);

        int highTemp = parseInt(items.get("TMX")); // 오늘 최고 기온
        int lowTemp = parseInt(items.get("TMN")); // 오늘 최저 기온
        int rainPer = parseInt(items.get("POP")); // 강수 확률

        WeatherData todayData = new WeatherData(highTemp, lowTemp, rainPer, null, null);

        // 4. Redis에 캐싱 (4시간 동안 유효)
        valueOps.set(redisKey, todayData, Duration.ofHours(4));

        return todayData;
    }

    private WeatherData fetchNow(double x, double y, String baseDate) {
        String baseDateYesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 1. 한 시간 단위의 basetime 찾기 (현재 시각)
        String baseTime = getNowBaseTime();

        // 2. Redis 키 생성
        String redisKeyNow = String.format("weather:temp:%s:%s:%f:%f", baseDate, baseTime, x, y);

        // 3. 캐시 데이터 확인
        ValueOperations<String, WeatherData> valueOps = redisTemplate.opsForValue();
        WeatherData cachedDataNow = valueOps.get(redisKeyNow);

        if (cachedDataNow != null) {
            // 캐시된 데이터가 있을 경우 바로 반환
            return cachedDataNow;
        }

        // 4. 어제의 기온 캐시데이터 확인
        String redisKeyYesterday = String.format("weather:temp:%s:%s:%f:%f", baseDateYesterday, baseTime, x, y);
        WeatherData cachedDataYesterday = valueOps.get(redisKeyYesterday);
        Integer yesterdayTemp;

        if (cachedDataYesterday != null) {
            // 캐시된 어제 데이터가 있을 경우
            yesterdayTemp = cachedDataYesterday.getCurrentTemp();
        } else {
            // 어제 데이터가 없을 경우, API 호출로 어제 기온 조회
            yesterdayTemp = fetchYesterday(x, y, baseDateYesterday, baseTime);
        }

        // 5. 오늘의 현재 기온 API 호출
        String url = String.format(
                "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0/getUltraSrtNcst?pageNo=1&numOfRows=1000&dataType=XML&base_date=20210628&base_time=0600&nx=55&ny=127&authKey=%s",
                baseDate, baseTime, (int) x, (int) y, WEATHER_API_KEY
        );

        Map<String, Object> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> items = extractItems(response);

        int currentTemp = parseInt(items.get("T1H")); // 현재 기온

        WeatherData tempData = new WeatherData(null, null, null, currentTemp, yesterdayTemp);

        // 4. Redis에 캐싱 (1일 동안 유효)
        valueOps.set(redisKeyNow, tempData, Duration.ofHours(25));

        return tempData;
    }

    private Integer fetchYesterday(double x, double y, String baseDateYesterday, String baseTime){
        // 어제 데이터가 없을 경우, API 호출로 어제 기온 조회
        String url = String.format(
                "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0/getUltraSrtNcst?pageNo=1&numOfRows=1000&dataType=XML&base_date=%s&base_time=%s&nx=%d&ny=%d&authKey=%s",
                baseDateYesterday, baseTime, (int) x, (int) y, WEATHER_API_KEY
        );

        Map<String, Object> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 데이터 추출 (어제 기온)
        Map<String, Object> items = extractItems(response);
        int yesterdayTemp = parseInt(items.get("T1H"));

        return yesterdayTemp;
    }

    private String getTodayBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();

        // 10분 이전인 경우 1시간 전으로 설정
        if (minute < 10) {
            hour -= 1;
            if (hour < 0) {
                hour = 23;
            }
        }

        // 현재 시간을 3으로 나눈 나머지가 2가 되도록 맞추기
        int baseHour = hour;

        if (hour % 3 != 2) {
            baseHour = hour - hour % 3 -1;
        }

        return String.format("%02d00", baseHour);
    }

    private String getNowBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();

        // 10분 이후라면 현재 시각을 사용, 10분 이전이라면 이전 시간을 사용
        if (minute >= 10) {
            return String.format("%02d00", hour);
        } else {
            int previousHour = (hour == 0) ? 23 : hour - 1; // 자정이면 전날 23시로 돌아감
            return String.format("%02d00", previousHour);
        }
    }

    private Map<String, Object> extractItems(Map<String, Object> response) {
        return (Map<String, Object>) ((Map<String, Object>) response.get("response")).get("body");
    }

    private int parseInt(Object value) {
        return Integer.parseInt(value.toString());
    }

    private String generateRedisKey(double lon, double lat, String baseDate, String baseTime) {
        return String.format("weather:%s:%s:%f:%f", baseDate, baseTime, lon, lat);
    }
}




