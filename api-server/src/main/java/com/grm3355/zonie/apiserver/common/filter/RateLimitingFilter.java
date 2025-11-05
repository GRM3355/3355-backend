package com.grm3355.zonie.apiserver.common.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.global.service.RateLimitingService;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

	//스프링시큐리티에서 과도한 요청회수 필터링
	private static final List<String> AUTH_ENDPOINTS = Arrays.asList(
		"/api/v1/auth/token-register",
		"/api/v1/auth/location-token",
		"/api/v1/location/update",
		"/api/v1/location/festivalVerify",
		"/api/v1/location/chatroomVerify"
	);

	private static final int MAX_REQUESTS = 5; // 예: 5회 요청
	private static final int WINDOW_SECONDS = 60; // 윈도우 시간 (초)
	//private static final int WINDOW_SECONDS = 300; // 5분
	private final RateLimitingService rateLimitingService;
	private final ObjectMapper objectMapper;

	@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		String requestUri = request.getRequestURI();

		if (AUTH_ENDPOINTS.contains(requestUri)) {
			String clientIp = getClientIp(request);
			String rateLimitKey = clientIp + ":" + requestUri;

			if (!rateLimitingService.allowRequest(rateLimitKey, MAX_REQUESTS, WINDOW_SECONDS)) {
				log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, requestUri);
				sendTooManyRequestsResponse(response);
				return;
			}

		}

		filterChain.doFilter(request, response);
	}

	private String getClientIp(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null || !xfHeader.contains(".")) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0];
	}

	private void sendTooManyRequestsResponse(HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Object> apiResponse = ApiResponse.failure(
			ErrorCode.TOO_MANY_REQUESTS.toString(),
			"요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요."
		);
		String jsonResponse = objectMapper.writeValueAsString(apiResponse);
		response.getWriter().write(jsonResponse);
	}
}
