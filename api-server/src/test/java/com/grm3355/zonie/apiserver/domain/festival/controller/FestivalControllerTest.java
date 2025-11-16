package com.grm3355.zonie.apiserver.domain.festival.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

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

import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalDetailResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.service.FestivalService;
import com.grm3355.zonie.apiserver.global.jwt.JwtAccessDeniedHandler;
import com.grm3355.zonie.apiserver.global.jwt.JwtAuthenticationEntryPoint;
import com.grm3355.zonie.apiserver.global.service.RateLimitingService;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

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

		Mockito.when(festivalService.getFestivalList(any(FestivalSearchRequest.class))) // 서비스에서 Page<T> 반환 (컨트롤러가 FestivalPageResponse로 변환)
			.thenReturn(pageList);

		// when & then
		mockMvc.perform(get("/api/v1/festivals")
				.param("page", "1")
				.param("pageSize", "10")
				.param("region", "SEOUL")
				.param("status", "ALL")
				.param("keyword", "테스트")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content[0]").exists())
			.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	@DisplayName("축제 내용 조회 테스트")
	void testGetFestivalContent() throws Exception {
		// given
		FestivalDetailResponse festivalResponse = FestivalDetailResponse.builder().build();
		Mockito.when(festivalService.getFestivalContent(1L))
			.thenReturn(festivalResponse);

		// when & then
		mockMvc.perform(get("/api/v1/festivals/{festivalId}", 1)
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
		List<Map<String, String>> serviceResponse = List.of(
			Map.of("region", "서울", "code", "SEOUL"),
			Map.of("region", "전라", "code", "JEOLLA")
		);

		Mockito.when(festivalService.getRegionList())
			.thenReturn(serviceResponse);

		// when & then
		mockMvc.perform(get("/api/v1/festivals/regions")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0].region").value("서울"))
			.andExpect(jsonPath("$.data[0].code").value("SEOUL"))
			.andExpect(jsonPath("$.data[1].region").value("전라"))
			.andExpect(jsonPath("$.data[1].code").value("JEOLLA"));
	}

	@Test
	@DisplayName("지역별 축제 개수 조회 테스트")
	void testGetFestivalCount() throws Exception {
		// given
		Mockito.when(festivalService.getFestivalCountByRegion(eq(Region.SEOUL)))
			.thenReturn(15L); // "서울" 지역 축제 15개

		// when & then
		mockMvc.perform(get("/api/v1/festivals/count")
				.param("region", "SEOUL") // Region Enum 이름과 일치
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.count").value(15)); // FestivalCountResponse DTO 검증
	}

	@Test
	@DisplayName("축제 목록 조회 - 유효성 검사 실패 (잘못된 위도)")
	void testGetFestivalListInvalidLat() throws Exception {
		// given
		// service.getFestivalList()는 어차피 호출되지 않아야 함

		// when & then
		mockMvc.perform(get("/api/v1/festivals")
				.param("ps", "true")
				.param("lat", "200.0") // @Max(90) 위반
				.param("lon", "127.0")
				.param("radius", "1.0")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest()); // HTTP 400
	}

	@Test
	@DisplayName("축제 목록 조회 - 위치 기반 검색 성공")
	void testGetFestivalListLocationBasedSuccess() throws Exception {
		// given
		Page<FestivalResponse> pageList = new PageImpl<>(List.of(FestivalResponse.builder().build()));
		Mockito.when(festivalService.getFestivalList(any(FestivalSearchRequest.class)))
			.thenReturn(pageList);

		// when & then
		mockMvc.perform(get("/api/v1/festivals")
				.param("ps", "true") // 위치 기반 검색 활성화
				.param("lat", "37.5")
				.param("lon", "127.0")
				.param("radius", "1.0")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk()) // HTTP 200
			.andExpect(jsonPath("$.success").value(true));
	}
}
