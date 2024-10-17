package com.example.goready.global.exception;

import com.example.goready.global.response.status.ErrorStatus;
import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {
   private final ErrorStatus errorStatus;

    public GlobalException(ErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }
}
