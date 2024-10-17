package com.example.goready.global.response;

import com.example.goready.global.response.status.ErrorStatus;
import com.example.goready.global.response.status.SuccessStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@RequiredArgsConstructor
@JsonPropertyOrder({"status", "message", "data"})
public class ApiResponse<T> {
    private final HttpStatus status;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    // 성공 응답 (데이터 없음)
    public static <T> ResponseEntity<ApiResponse<T>> success(SuccessStatus successStatus) {
        return ResponseEntity.status(successStatus.getHttpStatus())
                .body(new ApiResponse<>(successStatus.getHttpStatus(), successStatus.getMessage(), null));
    }

    // 성공 응답 (데이터 있음)
    public static <T> ResponseEntity<ApiResponse<T>> success(SuccessStatus successStatus, T data) {
        return ResponseEntity.status(successStatus.getHttpStatus())
                .body(new ApiResponse<>(successStatus.getHttpStatus(), successStatus.getMessage(), data));
    }

    // 에러 응답 (데이터 없음)
    public static <T> ResponseEntity<ApiResponse<T>> error(ErrorStatus errorStatus) {
        return ResponseEntity.status(errorStatus.getHttpStatus())
                .body(new ApiResponse<>(errorStatus.getHttpStatus(), errorStatus.getMessage(), null));
    }

    // 에러 응답 (데이터 있음)
    public static <T> ResponseEntity<ApiResponse<T>> error(ErrorStatus errorStatus, T data) {
        return ResponseEntity.status(errorStatus.getHttpStatus())
                .body(new ApiResponse<>(errorStatus.getHttpStatus(), errorStatus.getMessage(), data));
    }
}

