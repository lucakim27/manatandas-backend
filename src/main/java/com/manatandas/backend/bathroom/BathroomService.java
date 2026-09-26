package com.manatandas.backend.bathroom;

import com.manatandas.backend.bathroom.dto.BathroomRequest;
import com.manatandas.backend.bathroom.dto.BathroomResponse;
import com.manatandas.backend.user.User;
import com.manatandas.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BathroomService {

    private final BathroomRepository bathroomRepository;
    private final UserRepository userRepository;
    private final SavedBathroomRepository savedBathroomRepository;

    public List<BathroomResponse> findAll() {
        return bathroomRepository.findAll().stream()
            .map(BathroomResponse::from)
            .toList();
    }

    public List<BathroomResponse> findWithinBounds(double minLat, double maxLat, double minLng, double maxLng) {
        return bathroomRepository.findWithinBounds(minLat, maxLat, minLng, maxLng).stream()
            .map(BathroomResponse::from)
            .toList();
    }

    public BathroomResponse create(BathroomRequest request, String submittedByEmail) {
        User submitter = userRepository.findByEmail(submittedByEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account not found"));
        Bathroom bathroom = Bathroom.builder()
            .name(request.getName())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .address(request.getAddress())
            .accessType(Bathroom.AccessType.PUBLIC)
            .source(Bathroom.Source.USER)
            .submittedBy(submitter)
            .lastVerifiedAt(Instant.now())
            .build();

        return BathroomResponse.from(bathroomRepository.save(bathroom));
    }

    @Transactional
    public void delete(Long bathroomId, String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account not found"));
        Bathroom bathroom = bathroomRepository.findById(bathroomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bathroom not found"));
        if (bathroom.getSubmittedBy() == null || !bathroom.getSubmittedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the bathroom creator can delete it");
        }

        savedBathroomRepository.deleteAllByBathroom(bathroom);
        bathroomRepository.delete(bathroom);
    }
}
