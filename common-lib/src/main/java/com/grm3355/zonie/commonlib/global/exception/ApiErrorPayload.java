package com.grm3355.zonie.commonlib.global.exception;

public record ApiErrorPayload(
	//String code,      // 표준 에러 코드 (e.g., VALIDATION_FAILED)
	String message,   // 사람친화 메시지
	String path,      // 요청 경로
	Object errors     // 필드 오류/추가 컨텍스트 (Map or List))
) {
}
