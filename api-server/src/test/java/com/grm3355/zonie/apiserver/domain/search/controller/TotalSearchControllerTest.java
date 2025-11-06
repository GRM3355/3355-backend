package com.grm3355.zonie.apiserver.domain.search.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.grm3355.zonie.apiserver.common.service.RateLimitingService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchDto;
import com.grm3355.zonie.apiserver.domain.festival.service.TotalSearchService;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TotalSearchController.class)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
class TotalSearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TotalSearchService totalSearchService;

	@MockitoBean
	private RateLimitingService rateLimitingService;  // Mock으로 등록

	@Test
	@DisplayName("통합검색 GET 요청 성공 테스트")
	void testGetFestivalTotalSearch_success() throws Exception {
		// given
		TotalSearchResponse dummyResponse = new TotalSearchResponse(
			null, // festivals
			null  // chatRooms
		);
		Mockito.when(totalSearchService.getTotalSearch(any(FestivalSearchDto.class)))
			.thenReturn(dummyResponse);

		// when & then
		mockMvc.perform(get("/api/v1/search")
				.param("keyword", "테스트키워드")
				.param("region", "SEOUL")  // 필요 시 enum 값
				.param("status", "ALL")
				.param("order", "DATE_ASC")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").exists());
	}
}