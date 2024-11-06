package com.example.goready.global.response.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus {

    LOCATION_BAD_REQUEST(HttpStatus.BAD_REQUEST, "위치 요청이 잘못되었습니다."),
    GET_ADDRESS_FAIL(HttpStatus.NOT_FOUND, "주소를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    DUST_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "미세먼지 정보를 가져오는 중 오류가 발생했습니다."),
    DUST_CLIENT_ERROR(HttpStatus.BAD_REQUEST, "미세먼지 정보에 요청에 오류가 발생했습니다."),
    DUST_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 지역의 미세먼지 정보를 찾을 수 없습니다."),;

    private final HttpStatus httpStatus;
    private final String message;
}
