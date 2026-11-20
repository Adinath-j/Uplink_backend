package com.example.Uplink_Backend.common.seeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Startup runner for safe runtime database migrations.
 * Legacy dummy seed records have been disabled in favor of idempotent SQL seeding.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS client_message_id VARCHAR(100);");
            logger.info("DataSeeder: Schema migration for client_message_id verified.");
        } catch (Exception e) {
            logger.warn("DataSeeder: Schema migration note: {}", e.getMessage());
        }

        logger.info("DataSeeder: Legacy dummy seeding disabled. Live seeding handled via seed_uplink_live.sql.");
    }
}
