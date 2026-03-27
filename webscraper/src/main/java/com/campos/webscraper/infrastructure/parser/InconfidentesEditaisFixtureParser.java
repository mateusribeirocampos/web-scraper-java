package com.campos.webscraper.infrastructure.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fixture-driven parser for the Inconfidentes municipal editais page.
 */
@Component
public class InconfidentesEditaisFixtureParser {

    private static final Pattern POSITION_AFTER_CONTRATACAO_PATTERN =
            Pattern.compile("(?i)contrata[cç][aã]o de\\s+(.+)$");
    private static final Pattern POSITION_AFTER_PARA_PATTERN =
            Pattern.compile("(?i)(?:processo seletivo|concurso|edital)\\s+\\d+/\\d+\\s*-\\s*(?:.*?\\s+-\\s+)?(?:para|de)\\s+(.+)$");
    private static final Pattern EDITAL_YEAR_PATTERN =
            Pattern.compile("(?i)(?:edital|processo seletivo|concurso)\\s+\\d+/(\\d{4})");

    public InconfidentesParsePreview parse(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        Element root = document.selectFirst(".entry-content");
        if (root == null) {
            root = document.body();
        }

        List<InconfidentesContestPreviewItem> items = new ArrayList<>();
        String currentDepartment = null;
        String currentTitle = null;
        List<InconfidentesContestAttachment> currentAttachments = new ArrayList<>();

        for (Element element : root.children()) {
            String cssClass = element.className();
            String text = element.text().trim();
            if (text.isBlank()) {
                continue;
            }

            if (cssClass.contains("department")) {
                flushItem(items, currentDepartment, currentTitle, currentAttachments);
                currentDepartment = text;
                currentTitle = null;
                currentAttachments = new ArrayList<>();
                continue;
            }

            if (cssClass.contains("contest-title")) {
                flushItem(items, currentDepartment, currentTitle, currentAttachments);
                currentTitle = text;
                currentAttachments = new ArrayList<>();
                continue;
            }

            Elements links = element.select("a[href]");
            if (currentTitle != null && !links.isEmpty()) {
                for (Element link : links) {
                    String url = link.absUrl("href");
                    if (url == null || url.isBlank()) {
                        continue;
                    }
                    currentAttachments.add(new InconfidentesContestAttachment(link.text().trim(), url));
                }
            }
        }

        flushItem(items, currentDepartment, currentTitle, currentAttachments);

        return new InconfidentesParsePreview(sourceUrl, items.size(), List.copyOf(items));
    }

    private void flushItem(
            List<InconfidentesContestPreviewItem> items,
            String department,
            String title,
            List<InconfidentesContestAttachment> attachments
    ) {
        if (title == null || title.isBlank() || !isOperationalContest(title)) {
            return;
        }
        List<InconfidentesContestAttachment> normalizedAttachments =
                attachments == null ? List.of() : List.copyOf(attachments);
        InconfidentesContestAttachment editalAttachment = selectEditalAttachment(normalizedAttachments);
        if (editalAttachment == null) {
            return;
        }
        String organizer = department == null || department.isBlank()
                ? "Prefeitura Municipal de Inconfidentes"
                : "Prefeitura Municipal de Inconfidentes - " + department;
        items.add(new InconfidentesContestPreviewItem(
                department,
                title,
                organizer,
                inferPositionTitle(title),
                inferEducationLevel(title),
                inferEditalYear(title, normalizedAttachments),
                editalAttachment.url(),
                editalAttachment.url(),
                normalizedAttachments
        ));
    }

    private InconfidentesContestAttachment selectEditalAttachment(List<InconfidentesContestAttachment> attachments) {
        return attachments.stream()
                .filter(attachment -> isPrimaryEditalLabel(attachment.label()))
                .findFirst()
                .or(() -> attachments.stream()
                        .filter(attachment -> isRetificacaoEditalLabel(attachment.label()))
                        .findFirst())
                .orElse(null);
    }

    private boolean isPrimaryEditalLabel(String label) {
        String normalized = normalize(label == null ? "" : label);
        boolean openingDocument = normalized.contains("edital")
                || normalized.contains("abertura");
        boolean followUpDocument = isFollowUpLabel(normalized) || normalized.contains("retificacao");
        return openingDocument && !followUpDocument;
    }

    private boolean isRetificacaoEditalLabel(String label) {
        String normalized = normalize(label == null ? "" : label);
        return normalized.contains("retificacao") && !isFollowUpLabel(normalized);
    }

    private boolean isFollowUpLabel(String normalized) {
        return normalized.contains("resultado")
                || normalized.contains("gabarito")
                || normalized.contains("homologacao")
                || normalized.contains("inscricoes deferidas")
                || normalized.contains("inscricoes indeferidas")
                || normalized.contains("classificacao")
                || normalized.contains("convocacao");
    }

    private Integer inferEditalYear(String title, List<InconfidentesContestAttachment> attachments) {
        Integer yearFromTitle = extractEditalYear(title);
        if (yearFromTitle != null) {
            return yearFromTitle;
        }
        return attachments.stream()
                .map(InconfidentesContestAttachment::label)
                .map(this::extractEditalYear)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Integer extractEditalYear(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        Matcher matcher = EDITAL_YEAR_PATTERN.matcher(rawValue);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private boolean isOperationalContest(String title) {
        String normalized = normalize(title);
        boolean contestLike = normalized.contains("processo seletivo")
                || normalized.contains("concurso")
                || normalized.contains("contratacao")
                || normalized.contains("edital");
        boolean excluded = normalized.contains("transporte")
                || normalized.contains("cadastro cultural")
                || normalized.contains("chamamento publico")
                || normalized.contains("beneficiados")
                || isFollowUpLabel(normalized)
                || normalized.startsWith("retificacao")
                || normalized.startsWith("retificado")
                || isNumberedRetificationTitle(normalized);
        return contestLike && !excluded;
    }

    private boolean isNumberedRetificationTitle(String normalized) {
        return normalized.matches("^\\d+.*retific.*");
    }

    private String inferPositionTitle(String title) {
        Matcher contratacaoMatcher = POSITION_AFTER_CONTRATACAO_PATTERN.matcher(title);
        if (contratacaoMatcher.find()) {
            return cleanPosition(contratacaoMatcher.group(1));
        }

        Matcher paraMatcher = POSITION_AFTER_PARA_PATTERN.matcher(title);
        if (paraMatcher.find()) {
            return cleanPosition(paraMatcher.group(1));
        }

        String normalized = normalize(title);
        if (normalized.contains("professor")) {
            return "Professor";
        }
        if (normalized.contains("analista")) {
            return "Analista";
        }
        if (normalized.contains("tecnico")) {
            return "Tecnico";
        }
        return title;
    }

    private String inferEducationLevel(String title) {
        String normalized = normalize(title);
        if (normalized.contains("mestrado") || normalized.contains("doutorado")
                || normalized.contains("especializacao") || normalized.contains("pos graduacao")) {
            return "POS_GRADUACAO";
        }
        if (normalized.contains("tecnico")) {
            return "TECNICO";
        }
        if (normalized.contains("ensino medio") || normalized.contains("nivel medio")) {
            return "MEDIO";
        }
        if (normalized.contains("ensino fundamental") || normalized.contains("nivel fundamental")) {
            return "FUNDAMENTAL";
        }
        if (normalized.contains("professor")
                || normalized.contains("analista")
                || normalized.contains("engenheiro")
                || normalized.contains("desenvolvedor")
                || normalized.contains("ciencia da computacao")
                || normalized.contains("sistemas de informacao")) {
            return "SUPERIOR";
        }
        return null;
    }

    private String cleanPosition(String rawValue) {
        String normalized = rawValue.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }
        return builder.toString();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
