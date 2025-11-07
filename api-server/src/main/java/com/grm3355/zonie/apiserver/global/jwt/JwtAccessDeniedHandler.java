package com.grm3355.zonie.apiserver.global.jwt;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
		AccessDeniedException accessDeniedException)
		throws IOException {
		log.warn("사용자 접근 거부: {}, 경로: {}", request.getRemoteUser(), request.getRequestURI());

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Object> apiResponse = ApiResponse.failure(ErrorCode.FORBIDDEN.getCode(), "접근 권한이 없습니다.");
		String jsonResponse = objectMapper.writeValueAsString(apiResponse);

		response.getWriter().write(jsonResponse);
	}
}
