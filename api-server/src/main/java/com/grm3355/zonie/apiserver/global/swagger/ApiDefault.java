package com.grm3355.zonie.apiserver.global.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * 공통 Swagger Error Response (400, 405, 415, 429)를 묶어주는 커스텀 어노테이션 (+ 204까지)
 * : SwaggerConfig에 등록한 Components를 $ref로 참조합니다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
@ApiResponse(responseCode = "405", ref = "#/components/responses/MethodNotAllowed")
@ApiResponse(responseCode = "415", ref = "#/components/responses/UnsupportedMediaType")
@ApiResponse(responseCode = "429", ref = "#/components/responses/TooManyRequests")
@ApiResponse(responseCode = "204", ref = "#/components/responses/NoContent")
public @interface ApiDefault {
}
