package com.campos.webscraper.interfaces.dto;

import java.util.List;

/**
 * DTO representing the root payload returned by the DOU API.
 */
public record DouApiResponse(
        List<DouApiItemResponse> items
) {
}
