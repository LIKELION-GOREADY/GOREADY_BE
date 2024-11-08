package com.example.goready.service.weather;

import com.example.goready.converter.MaskConverter;
import com.example.goready.converter.WeatherConverter;
import com.example.goready.dto.Address;
import com.example.goready.dto.MaskResponse;
import com.example.goready.dto.WeatherData;
import com.example.goready.dto.WeatherResponse;
import com.example.goready.global.exception.GlobalException;
import com.example.goready.global.response.status.ErrorStatus;
import com.example.goready.utils.GridUtils;
import com.example.goready.utils.LonXLatY;
import com.example.goready.utils.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherApiService {

    @Value("${weather.api-key}")
    private String WEATHER_API_KEY;

    private final WebClient webClient;
    private final RedisUtil redisUtil;
    private final GridUtils gridUtils;

    /**
     * 위도와 경도를 기반으로 날씨 정보를 조회합니다.
     * @param lon 경도 - x
     * @param lat 위도 - y
     * @return 날씨 Data
     */
    public Mono<WeatherData> getWeatherInfo(double lon, double lat) {
        // 오늘 날짜 baseDate와 3시간 단위의 baseTime을 결정합니다.
        String baseDate = getBaseDate();
        String baseTime = getBaseTime();
        String yesterDate = getYesterDate();

        // 위도, 경도를 사용하여 격좌좌표를 계산합니다.
        LonXLatY xy = gridUtils.convertGRID_GPS(0, lon, lat);

        // Redis 키 생성 - 오늘 날짜 기준
        String redisKey = generateRedisKey(0, xy);

        // Redis 키 생성 - 어제 날짜 기준
        String yesterdayRedisKey = generateRedisKey(1, xy);

        // Redis에 저장된 WeatherData 가 있는지 확인
        String cachedWeatherData = redisUtil.getValues(redisKey);
        String cachedYesterdayData = redisUtil.getValues(yesterdayRedisKey);
        if (redisUtil.checkExistsValue(cachedWeatherData)) {
            // 캐시된 데이터가 있으면 WeatherData를 생성하여 반환
            return Mono.just(createWeatherDtoFromCache(cachedWeatherData));
        }
        else if (redisUtil.checkExistsValue(cachedYesterdayData)) {
            // 어제 날짜의 캐시된 데이터가 있으면 해당 캐시 데이터의 currentTemp를 yesterdayTemp로 가져와 WeatherData 생성
            Mono<Integer> yesterdayTempMono = getyesterDataFromCache(cachedYesterdayData);
        }
        // 캐시된 데이터가 없으면 API 호출하여 데이터 조회
        Mono<Integer> yesterdayTempMono = fetchYesterDataFromApi(xy, yesterDate, baseTime);
        Mono<WeatherData> weatherDataMono = fetchWeatherDataFromApi(xy, redisKey, baseDate, baseTime);
        weatherDataMono = yesterdayTempMono.flatMap(y -> {
            WeatherData weatherData = new WeatherData();
            weatherData.setYesterdayTemp(y);
            // 현재 날씨 데이터를 가져오는 부분이 필요 (예: fetchWeatherDataFromApi)
            return Mono.just(weatherData);  // WeatherData 객체 반환
        });
        weatherData.setYesterdayTemp(yesterdayTemp);

        // Redis에 저장
        setRedis(weatherData, redisKey);

        return Mono.just(weatherData);
    }

    /**
     * 캐시된 날씨 데이터를 기반으로 WeatherResponse를 생성합니다.
     * @param cachedWeatherData 캐시된 오늘 날씨 데이터
     * @return WeatherResponse.RainDto
     */
    private WeatherData createWeatherDtoFromCache(String cachedWeatherData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // JSON 문자열을 weatherData 객체로 변환
            WeatherData weatherData = objectMapper.readValue(cachedWeatherData, WeatherData.class);

            return weatherData;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * 캐시된 어제의 데이터로 yesterdayTemp를 생성합니다.
     * @param cachedYesterdayData 캐시된 어제의 날씨 데이터
     * @return yesterdayTemp
     */
    private Mono<Integer> getyesterDataFromCache(String cachedYesterdayData) {
        return Mono.just(cachedYesterdayData)
                .flatMap(data -> {
                    try {
                        // JSON 문자열을 JsonNode로 변환하여 yesterdayTemp 필드만 가져오기
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode jsonNode = objectMapper.readTree(data);
                        int yesterdayTemp = jsonNode.get("yesterdayTemp").asInt();  // yesterdayTemp 필드 추출
                        return Mono.just(yesterdayTemp);  // Mono로 반환
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Mono.error(new GlobalException(ErrorStatus.INTERNAL_SERVER_ERROR));  // 에러 처리
                    }
                });
    }

    /**
     * 외부 API에서 강수 데이터를 조회하고 Redis에 저장합니다.
     * @param xy 격좌 좌표
     * @param redisKey Redis 키
     * @return WeatherResponse.RainDto
     */
    private Mono<WeatherData> fetchWeatherDataFromApi(LonXLatY xy, String redisKey, String baseDate, String baseTime) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("apihub.kma.go.kr")
                        .path("/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst")
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 50)
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", xy.x)
                        .queryParam("ny", xy.y)
                        .queryParam("authKey", WEATHER_API_KEY)
                        .queryParam("returnType", "json")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> processApiResponse(response, redisKey, baseTime));
    }

    /**
     * API 응답을 처리하고, 강수확률, 최고기온, 최저기온 값을 추출하여 Redis에 캐싱하며, 응답 DTO를 생성합니다.
     * @param response API 응답 JSON

     * @param redisKey Redis 키
     * @return WeatherResponse.RainDTO
     */
    private WeatherData processApiResponse(String response, String redisKey, String baseTime) {
        int maxTemp = extractValue(response, "TMX");
        int minTemp = extractValue(response, "TMN");
        int rainPer = extractValue(response, "POP");
        int currentTemp = extractValue(response, "TMP");

        WeatherData weatherData = WeatherConverter.toWeatherData(maxTemp, minTemp, rainPer, currentTemp);

        // JSON 직렬화를 위해 ObjectMapper 사용
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String weatherDataJson = objectMapper.writeValueAsString(weatherData);
            // Redis에 저장
            redisUtil.setValues(redisKey, weatherDataJson, Duration.ofHours(24));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.INTERNAL_SERVER_ERROR); // 예외 처리
        }

        // WeatherData 반환
        return weatherData;
    }

    /**
     * API 응답 JSON 에서 category와 현시각에 가장 가까운 데이터 값들 추출
     * @param response API 응답 JSON
     * @param category 필요한 데이터 카테고리
     * @return 데이터 값
     */
    private int extractValue(String response, String category) {
        LocalDateTime now = LocalDateTime.now();
        String hour = now.format(DateTimeFormatter.ofPattern("%02d00"));
        String date = now.format(DateTimeFormatter.ofPattern("yyyymmdd"));
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items");

            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    if (category.equals(itemNode.get("category").asText()) && date.equals(itemNode.get("fcstDate").asText()) && hour.equals(itemNode.get("fcstTime").asText())) {
                        System.out.println(itemNode.path("fcstValue").asInt());
                        return itemNode.path("fcstValue").asInt();
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.INTERNAL_SERVER_ERROR);
        }
        return 0;
    }

    /**
     * 어제 기온 api 조회
     * @param baseTime api 요청 시각
     * @return 어제 기온 데이터
     */

    private Mono<Integer> fetchYesterDataFromApi(LonXLatY xy, String yesterDate, String baseTime) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("apihub.kma.go.kr")
                        .path("/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst")
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 50)
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", yesterDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", xy.x)
                        .queryParam("ny", xy.y)
                        .queryParam("authKey", WEATHER_API_KEY)
                        .queryParam("returnType", "json")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> extractValue(response, "TMP"));
    }

    private String getBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        if (hour >= 0 && hour <= 2) {
            return "2300";
        }

        int baseHour = hour - hour % 3 -1;
        return String.format("%02d00", baseHour);
    }

    private String getBaseDate() {
        LocalDateTime now = LocalDateTime.now();

        if (now.getHour() >= 0 && now.getHour() <= 2) {
            return now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        return now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    private String getYesterDate(){
        LocalDateTime now = LocalDateTime.now();

        if (now.getHour() >= 0 && now.getHour() <= 2) {
            return now.minusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        return now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    private String generateRedisKey(int mode, LonXLatY xy) {
        String timeKey = "";
        // 모드에 따라 날짜 설정
        if (mode == 1) {
            timeKey = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));
        } else {
            timeKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));
        }

        return "weatherInfo:" + xy + ":" + timeKey;
    }
    public Mono<Void> saveWeatherDataToRedis(WeatherData weatherData, String redisKey, ReactiveRedisTemplate<String, String> redisTemplate) {
        try {
            // WeatherData 객체를 JSON으로 직렬화
            ObjectMapper objectMapper = new ObjectMapper();
            String weatherDataJson = objectMapper.writeValueAsString(weatherData);

            // Redis에 저장
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            return valueOperations.set(redisKey, weatherDataJson).then();  // 저장 후 Mono<Void> 반환
        } catch (Exception e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
//
//    public WeatherData fetchToday(double x, double y, String baseDate) {
//
//        // 1. 가장 가까운 baseTime 찾기 (3시간 단위)
//        String baseTime = getTodayBaseTime();
//
//        // 2. Redis 키 생성
//        String redisKey = String.format("weather:today:%s:%s:%f:%f", baseDate, baseTime, x, y);
//
//        // 3. 캐시 데이터 확인
//        ValueOperations<String, WeatherData> valueOps = redisTemplate.opsForValue();
//        WeatherData cachedData = valueOps.get(redisKey);
//
//        if (cachedData != null) {
//            // 캐시된 데이터가 있을 경우 바로 반환
//            return cachedData;
//        }
//
//        // 4. 캐시 데이터 없을 경우, API 호출
//        String url = String.format(
//                "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst?pageNo=1&numOfRows=1000&dataType=JSON&base_date=%s&base_time=%s&nx=%d&ny=%d&authKey=%s",
//                baseDate, baseTime, (int) x, (int) y, WEATHER_API_KEY
//        );
//
//        Map<String, Object> response = webClient.get()
//                .uri(url)
//                .retrieve()
//                .bodyToMono(Map.class)
//                .block();
//
//        // 응답 출력
//        System.out.println("API response: " + response);
//
//        // 5. response 구조 확인 후 items 추출
//        Map<String, Object> items = null;
//        if (response != null && response.containsKey("response")) {
//            Map<String, Object> responseMap = (Map<String, Object>) response.get("response");
//            if (responseMap.containsKey("body")) {
//                items = (Map<String, Object>) responseMap.get("body");
//            }
//        }
//
//        // items가 null인 경우 처리
//        if (items == null) {
//            System.err.println("items가 null입니다. 응답 구조를 다시 확인하세요.");
//            return null;  // 혹은 적절한 예외 처리
//        }
////        Map<String, Object> items = extractItems(response);
//
//        int highTemp = parseInt(items.get("TMX")); // 오늘 최고 기온
//        int lowTemp = parseInt(items.get("TMN")); // 오늘 최저 기온
//        int rainPer = parseInt(items.get("POP")); // 강수 확률
//
//        WeatherData todayData = new WeatherData(highTemp, lowTemp, rainPer, null, null);
//
//        // 4. Redis에 캐싱 (4시간 동안 유효)
//        valueOps.set(redisKey, todayData, Duration.ofHours(4));
//
//        return todayData;
//    }
//
//    private WeatherData fetchNow(double x, double y, String baseDate) {
//        String baseDateYesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//
//        // 1. 한 시간 단위의 basetime 찾기 (현재 시각)
//        String baseTime = getNowBaseTime();
//
//        // 2. Redis 키 생성
//        String redisKeyNow = String.format("weather:temp:%s:%s:%f:%f", baseDate, baseTime, x, y);
//
//        // 3. 캐시 데이터 확인
//        ValueOperations<String, WeatherData> valueOps = redisTemplate.opsForValue();
//        WeatherData cachedDataNow = valueOps.get(redisKeyNow);
//
//        if (cachedDataNow != null) {
//            // 캐시된 데이터가 있을 경우 바로 반환
//            return cachedDataNow;
//        }
//
//        // 4. 어제의 기온 캐시데이터 확인
//        String redisKeyYesterday = String.format("weather:temp:%s:%s:%f:%f", baseDateYesterday, baseTime, x, y);
//        WeatherData cachedDataYesterday = valueOps.get(redisKeyYesterday);
//        Integer yesterdayTemp;
//
//        if (cachedDataYesterday != null) {
//            // 캐시된 어제 데이터가 있을 경우
//            yesterdayTemp = cachedDataYesterday.getCurrentTemp();
//        } else {
//            // 어제 데이터가 없을 경우, API 호출로 어제 기온 조회
//            yesterdayTemp = fetchYesterday(x, y, baseDateYesterday, baseTime);
//        }
//
//        // 5. 오늘의 현재 기온 API 호출
//        String url = String.format(
//                "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0/getUltraSrtNcst?pageNo=1&numOfRows=1000&dataType=XML&base_date=20210628&base_time=0600&nx=55&ny=127&authKey=%s",
//                baseDate, baseTime, (int) x, (int) y, WEATHER_API_KEY
//        );
//
//        Map<String, Object> response = webClient.get()
//                .uri(url)
//                .retrieve()
//                .bodyToMono(Map.class)
//                .block();
//
//        Map<String, Object> items = extractItems(response);
//
//        int currentTemp = parseInt(items.get("T1H")); // 현재 기온
//
//        WeatherData tempData = new WeatherData(null, null, null, currentTemp, yesterdayTemp);
//
//        // 4. Redis에 캐싱 (1일 동안 유효)
//        valueOps.set(redisKeyNow, tempData, Duration.ofHours(25));
//
//        return tempData;
//    }
//
//    private Integer fetchYesterday(double x, double y, String baseDateYesterday, String baseTime){
//        // 어제 데이터가 없을 경우, API 호출로 어제 기온 조회
//        String url = String.format(
//                "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0/getUltraSrtNcst?pageNo=1&numOfRows=1000&dataType=XML&base_date=%s&base_time=%s&nx=%d&ny=%d&authKey=%s",
//                baseDateYesterday, baseTime, (int) x, (int) y, WEATHER_API_KEY
//        );
//
//        Map<String, Object> response = webClient.get()
//                .uri(url)
//                .retrieve()
//                .bodyToMono(Map.class)
//                .block();
//
//        // 데이터 추출 (어제 기온)
//        Map<String, Object> items = extractItems(response);
//        int yesterdayTemp = parseInt(items.get("T1H"));
//
//        return yesterdayTemp;
//    }
//

//
//    private String getNowBaseTime() {
//        LocalDateTime now = LocalDateTime.now();
//        int hour = now.getHour();
//        int minute = now.getMinute();
//
//        // 10분 이후라면 현재 시각을 사용, 10분 이전이라면 이전 시간을 사용
//        if (minute >= 10) {
//            return String.format("%02d00", hour);
//        } else {
//            int previousHour = (hour == 0) ? 23 : hour - 1; // 자정이면 전날 23시로 돌아감
//            return String.format("%02d00", previousHour);
//        }
//    }
//
//    private Map<String, Object> extractItems(Map<String, Object> response) {
//        return (Map<String, Object>) ((Map<String, Object>) response.get("response")).get("body");
//    }
//
//    private int parseInt(Object value) {
//        return Integer.parseInt(value.toString());
//    }
//
//    private String generateRedisKey(double lon, double lat, String baseDate, String baseTime) {
//        return String.format("weather:%s:%s:%f:%f", baseDate, baseTime, lon, lat);
//    }
//}




