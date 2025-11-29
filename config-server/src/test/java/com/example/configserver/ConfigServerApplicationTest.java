package com.example.configserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ConfigServerApplicationTest {

	@Test
	void controllerCanBeInstantiated() {
		ConfigService service = mock(ConfigService.class);
		ConfigServerController controller = new ConfigServerController(service);
		assertNotNull(controller);
	}

	@Test
	void healthCheckReturnsOk() {
		ConfigService service = mock(ConfigService.class);
		ConfigServerController controller = new ConfigServerController(service);

		var response = controller.health();
		assertNotNull(response);
		assertTrue(response.getStatusCode().is2xxSuccessful());
	}
}
