package com.example.Uplink_Backend.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

@Configuration
public class JacksonConfig {

    public static class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            text = text.trim();

            // 1. If ISO date-time like "2025-06-01T00:00:00" or contains 'T'
            if (text.contains("T")) {
                text = text.substring(0, text.indexOf("T"));
            }

            // 2. Standard ISO yyyy-MM-dd (e.g. "2025-06-01")
            try {
                return LocalDate.parse(text);
            } catch (Exception ignored) {}

            // 3. Year-Month: yyyy-MM (e.g. "2025-06")
            try {
                return YearMonth.parse(text, DateTimeFormatter.ofPattern("yyyy-MM")).atDay(1);
            } catch (Exception ignored) {}

            // 4. Month/Year: MM/yyyy or MM-yyyy (e.g. "06/2025" or "06-2025")
            try {
                return YearMonth.parse(text, DateTimeFormatter.ofPattern("MM/yyyy")).atDay(1);
            } catch (Exception ignored) {}
            try {
                return YearMonth.parse(text, DateTimeFormatter.ofPattern("MM-yyyy")).atDay(1);
            } catch (Exception ignored) {}

            // 5. Day-Month-Year: dd-MM-yyyy or dd/MM/yyyy (e.g. "01-06-2025")
            try {
                return LocalDate.parse(text, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (Exception ignored) {}
            try {
                return LocalDate.parse(text, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ignored) {}

            // 6. Textual Month and Year: e.g. "June 2025", "Jun 2025", "Jun-2025"
            try {
                String sanitized = text.replace("-", " ");
                return YearMonth.parse(sanitized, new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("[MMMM][MMM] yyyy")
                        .toFormatter(Locale.ENGLISH)).atDay(1);
            } catch (Exception ignored) {}

            // 7. Year only (e.g. "2025")
            try {
                int year = Integer.parseInt(text);
                if (year >= 1900 && year <= 2100) {
                    return LocalDate.of(year, 1, 1);
                }
            } catch (Exception ignored) {}

            // Fallback: standard parse
            return LocalDate.parse(text);
        }
    }

    @Bean
    public Module flexibleLocalDateModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDate.class, new FlexibleLocalDateDeserializer());
        return module;
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(flexibleLocalDateModule());
        return mapper;
    }
}
