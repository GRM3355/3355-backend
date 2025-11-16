package com.grm3355.zonie.apiserver.domain.auth.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.apiserver.domain.auth.dto.AuthResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.auth.LoginRequest;
import com.grm3355.zonie.apiserver.domain.auth.dto.auth.LoginResponse;
import com.grm3355.zonie.apiserver.domain.auth.service.AuthService;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.global.jwt.JwtAccessDeniedHandler;
import com.grm3355.zonie.apiserver.global.jwt.JwtAuthenticationEntryPoint;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.global.service.RateLimitingService;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@DisplayName("토큰 발행 통합테스트")
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false) //시큐리티 제외
class AuthControllerLooutTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@MockitoBean
	private RedisTokenService redisTokenService;

	@Test
	void 로그아웃_테스트_성공() throws Exception {
		// given: UserDetailsImpl 생성
		UserDetailsImpl userDetails = new UserDetailsImpl(
			"test-user","password","test@example.com",
			null,false,false
		);

		// Redis deleteByToken 호출 모킹
		doNothing().when(redisTokenService).deleteByToken("test-user");

		// SecurityContext에 인증 객체 설정
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userDetails,null,List.of(() -> "ROLE_USER")
			);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		// when: /logout 요청
		mockMvc.perform(post("/api/auth/logout"))
			.andExpect(status().isOk())
			.andExpect(header().string("Set-Cookie", containsString("refreshToken=;")))
			.andExpect(content().string(containsString("success")))
			.andDo(result -> {
				System.out.println("Set-Cookie header: " + result.getResponse().getHeader("Set-Cookie"));
				System.out.println("Response body: " + result.getResponse().getContentAsString());
			});

		// then: Redis 삭제 호출 검증
		verify(redisTokenService, times(1)).deleteByToken("test-user");

		// SecurityContext 초기화
		SecurityContextHolder.clearContext();
	}

}
