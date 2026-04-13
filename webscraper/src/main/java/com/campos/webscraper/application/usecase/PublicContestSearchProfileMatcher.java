package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.PublicContestSearchProfile;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Applies product-level relevance rules on top of persisted public contest postings.
 *
 * <p>Unlike the private-sector matcher which filters by seniority and tech stack,
 * this matcher filters by education/degree requirements and IT-compatible position
 * titles. The concept of "junior/senior" does not exist in Brazilian public contests.
 */
@Component
public class PublicContestSearchProfileMatcher {

    // -- IT-compatible position signals --
    // All alternatives must use accent-stripped forms since normalize() runs before matching.
    // The tecnico pattern uses .{0,30} to bridge "de nível médio em" and similar qualifiers.
    private static final Pattern IT_POSITION_SIGNAL = Pattern.compile(
            "\\b(analista de (ti|tecnologia da informacao|sistemas)|"
                    + "tecnico.{0,30}(informatica|processamento de dados|computacao|telecomunicacoes|redes)|"
                    + "desenvolvedor|programador|"
                    + "administrador de banco de dados|dba|"
                    + "engenheiro de (software|sistemas)|"
                    + "cientista de dados|analista de dados|"
                    + "analista de (seguranca da informacao|redes|infraestrutura|suporte)|"
                    + "webdesigner|web developer|"
                    + "suporte.{0,15}informatica)\\b"
    );

    // -- IT degree signals (checked in payloadJson and positionTitle) --
    private static final Pattern IT_DEGREE_SIGNAL = Pattern.compile(
            "\\b(ciencias? da computacao|"
                    + "engenharia da computacao|"
                    + "sistemas de informacao|"
                    + "analise e desenvolvimento de sistemas|"
                    + "tecnologia da informacao|"
                    + "processamento de dados|"
                    + "ciencias? de dados|"
                    + "engenharia de software|"
                    + "redes de computadores|"
                    + "gestao de ti|"
                    + "banco de dados|"
                    + "seguranca da informacao)\\b"
    );

    public boolean matches(PublicContestPostingEntity contest, PublicContestSearchProfile profile) {
        Objects.requireNonNull(contest, "contest must not be null");
        Objects.requireNonNull(profile, "profile must not be null");

        if (profile == PublicContestSearchProfile.UNFILTERED) {
            return true;
        }

        if (profile == PublicContestSearchProfile.TI_ROLE_BROAD) {
            return matchesTiRoleBroad(contest);
        }

        return matchesTiDegreeAndRole(contest);
    }

    private boolean matchesTiDegreeAndRole(PublicContestPostingEntity contest) {
        if (isFundamental(contest)) {
            return false;
        }

        return hasItPositionSignal(contest) || hasItDegreeSignal(contest);
    }

    private boolean matchesTiRoleBroad(PublicContestPostingEntity contest) {
        if (isFundamental(contest)) {
            return false;
        }

        return hasItPositionSignal(contest);
    }

    /**
     * Only FUNDAMENTAL is unconditionally rejected. MEDIO contests can still be
     * relevant when the position title carries a clear IT signal (e.g. "Técnico
     * de Nível Médio em Informática"). UNKNOWN is not rejected either, because
     * many municipal sources import contests before the normalizer can infer the
     * education level.
     */
    private static boolean isFundamental(PublicContestPostingEntity contest) {
        return contest.getEducationLevel() == EducationLevel.FUNDAMENTAL;
    }

    private static boolean hasItPositionSignal(PublicContestPostingEntity contest) {
        String haystack = normalize(safe(contest.getPositionTitle()));
        return IT_POSITION_SIGNAL.matcher(haystack).find();
    }

    private static boolean hasItDegreeSignal(PublicContestPostingEntity contest) {
        String haystack = normalize(
                safe(contest.getPositionTitle()) + " " + safe(contest.getPayloadJson())
        );
        return IT_DEGREE_SIGNAL.matcher(haystack).find();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }
}
