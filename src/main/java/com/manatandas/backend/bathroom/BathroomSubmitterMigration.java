package com.manatandas.backend.bathroom;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BathroomSubmitterMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer legacyColumnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = current_schema() AND table_name = 'bathrooms' "
                + "AND column_name = 'submitted_by_email'",
            Integer.class
        );
        if (legacyColumnCount == null || legacyColumnCount == 0) return;

        jdbcTemplate.update(
            "UPDATE bathrooms b SET submitted_by_user_id = u.id "
                + "FROM users u WHERE b.submitted_by_email = u.email "
                + "AND b.submitted_by_user_id IS NULL"
        );

        Integer unmatchedOwners = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bathrooms WHERE submitted_by_email IS NOT NULL "
                + "AND submitted_by_user_id IS NULL",
            Integer.class
        );
        if (unmatchedOwners != null && unmatchedOwners > 0) {
            throw new IllegalStateException("Cannot migrate bathroom submitters: " + unmatchedOwners
                + " submitted_by_email value(s) do not match a user email. The legacy column was retained.");
        }

        jdbcTemplate.execute("ALTER TABLE bathrooms DROP COLUMN submitted_by_email");
    }
}
