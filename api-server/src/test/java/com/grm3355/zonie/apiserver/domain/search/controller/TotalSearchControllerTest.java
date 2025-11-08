package com.grm3355.zonie.apiserver.domain.search.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.grm3355.zonie.apiserver.domain.festival.controller.FestivalController;
import com.grm3355.zonie.apiserver.global.jwt.JwtAccessDeniedHandler;
import com.grm3355.zonie.apiserver.global.jwt.JwtAuthenticationEntryPoint;
import com.grm3355.zonie.apiserver.global.service.RateLimitingService;
import com.grm3355.zonie.apiserver.domain.search.service.TotalSearchService;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchDto;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchResponse;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
	void testGetFestivalTotalSearch_success() throws Exception {
		// given
		TotalSearchResponse dummyResponse = new TotalSearchResponse(
			null, // festivals
			null  // chatRooms
		);
		Mockito.when(totalSearchService.getTotalSearch(any(TotalSearchDto.class)))
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