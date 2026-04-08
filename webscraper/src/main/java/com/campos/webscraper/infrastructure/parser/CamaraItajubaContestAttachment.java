package com.campos.webscraper.infrastructure.parser;

import java.util.Objects;

public record CamaraItajubaContestAttachment(
        String label,
        String url
) {

    public CamaraItajubaContestAttachment {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(url, "url must not be null");
    }
}
