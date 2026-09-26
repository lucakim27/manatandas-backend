package com.manatandas.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequest {

    // The ID token from Google Identity Services on the frontend
    // (google.accounts.id.initialize / the One Tap / Sign-In-With-Google
    // button's credential response). This is a signed JWT from Google, not
    // a raw access token — the backend verifies it before trusting anything
    // inside it.
    @NotBlank
    private String idToken;
}
