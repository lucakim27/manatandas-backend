package com.manatandas.backend.bathroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BathroomRepository extends JpaRepository<Bathroom, Long> {

    @Query("SELECT b FROM Bathroom b WHERE b.latitude BETWEEN :minLat AND :maxLat "
        + "AND b.longitude BETWEEN :minLng AND :maxLng")
    List<Bathroom> findWithinBounds(
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLng") double minLng,
        @Param("maxLng") double maxLng
    );

    // The real dedup key across importers: a given source's own identifier
    // (OSM node id, Google place_id, etc), not raw coordinates, since the
    // same physical toilet will rarely match exactly across sources.
    boolean existsBySourceAndExternalId(Bathroom.Source source, String externalId);
}
