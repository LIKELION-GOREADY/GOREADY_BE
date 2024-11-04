package com.example.goready.global.response.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus {

    SUCCESS_GET_DUST(HttpStatus.OK, "미세먼지 조회 성공입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
