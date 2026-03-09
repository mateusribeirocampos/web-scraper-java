package com.campos.webscraper;

import org.springframework.boot.SpringApplication;

public class TestWebscraperApplication {

	public static void main(String[] args) {
		SpringApplication.from(WebscraperApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
