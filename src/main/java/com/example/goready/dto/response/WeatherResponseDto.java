package com.example.goready.dto.response;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeatherResponseDto {
    // 기온 및 강수량, 현 주소 반환
    private String status; // 어제와 비교 hot/same/cold
    private boolean isUmbrella; // 우산 필요 여부
    private int highTemp; // 최고 기온
    private int lowTemp; // 최저 기온
    private int currentTemp; // 현재 기온
    private int diffTemp; // 어제와 기온차
    private int rainPer; // 강수 확률
}
