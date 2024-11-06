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
     * 위도와 경도를 기반으로 오늘의 최고, 최저 기온 및 강수 정보를 조회합니다.
     * @param lon 경도 - x
     * @param lat 위도 - y
     * @return 기온 및 강수 정보 DTO
     */
    public Mono<WeatherResponse.RainDto> getRainInfo(double lon, double lat) {
        // 오늘 날짜 baseDate와 3시간 단위의 baseTime을 결정합니다.
        String baseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = getTodayBaseTime();

        // 위도, 경도를 사용하여 격좌좌표를 계산합니다.
        LonXLatY xy = GridUtils.convertGRID_GPS(GridUtils.TO_GRID, lon, lat);

        // Redis 키 생성
        String redisKey = generateRedisKey(xy, baseDate, baseTime);

        // Redis에 저장된 기온 및 강수 데이터(today)가 있는지 확인
        String cachedRainData = redisUtil.getValues(redisKey);
        if (redisUtil.checkExistsValue(cachedRainData)) {
            // 캐시된 데이터가 있으면 WeatherResponse를 생성하여 반환
            return Mono.just(createRainDtoFromCache(cachedRainData));
        }
        // 캐시된 데이터가 없으면 API 호출하여 데이터 조회
        return fetchRainDataFromApi(xy, redisKey, baseDate, baseTime);
    }

    /**
     * 격좌 좌표 xy와 날짜와 현재 시각을 기반으로 Redis 키를 생성합니다.
     * @param xy 격좌좌표
     * @param baseDate 오늘 날짜
     * @param baseTime 3시간 단위의 현재 시각
     * @return Redis 키 문자열
     */
    private String generateRedisKey(LonXLatY xy, String baseDate, String baseTime) {
        String redisKey = String.format("todayWeather:%f:%f:%s:%s", xy.x, xy.y, baseDate, baseTime);
        return redisKey;
    }

    /**
     * 캐시된 강수 및 기온 데이터를 기반으로 WeatherResponse를 생성합니다.
     * @param cachedRainData 캐시된 강수 데이터
     * @return WeatherResponse.RainDto
     */
    private WeatherResponse.RainDto createRainDtoFromCache(String cachedRainData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // JSON 문자열을 RainDto 객체로 변환
            WeatherResponse.RainDto rainDto = objectMapper.readValue(cachedRainData, WeatherResponse.RainDto.class);

            return rainDto
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * 외부 API에서 강수 데이터를 조회하고 Redis에 저장합니다.
     * @param xy 격좌 좌표
     * @param redisKey Redis 키
     * @return WeatherResponse.RainDto
     */
    public Mono<WeatherResponse.RainDto> fetchRainDataFromApi(LonXLatY xy, String redisKey, String baseDate, String baseTime) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("apihub.kma.go.kr")
                        .path("/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst")
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 1000)
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
                .map(response -> processApiResponse(response, redisKey, baseDate, baseTime));
    }

    /**
     * API 응답을 처리하고, 강수확률, 최고기온, 최저기온 값을 추출하여 Redis에 캐싱하며, 응답 DTO를 생성합니다.
     * @param response API 응답 JSON

     * @param redisKey Redis 키
     * @return WeatherResponse.RainDTO
     */
    private WeatherResponse.RainDto processApiResponse(String response, String redisKey, String baseDate, String baseTime) {
        int maxTemp = extractValue(response, "TMX", baseDate, baseTime);
        int minTemp = extractValue(response, "TMN", baseDate, baseTime);
        int rainPer = extractValue(response, "POP", baseDate, baseTime);
        // RainDto 객체 생성
        WeatherResponse.RainDto rainDto = WeatherConverter.toRainDto(maxTemp, minTemp, rainPer);

        // JSON 직렬화를 위해 ObjectMapper 사용
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String rainDtoJson = objectMapper.writeValueAsString(rainDto);
            // Redis에 저장
            redisUtil.setValues(redisKey, rainDtoJson, Duration.ofHours(4));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.INTERNAL_SERVER_ERROR); // 예외 처리
        }

        // RainDto 반환
        return rainDto;
    }

    /**
     * API 응답 JSON 에서 category와 현시각에 가장 가까운 데이터 값들 추출
     * @param response API 응답 JSON
     * @param category 필요한 데이터 카테고리
     * @return 데이터 값
     */
    private int extractValue(String response, String category, String baseDate, String baseTime) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items");

            if (itemsNode.isArray()) {
                // baseTime에 1시간을 더한 값을 계산
                int baseTimeInt = Integer.parseInt(baseTime);
                int nextHourTime = baseTimeInt + 100;
                if (nextHourTime >= 2400) {
                    nextHourTime -= 2400; // 시간을 24시간 형식으로 맞추기 위해 2400을 빼줌
                }
                String nextHourTimeStr = String.format("%04d", nextHourTime); // 4자리 형식으로 맞추기

                for (JsonNode itemNode : itemsNode) {
                    String itemCategory = itemNode.path("category").asText();
                    String itemFcstDate = itemNode.path("fcstDate").asText();
                    String itemFcstTime = itemNode.path("fcstTime").asText();

                    // 조건에 맞는 데이터 찾기
                    if (category.equals(itemCategory)
                            && baseDate.equals(itemFcstDate)
                            && nextHourTimeStr.equals(itemFcstTime)) {
                        return itemNode.path("fcstValue").asInt();
                    }
                }

                // 매칭된 시/군/구 이름이 없는 경우 예외 처리
                System.out.println("매칭된 데이터가 없습니다: " + category + ", " + baseDate + ", " + baseTime);
                throw new GlobalException(ErrorStatus.DUST_DATA_NOT_FOUND);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.DUST_SERVER_ERROR);
        }
        return 0;
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




