package com.grm3355.zonie.commonlib.global.enums;

import jakarta.persistence.Column;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProviderType {
	KAKAO("KAKAO", "카카오"),
	GOOGLE("GOOGLE", "구글"),
	NAVER("NAVER", "네이버"),
	APPLE("APPLE", "애플");

	private final String code;        // 영문 코드
	private final String description; // 한글 이름

	// 코드로 enum 찾기
	public static ProviderType fromCode(String code) {
		for (ProviderType type : ProviderType.values()) {
			if (type.code.equalsIgnoreCase(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown provider code: " + code);
	}
}

