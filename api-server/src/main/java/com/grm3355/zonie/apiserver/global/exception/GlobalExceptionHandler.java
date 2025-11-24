package com.grm3355.zonie.apiserver.global.exception;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.grm3355.zonie.commonlib.global.exception.ApiErrorPayload;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.CustomValidationException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.exception.NotFoundException;
import com.grm3355.zonie.commonlib.global.exception.ValidationException;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleBusiness(
		BusinessException ex, HttpServletRequest req) {
		log.error("=================================> BusinessException.class 에러 로그 찍기", ex); // 예외 로그 찍기
		ErrorCode code = ex.errorCode();
		return build(code, ex.getMessage(), ex.details(), req);
	}

	/* ======= Validation (@Valid/@Validated) ======= */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleMethodArgumentNotValid(
		MethodArgumentNotValidException ex, HttpServletRequest req) {

		List<String> errorData = ex.getBindingResult().getFieldErrors()
			.stream()
			.map(e -> e.getField() + " : " + e.getDefaultMessage())
			.collect(Collectors.toList());

		ApiResponse<Object> error = ApiResponse.failure(
			HttpStatus.BAD_REQUEST.toString(),
			ErrorCode.BAD_REQUEST.getMessage(),
			errorData);

		return ResponseEntity.badRequest().body(error);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleConstraintViolation(
		ConstraintViolationException ex, HttpServletRequest req) {
		log.error("=================================> ConstraintViolationException.class 에러 로그 찍기", ex); // 예외 로그 찍기

		// 검증 메시지 추출
		String message = ex.getConstraintViolations().stream()
			.map(violation -> violation.getMessage())
			.findFirst()
			.orElse("잘못된 요청입니다.");

		return ResponseEntity.badRequest().body(ApiResponse.failure("BAD_REQUEST", message));

	}

	/* ======= HTTP 스펙 관련 ======= */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleMissingParam(
		MissingServletRequestParameterException ex, HttpServletRequest req) {
		log.error("=================================> MissingServletRequestParameterException.class 에러로그 찍기",
			ex); // 예외 로그 찍기

		Map<String, String> detail = Map.of(ex.getParameterName(), "required");
		return build(ErrorCode.BAD_REQUEST, ex.getMessage(), detail, req);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleNotReadable(
		HttpMessageNotReadableException ex, HttpServletRequest req) {

		log.error("=================================> HttpMessageNotReadableException.class 에러로드 찍기", ex); // 예외 로그 찍기

		return build(ErrorCode.BAD_REQUEST, "유효하지 않은 JSON 본문입니다.", null, req);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleMethodNotAllowed(
		HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {

		log.error("=================================> HttpRequestMethodNotSupportedException.class 에러로드 찍기",
			ex); // 예외 로그 찍기

		return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), null, req);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleUnsupportedMediaType(
		HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {

		log.error("=================================> HttpMediaTypeNotSupportedException.class 에러로드 찍기",
			ex); // 예외 로그 찍기

		return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), null, req);
	}

	/* ======= 보안 관련 ======= */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleAccessDenied(
		AccessDeniedException ex, HttpServletRequest req) {

		log.error("=================================> handleAccessDenied.class 에러로드 찍기", ex); // 예외 로그 찍기

		return build(ErrorCode.FORBIDDEN, ex.getMessage(), null, req);
	}

	@ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleAuthentication(
		org.springframework.security.core.AuthenticationException ex, HttpServletRequest req) {

		log.error("=================================> handleAuthentication.class 에러로드 찍기", ex); // 예외 로그 찍기

		return build(ErrorCode.UNAUTHORIZED, "인증에 실패했습니다.", null, req);
	}

	@ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleExpiredJwt(
		io.jsonwebtoken.ExpiredJwtException ex, HttpServletRequest req) {

		log.error("=================================> ExpiredJwtException.class 에러로드 찍기", ex); // 예외 로그 찍기

		return build(ErrorCode.UNAUTHORIZED, "인증 토큰이 만료되었습니다.", null, req);
	}

	/* ======= 데이터/리소스 ======= */
	@ExceptionHandler({NoSuchElementException.class})
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleNotFound(
		RuntimeException ex, HttpServletRequest req) {

		log.error("=================================> NoSuchElementException.class 에러 로그 찍기", ex); // 예외 로그 찍기

		return build(ErrorCode.NOT_FOUND, ex.getMessage(), null, req);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleDataIntegrity(
		DataIntegrityViolationException ex, HttpServletRequest req) {

		log.error("=================================> DataIntegrityViolationException.class 에러 로그 찍기", ex); // 예외 로그 찍기

		// "idx_member_email" 문자열을 포함하는 경우, 이메일 중복으로 간주
		if (ex.getMessage() != null && ex.getMessage().contains("idx_member_email")) {
			return build(ErrorCode.CONFLICT, "이미 등록된 이메일입니다.", null, req);
		}

		return build(ErrorCode.DATA_INTEGRITY_VIOLATION, "데이터 제약조건 위반", null, req);
	}

	/* ======= Fallback ======= */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleUnknown(
		Exception ex, HttpServletRequest req) {

		log.error("=================================> Exception.class 에러 로그 찍기", ex); // 예외 로그 찍기

		// 전체 스택을 ERROR로 기록하여 커밋 실패 등 근본 원인 파악
		log.error("Unhandled exception occurred", ex);

		return build(ErrorCode.INTERNAL_SERVER_ERROR, "예상치 못한 오류가 발생했습니다.", null, req);
	}

	/* ======= 공통 빌더 ======= */

	/**
	 * 공통 에러 응답 페이로드(ApiErrorPayload)를 생성하고, 이를 ApiResponse로 감싸 ResponseEntity를 반환한다.
	 * MDC에 설정된 traceId와 요청 경로를 포함하여 에러 추적을 용이하게 한다.
	 */
	private ResponseEntity<ApiResponse<ApiErrorPayload>> build(
		ErrorCode code, String message, Object errors, HttpServletRequest req) {

		//String traceId = safe(MDC.get("traceId"));         // 로깅 필터에서 넣어두면 추적 가능
		String path = req != null ? req.getRequestURI() : null;

		if (message == null)
			code.getMessage();

		// 상위 ApiResponse의 message에는 "표준 에러 코드"를 올려 클라이언트 분기를 단순화
		ApiResponse<ApiErrorPayload> body =
			ApiResponse.of(false, code.getCode(), message, null);

		//return body.toResponseEntity(HttpStatus.valueOf(code.getCode()));
		return ResponseEntity.status(HttpStatus.valueOf(code.getCode())).body(body);
	}

	/**
	 * service에서 잘못입력된 부분을 400번 에러로 처리한다.
	 */
	@ExceptionHandler(CustomValidationException.class)
	public ResponseEntity<ApiResponse<Object>> customValidationException(CustomValidationException ex,
		HttpServletRequest req) {
		log.error("=================================> CustomValidationException.class 에러 로그 찍기", ex); // 예외 로그 찍기

		ApiResponse<Object> error = ApiResponse.failure(
			ErrorCode.BAD_REQUEST.getCode(),
			ex.getMessage()
		);
		return ResponseEntity.badRequest().body(error);
	}

	@ExceptionHandler(ValidationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ResponseEntity<ApiResponse<Object>> handleValidationException(ValidationException ex) {
		List<String> errors = ex.getFieldErrors().stream()
			.map(f -> f.getField() + " : " + f.getDefaultMessage())
			.toList();

		// return Map.of(
		// 	"success", false,
		// 	"data", errors,
		// 	"error", Map.of(
		// 		"code", "400 BAD_REQUEST",
		// 		"message", "잘못된 요청입니다."
		// 	),
		// 	"timestamp", LocalDateTime.now()
		// );

		ApiResponse<Object> error = ApiResponse.failure(
			ErrorCode.BAD_REQUEST.getCode(),
			ex.getMessage(), errors);
		return ResponseEntity.badRequest().body(error);

	}

	/**
	 * service에서 리소스 찾을수 없을때 404 에러 출력
	 */
	@ExceptionHandler({
		NotFoundException.class})
	public ResponseEntity<ApiResponse<Object>> handlerNotFoundException(NotFoundException ex,
		HttpServletRequest req) {
		log.error("=================================> handlerNotFoundException.class 에러 로그 찍기", ex); // 예외 로그 찍기

		ApiResponse<Object> error = ApiResponse.failure(
			ErrorCode.NOT_FOUND.getCode(),
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}
}
