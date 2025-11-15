package com.grm3355.zonie.apiserver.domain.location.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationTokenResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

@Service
public class LocationService {

	private final double locationRadiusLimit;
	private final RedisTokenService redisTokenService;
	private final FestivalRepository festivalRepository;

	public LocationService(@Value("${location.radius.limit}") double locationRadiusLimit, RedisTokenService redisTokenService, FestivalRepository festivalRepository) {
		this.locationRadiusLimit = 	locationRadiusLimit;
		this.redisTokenService = redisTokenService;
		this.festivalRepository = festivalRepository;
	}

	/**
	 * @deprecated PostGIS의 ST_Distance 함수를 사용하는 {@link com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository#findDistanceToFestival}
	 * 메서드 사용을 권장합니다. 이 메서드는 Java 기반의 부정확한 하버사인 공식을 사용합니다.
	 */
	@Deprecated
	public static double getDistanceCalculator(LocationDto location1, LocationDto location2) {
		//위도 경도, 거리예산
		double lat1 = location1.getLat();
		double lon1 = location1.getLon();
		double lat2 = location2.getLat();
		double lon2 = location2.getLon();

		double radius = 6371; // 지구 반지름 (km)
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLon / 2) * Math.sin(dLon / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = radius * c; // 단위: km

		// 소수점 첫째 자리까지 반올림
		return Math.round(distance * 100.0) / 100.0;
	}

	public LocationTokenResponse verifyAndGenerateToken(UserDetailsImpl userDetails, long festivalId, LocationDto userLocationDto) {

		String userId = userDetails.getUsername();
		String festivalIdStr = String.valueOf(festivalId);

		// 1. 축제 DB 정보 조회 -> PostGIS 거리 계산
		// Java가 아닌 DB(PostGIS)가 거리를 계산하도록 Native Query 호출
		double radius_km = festivalRepository.findDistanceToFestival(
				festivalId,
				userLocationDto.getLon(), // :lon
				userLocationDto.getLat()  // :lat
			)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관련 축제정보가 없습니다."));

		// 2. 반경 비교 (yml 설정값)
		boolean isInside = radius_km <= locationRadiusLimit;

		// 3. 토큰 발급 (반경 내)
		if (isInside) {
			UserTokenDto tokenInfo = UserTokenDto.builder()
				.userId(userId)
				.lat(userLocationDto.getLat())
				.lon(userLocationDto.getLon())
				.build();
			redisTokenService.generateLocationToken(tokenInfo, festivalIdStr);
			return new LocationTokenResponse(String.format("인증 성공. (%.2fkm / 반경 %.2fkm)", radius_km, locationRadiusLimit));
		} else {
			// 에러 반환 (반경 외)
			throw new BusinessException(ErrorCode.FORBIDDEN,
				String.format("축제 반경(%.2fkm) 외부에 있습니다. (현재 거리: %.2fkm)", locationRadiusLimit, radius_km));
		}
	}

	// Redis에서 토큰 정보 가져오기
	public UserTokenDto getLocationInfo(String userId, String contextId) {
		return redisTokenService.getLocationInfo(userId, contextId);
	}
}