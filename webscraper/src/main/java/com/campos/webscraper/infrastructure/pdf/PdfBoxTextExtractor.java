package com.campos.webscraper.infrastructure.pdf;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;

/**
 * PDF text extractor backed by Apache PDFBox.
 */
@Component
public class PdfBoxTextExtractor implements PdfTextExtractor {

    private final OkHttpClient httpClient;

    public PdfBoxTextExtractor() {
        this(new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(3))
                .followRedirects(true)
                .followSslRedirects(true)
                .build());
    }

    PdfBoxTextExtractor(OkHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public String extractText(String pdfUrl) {
        Objects.requireNonNull(pdfUrl, "pdfUrl must not be null");

        Request request = new Request.Builder().url(pdfUrl).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Failed to fetch PDF with status " + response.code() + " for url=" + pdfUrl);
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Failed to fetch PDF body for url=" + pdfUrl);
            }
            try (InputStream inputStream = body.byteStream();
                 RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(inputStream);
                 PDDocument document = Loader.loadPDF(buffer)) {
                return new PDFTextStripper().getText(document);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to extract PDF text for url=" + pdfUrl, exception);
        }
    }
}
