package com.grm3355.zonie.batchserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// @EnableBatchProcessing // 3.x 이상에선 사용X
@ActiveProfiles("test")
@SpringBootTest(classes = BatchServerApplication.class)
class BatchServerApplicationTests extends BaseIntegrationTest {

	@Test
	void contextLoads() {
	}

}
