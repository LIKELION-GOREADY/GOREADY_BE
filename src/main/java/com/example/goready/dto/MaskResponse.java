package com.example.goready.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

public class MaskResponse {

    @Builder
    @Data
    @AllArgsConstructor
    public static class MaskDto {
        private boolean alert;
        private boolean isMask;
        private String address;
    }
}
