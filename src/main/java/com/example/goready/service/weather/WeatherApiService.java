package com.example.goready.service.weather;

import com.example.goready.converter.WeatherConverter;
import com.example.goready.dto.WeatherData;
import com.example.goready.global.exception.GlobalException;
import com.example.goready.global.response.status.ErrorStatus;
import com.example.goready.utils.GridUtils;
import com.example.goready.dto.LonXLatY;
import com.example.goready.utils.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
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
    public WeatherData getWeatherInfo(double lon, double lat) {

        String baseDate = getBaseDate();
        String yesterDate = getYesterDate();

        LonXLatY xy = gridUtils.convertGRID_GPS(lon, lat);
        if (xy.x == 0 && xy.y == 0) {
            System.out.println("Error: Invalid location coordinates. x: " + xy.x + ", y: " + xy.y);
            throw new GlobalException(ErrorStatus.LOCATION_BAD_REQUEST);
        } // 위도 경도 잘못 요청

        String redisKey = generateRedisKey(0, xy); // 오늘 날짜 rediskey
        String yesterdayRedisKey = generateRedisKey(1, xy); // 어제 날짜 rediskey

        String cachedWeatherData = redisUtil.getValues(redisKey); // 오늘 데이터
        String cachedYesterdayData = redisUtil.getValues(yesterdayRedisKey); // 어제 데이터

        Integer yesterdayTemp;
        if (redisUtil.checkExistsValue(cachedWeatherData)) {
            // 오늘 캐시된 데이터가 있으면 바로 반환
            return createWeatherDtoFromCache(cachedWeatherData);
        } else if (redisUtil.checkExistsValue(cachedYesterdayData)) {
            // 어제 캐시된 데이터가 있으면 해당 데이터의 currentTemp를 yesterdayTemp로 가져옴
            yesterdayTemp = getyesterDataFromCache(cachedYesterdayData);
        } else {
            // 모두 없으면 api 호출
            yesterdayTemp = fetchYesterDataFromApi(xy, yesterDate);
        }
        WeatherData weatherData = fetchWeatherDataFromApi(xy, redisKey, baseDate);
        weatherData.setYesterdayTemp(yesterdayTemp);

        saveWeatherDataToRedis(redisKey, weatherData, Duration.ofDays(1));

        return weatherData;
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
            throw new GlobalException(ErrorStatus.WEATHER_CACHE_ERROR);
        }

    }

    /**
     * 캐시된 어제의 데이터로 yesterdayTemp를 생성합니다.
     * @param cachedYesterdayData 캐시된 어제의 날씨 데이터
     * @return yesterdayTemp
     */
    private Integer getyesterDataFromCache(String cachedYesterdayData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(cachedYesterdayData);
            int yesterdayTemp = jsonNode.get("currentTemp").asInt();  // 어제의 currentTemp = 오늘의 yesterdayTemp
            return yesterdayTemp;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.WEATHER_CACHE_ERROR);
        }
    }

    /**
     * 외부 API에서 오늘 날씨 데이터를 조회하고 Redis에 저장합니다.
     * @param xy 격좌 좌표
     * @param redisKey Redis 키
     * @param baseDate 오늘 날짜
     * @return weatherData
     */
    private WeatherData fetchWeatherDataFromApi(LonXLatY xy, String redisKey, String baseDate) {
        String baseTime = getBaseTime(false);
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apihub.kma.go.kr")
                            .path("/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst")
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 270)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", baseDate)
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", xy.x)
                            .queryParam("ny", xy.y)
                            .queryParam("authKey", WEATHER_API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return processApiResponse(response, redisKey);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new GlobalException(ErrorStatus.WEATHER_API_KEY_ERROR);
            } else {
                throw new GlobalException(ErrorStatus.WEATHER_SERVER_ERROR);
            }
        } catch (Exception e) {
            throw new GlobalException(ErrorStatus.WEATHER_SERVER_ERROR);
        }
    }

    /**
     * API 응답을 처리하고 강수확률, 최고기온, 최저기온, 현재기온 값을 추출하여 Redis에 캐싱하며, weatherData를 생성합니다.
     * @param response API 응답 JSON
     * @param redisKey Redis 키
     * @return weatherData
     */
    private WeatherData processApiResponse(String response, String redisKey) {
        System.out.println(response);
        int maxTemp = extractTM(response, "TMX");
        int minTemp = extractTM(response, "TMN");
        int rainPer = extractValue(response, "POP", false);
        int currentTemp = extractValue(response, "TMP", false);

        WeatherData weatherData = WeatherConverter.toWeatherData(maxTemp, minTemp, rainPer, currentTemp);

        // JSON 직렬화를 위해 ObjectMapper 사용
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String weatherDataJson = objectMapper.writeValueAsString(weatherData);
            // Redis에 저장
            redisUtil.setValues(redisKey, weatherDataJson, Duration.ofHours(24));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.WEATHER_SERVER_ERROR); // 예외 처리
        }

        // WeatherData 반환
        return weatherData;
    }

    /**
     * API 응답에서 오늘의 강수확률, 현재 기온, 어제의 기온을 추출합니다.
     * @param response API 응답 JSON
     * @param category 필요한 데이터 카테고리
     * @return fcstValue
     */
    private int extractValue(String response, String category, boolean isYesterday) {
        LocalDateTime now = LocalDateTime.now();
        String hour = now.format(DateTimeFormatter.ofPattern("HH00"));
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // isYesterday가 true이면 어제 날짜로 변경
        if (isYesterday) {
            now = now.minusDays(1);
            date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            for (JsonNode item : itemsNode) {
                if (category.equals(item.path("category").asText()) &&
                        date.equals(item.path("fcstDate").asText()) &&
                        hour.equals(item.path("fcstTime").asText())) {

                    JsonNode fcstValueNode = item.path("fcstValue");
                    if (fcstValueNode.isMissingNode()) {
                        return 0;
                    }
                    return fcstValueNode.asInt();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.WEATHER_DATA_NOT_FOUND);
        }
        return 0;
    }

    /**
     * 오늘의 최고기온 또는 최저기온을 추출합니다.
     * @param response
     * @param category
     * @return fcstValue
     */
    private int extractTM(String response, String category) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            for (JsonNode item : itemsNode) {
                if (category.equals(item.path("category").asText()) &&
                        date.equals(item.path("fcstDate").asText())){

                    JsonNode fcstValueNode = item.path("fcstValue");
                    if (fcstValueNode.isMissingNode()) {
                        return 0;
                    }
                    return fcstValueNode.asInt();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.WEATHER_DATA_NOT_FOUND);
        }
        return 0;
    }

    /**
     * 어제 기온 api 조회
     * @param xy 격자좌표
     * @param yesterDate 어제 날짜
     * @return 어제 기온 데이터
     */

    private Integer fetchYesterDataFromApi(LonXLatY xy, String yesterDate) {
        String baseTime = getBaseTime(true);
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apihub.kma.go.kr")
                            .path("/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst")
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 40)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", yesterDate)
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", xy.x)
                            .queryParam("ny", xy.y)
                            .queryParam("authKey", WEATHER_API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractValue(response, "TMP", true);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new GlobalException(ErrorStatus.WEATHER_API_KEY_ERROR);
            } else {
                throw new GlobalException(ErrorStatus.WEATHER_SERVER_ERROR);
            }
        } catch (Exception e) {
            throw new GlobalException(ErrorStatus.WEATHER_SERVER_ERROR);
        }
    }

    /**
     * url 경로에 지정할 baseTime을 설정합니다.
     * 현재시각이 02시 전이면 2300를 리턴
     * 02시 이후면 오늘 날씨를 조회하는 api는 0200를, 어제 날씨를 조회하는 api는 가까운 3시간 단위의 baseTime으로 설정한다.
     * @param isYesterday 어제 날씨를 조회하는지 확인
     * @return baseTime
     */
    private String getBaseTime(boolean isYesterday) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        if (hour >= 0 && hour <= 2) {
            return "2300";
        }
        else if (isYesterday) {
            int baseHour = hour - hour % 3 -1;
            return String.format("%02d00", baseHour);
        }
        else {
            return "0200";
        }
    }

    /**
     * url 경로에 지정할 baseDate를 설정합니다.
     * 현재 시각이 02시 이전이면 어제 날짜로 지정함.
     * @return baseDate
     */
    private String getBaseDate() {
        LocalDateTime now = LocalDateTime.now();

        if (now.getHour() >= 0 && now.getHour() <= 2) {
            return now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        return now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 어제 날씨 조회 api의 url에 지정할 baseDate를 설정합니다.
     * 현재 시각이 02시 이전이면 이틀 전 날짜로 지정함
     * @return yesterDate
     */
    private String getYesterDate(){
        LocalDateTime now = LocalDateTime.now();

        if (now.getHour() >= 0 && now.getHour() <= 2) {
            return now.minusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        return now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 격자좌표 x,y를 받아 RedisKey를 생성합니다.
     * mode가 1이면 어제 날짜의 RedisKey를 생성, 0이면 오늘 날짜로 생성합니다.
     * @param mode
     * @param xy
     * @return redisKey
     */
    private String generateRedisKey(int mode, LonXLatY xy) {
        String timeKey = "";
        // 모드에 따라 날짜 설정
        if (mode == 1) {
            timeKey = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));
        } else {
            timeKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));
        }

        return "weatherInfo:" + "X" + xy.x + "Y" + xy.y + ":" + timeKey;
    }

    /**
     * weatherData 객체를 Redis에 하루동안 저장합니다.
     * @param redisKey
     * @param weatherData
     * @param duration
     */
    private void saveWeatherDataToRedis(String redisKey, WeatherData weatherData, Duration duration) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String weatherDataJson = objectMapper.writeValueAsString(weatherData);
            redisUtil.setValues(redisKey, weatherDataJson, duration);
            log.info("Saving weather data to Redis with key: {}", redisKey);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.WEATHER_CACHE_ERROR);
        }
    }

}
