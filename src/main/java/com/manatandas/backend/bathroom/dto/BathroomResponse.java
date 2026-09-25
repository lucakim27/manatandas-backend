package com.manatandas.backend.bathroom.dto;

import com.manatandas.backend.bathroom.Bathroom;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class BathroomResponse {

    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String accessType;
    private Boolean isPaid;
    private Boolean isAccessible;
    private Double rating;
    private String source;
    private Instant lastVerifiedAt;

    public static BathroomResponse from(Bathroom bathroom) {
        return BathroomResponse.builder()
            .id(bathroom.getId())
            .name(bathroom.getName())
            .latitude(bathroom.getLatitude())
            .longitude(bathroom.getLongitude())
            .address(bathroom.getAddress())
            .accessType(bathroom.getAccessType().name())
            .isPaid(bathroom.getIsPaid())
            .isAccessible(bathroom.getIsAccessible())
            .rating(bathroom.getRating())
            .source(bathroom.getSource().name())
            .lastVerifiedAt(bathroom.getLastVerifiedAt())
            .build();
    }
}
