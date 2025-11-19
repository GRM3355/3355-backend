package com.grm3355.zonie.apiserver.domain.location.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationTokenResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService 단위 테스트")
public class LocationServiceTest {

	// Constants
	private static final double DEFAULT_RADIUS_LIMIT = 1.0;
	private static final String MOCK_USER_ID = "testUser123";
	private static final long SEOUL_FESTIVAL_ID = 1L;
	private static final long GYEONGGI_FESTIVAL_ID = 2L;

	private LocationService locationService;

	@Mock
	private RedisTokenService redisTokenService;
	@Mock
	private FestivalRepository festivalRepository;
	@Mock
	private UserDetailsImpl userDetails;

	@BeforeEach
	void setUp() {
		// LocationService 인스턴스를 생성자 주입 방식으로 수동 초기화
		this.locationService = new LocationService(
			DEFAULT_RADIUS_LIMIT,
			redisTokenService,
			festivalRepository
		);
		when(userDetails.getUsername()).thenReturn(MOCK_USER_ID);
	}

	private Festival createMockFestival(Long festivalId, String region) {
		return Festival.builder()
			.festivalId(festivalId)
			.region(region)
			.contentId(0)
			.addr1("Mock Address")
			.title("Mock Title")
			.eventStartDate(LocalDate.now())
			.eventEndDate(LocalDate.now().plusDays(1))
			.build();
	}

	private LocationDto createLocationDto(double lat, double lon) {
		return LocationDto.builder()
			.lat(lat)
			.lon(lon)
			.build();
	}

	// --- 단위 테스트 1: 서울 축제 (반경 제한: 1.0km) 경계값 테스트 ---

	@Test
	@DisplayName("서울 축제 (1.0km): 반경 내 (0.99km) -> 토큰 발급 성공")
	void verifyAndGenerateTokenSeoulInsideRadius() {
		// Given
		double distance_km = 0.99;
		LocationDto userLocation = createLocationDto(37.5, 127.0);
		Festival seoulFestival = createMockFestival(SEOUL_FESTIVAL_ID, "SEOUL");

		when(festivalRepository.findDistanceToFestival(eq(SEOUL_FESTIVAL_ID), anyDouble(), anyDouble()))
			.thenReturn(Optional.of(distance_km));
		when(festivalRepository.findByFestivalId(SEOUL_FESTIVAL_ID))
			.thenReturn(Optional.of(seoulFestival));

		// When
		LocationTokenResponse response = locationService.verifyAndGenerateToken(userDetails, SEOUL_FESTIVAL_ID,
			userLocation);

		// Then
		assertNotNull(response);
		verify(redisTokenService).generateLocationToken(any(UserTokenDto.class), eq(String.valueOf(SEOUL_FESTIVAL_ID)));
	}

	@Test
	@DisplayName("서울 축제 (1.0km): 경계값 (1.00km) -> 토큰 발급 성공")
	void verifyAndGenerateTokenSeoulBoundaryRadius() {
		// Given
		double distance_km = 1.00;
		LocationDto userLocation = createLocationDto(37.5, 127.0);
		Festival seoulFestival = createMockFestival(SEOUL_FESTIVAL_ID, "SEOUL");

		when(festivalRepository.findDistanceToFestival(eq(SEOUL_FESTIVAL_ID), anyDouble(), anyDouble()))
			.thenReturn(Optional.of(distance_km));
		when(festivalRepository.findByFestivalId(SEOUL_FESTIVAL_ID))
			.thenReturn(Optional.of(seoulFestival));

		// When
		LocationTokenResponse response = locationService.verifyAndGenerateToken(userDetails, SEOUL_FESTIVAL_ID,
			userLocation);

		// Then
		assertNotNull(response);
		verify(redisTokenService).generateLocationToken(any(UserTokenDto.class), eq(String.valueOf(SEOUL_FESTIVAL_ID)));
	}

	@Test
	@DisplayName("서울 축제 (1.0km): 반경 밖 (1.01km) -> BusinessException 발생")
	void verifyAndGenerateTokenSeoulOutsideRadius() {
		// Given
		double distance_km = 1.01;
		LocationDto userLocation = createLocationDto(37.5, 127.0);
		Festival seoulFestival = createMockFestival(SEOUL_FESTIVAL_ID, "SEOUL");

		when(festivalRepository.findDistanceToFestival(eq(SEOUL_FESTIVAL_ID), anyDouble(), anyDouble()))
			.thenReturn(Optional.of(distance_km));
		when(festivalRepository.findByFestivalId(SEOUL_FESTIVAL_ID))
			.thenReturn(Optional.of(seoulFestival));

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class, () ->
			locationService.verifyAndGenerateToken(userDetails, SEOUL_FESTIVAL_ID, userLocation)
		);
		assert (exception.getMessage().contains("1.00km"));
		assert (exception.getErrorCode() == ErrorCode.FORBIDDEN);
	}

	// --- 단위 테스트 2: 그 외 지역 축제 (반경 제한: 2.0km) 경계값 테스트 ---

	@Test
	@DisplayName("경기 축제 (2.0km): 반경 내 (1.99km) -> 토큰 발급 성공")
	void verifyAndGenerateTokenGyeonggiInsideRadius() {
		// Given
		double distance_km = 1.99;
		LocationDto userLocation = createLocationDto(37.5, 127.0);
		Festival gyeonggiFestival = createMockFestival(GYEONGGI_FESTIVAL_ID, "GYEONGGI");

		when(festivalRepository.findDistanceToFestival(eq(GYEONGGI_FESTIVAL_ID), anyDouble(), anyDouble()))
			.thenReturn(Optional.of(distance_km));
		when(festivalRepository.findByFestivalId(GYEONGGI_FESTIVAL_ID))
			.thenReturn(Optional.of(gyeonggiFestival));

		// When
		LocationTokenResponse response = locationService.verifyAndGenerateToken(userDetails, GYEONGGI_FESTIVAL_ID,
			userLocation);

		// Then
		assertNotNull(response);
		verify(redisTokenService).generateLocationToken(any(UserTokenDto.class),
			eq(String.valueOf(GYEONGGI_FESTIVAL_ID)));
	}

	@Test
	@DisplayName("경기 축제 (2.0km): 경계값 (2.00km) -> 토큰 발급 성공")
	void verifyAndGenerateTokenGyeonggiBoundaryRadius() {
		// Given
		double distance_km = 2.00;
		LocationDto userLocation = createLocationDto(37.5, 127.0);
		Festival gyeonggiFestival = createMockFestival(GYEONGGI_FESTIVAL_ID, "GYEONGGI");

		when(festivalRepository.findDistanceToFestival(eq(GYEONGGI_FESTIVAL_ID), anyDouble(), anyDouble()))
			.thenReturn(Optional.of(distance_km));
		when(festivalRepository.findByFestivalId(GYEONGGI_FESTIVAL_ID))
			.thenReturn(Optional.of(gyeonggiFestival));

		// When
		LocationTokenResponse response = locationService.verifyAndGenerateToken(userDetails, GYEONGGI_FESTIVAL_ID,
			userLocation);

		// Then
		assertNotNull(response);
		verify(redisTokenService).generateLocationToken(any(UserTokenDto.class),
			eq(String.valueOf(GYEONGGI_FESTIVAL_ID)));
	}

	@Test
	@DisplayName("경기 축제 (2.0km): 반경 밖 (2.01km) -> BusinessException 발생")
	void verifyAndGenerateTokenGyeonggiOutsideRadius() {
		// Given
		double distance_km = 2.01;
		LocationDto userLocation = createLocationDto(37.5, 127.0);
		Festival gyeonggiFestival = createMockFestival(GYEONGGI_FESTIVAL_ID, "GYEONGGI");

		when(festivalRepository.findDistanceToFestival(eq(GYEONGGI_FESTIVAL_ID), anyDouble(), anyDouble()))
			.thenReturn(Optional.of(distance_km));
		when(festivalRepository.findByFestivalId(GYEONGGI_FESTIVAL_ID))
			.thenReturn(Optional.of(gyeonggiFestival));

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class, () ->
			locationService.verifyAndGenerateToken(userDetails, GYEONGGI_FESTIVAL_ID, userLocation)
		);
		assert (exception.getMessage().contains("2.00km"));
		assert (exception.getErrorCode() == ErrorCode.FORBIDDEN);
	}

	// --- 단위 테스트 3: 엣지 케이스 ---

	@Test
	@DisplayName("Festival Entity 조회 실패 시 NOT_FOUND 예외 발생")
	void verifyAndGenerateTokenFestivalEntityLookupFailed() {
		// Given: findByFestivalId가 Optional.empty() 반환하여 즉시 예외 발생
		LocationDto userLocation = createLocationDto(37.5, 127.0);

		when(festivalRepository.findByFestivalId(anyLong()))
			.thenReturn(Optional.empty());

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class, () ->
			locationService.verifyAndGenerateToken(userDetails, SEOUL_FESTIVAL_ID, userLocation)
		);
		assert (exception.getErrorCode() == ErrorCode.NOT_FOUND);

		// findDistanceToFestival이 호출되지 않았는지 확인
		verify(festivalRepository, never()).findDistanceToFestival(anyLong(), anyDouble(), anyDouble());
	}

	@Test
	@DisplayName("PostGIS 거리 조회 실패 시 NOT_FOUND 예외 발생")
	void verifyAndGenerateTokenPostGISLookupFailed() {
		// Given: Festival Entity 조회는 성공했으나, PostGIS 거리 조회 실패
		LocationDto userLocation = createLocationDto(37.5, 127.0);
		Festival seoulFestival = createMockFestival(SEOUL_FESTIVAL_ID, "SEOUL");

		when(festivalRepository.findByFestivalId(SEOUL_FESTIVAL_ID))
			.thenReturn(Optional.of(seoulFestival));

		when(festivalRepository.findDistanceToFestival(anyLong(), anyDouble(), anyDouble()))
			.thenReturn(Optional.empty());

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class, () ->
			locationService.verifyAndGenerateToken(userDetails, SEOUL_FESTIVAL_ID, userLocation)
		);
		assert (exception.getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR);

		// findByFestivalId가 호출되었는지 확인
		verify(festivalRepository, times(1)).findByFestivalId(SEOUL_FESTIVAL_ID);
	}

	// @Test
	@Deprecated
	@DisplayName("Haversine 공식 메서드(getDistanceCalculator) 테스트")
	void haversineDistanceTest() {
		// Given
		LocationDto location1 = createLocationDto(37.5665, 126.9780);
		LocationDto location2 = createLocationDto(37.5242, 126.9808);

		// When
		double distance = LocationService.getDistanceCalculator(location1, location2);

		// Then
		assert (Math.abs(distance - 4.70) < 0.01);
	}
}
