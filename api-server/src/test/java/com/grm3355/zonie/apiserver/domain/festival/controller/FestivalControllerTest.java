package com.grm3355.zonie.apiserver.domain.festival.controller;

import com.grm3355.zonie.apiserver.global.jwt.JwtAccessDeniedHandler;
import com.grm3355.zonie.apiserver.global.jwt.JwtAuthenticationEntryPoint;
import com.grm3355.zonie.apiserver.global.service.RateLimitingService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.service.FestivalService;
import com.grm3355.zonie.apiserver.global.dto.PageResponse;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
	controllers = FestivalController.class,
	excludeAutoConfiguration = {
		DataSourceAutoConfiguration.class,
		JpaRepositoriesAutoConfiguration.class
	}
)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
class FestivalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FestivalService festivalService;

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
	@DisplayName("축제 목록 조회 테스트")
	void testGetFestivalList() throws Exception {
		// given
		FestivalResponse festival = FestivalResponse.builder().build();
		Page<FestivalResponse> pageList = new PageImpl<>(List.of(festival), PageRequest.of(0, 10), 1);
		PageResponse<FestivalResponse> pageResponse = new PageResponse<>(pageList, 10);

		Mockito.when(festivalService.getFestivalList(any(FestivalSearchRequest.class)))
			.thenReturn(pageList);

		// when & then
		mockMvc.perform(get("/api/v1/festivals")
				.param("page", "0")
				.param("pageSize", "10")
				.param("region", "SEOUL")
				.param("status", "ALL")
				.param("keyword", "테스트")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").exists());
	}

	@Test
	@DisplayName("축제 내용 조회 테스트")
	void testGetFestivalContent() throws Exception {
		// given
		FestivalResponse festivalResponse = FestivalResponse.builder().build();
		Mockito.when(festivalService.getFestivalContent(1L))
			.thenReturn(festivalResponse);

		// when & then
		mockMvc.perform(get("/api/v1/festivals/{festivalId}", 1)
				.param("keyword", "테스트")
				.param("order", "DATE_ASC")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").exists());
	}

	@Test
	@DisplayName("지역 목록 조회 테스트")
	void testGetFestivalRegion() throws Exception {
		// given
		List<Region> regions = List.of(Region.SEOUL, Region.JEOLLA);
		Mockito.when(festivalService.getRegionList()).thenReturn(regions);

		// when & then
		mockMvc.perform(get("/api/v1/festivals/region")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0]").value("SEOUL"))
			.andExpect(jsonPath("$.data[1]").value("JEOLLA"));
	}
}