package com.grm3355.zonie.apiserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ImportAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class ApiServerApplicationTests extends BaseIntegrationTest {

	@Test
	void contextLoads() {
	}

}
