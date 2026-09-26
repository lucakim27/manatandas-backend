package com.manatandas.backend.bathroom;

import com.manatandas.backend.bathroom.dto.BathroomRequest;
import com.manatandas.backend.bathroom.dto.BathroomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bathrooms")
@RequiredArgsConstructor
public class BathroomController {

    private final BathroomService bathroomService;

    // Spring auto-collects every bean implementing BathroomImportService
    // here. Adding a new data source (Google Places, petrol stations, etc.)
    // never requires touching this controller — just add a new @Service
    // class that implements the interface and it's picked up automatically.
    private final List<BathroomImportService> importServices;

    // GET /api/bathrooms                                      -> everything
    // GET /api/bathrooms?minLat=..&maxLat=..&minLng=..&maxLng=.. -> only pins within the current map viewport
    @GetMapping
    public List<BathroomResponse> getBathrooms(
        @RequestParam(required = false) Double minLat,
        @RequestParam(required = false) Double maxLat,
        @RequestParam(required = false) Double minLng,
        @RequestParam(required = false) Double maxLng
    ) {
        boolean hasBounds = minLat != null && maxLat != null && minLng != null && maxLng != null;
        return hasBounds
            ? bathroomService.findWithinBounds(minLat, maxLat, minLng, maxLng)
            : bathroomService.findAll();
    }

    @PostMapping
    public ResponseEntity<BathroomResponse> createBathroom(
        @Valid @RequestBody BathroomRequest request,
        Authentication authentication
    ) {
        // Guaranteed non-null here: SecurityConfig requires auth for this
        // route, so the JwtAuthFilter must have already set it.
        String submittedByEmail = authentication.getName();
        BathroomResponse created = bathroomService.create(request, submittedByEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // One-off/manual trigger to seed the database from a given source, e.g.
    // POST /api/bathrooms/import/osm or /api/bathrooms/import/google_places.
    // Not meant to be called automatically or often.
    @PostMapping("/import/{source}")
    public ResponseEntity<String> importFromSource(@PathVariable String source) {
        return importServices.stream()
            .filter(s -> s.getSource().name().equalsIgnoreCase(source))
            .findFirst()
            .map(s -> {
                int count = s.importBathrooms();
                return ResponseEntity.ok("Imported " + count + " bathrooms from " + source);
            })
            .orElseGet(() -> ResponseEntity.badRequest()
                .body("Unknown import source: " + source + ". Available: "
                    + importServices.stream().map(s -> s.getSource().name()).toList()));
    }
}
