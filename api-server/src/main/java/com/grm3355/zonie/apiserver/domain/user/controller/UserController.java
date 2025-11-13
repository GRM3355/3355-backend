package com.grm3355.zonie.apiserver.domain.user.controller;

import jakarta.validation.Valid;

import com.grm3355.zonie.apiserver.domain.auth.dto.UserProfileResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserQuitResponse;
import com.grm3355.zonie.apiserver.domain.user.dto.EmailUpdateRequest;
import com.grm3355.zonie.apiserver.domain.user.dto.PhoneNumberUpdateRequest;
import com.grm3355.zonie.apiserver.domain.user.service.UserService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;
	public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/update/email")
    public void updateEmail(@AuthenticationPrincipal UserDetailsImpl userDetails,
                            @RequestBody EmailUpdateRequest request) {
        userService.updateEmail(userDetails.getId(), request);
    }

	@Operation(summary = "내 프로필 조회", description = "현재 로그인된 사용자의 프로필(이메일) 및 주소 정보를 함께 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\":true,\"message\":\"OK\",\"data\":{\"userId\":\"아이디\",\"profileNickName\":\"닉네임\",\"accountEmail\":\"이메일\",\"createdAt\":\"등록일\"}},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		)
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		UserProfileResponse userProfile = userService.getUserProfile(userDetails.getId());
		return ResponseEntity.ok().body(ApiResponse.success(userProfile));
	}

	@Operation(summary = "회원탈퇴", description = "현재 로그인된 사용자의 비밀번호를 변경합니다. 변경 후 모든 기기에서 로그아웃됩니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "회원탈퇴 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NO_CONTENT",
					value = "{\"success\":true,\"status\":204,\"message\":\"NO_CONTENT\",\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		)
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/me/quit")
	public ResponseEntity<ApiResponse<Void>> quit(@Valid @RequestBody UserQuitResponse request,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		userService.quit(userDetails.getId(), request);
		return ResponseEntity.noContent().build();
	}

    //todo 휴대전화 컬럼필요
//    @PatchMapping("/update/phoneNumber")
//    public void updateEmail(@AuthenticationPrincipal UserDetailsImpl userDetails,
//                            @RequestBody PhoneNumberUpdateRequest request) {
//        userService.updatePhoneNumber(userDetails.getId(), request);
//    }

}
