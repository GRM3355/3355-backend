package com.grm3355.zonie.apiserver.global.exception;

import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

public class InternalServerException extends AuthCustomException {
    public InternalServerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InternalServerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
