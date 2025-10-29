/**
 * 멀티 모듈 설정 테스트를 위해 작성된 코드입니다.
 * 개발 시에 무시하고 진행하시면 됩니다.
 */

package com.grm3355.zonie.commonlib;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestEnum {
	SUCCESS("0000", "SUCCESS"),
	UNKNOWN_ERROR("9999", "UNKNOWN_ERROR");

	private String code;
	private String message;
}
