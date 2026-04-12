package com.campos.webscraper;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	public static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16");

	public static PostgreSQLContainer<?> newPostgresContainer() {
		return new PostgreSQLContainer<>(POSTGRES_IMAGE);
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return newPostgresContainer();
	}

}
