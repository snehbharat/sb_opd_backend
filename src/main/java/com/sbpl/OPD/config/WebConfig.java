package com.sbpl.OPD.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new org.springframework.format.Formatter<LocalDate>() {
            private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            private final DateTimeFormatter europeanFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            private final DateTimeFormatter usFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

            @Override
            public LocalDate parse(String text, Locale locale) throws ParseException {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }

                text = text.trim();
                
                // Try ISO format first (YYYY-MM-DD)
                try {
                    return LocalDate.parse(text, isoFormatter);
                } catch (DateTimeParseException e) {
                    // Continue to try other formats
                }

                // Try European format (DD-MM-YYYY)
                try {
                    return LocalDate.parse(text, europeanFormatter);
                } catch (DateTimeParseException e) {
                    // Continue to try other formats
                }

                // Try US format (MM/DD/YYYY)
                try {
                    return LocalDate.parse(text, usFormatter);
                } catch (DateTimeParseException e) {
                    throw new ParseException("Unable to parse date: " + text + ". Supported formats: yyyy-MM-dd, dd-MM-yyyy, MM/dd/yyyy", 0);
                }
            }

            @Override
            public String print(LocalDate object, Locale locale) {
                return object != null ? object.format(isoFormatter) : "";
            }
        });
    }
}