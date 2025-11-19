package com.grm3355.zonie.apiserver.domain.search.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchDto;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchResponse;
import com.grm3355.zonie.apiserver.domain.search.service.TotalSearchService;
import com.grm3355.zonie.apiserver.global.jwt.JwtAccessDeniedHandler;
import com.grm3355.zonie.apiserver.global.jwt.JwtAuthenticationEntryPoint;
import com.grm3355.zonie.apiserver.global.service.RateLimitingService;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

@WebMvcTest(
	controllers = TotalSearchController.class,
	excludeAutoConfiguration = {
		DataSourceAutoConfiguration.class,
		JpaRepositoriesAutoConfiguration.class
	}
)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
class TotalSearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TotalSearchService totalSearchService;

	@MockitoBean
	private RateLimitingService rateLimitingService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@MockitoBean
	private JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@Test
	@DisplayName("통합검색 GET 요청 성공 테스트")
	void testGetTotalSearchSuccess() throws Exception {
		// given
		TotalSearchResponse dummyResponse = new TotalSearchResponse(null, null);
		Mockito.when(totalSearchService.getTotalSearch(any(TotalSearchDto.class)))
			.thenReturn(dummyResponse);

		// when & then
		mockMvc.perform(get("/api/v1/search")
				.param("keyword", "테스트키워드")
				.param("region", "SEOUL")
				.param("status", "ALL")
				.param("order", "DATE_ASC")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").exists());
	}

	@Test
	@DisplayName("검색 - 채팅방 목록 GET 요청 성공 테스트")
	void testGetChatroomTotalSearchSuccess() throws Exception {
		// given
		// 서비스는 Page<T>를 반환합니다.
		ChatRoomResponse chatRoom = ChatRoomResponse.builder().chatRoomId("1L").title("테스트방").build();
		Page<ChatRoomResponse> mockPage = new PageImpl<>(List.of(chatRoom), PageRequest.of(0, 10), 1);

		Mockito.when(totalSearchService.getChatroomTotalSearch(any(ChatRoomSearchRequest.class)))
			.thenReturn(mockPage);

		// when & then
		mockMvc.perform(get("/api/v1/search/chat-rooms")
				.param("keyword", "테스트")
				.param("page", "1") // 1-based
				.param("pageSize", "10")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			// 컨트롤러가 MyChatRoomPageResponse로 변환했는지 검증
			.andExpect(jsonPath("$.data.content[0].chatRoomId").value("1L"))
			.andExpect(jsonPath("$.data.content[0].title").value("테스트방"))
			.andExpect(jsonPath("$.data.totalElements").value(1));
	}
}
