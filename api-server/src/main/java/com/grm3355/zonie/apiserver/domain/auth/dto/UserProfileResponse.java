package com.grm3355.zonie.apiserver.domain.auth.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileResponse(
	@Schema(description = "아이디", example = "12322@kakao")
	String userId,

	@Schema(description = "닉네임", example = "홍길동")
	String profileNickName,

	@Schema(description = "email", example = "hong@naver.com")
	String accountEmail,

	@Schema(description = "profileImage", example = "http://k.kakaocdn.net/img_110x110.jpg")
	String profileImage,

	@Schema(description = "등록일", example = "2025-11-01 00:00:00")
	LocalDateTime createdAt
) {
}