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

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS email_verification_otps (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    email VARCHAR(255) NOT NULL,
                    otp_hash VARCHAR(255) NOT NULL,
                    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    attempt_count INT DEFAULT 0,
                    last_sent_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
                CREATE INDEX IF NOT EXISTS idx_email_verification_otps_email ON email_verification_otps(email);
            """);
            logger.info("DataSeeder: Schema migration for email_verification_otps verified.");
        } catch (Exception e) {
            logger.warn("DataSeeder: email_verification_otps migration note: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("UPDATE users SET is_verified = TRUE WHERE email LIKE '%@uplink.edu';");
            logger.info("DataSeeder: Pre-seeded @uplink.edu users verified.");
        } catch (Exception e) {
            logger.warn("DataSeeder: Note verifying @uplink.edu users: {}", e.getMessage());
        }

        logger.info("DataSeeder: Legacy dummy seeding disabled. Live seeding handled via seed_uplink_live.sql.");
    }
}
