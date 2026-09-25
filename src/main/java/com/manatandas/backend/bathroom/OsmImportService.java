package com.manatandas.backend.bathroom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Seeds the bathroom database with real public-toilet locations from
 * OpenStreetMap, via the free Overpass API (no key required).
 *
 * This is meant to be triggered manually (e.g. once during initial setup,
 * or occasionally to pick up new OSM data) rather than on every app start —
 * querying all of Malaysia can take tens of seconds and Overpass has a
 * shared public rate limit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OsmImportService implements BathroomImportService {

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    // Bounding box around Malaysia (same box the frontend map uses). A plain
    // bbox query is far more reliable than an area["ISO3166-1"="MY"] lookup,
    // which can silently return zero results if the boundary relation isn't
    // tagged exactly as expected.
    private static final String OVERPASS_QUERY = """
        [out:json][timeout:60];
        node["amenity"="toilets"](0.5,99.0,7.5,119.5);
        out body;
        """;

    private final BathroomRepository bathroomRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Bathroom.Source getSource() {
        return Bathroom.Source.OSM;
    }

    @Override
    public int importBathrooms() {
        return importFromOsm();
    }

    public int importFromOsm() {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OVERPASS_URL))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString("data=" + OVERPASS_QUERY, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Overpass API returned status {}", response.statusCode());
                return 0;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode elements = root.path("elements");

            if (elements.isMissingNode() || elements.isEmpty()) {
                log.warn("Overpass returned no elements. Raw response: {}", response.body());
                return 0;
            }

            int imported = 0;
            int skipped = 0;
            for (JsonNode element : elements) {
                // OSM node id — stable across re-imports, used as our dedup
                // key instead of matching on raw coordinates.
                String externalId = element.path("id").asText(null);
                if (externalId == null) {
                    skipped++;
                    continue;
                }

                if (bathroomRepository.existsBySourceAndExternalId(Bathroom.Source.OSM, externalId)) {
                    continue;
                }

                double lat = element.path("lat").asDouble();
                double lon = element.path("lon").asDouble();

                JsonNode tags = element.path("tags");

                String rawAccess = tags.has("access") ? tags.get("access").asText() : null;
                String name = tags.has("name") ? tags.get("name").asText() : "Public Toilet";

                Bathroom.AccessType accessType = mapOsmAccess(rawAccess);

                // Also catch common restricted-access wording in the name
                // itself, since many contributors never set the access tag.
                if (accessType == Bathroom.AccessType.PRIVATE || looksRestrictedByName(name)) {
                    skipped++;
                    continue;
                }

                Boolean isAccessible = tags.has("wheelchair")
                    ? "yes".equals(tags.get("wheelchair").asText())
                    : null;

                Boolean isPaid = tags.has("fee")
                    ? "yes".equals(tags.get("fee").asText())
                    : null;

                Bathroom bathroom = Bathroom.builder()
                    .name(name)
                    .latitude(lat)
                    .longitude(lon)
                    .accessType(accessType)
                    .isAccessible(isAccessible)
                    .isPaid(isPaid)
                    .source(Bathroom.Source.OSM)
                    .externalId(externalId)
                    .metadata(tags.toString())
                    .lastVerifiedAt(Instant.now())
                    .build();

                bathroomRepository.save(bathroom);
                imported++;
            }

            log.info("Imported {} bathrooms from OpenStreetMap ({} skipped as likely non-public)", imported, skipped);
            return imported;
        } catch (Exception e) {
            log.error("Failed to import bathrooms from OSM", e);
            return 0;
        }
    }

    // Maps OSM's free-text access tag into our generic AccessType enum, so
    // the rest of the app never needs to know OSM's specific vocabulary.
    private static Bathroom.AccessType mapOsmAccess(String access) {
        if (access == null) {
            return Bathroom.AccessType.PUBLIC; // OSM convention: untagged means open to the public
        }
        return switch (access) {
            case "private", "no", "employees" -> Bathroom.AccessType.PRIVATE;
            case "customers" -> Bathroom.AccessType.CUSTOMERS_ONLY;
            case "yes", "permissive", "public" -> Bathroom.AccessType.PUBLIC;
            default -> Bathroom.AccessType.UNKNOWN;
        };
    }

    private static boolean looksRestrictedByName(String name) {
        String lower = name.toLowerCase();
        return lower.contains("student")
            || lower.contains("staff")
            || lower.contains("office")
            || lower.contains("employee")
            || lower.contains("resident");
    }
}
