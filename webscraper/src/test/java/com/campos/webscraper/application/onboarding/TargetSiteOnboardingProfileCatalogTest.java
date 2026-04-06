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
                .contains("greenhouse_bitso", "lever_ciandt", "lever_watchguard", "indeed-br", "dou-api", "pci_concursos",
                        "municipal_inconfidentes", "municipal_pouso_alegre", "municipal_munhoz",
                        "municipal_campinas", "camara_santa_rita_sapucai");
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
        TargetSiteOnboardingProfileTemplate lever = catalog.get("lever_ciandt");
        TargetSiteOnboardingProfileTemplate watchguard = catalog.get("lever_watchguard");
        TargetSiteOnboardingProfileTemplate dou = catalog.get("dou-api");
        TargetSiteOnboardingProfileTemplate pci = catalog.get("pci_concursos");
        TargetSiteOnboardingProfileTemplate inconfidentes = catalog.get("municipal_inconfidentes");
        TargetSiteOnboardingProfileTemplate pousoAlegre = catalog.get("municipal_pouso_alegre");
        TargetSiteOnboardingProfileTemplate munhoz = catalog.get("municipal_munhoz");
        TargetSiteOnboardingProfileTemplate campinas = catalog.get("municipal_campinas");
        TargetSiteOnboardingProfileTemplate camaraSantaRita = catalog.get("camara_santa_rita_sapucai");

        assertThat(indeed.sourceFamily()).isEqualTo("INDEED");
        assertThat(indeed.targetSite().getSiteCode()).isEqualTo("indeed-br");
        assertThat(indeed.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(indeed.jobsApiUrl()).isEqualTo("https://to.indeed.com");
        assertThat(indeed.checklist().officialApiEndpointUrl()).isEqualTo("https://to.indeed.com");

        assertThat(lever.sourceFamily()).isEqualTo("LEVER");
        assertThat(lever.targetSite().getSiteCode()).isEqualTo("lever_ciandt");
        assertThat(lever.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(lever.jobsApiUrl()).isEqualTo("https://api.lever.co/v0/postings/ciandt?mode=json");
        assertThat(lever.targetSite().getLegalStatus().name()).isEqualTo("PENDING_REVIEW");
        assertThat(lever.targetSite().isEnabled()).isFalse();

        assertThat(watchguard.sourceFamily()).isEqualTo("LEVER");
        assertThat(watchguard.targetSite().getSiteCode()).isEqualTo("lever_watchguard");
        assertThat(watchguard.targetSite().getDisplayName()).isEqualTo("WatchGuard Careers via Lever");
        assertThat(watchguard.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(watchguard.jobsApiUrl()).isEqualTo("https://api.lever.co/v0/postings/watchguard?mode=json");
        assertThat(watchguard.targetSite().getLegalStatus().name()).isEqualTo("PENDING_REVIEW");
        assertThat(watchguard.targetSite().isEnabled()).isFalse();

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

        assertThat(campinas.sourceFamily()).isEqualTo("MUNICIPAL_API");
        assertThat(campinas.targetSite().getSiteCode()).isEqualTo("municipal_campinas");
        assertThat(campinas.targetSite().getSelectorBundleVersion()).isEqualTo("campinas_jsonapi_v1");
        assertThat(campinas.targetSite().getLegalStatus().name()).isEqualTo("PENDING_REVIEW");
        assertThat(campinas.targetSite().isEnabled()).isFalse();
        assertThat(campinas.jobsApiUrl()).contains("portal-api.campinas.sp.gov.br/jsonapi/node/site");
        assertThat(campinas.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);

        assertThat(camaraSantaRita.sourceFamily()).isEqualTo("LEGISLATIVE_HTML");
        assertThat(camaraSantaRita.targetSite().getSiteCode()).isEqualTo("camara_santa_rita_sapucai");
        assertThat(camaraSantaRita.targetSite().getSelectorBundleVersion()).isEqualTo("camara_santa_rita_html_v1");
        assertThat(camaraSantaRita.targetSite().getLegalStatus().name()).isEqualTo("PENDING_REVIEW");
        assertThat(camaraSantaRita.targetSite().isEnabled()).isFalse();
        assertThat(camaraSantaRita.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.DADOS_PUBLICOS);
    }

    @Test
    @DisplayName("should find curated profile by target site code")
    void shouldFindCuratedProfileByTargetSiteCode() {
        assertThat(catalog.findBySiteCode("greenhouse_bitso"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("greenhouse_bitso");

        assertThat(catalog.findBySiteCode("lever_ciandt"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("lever_ciandt");

        assertThat(catalog.findBySiteCode("lever_watchguard"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("lever_watchguard");

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

        assertThat(catalog.findBySiteCode("municipal_campinas"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("municipal_campinas");

        assertThat(catalog.findBySiteCode("camara_santa_rita_sapucai"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("camara_santa_rita_sapucai");

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
