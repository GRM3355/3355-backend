package com.grm3355.zonie.apiserver.domain.location.service;
import com.grm3355.zonie.apiserver.common.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationTokenResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.location.dto.*;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.*;
import org.mockito.*;
import org.springframework.data.geo.Point;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationServiceTest {

	@Mock
	private RedisTokenService redisTokenService;

	@Mock
	private FestivalRepository festivalRepository;

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@InjectMocks
	private LocationService locationService;

	private GeometryFactory geometryFactory = new GeometryFactory();

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	// =============================================
	// 거리 계산 메서드
	// =============================================
	@Test
	@DisplayName("getDistanceCalculator - 위경도 거리 계산이 정확히 동작한다")
	void testGetDistanceCalculator() {
		// given
		LocationDto loc1 = new LocationDto(37.5665, 126.9780); // 서울
		LocationDto loc2 = new LocationDto(35.1796, 129.0756); // 부산

		// when
		double distance = LocationService.getDistanceCalculator(loc1, loc2);

		// then
		assertThat(distance).isBetween(320.0, 350.0); // 약 325km 예상
	}

	// =============================================
	// update()
	// =============================================
	@Test
	@DisplayName("update() - RedisTokenService가 true 반환 시 성공 메시지 반환")
	void testUpdate_Success() {
		// given
		UserDetailsImpl user = mock(UserDetailsImpl.class);
		when(user.getUsername()).thenReturn("user123");

		LocationDto locationDto = new LocationDto(37.0, 127.0);
		when(redisTokenService.updateLocationInfo(locationDto, "user123")).thenReturn(true);

		// when
		LocationTokenResponse response = locationService.update(locationDto, user);

		// then
		assertThat(response).isEqualTo("갱신되었습니다.");
		verify(redisTokenService, times(1)).updateLocationInfo(locationDto, "user123");
	}

	@Test
	@DisplayName("update() - RedisTokenService가 false 반환 시 실패 메시지 반환")
	void testUpdate_Failure() {
		UserDetailsImpl user = mock(UserDetailsImpl.class);
		when(user.getUsername()).thenReturn("user123");

		LocationDto locationDto = new LocationDto(37.0, 127.0);
		when(redisTokenService.updateLocationInfo(locationDto, "user123")).thenReturn(false);

		LocationTokenResponse response = locationService.update(locationDto, user);

		assertThat(response).isEqualTo("갱신에 실패하였습니다.");
	}

	// =============================================
	// getFestivalVerify()
	// =============================================
	@Test
	@DisplayName("getFestivalVerify() - 반경 1km 이내면 accessValue=true")
	void testGetFestivalVerify_SuccessWithinRange() {
		// given
		UserDetailsImpl user = mock(UserDetailsImpl.class);
		when(user.getUsername()).thenReturn("user1");

		// Redis mock 데이터
		UserTokenDto userToken = UserTokenDto.builder().userId("user1").lat(37.5665).lon(126.9780).build();
		when(redisTokenService.getLocationInfo("user1")).thenReturn(userToken);

		// Festival mock (위치 거의 동일)
		Point point = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));
		Festival festival = new Festival();
		festival.setPosition(point);
		when(festivalRepository.findByFestivalId(1L)).thenReturn(Optional.of(festival));

		// when
		var response = locationService.getFestivalVerify(user, 1L);

		// then
		assertThat(response.isAccessValue()).isTrue();
		assertThat(response.getRadiusKm()).isLessThan(0.1);
		verify(festivalRepository, times(1)).findByFestivalId(1L);
	}

	@Test
	@DisplayName("getFestivalVerify() - 반경 1km 초과면 accessValue=false")
	void testGetFestivalVerify_OutOfRange() {
		UserDetailsImpl user = mock(UserDetailsImpl.class);
		when(user.getUsername()).thenReturn("user2");

		UserTokenDto userToken = new UserTokenDto("user2", 37.5665, 126.9780);
		when(redisTokenService.getLocationInfo("user2")).thenReturn(userToken);

		Point point = geometryFactory.createPoint(new Coordinate(126.9, 37.5)); // 약 8km 차이
		Festival festival = new Festival();
		festival.setPosition(point);
		when(festivalRepository.findByFestivalId(2L)).thenReturn(Optional.of(festival));

		var response = locationService.getFestivalVerify(user, 2L);

		assertThat(response.isAccessValue()).isFalse();
	}

	@Test
	@DisplayName("getFestivalVerify() - Festival이 존재하지 않으면 예외 발생")
	void testGetFestivalVerify_NotFound() {
		UserDetailsImpl user = mock(UserDetailsImpl.class);
		when(user.getUsername()).thenReturn("user3");
		when(redisTokenService.getLocationInfo("user3"))
			.thenReturn(new UserTokenDto("user3", 37.0, 127.0));
		when(festivalRepository.findByFestivalId(99L))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> locationService.getFestivalVerify(user, 99L))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("관련 축제정보가 없습니다.");
	}

	// =============================================
	// getChatroomVerify()
	// =============================================
	@Test
	@DisplayName("getChatroomVerify() - 반경 1km 이내면 accessValue=true")
	void testGetChatroomVerify_Success() {
		UserDetailsImpl user = mock(UserDetailsImpl.class);
		when(user.getUsername()).thenReturn("user4");
		when(redisTokenService.getLocationInfo("user4"))
			.thenReturn(new UserTokenDto("user4", 37.5665, 126.9780));

		ChatRoom chatRoom = new ChatRoom();
		chatRoom.setPosition(geometryFactory.createPoint(new Coordinate(126.9781, 37.5666)));
		when(chatRoomRepository.findByChatRoomId("room123")).thenReturn(Optional.of(chatRoom));

		var response = locationService.getChatroomVerify(user, "room123");

		assertThat(response.isAccessValue()).isTrue();
	}

	@Test
	@DisplayName("getChatroomVerify() - ChatRoom이 존재하지 않으면 예외 발생")
	void testGetChatroomVerify_NotFound() {
		UserDetailsImpl user = mock(UserDetailsImpl.class);
		when(user.getUsername()).thenReturn("user5");
		when(redisTokenService.getLocationInfo("user5"))
			.thenReturn(new UserTokenDto("user5", 37.0, 127.0));
		when(chatRoomRepository.findByChatRoomId("none"))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> locationService.getChatroomVerify(user, "none"))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("관련 정보가 없습니다.");
	}
}
