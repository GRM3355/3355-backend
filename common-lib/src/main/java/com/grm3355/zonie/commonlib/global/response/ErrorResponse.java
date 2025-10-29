package com.grm3355.zonie.commonlib.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 실패 시 응답 내 'error' 필드 구조체
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
	private String code;
	private String message;

	/**
	 * ErrorResponse 객체를 생성하는 정적 팩토리 메서드
	 * @param code 에러 코드
	 * @param message 에러 메시지
	 * @return ErrorResponse 인스턴스
	 */
	public static ErrorResponse of(String code, String message) {
		return new ErrorResponse(code, message);
	}
}
