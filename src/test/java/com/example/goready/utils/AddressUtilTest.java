package com.example.goready.utils;

import com.example.goready.dto.Address;
import com.example.goready.global.exception.GlobalException;
import com.example.goready.global.response.status.ErrorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AddressUtilTest {

    private AddressUtil addressUtil;

    @Value("${kakao.rest-api-key}")
    private String apiKey;

    @BeforeEach
    void setUp() {
        // 실제 AddressUtil 인스턴스 생성
        addressUtil = new AddressUtil();
        ReflectionTestUtils.setField(addressUtil, "API_KEY", apiKey);
    }

    @Test
    void testGetAddressSuccess() {
        // given: 유효한 위도와 경도 (서울역 근처)
        double latitude = 37.5665;
        double longitude = 126.9780;

        // when: 실제 API 호출
        Address result = addressUtil.getAddress(latitude, longitude);

        // then: 결과 검증 (정상적으로 주소 정보가 반환되는지 확인)
        assertNotNull(result);
        assertEquals("서울", result.sidoName()); // 시도 검증
        assertEquals("중구", result.cityName());        // 시군구 검증
        assertNotNull(result.dongName());               // 동/읍/면 정보가 있는지 확인
    }

    @Test
    void testGetAddressFailure() {
        // given: 잘못된 위도와 경도
        double latitude = 0.0;
        double longitude = 0.0;

        // when & then: 예외 발생 여부 확인
        GlobalException exception = assertThrows(GlobalException.class, () -> {
            addressUtil.getAddress(latitude, longitude);
        });

        // 예외 검증
        assertEquals(ErrorStatus.GET_ADDRESS_FAIL, exception.getErrorStatus());
    }
}
