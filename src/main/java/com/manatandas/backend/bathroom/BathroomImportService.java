package com.manatandas.backend.bathroom;

/**
 * Contract for any service that imports bathroom data from an external
 * source (OpenStreetMap, Google Places, petrol station locators, government
 * open data, etc).
 *
 * Spring collects every bean implementing this interface into a single
 * List&lt;BathroomImportService&gt;, so BathroomController can dispatch to
 * the right importer by source name without needing a new endpoint or any
 * code change every time a new source is added — new importers just
 * implement this interface and register as a @Service.
 */
public interface BathroomImportService {

    Bathroom.Source getSource();

    int importBathrooms();
}
