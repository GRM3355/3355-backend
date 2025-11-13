package com.grm3355.zonie.apiserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.grm3355.zonie.apiserver.domain.auth.service.AuthService;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

@SpringBootTest
class ApiServerApplicationTests extends BaseIntegrationTest {

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthService authService;

	@Test
	void contextLoads() {
	}

}
