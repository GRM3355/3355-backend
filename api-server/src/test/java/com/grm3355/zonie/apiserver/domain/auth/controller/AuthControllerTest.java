package com.grm3355.zonie.apiserver.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.apiserver.global.service.RateLimitingService;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.service.AuthService;
import com.grm3355.zonie.apiserver.domain.auth.dto.AuthResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("토큰 발행 통합테스트")
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) //시큐리티 제외
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

	@Test
	void registerToken_Success() throws Exception {
		LocationDto locationDto = new LocationDto();
		locationDto.setLat(37.5665);
		locationDto.setLon(126.9780);

		AuthResponse mockResponse = new AuthResponse("access-token-12345");

		Mockito.when(authService.register(
			ArgumentMatchers.any(LocationDto.class),
			ArgumentMatchers.any(HttpServletRequest.class))
		).thenReturn(mockResponse);

		mockMvc.perform(post("/api/v1/auth/token-register")
				.header("Authorization", "Bearer test")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(locationDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("access-token-12345"))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	// @Test
	// @DisplayName("입력값 누락 시 400 에러 반환")
	// void registerToken_BadRequest() throws Exception {
	// 	// given - 위도/경도 누락된 경우
	// 	LocationDto invalidDto = null;
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/auth/token-register")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(invalidDto)))
	// 		.andExpect(status().isBadRequest());
	// }
}