package com.campos.webscraper.infrastructure.parser;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Versioned selector bundle for static HTML parsers.
 */
public record SelectorBundle(
        String siteCode,
        String strategyName,
        String parserVersion,
        String selectorBundleVersion,
        LocalDate effectiveFrom,
        LocalDate deprecatedAt,
        Map<String, String> selectors
) {

    public SelectorBundle {
        Objects.requireNonNull(siteCode, "siteCode must not be null");
        Objects.requireNonNull(strategyName, "strategyName must not be null");
        Objects.requireNonNull(parserVersion, "parserVersion must not be null");
        Objects.requireNonNull(selectorBundleVersion, "selectorBundleVersion must not be null");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        selectors = Map.copyOf(Objects.requireNonNull(selectors, "selectors must not be null"));
    }

    public String selector(String field) {
        Objects.requireNonNull(field, "field must not be null");
        return selectors.get(field);
    }

    public void requireSelectors(Collection<String> requiredFields) {
        Objects.requireNonNull(requiredFields, "requiredFields must not be null");

        for (String field : requiredFields) {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("required selector field must not be blank");
            }

            String selector = selectors.get(field);
            if (selector == null || selector.isBlank()) {
                throw new IllegalArgumentException(
                        "SelectorBundle %s is missing required selector mapping for field '%s'"
                                .formatted(selectorBundleVersion, field)
                );
            }
        }
    }
}
