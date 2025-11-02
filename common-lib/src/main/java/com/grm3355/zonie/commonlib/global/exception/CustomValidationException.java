package com.grm3355.zonie.commonlib.global.exception;

import java.util.Map;

public class CustomValidationException extends RuntimeException {

	//public CustomValidationException(String message) {
	//	super(message);
	//}

	private final CustomErrorCode errorCode;
	private final Map<String, Object> details;

	public CustomValidationException(CustomErrorCode errorCode) {
		super(errorCode.defaultMessage());
		this.errorCode = errorCode;
		this.details = null;
	}

	public CustomValidationException(CustomErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.details = null;
	}

	public CustomValidationException(CustomErrorCode errorCode, String message, Map<String, Object> details) {
		super(message);
		this.errorCode = errorCode;
		this.details = details;
	}

	public CustomErrorCode errorCode() {
		return errorCode;
	}

	public Map<String, Object> details() {
		return details;
	}

}
