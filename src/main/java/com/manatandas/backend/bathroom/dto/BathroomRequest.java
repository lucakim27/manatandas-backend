package com.manatandas.backend.bathroom.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BathroomRequest {

    @NotBlank
    private String name;

    // Loosely bounded to Malaysia's bounding box, same as the frontend map
    @NotNull
    @DecimalMin("0.5")
    @DecimalMax("7.5")
    private Double latitude;

    @NotNull
    @DecimalMin("99.0")
    @DecimalMax("119.5")
    private Double longitude;

    private String address;
}
