package com.example.goready.converter;

import com.example.goready.dto.Address;
import com.example.goready.dto.MaskResponse;

public class MaskConverter {

        public static MaskResponse.MaskDto toMaskDto(boolean isAlert, boolean isMask, Address address) {
            return MaskResponse.MaskDto.builder()
                    .alert(isAlert)
                    .isMask(isMask)
                    .address(address.dongName())
                    .build();
        }
}
