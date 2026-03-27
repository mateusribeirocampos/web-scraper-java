package com.campos.webscraper.infrastructure.pdf;

/**
 * Extracts raw text content from a PDF URL.
 */
public interface PdfTextExtractor {

    String extractText(String pdfUrl);
}
