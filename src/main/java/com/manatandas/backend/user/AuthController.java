package com.manatandas.backend.user;

import com.manatandas.backend.security.AuthCookieService;
import com.manatandas.backend.security.GoogleTokenVerifier;
import com.manatandas.backend.security.JwtService;
import com.manatandas.backend.user.dto.GoogleAuthRequest;
import com.manatandas.backend.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthCookieService authCookieService;

    @PostMapping("/google")
    public UserResponse loginWithGoogle(
        @Valid @RequestBody GoogleAuthRequest request,
        HttpServletResponse response
    ) {
        GoogleTokenVerifier.GoogleUser googleUser = googleTokenVerifier.verify(request.getIdToken());

        User user = userRepository.findByGoogleId(googleUser.googleId())
            .map(existing -> {
                // Keep profile info fresh in case it changed on Google's side
                existing.setDisplayName(googleUser.displayName());
                existing.setPictureUrl(googleUser.pictureUrl());
                return userRepository.save(existing);
            })
            .orElseGet(() -> userRepository.save(User.builder()
                .googleId(googleUser.googleId())
                .email(googleUser.email())
                .displayName(googleUser.displayName())
                .pictureUrl(googleUser.pictureUrl())
                .build()));

        // The JWT never touches the response body anymore -- it's set as an
        // httpOnly cookie, so it's invisible to any JS running on the page.
        String token = jwtService.generateToken(user.getEmail());
        authCookieService.setAuthCookie(response, token);

        return UserResponse.from(user);
    }

    // Called once on app load so the frontend can find out "am I still
    // logged in?" without ever having had access to the token itself. The
    // JwtAuthFilter has already validated the cookie by the time this runs;
    // if it hadn't, SecurityConfig would have rejected the request with 401
    // before this method is even reached.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        String email = authentication.getName();

        return userRepository.findByEmail(email)
            .map(user -> ResponseEntity.ok(UserResponse.from(user)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authCookieService.clearAuthCookie(response);
        return ResponseEntity.noContent().build();
    }
}
