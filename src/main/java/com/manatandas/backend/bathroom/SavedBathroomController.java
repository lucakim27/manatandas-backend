package com.manatandas.backend.bathroom;

import com.manatandas.backend.bathroom.dto.BathroomResponse;
import com.manatandas.backend.user.User;
import com.manatandas.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Handles a logged-in user's bookmarked bathrooms. Every endpoint here
// requires authentication (enforced in SecurityConfig) — there is no
// concept of an anonymous "saved" list.
@RestController
@RequestMapping("/api/bathrooms")
@RequiredArgsConstructor
public class SavedBathroomController {

    private final SavedBathroomRepository savedBathroomRepository;
    private final BathroomRepository bathroomRepository;
    private final UserRepository userRepository;

    @GetMapping("/saved")
    public List<BathroomResponse> getSavedBathrooms(Authentication authentication) {
        User user = currentUser(authentication);
        return savedBathroomRepository.findByUser(user).stream()
            .map(saved -> BathroomResponse.from(saved.getBathroom()))
            .toList();
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<Void> saveBathroom(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        Bathroom bathroom = findBathroomOrThrow(id);

        if (!savedBathroomRepository.existsByUserAndBathroom(user, bathroom)) {
            savedBathroomRepository.save(SavedBathroom.builder()
                .user(user)
                .bathroom(bathroom)
                .build());
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/save")
    public ResponseEntity<Void> unsaveBathroom(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        Bathroom bathroom = findBathroomOrThrow(id);

        savedBathroomRepository.deleteByUserAndBathroom(user, bathroom);
        return ResponseEntity.noContent().build();
    }

    private Bathroom findBathroomOrThrow(Long id) {
        return bathroomRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bathroom not found"));
    }

    // JwtAuthFilter puts the user's email as the authentication name (see
    // JwtService — the JWT subject is the email). Same lookup pattern as
    // AuthController.me().
    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
