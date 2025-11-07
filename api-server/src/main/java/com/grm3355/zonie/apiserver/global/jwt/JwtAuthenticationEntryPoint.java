package com.grm3355.zonie.apiserver.global.jwt;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException)
		throws IOException {
		log.warn("인증되지 않은 접근 시도: {}", authException.getMessage());

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Object> apiResponse = ApiResponse.failure(ErrorCode.ACCESS_DENIED.getCode(), "인증이 필요합니다.");
		String jsonResponse = objectMapper.writeValueAsString(apiResponse);

		response.getWriter().write(jsonResponse);
	}
}
