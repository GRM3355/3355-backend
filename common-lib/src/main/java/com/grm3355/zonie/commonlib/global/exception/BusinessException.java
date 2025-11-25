package com.grm3355.zonie.commonlib.global.exception;

import java.util.Map;

import lombok.Getter;

/**
 * 도메인/애플리케이션 계층에서 throw 하는 런타임 예외의 표준 베이스.
 * - ErrorCode 로 HTTP 상태/코드/디폴트 메시지를 통일
 * - details 로 추가 컨텍스트를 전달(선택)
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	private final Map<String, Object> details;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.details = null;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.details = null;
	}

	public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
		super(message);
		this.errorCode = errorCode;
		this.details = details;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

	public Map<String, Object> details() {
		return details;
	}
}
