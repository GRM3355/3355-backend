package com.grm3355.zonie.commonlib.global.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 공통 응답 구조체
 * @param <T> 응답 데이터 타입
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

	private boolean success;
	private T data;
	private ErrorResponse error;
	private LocalDateTime timestamp; // ISO 8601 포맷으로 자동 직렬화됨

	/**
	 * 성공 응답 생성 (데이터 포함)
	 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null, LocalDateTime.now());
	}

	/**
	 * 성공 응답 생성 (데이터 없음, e.g., 204 No Content)
	 */
	public static <T> ApiResponse<T> success() {
		return new ApiResponse<>(true, null, null, LocalDateTime.now());
	}

	/**
	 * 실패 응답 생성
	 */
	public static <T> ApiResponse<T> failure(String errorCode, String message) {
		return new ApiResponse<>(false, null, new ErrorResponse(errorCode, message), LocalDateTime.now());
	}
}
