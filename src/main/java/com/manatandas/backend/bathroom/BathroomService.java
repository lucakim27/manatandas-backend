package com.manatandas.backend.bathroom;

import com.manatandas.backend.bathroom.dto.BathroomRequest;
import com.manatandas.backend.bathroom.dto.BathroomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BathroomService {

    private final BathroomRepository bathroomRepository;

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

    public BathroomResponse create(BathroomRequest request) {
        Bathroom bathroom = Bathroom.builder()
            .name(request.getName())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .address(request.getAddress())
            .accessType(Bathroom.AccessType.PUBLIC)
            .source(Bathroom.Source.USER)
            .lastVerifiedAt(Instant.now())
            .build();

        return BathroomResponse.from(bathroomRepository.save(bathroom));
    }
}
