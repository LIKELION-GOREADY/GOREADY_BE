package com.example.goready.service.mask;

import com.example.goready.converter.MaskConverter;
import com.example.goready.dto.Address;
import com.example.goready.dto.MaskResponse;
import com.example.goready.global.exception.GlobalException;
import com.example.goready.global.response.status.ErrorStatus;
import com.example.goready.utils.AddressUtil;
import com.example.goready.utils.RedisUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class MaskService {

    private final AddressUtil addressUtil;
    private final RedisUtil redisUtil;
    private final WebClient webClient;

    @Value("${mask.api-key}")
    private String maskApiKey;

    /**
     * 위도와 경도를 기반으로 마스크 정보를 조회합니다.
     * @param lat 위도
     * @param lon 경도
     * @return 마스크 정보 DTO
     */
    public Mono<MaskResponse.MaskDto> getMaskInfo(double lat, double lon) {
        // 위도, 경도를 사용하여 주소 정보를 가져옵니다.
        return addressUtil.getAddress(lat, lon)
                .flatMap(address -> {
                    // Redis 키 생성
                    String redisKey = generateRedisKey(address);

                    // Redis에 저장된 PM10 데이터가 있는지 확인
                    String cachedPm10Value = redisUtil.getValues(redisKey);
                    if (redisUtil.checkExistsValue(cachedPm10Value)) {
                        // 캐시된 데이터가 있으면 MaskResponse를 생성하여 반환
                        return Mono.just(createMaskResponseFromCache(cachedPm10Value, address));
                    }
                    // 캐시된 데이터가 없으면 API 호출하여 데이터 조회
                    return fetchMaskDataFromApi(address, redisKey);
                });
    }

    /**
     * 주소와 현재 시각을 기반으로 Redis 키를 생성합니다.
     * @param address 주소 정보
     * @return Redis 키 문자열
     */
    private String generateRedisKey(Address address) {
        String timeKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));
        return "maskInfo:" + address.sidoName() + address.cityName() + ":" + timeKey;
    }

    /**
     * 캐시된 PM10 값을 기반으로 MaskResponse를 생성합니다.
     * @param cachedPm10Value 캐시된 PM10 값
     * @param address 주소 정보
     * @return MaskResponse DTO
     */
    private MaskResponse.MaskDto createMaskResponseFromCache(String cachedPm10Value, Address address) {
        int pm10Value = Integer.parseInt(cachedPm10Value);
        boolean isMaskRequired = pm10Value >= 80;
        boolean isAlert = pm10Value >= 300;
        return MaskConverter.toMaskDto(isAlert, isMaskRequired, address);
    }

    /**
     * 외부 API에서 마스크 데이터를 조회하고 Redis에 저장합니다.
     * @param address 주소 정보
     * @param redisKey Redis 캐시 키
     * @return MaskResponse DTO
     */
    private Mono<MaskResponse.MaskDto> fetchMaskDataFromApi(Address address, String redisKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("apis.data.go.kr")
                        .path("/B552584/ArpltnStatsSvc/getCtprvnMesureSidoLIst")
                        .queryParam("serviceKey", maskApiKey)
                        .queryParam("sidoName", address.sidoName())
                        .queryParam("cityName", address.cityName())
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 50)
                        .queryParam("searchCondition", "HOUR")
                        .queryParam("returnType", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
                .bodyToMono(String.class)
                .map(response -> processApiResponse(response, address, redisKey));
    }

    /**
     * API 응답을 처리하고, PM10 값을 추출하여 Redis에 캐싱하며, 응답 DTO를 생성합니다.
     * @param response API 응답 JSON
     * @param address 주소 정보
     * @param redisKey Redis 캐시 키
     * @return MaskResponse DTO
     */
    private MaskResponse.MaskDto processApiResponse(String response, Address address, String redisKey) {
        int pm10Value = extractPm10Value(response, address.cityName());
        redisUtil.setValues(redisKey, String.valueOf(pm10Value), Duration.ofMinutes(60 - LocalDateTime.now().getMinute()));
        boolean isMaskRequired = pm10Value >= 80;
        boolean isAlert = pm10Value >= 300;
        return MaskConverter.toMaskDto(isAlert, isMaskRequired, address);
    }

    /**
     * 4xx 클라이언트 오류를 처리합니다.
     * @param clientResponse 클라이언트 응답
     * @return 커스텀 예외와 함께 Mono 오류 반환
     */
    private Mono<GlobalException> handleClientError(org.springframework.web.reactive.function.client.ClientResponse clientResponse) {
        System.out.println("4xx error occurred: " + clientResponse.statusCode());
        return clientResponse.bodyToMono(String.class)
                .flatMap(errorBody -> Mono.error(new GlobalException(ErrorStatus.DUST_CLIENT_ERROR)));
    }

    /**
     * 5xx 서버 오류를 처리합니다.
     * @param clientResponse 서버 응답
     * @return 커스텀 예외와 함께 Mono 오류 반환
     */
    private Mono<GlobalException> handleServerError(org.springframework.web.reactive.function.client.ClientResponse clientResponse) {
        System.out.println("5xx error occurred: " + clientResponse.statusCode());
        return clientResponse.bodyToMono(String.class)
                .flatMap(errorBody -> Mono.error(new GlobalException(ErrorStatus.DUST_SERVER_ERROR)));
    }

    /**
     * API 응답 JSON에서 도시 이름을 기준으로 PM10 값을 추출합니다.
     * @param response API 응답 JSON
     * @param cityName 도시 이름
     * @return PM10 값
     */
    private int extractPm10Value(String response, String cityName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items");

            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    if (cityName.equals(itemNode.path("cityName").asText())) {
                        return itemNode.path("pm10Value").asInt();
                    }
                }
                // 매칭된 시/군/구 이름이 없는 경우 첫 번째 항목의 pm10Value 반환
                if (!itemsNode.isEmpty()) {
                    System.out.println("매칭된 시/군/구 이름이 없습니다. 첫 번째 항목의 pm10Value를 반환합니다.");
                    return itemsNode.get(0).path("pm10Value").asInt();
                } else {
                    // itemsNode가 비어 있는 경우
                    System.out.println("데이터가 비어 있습니다.");
                    throw new GlobalException(ErrorStatus.DUST_DATA_NOT_FOUND);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GlobalException(ErrorStatus.DUST_SERVER_ERROR);
        }
        return 0;
    }
}

