package com.grm3355.zonie.apiserver.domain.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.grm3355.zonie.apiserver.domain.auth.dto.UserProfileResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserQuitResponse;
import com.grm3355.zonie.apiserver.domain.auth.util.CookieProperties;
import com.grm3355.zonie.apiserver.domain.user.dto.EmailUpdateRequest;
import com.grm3355.zonie.apiserver.domain.user.service.UserService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "User", description = "사용자 프로필 페이지")
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;
	private final CookieProperties cookieProperties;

	public UserController(UserService userService, CookieProperties cookieProperties) {
        this.userService = userService;
		this.cookieProperties = cookieProperties;
	}

	//현재는 사용안함.
	@Deprecated
	@Hidden
	@Operation(summary = "내 프로필 이메일 패치", description = "현재 로그인된 사용자의 이메일 정보를 수정합니다.")
	@PatchMapping("/update/email")
    public void updateEmail(@AuthenticationPrincipal UserDetailsImpl userDetails,
                            @RequestBody EmailUpdateRequest request) {
        userService.updateEmail(userDetails.getUserId(), request);
    }

	@Operation(summary = "내 프로필 조회", description = "현재 아이디, 닉네임, Email, profileImage를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "리프레시 토큰 재발급",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = UserProfileResponse.class)
			)
		)
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		UserProfileResponse userProfile = userService.getUserProfile(userDetails.getUserId());
		return ResponseEntity.ok().body(ApiResponse.success(userProfile));
	}

	@Operation(summary = "회원탈퇴", description = "현재 가입한 회원의 정보를 삭제처리합니다. 클라이언트 측에서도 저장된 액세스토큰, 리프레시 토큰을 모두 삭제해야 합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "로그아웃 성공",
			content = @Content(mediaType = "application/json"
			))
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/me/quit")
	public ResponseEntity<ApiResponse<Void>> quit(@Valid @RequestBody UserQuitResponse request,
		HttpServletResponse response,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		//사용자정보, Redis 정보 삭제
		userService.quit(userDetails.getUserId(), request);

		//리프레시 토큰 값 제git
		ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.secure(cookieProperties.isSecure()) // 로컬 환경
			.path("/")
			.maxAge(cookieProperties.getMaxAge())
			.sameSite(cookieProperties.getSameSite())
			.domain(cookieProperties.getDomain())
			.build();
		response.addHeader("Set-Cookie", cookie.toString());

		return ResponseEntity.noContent().build();
	}

}
