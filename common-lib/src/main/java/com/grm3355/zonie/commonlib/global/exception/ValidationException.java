package com.grm3355.zonie.commonlib.global.exception;

import java.util.List;

import org.springframework.validation.FieldError;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {
	private final List<FieldError> fieldErrors;

	public ValidationException(List<FieldError> fieldErrors) {
		super("Validation failed");
		this.fieldErrors = fieldErrors;
	}
}
