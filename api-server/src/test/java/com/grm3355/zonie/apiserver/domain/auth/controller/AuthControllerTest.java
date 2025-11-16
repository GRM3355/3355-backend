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

@DisplayName("토큰 발행 통합테스트")
@WebMvcTest(
	controllers = AuthController.class,
	excludeAutoConfiguration = {
		DataSourceAutoConfiguration.class,
		JpaRepositoriesAutoConfiguration.class
	}
)
@AutoConfigureMockMvc(addFilters = false) //시큐리티 제외
	//@AutoConfigureMockMvc(addFilters = true)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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

	private String testAccessToken;
	private String testRefreshToken;

	@Value("${client.redirect.uri:http://localhost:8082}") // 실제 사용하는 프론트 주소
	private String clientRedirectUri;

	@Test
	void registerToken_Success() throws Exception {
		LocationDto locationDto = new LocationDto();
		locationDto.setLat(37.5665);
		locationDto.setLon(126.9780);

		AuthResponse mockResponse = new AuthResponse("access-token-12345", null);

		Mockito.when(authService.register(
			ArgumentMatchers.any(LocationDto.class))
		).thenReturn(mockResponse);

		mockMvc.perform(post("/api/auth/tokens")
				//.header("Authorization", "Bearer test")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(locationDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("access-token-12345"))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void 카카오_OAuth2_로그인을_한다() throws Exception {
		LoginResponse expected = new LoginResponse("accesstoken", "nickname");
		given(authService.login(any(LoginRequest.class)))
			.willReturn(expected);

		String response = mockMvc.perform(get("/api/auth/kakao/callback")
				.param("code", "code"))
			.andExpect(status().isOk())
			.andDo(print())
			.andReturn()
			.getResponse()
			.getContentAsString();
		LoginResponse actual = objectMapper.readValue(response, LoginResponse.class);

		assertThat(actual).isEqualTo(expected);
	}
	//
	// @Test
	// void testAccessTokenAndRefreshTokenFlow() throws Exception {
	//
	// 	String code = "testCode";
	// 	LoginResponse loginResponse = new LoginResponse("access-token-123", "refresh-token-123");
	// 	given(authService.login(any(LoginRequest.class))).willReturn(loginResponse);
	//
	//
	// 	// 실제 로그인 -> 토큰 발급 -> refresh -> logout 전체 흐름 테스트
	// 	// 로그인을 위해 kakao callback 호출
	// 	MockHttpServletResponse loginResp = mockMvc.perform(get("/api/auth/kakao/callback")
	// 			.param("code", code))
	// 		.andExpect(status().isOk())
	// 		.andReturn().getResponse();
	//
	// 	// refresh token으로 액세스 토큰 재발급
	// 	MockHttpServletResponse refreshResp = mockMvc.perform(post("/api/auth/refresh")
	// 			.cookie(new jakarta.servlet.http.Cookie("refreshToken", testRefreshToken))
	// 			.contentType(MediaType.APPLICATION_JSON))
	// 		.andExpect(status().isOk())
	// 		.andReturn().getResponse();
	//
	// 	// 로그아웃
	// 	mockMvc.perform(post("/api/auth/logout")
	// 			.header("Authorization", "Bearer " + testAccessToken))
	// 		.andExpect(status().isOk())
	// 		.andExpect(cookie().maxAge("refreshToken", 0));
	// }

	@Test
	@DisplayName("카카오 로그인후 callback")
	void loginWithKakao_shouldReturnHtmlAndSetCookie() throws Exception {
		// given: 카카오 로그인 코드
		String code = "testCode";

		// 가짜 로그인 응답
		LoginResponse loginResponse = new LoginResponse(
			"mock-access-token",
			"mock-refresh-token"
		);
		given(authService.login(any(LoginRequest.class))).willReturn(loginResponse);

		// when: 컨트롤러 호출
		mockMvc.perform(get("/api/auth/kakao/callback")
				.param("code", code))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.TEXT_HTML))
			.andExpect(header().string("Set-Cookie", containsString("refreshToken=mock-refresh-token")))
			.andExpect(content().string(containsString("accessToken: 'mock-access-token'")))
			.andDo(result -> {
				System.out.println(result.getResponse().getContentAsString());
				System.out.println(result.getResponse().getHeader("Set-Cookie"));
			});

		// then: authService.login이 호출되었는지 검증
		verify(authService, times(1)).login(any(LoginRequest.class));
	}

	@Test
	@DisplayName("새로운 액세스 토큰 발급")
	void 새로운_액세스_토큰_발급() throws Exception {
		// given
		String oldRefreshToken = "mock-old-refresh-token";
		String newAccessToken = "mock-new-access-token";
		String newRefreshToken = "mock-new-refresh-token";

		LoginResponse loginResponse = new LoginResponse(newAccessToken, newRefreshToken);

		// Redis 토큰 검증 모킹
		given(redisTokenService.validateRefreshToken(oldRefreshToken)).willReturn(true);

		// AuthService 토큰 재발급 모킹
		given(authService.refreshAccessToken(oldRefreshToken)).willReturn(loginResponse);

		// when & then
		mockMvc.perform(post("/api/auth/refresh")
				.cookie(new jakarta.servlet.http.Cookie("refreshToken", oldRefreshToken)))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
			.andExpect(cookie().value("refreshToken", newRefreshToken))
			.andDo(result -> {
				MockHttpServletResponse response = result.getResponse();
				System.out.println("Response Cookie: " + response.getHeader("Set-Cookie"));
				System.out.println("Response Body: " + response.getContentAsString());
			});
	}

	@Test
	@DisplayName("refreshToken 존재여부 실패")
	void 리프레시토큰존재_실패() throws Exception {
		mockMvc.perform(post("/api/auth/refresh"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("refreshToken 존재하고 Redis에 존재하지 않아서 실패")
	void 리프레시토큰_존재_Redis_존재하지않음_실패() throws Exception {
		String invalidToken = "invalid-token";

		given(redisTokenService.validateRefreshToken(invalidToken)).willReturn(false);

		// when & then
		mockMvc.perform(post("/api/auth/refresh")
				.cookie(new jakarta.servlet.http.Cookie("refreshToken", invalidToken)))
			.andExpect(status().isUnauthorized());
	}

}
