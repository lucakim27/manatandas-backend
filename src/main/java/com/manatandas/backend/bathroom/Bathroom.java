package com.manatandas.backend.bathroom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bathrooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bathroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Double latitude;

    private Double longitude;

    private String address;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccessType accessType = AccessType.UNKNOWN;

    private Boolean isPaid;

    private Boolean isAccessible;

    private Double rating;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Source source = Source.USER;

    // The identifier this record uses in its own source system (OSM node id,
    // Google place_id, a hash of name+address for government CSVs, etc).
    // Combined with `source`, this is the real dedup key across imports —
    // coordinates alone aren't reliable once multiple sources are involved,
    // since the same physical toilet will rarely have byte-identical lat/lng
    // across OSM, Google Places, and manually-entered government data.
    private String externalId;

    // Freeform JSON for anything source-specific that doesn't warrant its
    // own column yet (opening hours, raw tags, photo URLs...). Lets each
    // importer carry extra info without requiring a schema change.
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant lastVerifiedAt;

    public enum Source {
        USER,
        OSM,
        GOOGLE_PLACES,
        PETROL_STATION,
        GOVERNMENT,
        PLUS_RR
    }

    public enum AccessType {
        PUBLIC,
        CUSTOMERS_ONLY,
        PRIVATE,
        UNKNOWN
    }
}
