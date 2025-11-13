package com.grm3355.zonie.apiserver.global.exception;

import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

public class BadRequestException extends AuthCustomException {

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }
}
