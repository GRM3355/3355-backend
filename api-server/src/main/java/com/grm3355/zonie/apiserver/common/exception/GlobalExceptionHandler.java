package com.grm3355.zonie.apiserver.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.grm3355.zonie.commonlib.global.exception.ApiErrorPayload;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.CustomErrorCode;
import com.grm3355.zonie.commonlib.global.exception.CustomValidationException;
import com.grm3355.zonie.commonlib.global.exception.NotFoundException;
import com.grm3355.zonie.commonlib.global.response.CustomApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// @ExceptionHandler(Exception.class)
	// public ResponseEntity<String> fallback(Exception ex) {
	// 	return ResponseEntity.status(500).body("Internal Server Error");
	// }

	/**
	 * 문자열이 null이거나 비어있는 경우 null을 반환하고, 그렇지 않으면 원본 문자열을 반환한다.
	 * 주로 MDC(Mapped Diagnostic Context)에서 traceId를 안전하게 가져올 때 사용된다.
	 */
	private static String safe(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}

	/**
	 * 에러 코드 매핑 표준화
	 * - 400: 잘못된 요청(검증 실패, 타입 불일치, 잘못된 인자 등)
	 * - 401: 인증 실패
	 * - 403: 권한 없음
	 * - 404: 리소스 없음
	 * - 409: 충돌(비즈니스 규칙 위반 등)
	 * - 500: 내부 서버 오류
	 *
	 * 검증 계층(MethodArgumentNotValid 등)은 ApiValidationAdvice가 우선 처리(@Order(HIGHEST_PRECEDENCE)).
	 * 본 핸들러는 그 외 전역 예외를 표준 스키마로 응답합니다.
	 */

	/* ======= BusinessException ======= */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleBusiness(
		BusinessException ex, HttpServletRequest req) {

		CustomErrorCode code = ex.errorCode();
		return build(code, ex.getMessage(), ex.details(), req);
	}

	/* ======= Validation (@Valid/@Validated) ======= */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleMethodArgumentNotValid(
		MethodArgumentNotValidException ex, HttpServletRequest req) {

		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
			.forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

		return build(CustomErrorCode.VALIDATION_FAILED, "요청 본문 검증 실패", fieldErrors, req);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleConstraintViolation(
		ConstraintViolationException ex, HttpServletRequest req) {

		Map<String, String> paramErrors = new LinkedHashMap<>();
		for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
			paramErrors.put(v.getPropertyPath().toString(), v.getMessage());
		}
		return build(CustomErrorCode.VALIDATION_FAILED, "요청 파라미터 검증 실패", paramErrors, req);
	}

	/* ======= HTTP 스펙 관련 ======= */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleMissingParam(
		MissingServletRequestParameterException ex, HttpServletRequest req) {

		Map<String, String> detail = Map.of(ex.getParameterName(), "required");
		return build(CustomErrorCode.INVALID_INPUT, ex.getMessage(), detail, req);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleNotReadable(
		HttpMessageNotReadableException ex, HttpServletRequest req) {

		return build(CustomErrorCode.PAYLOAD_MALFORMED, "유효하지 않은 JSON 본문입니다.", null, req);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleMethodNotAllowed(
		HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {

		return build(CustomErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), null, req);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleUnsupportedMediaType(
		HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {

		return build(CustomErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), null, req);
	}

	/* ======= 보안 관련 ======= */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleAccessDenied(
		AccessDeniedException ex, HttpServletRequest req) {

		return build(CustomErrorCode.ACCESS_DENIED, ex.getMessage(), null, req);
	}

	@ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleAuthentication(
		org.springframework.security.core.AuthenticationException ex, HttpServletRequest req) {

		return build(CustomErrorCode.UNAUTHORIZED, "인증에 실패했습니다.", null, req);
	}

	@ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleExpiredJwt(
		io.jsonwebtoken.ExpiredJwtException ex, HttpServletRequest req) {

		return build(CustomErrorCode.TOKEN_EXPIRED, "인증 토큰이 만료되었습니다.", null, req);
	}

	/* ======= 데이터/리소스 ======= */
	@ExceptionHandler({NoSuchElementException.class})
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleNotFound(
		RuntimeException ex, HttpServletRequest req) {

		return build(CustomErrorCode.NOT_FOUND, ex.getMessage(), null, req);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleDataIntegrity(
		DataIntegrityViolationException ex, HttpServletRequest req) {

		// "idx_member_email" 문자열을 포함하는 경우, 이메일 중복으로 간주
		if (ex.getMessage() != null && ex.getMessage().contains("idx_member_email")) {
			return build(CustomErrorCode.CONFLICT, "이미 등록된 이메일입니다.", null, req);
		}

		return build(CustomErrorCode.DATA_INTEGRITY_VIOLATION, "데이터 제약조건 위반", null, req);
	}

	/* ======= Fallback ======= */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<CustomApiResponse<ApiErrorPayload>> handleUnknown(
		Exception ex, HttpServletRequest req) {

		// 전체 스택을 ERROR로 기록하여 커밋 실패 등 근본 원인 파악
		log.error("Unhandled exception occurred", ex);

		return build(CustomErrorCode.INTERNAL_ERROR, "예상치 못한 오류가 발생했습니다.", null, req);
	}

	/* ======= 공통 빌더 ======= */

	/**
	 * 공통 에러 응답 페이로드(ApiErrorPayload)를 생성하고, 이를 ApiResponse로 감싸 ResponseEntity를 반환한다.
	 * MDC에 설정된 traceId와 요청 경로를 포함하여 에러 추적을 용이하게 한다.
	 */
	private ResponseEntity<CustomApiResponse<ApiErrorPayload>> build(
		CustomErrorCode code, String message, Object errors, HttpServletRequest req) {

		//String traceId = safe(MDC.get("traceId"));         // 로깅 필터에서 넣어두면 추적 가능
		String path = req != null ? req.getRequestURI() : null;

		ApiErrorPayload payload = new ApiErrorPayload(
			//code.code(),
			message != null ? message : code.defaultMessage(),
			path,
			errors
		);

		// 상위 ApiResponse의 message에는 "표준 에러 코드"를 올려 클라이언트 분기를 단순화
		CustomApiResponse<ApiErrorPayload> body =
			CustomApiResponse.of(false, code.status(), code.code(), payload);

		return body.toResponseEntity();
	}

	/**
	 * service에서 잘못입력된 부분을 400번 에러로 처리한다.
	 */
	@ExceptionHandler(CustomValidationException.class)
	public ResponseEntity<CustomApiResponse<Object>> CustomValidationException(CustomValidationException ex,
		HttpServletRequest req) {

		CustomApiResponse<Object> error = CustomApiResponse.error(
			HttpStatus.BAD_REQUEST,
			ex.getMessage()
		);

		return ResponseEntity.badRequest().body(error);
	}

	/**
	 * service에서 리소스 찾을수 없을때 404 에러 출력
	 */
	@ExceptionHandler({
		NotFoundException.class})
	public ResponseEntity<CustomApiResponse<Object>> handlerNotFoundException(NotFoundException ex,
		HttpServletRequest req) {

		CustomApiResponse<Object> error = CustomApiResponse.error(
			HttpStatus.NOT_FOUND,
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

	}

}
