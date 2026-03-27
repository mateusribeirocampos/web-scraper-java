package com.campos.webscraper.application.enrichment;

import com.campos.webscraper.infrastructure.parser.InconfidentesContestPreviewItem;
import com.campos.webscraper.infrastructure.pdf.PdfTextExtractor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Enriches Inconfidentes HTML preview items with metadata extracted from edital PDFs.
 */
@Component
public class InconfidentesContestPdfEnricher {

    static final Duration DEFAULT_TOTAL_ENRICHMENT_BUDGET = Duration.ofSeconds(3);
    private static final java.util.concurrent.ExecutorService PDF_ENRICHMENT_EXECUTOR = Executors.newCachedThreadPool(
            new PdfEnrichmentThreadFactory()
    );

    private final PdfTextExtractor pdfTextExtractor;
    private final InconfidentesEditalPdfMetadataParser metadataParser;
    private final Duration totalEnrichmentBudget;

    public InconfidentesContestPdfEnricher(
            PdfTextExtractor pdfTextExtractor,
            InconfidentesEditalPdfMetadataParser metadataParser
    ) {
        this(pdfTextExtractor, metadataParser, DEFAULT_TOTAL_ENRICHMENT_BUDGET);
    }

    InconfidentesContestPdfEnricher(
            PdfTextExtractor pdfTextExtractor,
            InconfidentesEditalPdfMetadataParser metadataParser,
            Duration totalEnrichmentBudget
    ) {
        this.pdfTextExtractor = Objects.requireNonNull(pdfTextExtractor, "pdfTextExtractor must not be null");
        this.metadataParser = Objects.requireNonNull(metadataParser, "metadataParser must not be null");
        this.totalEnrichmentBudget = Objects.requireNonNull(
                totalEnrichmentBudget,
                "totalEnrichmentBudget must not be null"
        );
    }

    public List<InconfidentesContestPreviewItem> enrichAll(List<InconfidentesContestPreviewItem> items) {
        Objects.requireNonNull(items, "items must not be null");

        long deadlineNanos = System.nanoTime() + totalEnrichmentBudget.toNanos();
        List<InconfidentesContestPreviewItem> enrichedItems = new ArrayList<>(items.size());
        for (InconfidentesContestPreviewItem item : items) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0L) {
                enrichedItems.add(item);
                continue;
            }
            enrichedItems.add(enrich(item, Duration.ofNanos(remainingNanos)));
        }
        return List.copyOf(enrichedItems);
    }

    public InconfidentesContestPreviewItem enrich(InconfidentesContestPreviewItem item) {
        return enrich(item, totalEnrichmentBudget);
    }

    InconfidentesContestPreviewItem enrich(InconfidentesContestPreviewItem item, Duration extractionBudget) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(extractionBudget, "extractionBudget must not be null");
        if (item.editalUrl() == null || item.editalUrl().isBlank()) {
            return item;
        }
        if (extractionBudget.isZero() || extractionBudget.isNegative()) {
            return item;
        }

        try {
            String pdfText = extractTextWithinBudget(item.editalUrl(), extractionBudget);
            if (pdfText == null || pdfText.isBlank()) {
                return item;
            }
            InconfidentesEditalPdfMetadata metadata = metadataParser.parse(pdfText);
            return new InconfidentesContestPreviewItem(
                    item.department(),
                    item.contestTitle(),
                    item.organizer(),
                    metadata.positionTitle() != null ? metadata.positionTitle() : item.positionTitle(),
                    metadata.educationLevel() != null ? metadata.educationLevel() : item.educationLevel(),
                    metadata.formationRequirements(),
                    item.editalYear(),
                    item.contestUrl(),
                    item.editalUrl(),
                    metadata.registrationStartDate(),
                    metadata.registrationEndDate(),
                    metadata.examDate(),
                    item.attachments()
            );
        } catch (RuntimeException ignored) {
            return item;
        }
    }

    private String extractTextWithinBudget(String editalUrl, Duration extractionBudget) {
        Future<String> extraction = PDF_ENRICHMENT_EXECUTOR.submit(() -> pdfTextExtractor.extractText(editalUrl));
        try {
            return extraction.get(extractionBudget.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException exception) {
            extraction.cancel(true);
            Thread.currentThread().interrupt();
            return null;
        } catch (TimeoutException exception) {
            extraction.cancel(true);
            return null;
        } catch (ExecutionException exception) {
            extraction.cancel(true);
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(cause);
        }
    }

    private static final class PdfEnrichmentThreadFactory implements ThreadFactory {

        private int threadNumber = 1;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "inconfidentes-pdf-enricher-" + threadNumber++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
