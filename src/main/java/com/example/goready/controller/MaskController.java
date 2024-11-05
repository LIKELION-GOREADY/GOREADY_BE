package com.example.goready.controller;

import com.example.goready.dto.MaskResponse;
import com.example.goready.global.response.ApiResponse;
import com.example.goready.global.response.status.SuccessStatus;
import com.example.goready.service.mask.MaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mask")
public class MaskController {

    private final MaskService maskService;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<MaskResponse.MaskDto>>> getMaskInfo(
            @RequestParam(name = "lat") double lat,
            @RequestParam(name = "lon") double lon
    ) {
        return maskService.getMaskInfo(lat, lon)
                .map(maskDto -> ApiResponse.success(SuccessStatus.SUCCESS_GET_DUST, maskDto));
    }
}
