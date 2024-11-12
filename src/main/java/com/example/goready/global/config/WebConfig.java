package com.example.goready.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class WebConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    public WebClient kakaoWebClient() {
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true); // 쿠키 전송을 허용
        config.setAllowedOrigins(Arrays.asList(
                "https://goready.site",
                "https://www.goready.site",
                "https://goready.vercel.app"
        ));
        config.addExposedHeader("Set-Cookie");
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
