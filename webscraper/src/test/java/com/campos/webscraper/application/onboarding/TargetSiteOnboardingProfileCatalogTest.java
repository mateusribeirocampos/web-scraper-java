package com.campos.webscraper.application.onboarding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("TargetSiteOnboardingProfileCatalog")
class TargetSiteOnboardingProfileCatalogTest {

    private final TargetSiteOnboardingProfileCatalog catalog = new TargetSiteOnboardingProfileCatalog();

    @Test
    @DisplayName("should expose curated onboarding profiles for supported sources")
    void shouldExposeCuratedOnboardingProfilesForSupportedSources() {
        assertThat(catalog.list())
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .contains("greenhouse_bitso", "indeed-br", "dou-api", "pci_concursos",
                        "municipal_inconfidentes", "municipal_pouso_alegre", "municipal_munhoz");
    }

    @Test
    @DisplayName("should return detailed onboarding template for known profile key")
    void shouldReturnDetailedOnboardingTemplateForKnownProfileKey() {
        TargetSiteOnboardingProfileTemplate template = catalog.get("greenhouse_bitso");

        assertThat(template.profileKey()).isEqualTo("greenhouse_bitso");
        assertThat(template.sourceFamily()).isEqualTo("GREENHOUSE");
        assertThat(template.targetSite().getSiteCode()).isEqualTo("greenhouse_bitso");
        assertThat(template.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
    }

    @Test
    @DisplayName("should expose multiple onboarding families with consistent legal categories")
    void shouldExposeMultipleOnboardingFamiliesWithConsistentLegalCategories() {
        TargetSiteOnboardingProfileTemplate indeed = catalog.get("indeed-br");
        TargetSiteOnboardingProfileTemplate dou = catalog.get("dou-api");
        TargetSiteOnboardingProfileTemplate pci = catalog.get("pci_concursos");
        TargetSiteOnboardingProfileTemplate inconfidentes = catalog.get("municipal_inconfidentes");
        TargetSiteOnboardingProfileTemplate pousoAlegre = catalog.get("municipal_pouso_alegre");
        TargetSiteOnboardingProfileTemplate munhoz = catalog.get("municipal_munhoz");

        assertThat(indeed.sourceFamily()).isEqualTo("INDEED");
        assertThat(indeed.targetSite().getSiteCode()).isEqualTo("indeed-br");
        assertThat(indeed.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(indeed.jobsApiUrl()).isEqualTo("https://to.indeed.com");
        assertThat(indeed.checklist().officialApiEndpointUrl()).isEqualTo("https://to.indeed.com");

        assertThat(dou.sourceFamily()).isEqualTo("DOU");
        assertThat(dou.targetSite().getSiteCode()).isEqualTo("dou-api");
        assertThat(dou.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);

        assertThat(pci.sourceFamily()).isEqualTo("PCI_CONCURSOS");
        assertThat(pci.targetSite().getSiteCode()).isEqualTo("pci_concursos");
        assertThat(pci.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.DADOS_PUBLICOS);

        assertThat(inconfidentes.sourceFamily()).isEqualTo("MUNICIPAL_HTML");
        assertThat(inconfidentes.targetSite().getSiteCode()).isEqualTo("municipal_inconfidentes");
        assertThat(inconfidentes.targetSite().getSelectorBundleVersion()).isEqualTo("inconfidentes_html_v1");
        assertThat(inconfidentes.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(inconfidentes.targetSite().isEnabled()).isTrue();
        assertThat(inconfidentes.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.DADOS_PUBLICOS);

        assertThat(pousoAlegre.sourceFamily()).isEqualTo("MUNICIPAL_HTML");
        assertThat(pousoAlegre.targetSite().getSiteCode()).isEqualTo("municipal_pouso_alegre");
        assertThat(pousoAlegre.targetSite().getSelectorBundleVersion()).isEqualTo("pouso_alegre_html_v1");
        assertThat(pousoAlegre.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(pousoAlegre.targetSite().isEnabled()).isTrue();
        assertThat(pousoAlegre.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.DADOS_PUBLICOS);

        assertThat(munhoz.sourceFamily()).isEqualTo("MUNICIPAL_HTML");
        assertThat(munhoz.targetSite().getSiteCode()).isEqualTo("municipal_munhoz");
        assertThat(munhoz.targetSite().getSelectorBundleVersion()).isEqualTo("munhoz_html_v1");
        assertThat(munhoz.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(munhoz.targetSite().isEnabled()).isTrue();
        assertThat(munhoz.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.DADOS_PUBLICOS);
    }

    @Test
    @DisplayName("should find curated profile by target site code")
    void shouldFindCuratedProfileByTargetSiteCode() {
        assertThat(catalog.findBySiteCode("greenhouse_bitso"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("greenhouse_bitso");

        assertThat(catalog.findBySiteCode("municipal_inconfidentes"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("municipal_inconfidentes");

        assertThat(catalog.findBySiteCode("municipal_pouso_alegre"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("municipal_pouso_alegre");

        assertThat(catalog.findBySiteCode("municipal_munhoz"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("municipal_munhoz");

        assertThat(catalog.findBySiteCode("unknown-site")).isEmpty();
    }

    @Test
    @DisplayName("should fail when onboarding profile key is unknown")
    void shouldFailWhenOnboardingProfileKeyIsUnknown() {
        assertThatThrownBy(() -> catalog.get("unknown_profile"))
                .isInstanceOf(TargetSiteOnboardingProfileNotFoundException.class)
                .hasMessage("Target site onboarding profile not found: unknown_profile");
    }
}
