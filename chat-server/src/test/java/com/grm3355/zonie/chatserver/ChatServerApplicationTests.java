package com.grm3355.zonie.chatserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.grm3355.zonie.chatserver.util.JwtChannelInterceptor;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

@SpringBootTest
@TestPropertySource(properties = {
	"location.token.ttl-minutes=15",
	"chat.radius=1.0",
	"chat.max-chat-person=300"
})
public class ChatServerApplicationTests extends BaseIntegrationTest {

	@MockitoBean
	private JwtChannelInterceptor jwtChannelInterceptor;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void contextLoads() {
	}

}
