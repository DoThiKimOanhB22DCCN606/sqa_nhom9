package com.example.user_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test — cần DB/Kafka và cấu hình đầy đủ (chỉ chạy Mockito unit test trong service package)")
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
