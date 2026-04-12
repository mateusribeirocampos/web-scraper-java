package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan(basePackageClasses = TargetSiteEntity.class)
@EnableJpaRepositories(basePackageClasses = TargetSiteRepository.class)
class RepositoryPersistenceTestApplication {
}
