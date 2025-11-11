package com.grm3355.zonie.apiserver.domain.message.controller;

import java.util.Map;

import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.domain.message.dto.MessageResponse;
import com.grm3355.zonie.apiserver.domain.message.service.MessageLikeService;
import com.grm3355.zonie.apiserver.domain.message.service.MessageQueryService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "메시지 기능", description = "메시지 좋아요, 과거 내역 조회 등")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessageController {

	private final MessageLikeService messageLikeService;
	private final MessageQueryService messageQueryService;

	@Operation(summary = "메시지 '좋아요' 토글", description = "메시지 '좋아요'를 누르거나 취소합니다. (위치 인증 필요)")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = {
				@ExampleObject(name = "좋아요 누르기 성공",
					value = "{\"success\":true,\"data\":{\"liked\":true,\"likeCount\":6},\"error\":null,\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"),
				@ExampleObject(name = "좋아요 취소 성공",
					value = "{\"success\":true,\"data\":{\"liked\":false,\"likeCount\":5},\"error\":null,\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}")
			}
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패 (e.g., messageId 형식 오류)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(name = "BAD_REQUEST",
				value = "{\"success\":false,\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}")
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(name = "UNAUTHORIZED",
				value = "{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증에 실패했습니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}")
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "위치 인증 실패 (거리 초과 또는 토큰 없음)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(name = "FORBIDDEN (거리 초과)",
				value = "{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"채팅방 반경 1.0km 이내에서만 '좋아요'를 누를 수 있습니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}")
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "메시지 또는 채팅방을 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(name = "NOT_FOUND",
				value = "{\"success\":false,\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"메시지를 찾을 수 없습니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}")
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 횟수 초과", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(name = "TOO_MANY_REQUESTS",
				value = "{\"success\":false,\"error\":{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"요청 횟수를 초과했습니다.\"},\"timestamp\":\"2025-09-02T10:45:00.123456Z\"}")
		))
	})
	@PostMapping("/messages/{messageId}/like")
	@PreAuthorize("hasAnyRole('GUEST', 'USER')")
	public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(
		@PathVariable String messageId,
		@AuthenticationPrincipal UserDetailsImpl userDetails
	) {
		String userId = userDetails.getUsername();
		Map<String, Object> result = messageLikeService.toggleLike(userId, messageId);
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@Operation(summary = "채팅방 과거 메시지 조회", description = "채팅방의 과거 메시지 목록을 커서 기반 페이지네이션으로 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
	})
	@GetMapping("/chat-rooms/{roomId}/messages")
	@PreAuthorize("hasAnyRole('GUEST', 'USER')")
	public ResponseEntity<ApiResponse<Slice<MessageResponse>>> getMessages(
		@PathVariable String roomId,
		@RequestParam(required = false) String before, // (커서 ID)
		@AuthenticationPrincipal UserDetailsImpl userDetails
	) {
		String userId = userDetails.getUsername();
		Slice<MessageResponse> messages = messageQueryService.getMessages(roomId, userId, before);
		return ResponseEntity.ok(ApiResponse.success(messages));
	}
}
