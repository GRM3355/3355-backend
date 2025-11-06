package com.grm3355.zonie.chatserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.grm3355.zonie.chatserver.util.JwtChannelInterceptor;
import com.grm3355.zonie.commonlib.domain.auth.JwtTokenProvider;

@SpringBootTest
class ChatServerApplicationTests extends BaseIntegrationTest {

	@MockitoBean
	private JwtChannelInterceptor jwtChannelInterceptor;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void contextLoads() {
	}

}
