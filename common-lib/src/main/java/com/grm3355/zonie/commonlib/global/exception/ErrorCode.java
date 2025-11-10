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
    BAD_REQUEST("BAD_REQUEST", "잘못된 요청입니다."),
    INVALID_INPUT("INVALID_INPUT", "잘못된 입력입니다."),
    INVALID_INPUT_VALUE("INVALID_INPUT_VALUE", "입력값이 유효하지 않습니다."),
    INVALID_REQUEST_PARAM("INVALID_REQUEST_PARAM", "요청 파라미터가 잘못되었습니다."),
    PAYLOAD_MALFORMED("PAYLOAD_MALFORMED", "잘못된 요청 페이로드입니다."),
    VALIDATION_FAILED("VALIDATION_FAILED", "검증 실패"),

    // Auth Errors (401)
    UNAUTHORIZED("UNAUTHORIZED", "인증되지 않은 사용자입니다."),
    OAUTH2_INVALID_CODE("UNAUTHORIZED", "잘못된 인가 코드 입니다."),
    OAUTH2_NOT_SUPPORTED_PROVIDER_TYPE("UNAUTHORIZED", "지원하지 않는 소셜 로그인 타입입니다."),
    DUPLICATE_SOCIAL_TYPE("UNAUTHORIZED", "중복되는 타입입니다."),

    //403
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다."),

    //403
    ACCESS_DENIED("ACCESS_DENIED", "접근 권한이 없습니다."),

    // Resource Errors (404)
    NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),

    URL_NOT_FOUND("NOT_FOUND_URL", "요청한 URL을 찾을 수 없습니다."),

    //405
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "요청한 HTTP 메서드가 허용되지 않음"),

    //415
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", "잘못된 콘텐츠 타입입니다."),

    // Rate Limiting (429)
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", "요청 횟수를 초과했습니다."),

    //409
    DATA_INTEGRITY_VIOLATION("CONFLICT", "데이터 제약조건 위반"),

    CONFLICT("CONFLICT", "데이터 충돌입니다."),

    // Server Errors (500)
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;
}
