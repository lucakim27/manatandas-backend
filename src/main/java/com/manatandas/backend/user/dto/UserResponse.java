package com.manatandas.backend.user.dto;

import com.manatandas.backend.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String displayName;
    private String pictureUrl;
    private Instant createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .pictureUrl(user.getPictureUrl())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
