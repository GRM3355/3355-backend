package com.grm3355.zonie.commonlib.global.exception;

import java.util.Map;

public class NotFoundException extends RuntimeException {
	//public HandlerNotFoundException(String message) {
	//	super(message);
	//}

	private final CustomErrorCode errorCode;
	private final Map<String, Object> details;

	public NotFoundException(CustomErrorCode errorCode) {
		super(errorCode.defaultMessage());
		this.errorCode = errorCode;
		this.details = null;
	}

	public NotFoundException(CustomErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.details = null;
	}

	public NotFoundException(CustomErrorCode errorCode, String message, Map<String, Object> details) {
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
