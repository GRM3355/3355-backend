/**
 * 멀티 모듈 설정 테스트를 위해 작성된 코드입니다.
 * 개발 시에 무시하고 진행하시면 됩니다.
 */

package com.grm3355.zonie.apiserver.domain.test.service;

import org.springframework.stereotype.Service;

import com.grm3355.zonie.commonlib.CommonTestService;
import com.grm3355.zonie.commonlib.TestEnum;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestService {

	private final CommonTestService commonTestService;

	public String save() {
		System.out.println(TestEnum.SUCCESS.getCode());
		System.out.println(commonTestService.commonService());
		return "save";
	}

	public String find() {
		return "find";
	}
}

