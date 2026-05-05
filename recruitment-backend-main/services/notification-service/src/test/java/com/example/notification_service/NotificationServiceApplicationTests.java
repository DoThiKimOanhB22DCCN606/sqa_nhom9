package com.example.notification_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NotificationServiceApplicationTests {

	@Test
	// Test Case ID: TC-NSA-001 - application context smoke test.
	@DisplayName("TC-NSA-001 - Spring context should load for notification-service")
	void tc_nsa_001_contextLoads_whenSpringBootStarts_shouldLoadApplicationContext() {
	}

}
