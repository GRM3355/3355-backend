package com.grm3355.zonie.commonlib.global.response;


import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author rua
 */
@JsonInclude(Include.NON_NULL)
public record CustomApiResponse<T>(
	boolean success,
	int status,
	String message,
	T data,
	Instant timestamp
) {
	/* ---------- Factory: Success ---------- */

	public static <T> CustomApiResponse<T> ok(T data) {
		return of(true, HttpStatus.OK, "OK", data);
	}

	public static <T> CustomApiResponse<T> created(T data) {
		return of(true, HttpStatus.CREATED, "CREATED", data);
	}

	/** 데이터 없는 성공 응답(예: DELETE 204 등) */
	public static <T> CustomApiResponse<T> noContent() {
		return of(true, HttpStatus.NO_CONTENT, "NO_CONTENT", null);
	}

	/* ---------- Factory: Error ---------- */

	public static <T> CustomApiResponse<T> error(HttpStatus status, String message) {
		return of(false, status, message, null);
	}

	public static <T> CustomApiResponse<T> error(int statusCode, String message) {
		return of(false, HttpStatus.valueOf(statusCode), message, null);
	}

	public static <T> CustomApiResponse<T> error(HttpStatus status, String message, T data) {
		return of(false, status, message, data);
	}

	/* ---------- Common Builder ---------- */

	public static <T> CustomApiResponse<T> of(boolean success, HttpStatus status, String message, T data) {
		return new CustomApiResponse<>(success, status.value(), message, data, Instant.now());
	}

	/* ---------- ResponseEntity Helper (선택) ---------- */

	/** 컨트롤러에서 바로 반환하고 싶을 때 사용 */
	public ResponseEntity<CustomApiResponse<T>> toResponseEntity() {
		return ResponseEntity.status(this.status).body(this);
	}
}
