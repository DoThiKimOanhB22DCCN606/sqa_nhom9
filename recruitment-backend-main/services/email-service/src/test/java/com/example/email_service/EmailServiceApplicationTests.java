package com.example.email_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmailServiceApplicationTests {

	@Test
	// Test Case ID: TC-ESA-001 - application context smoke test.
	@DisplayName("TC-ESA-001 - Spring context should load for email-service")
	void tc_esa_001_contextLoads_whenSpringBootStarts_shouldLoadApplicationContext() {
	}

}
