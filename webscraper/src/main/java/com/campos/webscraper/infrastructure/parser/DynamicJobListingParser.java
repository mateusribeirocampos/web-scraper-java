package com.campos.webscraper.infrastructure.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses dynamic job cards rendered by a Playwright browser session.
 */
import org.springframework.stereotype.Component;

@Component
public class DynamicJobListingParser {

    /**
     * Extracts job listings from the provided HTML payload.
     */
    public List<DynamicJobListing> parse(String html, String baseUrl) {
        Document document = Jsoup.parse(html, baseUrl);
        Elements cards = document.select("article.job-card");
        List<DynamicJobListing> listings = new ArrayList<>();

        for (Element card : cards) {
            String externalId = card.attr("data-id").isBlank() ? card.id() : card.attr("data-id");
            String title = card.selectFirst(".job-title") != null
                    ? card.selectFirst(".job-title").text()
                    : "Untitled";
            String company = card.selectFirst(".company-name") != null
                    ? card.selectFirst(".company-name").text()
                    : "Unknown";
            String location = card.selectFirst(".job-location") != null
                    ? card.selectFirst(".job-location").text()
                    : "Remote";
            Element anchor = card.selectFirst("a.job-link");
            String url = anchor != null ? anchor.absUrl("href") : baseUrl;
            String postedAt = card.attr("data-posted");
            boolean remote = card.selectFirst(".remote-badge") != null
                    || location.toLowerCase().contains("remote");
            String description = card.selectFirst(".job-description") != null
                    ? card.selectFirst(".job-description").text()
                    : "";

            if (postedAt == null || postedAt.isBlank()) {
                postedAt = "1970-01-01";
            }

            listings.add(new DynamicJobListing(
                    externalId,
                    title,
                    company,
                    location,
                    url,
                    postedAt,
                    remote,
                    description
            ));
        }

        return listings;
    }
}
