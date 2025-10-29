package com.grm3355.zonie.commonlib.global.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공통 에러 코드 정의 (HttpStatus와 별개로 내부 정의)
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// Common Errors (400)
	BAD_REQUEST("COMMON_BAD_REQUEST", "잘못된 요청입니다."),
	INVALID_INPUT_VALUE("INVALID_INPUT_VALUE", "입력값이 유효하지 않습니다."),
	INVALID_REQUEST_PARAM("INVALID_REQUEST_PARAM", "요청 파라미터가 잘못되었습니다."),

	// Auth Errors (401, 403)
	UNAUTHORIZED("UNAUTHORIZED", "인증되지 않은 사용자입니다."),
	FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다."),

	// Resource Errors (404)
	NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
	URL_NOT_FOUND("NOT_FOUND_URL", "요청한 URL을 찾을 수 없습니다."),

	// Rate Limiting (429)
	TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", "요청 횟수를 초과했습니다."),

	// Server Errors (500)
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

	private final String code;
	private final String message;
}
