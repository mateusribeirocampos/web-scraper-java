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
                .contains("greenhouse_bitso", "gupy_specialdog_extrema", "lever_ciandt", "lever_watchguard", "airbus_helibras_workday", "alcoa_pocos_caldas_workday", "indeed-br", "dou-api", "pci_concursos",
                        "municipal_inconfidentes", "municipal_pouso_alegre", "municipal_munhoz",
                        "municipal_campinas", "municipal_pocos_caldas", "municipal_extrema", "camara_santa_rita_sapucai", "camara_itajuba");
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
        TargetSiteOnboardingProfileTemplate gupy = catalog.get("gupy_specialdog_extrema");
        TargetSiteOnboardingProfileTemplate lever = catalog.get("lever_ciandt");
        TargetSiteOnboardingProfileTemplate watchguard = catalog.get("lever_watchguard");
        TargetSiteOnboardingProfileTemplate workday = catalog.get("airbus_helibras_workday");
        TargetSiteOnboardingProfileTemplate workdayAlcoa = catalog.get("alcoa_pocos_caldas_workday");
        TargetSiteOnboardingProfileTemplate dou = catalog.get("dou-api");
        TargetSiteOnboardingProfileTemplate pci = catalog.get("pci_concursos");
        TargetSiteOnboardingProfileTemplate inconfidentes = catalog.get("municipal_inconfidentes");
        TargetSiteOnboardingProfileTemplate pousoAlegre = catalog.get("municipal_pouso_alegre");
        TargetSiteOnboardingProfileTemplate munhoz = catalog.get("municipal_munhoz");
        TargetSiteOnboardingProfileTemplate campinas = catalog.get("municipal_campinas");
        TargetSiteOnboardingProfileTemplate pocosCaldas = catalog.get("municipal_pocos_caldas");
        TargetSiteOnboardingProfileTemplate extrema = catalog.get("municipal_extrema");
        TargetSiteOnboardingProfileTemplate camaraSantaRita = catalog.get("camara_santa_rita_sapucai");
        TargetSiteOnboardingProfileTemplate camaraItajuba = catalog.get("camara_itajuba");

        assertThat(indeed.sourceFamily()).isEqualTo("INDEED");
        assertThat(indeed.targetSite().getSiteCode()).isEqualTo("indeed-br");
        assertThat(indeed.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(indeed.jobsApiUrl()).isEqualTo("https://to.indeed.com");
        assertThat(indeed.checklist().officialApiEndpointUrl()).isEqualTo("https://to.indeed.com");

        assertThat(gupy.sourceFamily()).isEqualTo("GUPY");
        assertThat(gupy.targetSite().getSiteCode()).isEqualTo("gupy_specialdog_extrema");
        assertThat(gupy.targetSite().getDisplayName()).isEqualTo("Special Dog Company Careers via Gupy - Extrema");
        assertThat(gupy.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(gupy.jobsApiUrl()).isEqualTo("https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema");
        assertThat(gupy.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(gupy.targetSite().isEnabled()).isTrue();

        assertThat(lever.sourceFamily()).isEqualTo("LEVER");
        assertThat(lever.targetSite().getSiteCode()).isEqualTo("lever_ciandt");
        assertThat(lever.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(lever.jobsApiUrl()).isEqualTo("https://api.lever.co/v0/postings/ciandt?mode=json");
        assertThat(lever.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(lever.targetSite().isEnabled()).isTrue();

        assertThat(watchguard.sourceFamily()).isEqualTo("LEVER");
        assertThat(watchguard.targetSite().getSiteCode()).isEqualTo("lever_watchguard");
        assertThat(watchguard.targetSite().getDisplayName()).isEqualTo("WatchGuard Careers via Lever");
        assertThat(watchguard.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.SCRAPING_PROIBIDO);
        assertThat(watchguard.jobsApiUrl()).isEqualTo("https://api.lever.co/v0/postings/watchguard?mode=json");
        assertThat(watchguard.targetSite().getLegalStatus().name()).isEqualTo("SCRAPING_PROIBIDO");
        assertThat(watchguard.targetSite().isEnabled()).isFalse();

        assertThat(workday.sourceFamily()).isEqualTo("WORKDAY");
        assertThat(workday.targetSite().getSiteCode()).isEqualTo("airbus_helibras_workday");
        assertThat(workday.targetSite().getDisplayName()).isEqualTo("Airbus / Helibras Careers via Workday");
        assertThat(workday.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(workday.jobsApiUrl()).isEqualTo("https://ag.wd3.myworkdayjobs.com/wday/cxs/ag/Airbus/jobs");
        assertThat(workday.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(workday.targetSite().isEnabled()).isTrue();

        assertThat(workdayAlcoa.sourceFamily()).isEqualTo("WORKDAY");
        assertThat(workdayAlcoa.targetSite().getSiteCode()).isEqualTo("alcoa_pocos_caldas_workday");
        assertThat(workdayAlcoa.targetSite().getDisplayName()).isEqualTo("Alcoa Careers via Workday - Poços de Caldas");
        assertThat(workdayAlcoa.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(workdayAlcoa.jobsApiUrl()).isEqualTo("https://alcoa.wd5.myworkdayjobs.com/wday/cxs/alcoa/Careers/jobs");
        assertThat(workdayAlcoa.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(workdayAlcoa.targetSite().isEnabled()).isTrue();

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
        assertThat(campinas.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(campinas.targetSite().isEnabled()).isTrue();
        assertThat(campinas.jobsApiUrl()).contains("portal-api.campinas.sp.gov.br/jsonapi/node/site");
        assertThat(campinas.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);

        assertThat(pocosCaldas.sourceFamily()).isEqualTo("MUNICIPAL_PDF");
        assertThat(pocosCaldas.targetSite().getSiteCode()).isEqualTo("municipal_pocos_caldas");
        assertThat(pocosCaldas.targetSite().getSelectorBundleVersion()).isEqualTo("pocos_caldas_pdf_v1");
        assertThat(pocosCaldas.targetSite().getLegalStatus().name()).isEqualTo("PENDING_REVIEW");
        assertThat(pocosCaldas.targetSite().isEnabled()).isFalse();
        assertThat(pocosCaldas.jobsApiUrl())
                .isEqualTo("https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609");
        assertThat(pocosCaldas.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.DADOS_PUBLICOS);

        assertThat(extrema.sourceFamily()).isEqualTo("MUNICIPAL_HTML");
        assertThat(extrema.targetSite().getSiteCode()).isEqualTo("municipal_extrema");
        assertThat(extrema.targetSite().getSelectorBundleVersion()).isEqualTo("extrema_html_v1");
        assertThat(extrema.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(extrema.targetSite().isEnabled()).isTrue();
        assertThat(extrema.jobsApiUrl()).isEqualTo("https://www.extrema.mg.gov.br/secretarias/educacao");
        assertThat(extrema.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.DADOS_PUBLICOS);

        assertThat(camaraSantaRita.sourceFamily()).isEqualTo("LEGISLATIVE_HTML");
        assertThat(camaraSantaRita.targetSite().getSiteCode()).isEqualTo("camara_santa_rita_sapucai");
        assertThat(camaraSantaRita.targetSite().getSelectorBundleVersion()).isEqualTo("camara_santa_rita_html_v1");
        assertThat(camaraSantaRita.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(camaraSantaRita.targetSite().isEnabled()).isTrue();
        assertThat(camaraSantaRita.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.DADOS_PUBLICOS);

        assertThat(camaraItajuba.sourceFamily()).isEqualTo("LEGISLATIVE_HTML");
        assertThat(camaraItajuba.targetSite().getSiteCode()).isEqualTo("camara_itajuba");
        assertThat(camaraItajuba.targetSite().getSelectorBundleVersion()).isEqualTo("camara_itajuba_html_v1");
        assertThat(camaraItajuba.targetSite().getLegalStatus().name()).isEqualTo("APPROVED");
        assertThat(camaraItajuba.targetSite().isEnabled()).isTrue();
        assertThat(camaraItajuba.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.DADOS_PUBLICOS);
    }

    @Test
    @DisplayName("should find curated profile by target site code")
    void shouldFindCuratedProfileByTargetSiteCode() {
        assertThat(catalog.findBySiteCode("greenhouse_bitso"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("greenhouse_bitso");

        assertThat(catalog.findBySiteCode("gupy_specialdog_extrema"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("gupy_specialdog_extrema");

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

        assertThat(catalog.findBySiteCode("airbus_helibras_workday"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("airbus_helibras_workday");

        assertThat(catalog.findBySiteCode("alcoa_pocos_caldas_workday"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("alcoa_pocos_caldas_workday");

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

        assertThat(catalog.findBySiteCode("municipal_pocos_caldas"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("municipal_pocos_caldas");

        assertThat(catalog.findBySiteCode("municipal_extrema"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("municipal_extrema");

        assertThat(catalog.findBySiteCode("camara_santa_rita_sapucai"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("camara_santa_rita_sapucai");

        assertThat(catalog.findBySiteCode("camara_itajuba"))
                .isPresent()
                .get()
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .isEqualTo("camara_itajuba");

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
