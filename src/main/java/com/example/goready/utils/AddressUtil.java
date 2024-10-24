package com.example.goready.utils;

import com.example.goready.dto.Address;
import com.example.goready.global.exception.GlobalException;
import com.example.goready.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddressUtil {

    @Value("${kakao.rest-api-key}")
    private String API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();

    public Address getAddress(double latitude, double longitude) {
        URI uri = buildKakaoApiUri(latitude, longitude);
        HttpEntity<String> requestEntity = buildKakaoApiRequestEntity();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, Map.class);
            return parseAndReturnAddress(response);
        } catch (Exception e) {
            log.error("위도/경도 기반 주소 조회 중 오류 발생: {}", e.getMessage());
            throw new GlobalException(ErrorStatus.GET_ADDRESS_FAIL);
        }
    }

    private URI buildKakaoApiUri(double latitude, double longitude) {
        try {
            String url = String.format("https://dapi.kakao.com/v2/local/geo/coord2address.json?x=%f&y=%f", longitude, latitude);
            return new URI(url);
        } catch (URISyntaxException e) {
            log.error("URI 생성 실패: {}", e.getMessage());
            throw new GlobalException(ErrorStatus.GET_ADDRESS_FAIL);
        }
    }


    private HttpEntity<String> buildKakaoApiRequestEntity() {
        log.info("Kakao API Key: {}", API_KEY); // 주입된 API 키 출력
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "KakaoAK " + API_KEY);
        return new HttpEntity<>(headers);
    }


    private Address parseAndReturnAddress(ResponseEntity<Map> response) {
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("documents")) {
            throw new GlobalException(ErrorStatus.GET_ADDRESS_FAIL);
        }

        List<Map<String, Object>> documents = (List<Map<String, Object>>) body.get("documents");
        if (documents.isEmpty()) {
            throw new GlobalException(ErrorStatus.GET_ADDRESS_FAIL);
        }

        Map<String, Object> addressInfo = (Map<String, Object>) documents.get(0).get("address");
        String region1Depth = (String) addressInfo.get("region_1depth_name"); // 시도
        String region2Depth = (String) addressInfo.get("region_2depth_name"); // 시군구
        String region3Depth = (String) addressInfo.get("region_3depth_name"); // 동/읍/면

        return new Address(region1Depth, region2Depth, region3Depth);
    }
}
