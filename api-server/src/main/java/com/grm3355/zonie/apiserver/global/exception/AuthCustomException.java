package com.grm3355.zonie.apiserver.global.exception;

import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import org.springframework.core.NestedRuntimeException;

public abstract class AuthCustomException extends NestedRuntimeException {

    private final ErrorCode errorCode;

    protected AuthCustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected AuthCustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    protected AuthCustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
