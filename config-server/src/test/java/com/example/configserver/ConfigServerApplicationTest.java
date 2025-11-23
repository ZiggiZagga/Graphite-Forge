package com.example.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class ConfigServerApplicationTest {

	@Autowired
	private ConfigServerController controller;

	@Test
	void contextLoads() {
		assertNotNull(controller);
	}

	@Test
	void healthCheckReturnsOk() {
		var response = controller.health();
		assertNotNull(response);
		assert response.getStatusCode().is2xxSuccessful();
	}
}
