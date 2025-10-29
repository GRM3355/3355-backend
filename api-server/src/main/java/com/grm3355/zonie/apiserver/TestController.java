/**
 * 멀티 모듈 설정 테스트를 위해 작성된 코드입니다.
 * 개발 시에 무시하고 진행하시면 됩니다.
 */

package com.grm3355.zonie.apiserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TestController {
	private final TestService testService;

	@GetMapping("/save")
	public String save() {
		return testService.save();
	}

	@GetMapping("/find")
	public String find() {
		return testService.find();
	}
}
