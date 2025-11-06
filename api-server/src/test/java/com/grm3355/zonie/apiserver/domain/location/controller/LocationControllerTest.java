package com.grm3355.zonie.apiserver.domain.location.controller;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@DisplayName("위치 갱신 통합테스트")
@SpringBootTest
@AutoConfigureMockMvc
class LocationControllerTest {
/*
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private LocationService locationService;

	@MockitoBean
	private RedisTokenService redisTokenService;


	// Redis 컨테이너 설정
	@Container
	static GenericContainer<?> redisContainer =
		new GenericContainer<>(DockerImageName.parse("redis:7.0.12"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.redis.host", redisContainer::getHost);
		registry.add("spring.redis.port", () -> redisContainer.getFirstMappedPort());
	}

	@Test
	@DisplayName("위치 정보 업데이트")
	@WithMockUser(roles = "GUEST")
	void updateLocationTest() throws Exception {
		LocationDto request = new LocationDto();
		request.setLat(37.5);
		request.setLon(127.0);

		LocationTokenResponse response = new LocationTokenResponse("갱신되었습니다.");

		when(locationService.update(any(LocationDto.class), any())).thenReturn(response);

		mockMvc.perform(put("/api/v1/location/update")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.message").value("갱신되었습니다."));
	}

	@Test
	@DisplayName("축제 영역 확인")
	@WithMockUser(roles = "GUEST")
	void getFestivalVerifyTest() throws Exception {

		FestivalZoneVarifyResponse response = new FestivalZoneVarifyResponse(
			true,           // accessValue
			1.1,            // distance
			1,            // festivalId
			"user:aaaa-bbbb-cccc-dddd" // userId
		);

		when(locationService.getFestivalVerify(any(), anyLong())).thenReturn(response);

		mockMvc.perform(get("/api/v1/location/festivalVerify")
				.param("festivalId", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	@DisplayName("채팅방 영역 확인")
	@WithMockUser(roles = "GUEST")
	void getChatRoomVerifyTest() throws Exception {

		ChatRoomZoneVarifyResponse response = new ChatRoomZoneVarifyResponse(
			true,           // accessValue
			1.1,            // distance
			"room:abcd-asf-asdfasf",            // roomId
			"user:aaaa-bbbb-cccc-dddd" // userId
		);
		when(locationService.getChatroomVerify(any(), any())).thenReturn(response);

		mockMvc.perform(get("/api/v1/location/chatroomVerify")
				.param("chatroomId", "room123"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

	}*/
}
