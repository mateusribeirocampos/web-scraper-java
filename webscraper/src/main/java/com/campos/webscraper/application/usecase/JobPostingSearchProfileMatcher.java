package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.enums.JobPostingSearchProfile;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Applies product-level relevance rules on top of persisted postings.
 */
@Component
public class JobPostingSearchProfileMatcher {

    private static final Pattern JAVA_SIGNAL = Pattern.compile("\\bjava\\b");
    private static final Pattern SPRING_SIGNAL = Pattern.compile("\\bspring\\b");
    private static final Pattern KOTLIN_SIGNAL = Pattern.compile("\\bkotlin\\b");
    private static final Pattern ROLE_SIGNAL = Pattern.compile(
            "\\b(backend|back-end|desenvolvedor(?:a)?|developer|software engineer|engenheiro de software|programador(?:a)?)\\b"
    );
    private static final Pattern TALENT_POOL_SIGNAL = Pattern.compile(
            "\\b(banco de talentos|banco talentos|talent pool|talent community|talent network)\\b"
    );
    private static final Pattern LEADERSHIP_SIGNAL = Pattern.compile(
            "\\b(manager|gerente|lead|lider|principal|staff|architect|arquiteto|head|director|diretor|coordinator|coordenador)\\b"
    );

    public boolean matches(JobPostingEntity posting, JobPostingSearchProfile profile) {
        Objects.requireNonNull(posting, "posting must not be null");
        Objects.requireNonNull(profile, "profile must not be null");

        if (profile == JobPostingSearchProfile.UNFILTERED) {
            return true;
        }

        return matchesJavaJuniorBackend(posting);
    }

    private boolean matchesJavaJuniorBackend(JobPostingEntity posting) {
        if (isTalentPool(posting)) {
            return false;
        }

        if (isLeadershipRole(posting)) {
            return false;
        }

        if (posting.getSeniority() == SeniorityLevel.SENIOR || posting.getSeniority() == SeniorityLevel.LEAD) {
            return false;
        }

        return hasPrimaryStackSignal(posting) && hasRoleSignal(posting);
    }

    private static boolean isTalentPool(JobPostingEntity posting) {
        String haystack = normalize(
                safe(posting.getTitle()) + " " + safe(posting.getCompany()) + " " + safe(posting.getCanonicalUrl())
        );
        return TALENT_POOL_SIGNAL.matcher(haystack).find();
    }

    private static boolean hasPrimaryStackSignal(JobPostingEntity posting) {
        String haystack = normalize(
                safe(posting.getTitle()) + " " + safe(posting.getTechStackTags()) + " " + safe(posting.getDescription())
        );
        return JAVA_SIGNAL.matcher(haystack).find()
                || SPRING_SIGNAL.matcher(haystack).find()
                || KOTLIN_SIGNAL.matcher(haystack).find();
    }

    private static boolean hasRoleSignal(JobPostingEntity posting) {
        String haystack = normalize(safe(posting.getTitle()) + " " + safe(posting.getDescription()));
        return ROLE_SIGNAL.matcher(haystack).find();
    }

    private static boolean isLeadershipRole(JobPostingEntity posting) {
        String haystack = normalize(safe(posting.getTitle()) + " " + safe(posting.getDescription()));
        return LEADERSHIP_SIGNAL.matcher(haystack).find();
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
