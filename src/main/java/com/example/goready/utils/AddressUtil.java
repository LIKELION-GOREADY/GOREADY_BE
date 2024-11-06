package com.example.goready.utils;

import com.example.goready.dto.Address;
import com.example.goready.global.exception.GlobalException;
import com.example.goready.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddressUtil {

    @Value("${kakao.rest-api-key}")
    private String API_KEY;

    private final WebClient kakaoWebClient;

    public Mono<Address> getAddress(double latitude, double longitude) {
        String uri = String.format("/v2/local/geo/coord2address.json?x=%f&y=%f", longitude, latitude);

        return kakaoWebClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + API_KEY)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(this::parseAndReturnAddress)
                .onErrorMap(e -> new GlobalException(ErrorStatus.GET_ADDRESS_FAIL));
    }

    private Mono<Address> parseAndReturnAddress(Map<String, Object> body) {
        if (body == null || !body.containsKey("documents")) {
            return Mono.error(new GlobalException(ErrorStatus.GET_ADDRESS_FAIL));
        }

        List<Map<String, Object>> documents = (List<Map<String, Object>>) body.get("documents");
        if (documents.isEmpty()) {
            return Mono.error(new GlobalException(ErrorStatus.GET_ADDRESS_FAIL));
        }

        Map<String, Object> addressInfo = (Map<String, Object>) documents.get(0).get("address");
        String region1Depth = (String) addressInfo.get("region_1depth_name"); // 시도
        String region2Depth = (String) addressInfo.get("region_2depth_name"); // 시군구
        String region3Depth = (String) addressInfo.get("region_3depth_name"); // 동/읍/면

        return Mono.just(new Address(region1Depth, region2Depth, region3Depth));
    }
}

