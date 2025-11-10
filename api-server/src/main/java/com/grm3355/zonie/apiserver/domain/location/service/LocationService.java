package com.grm3355.zonie.apiserver.domain.location.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationTokenResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
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

	//위도 경도, 거리예산
	public static double getDistanceCalculator(LocationDto location1, LocationDto location2) {
		double lat1 = location1.getLat();
		double lon1 = location1.getLon();
		double lat2 = location2.getLat();
		double lon2 = location2.getLon();

		double R = 6371; // 지구 반지름 (km)
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLon / 2) * Math.sin(dLon / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c; // 단위: km

		// 소수점 첫째 자리까지 반올림
		return Math.round(distance * 100.0) / 100.0;
	}

	@Transactional
	public LocationTokenResponse update(LocationDto locationDto, UserDetailsImpl userDetails, String festivalId) {
		String userId = userDetails.getUsername();
		boolean value = redisTokenService.updateLocationInfo(locationDto, userId, festivalId);
		String message = value ? "갱신되었습니다." : "갱신에 실패하였습니다.";
		return new LocationTokenResponse(message);
	}

	// [신설] 이 메서드가 인증/발급을 모두 처리
	public LocationTokenResponse verifyAndGenerateToken(UserDetailsImpl userDetails, long festivalId, LocationDto userLocationDto) {

		String userId = userDetails.getUsername();
		String festivalIdStr = String.valueOf(festivalId);

		// 1. 축제 DB 정보 조회
		Festival festival = festivalRepository.findByFestivalId(festivalId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관련 축제정보가 없습니다."));

		LocationDto festivalPosition = LocationDto.builder()
			.lat(festival.getPosition().getY())
			.lon(festival.getPosition().getX()).build();

		// 2. 거리 계산
		double radius_km = getDistanceCalculator(userLocationDto, festivalPosition);
		boolean isInside = radius_km <= locationRadiusLimit;

		// 3. 토큰 발급
		if (isInside) {
			// (반경 내) 토큰 발급
			UserTokenDto tokenInfo = UserTokenDto.builder()
				.userId(userId)
				.lat(userLocationDto.getLat())
				.lon(userLocationDto.getLon())
				// .clientIp( ... ) // (필요 시 Request에서 추출)
				// .device( ... ) // (필요 시 Request에서 추출)
				.build();

			redisTokenService.generateLocationToken(tokenInfo, festivalIdStr);
			return new LocationTokenResponse(String.format("인증 성공. (%.2fkm / 반경 %.2fkm)", radius_km, locationRadiusLimit));
		} else {
			// (반경 외) 에러 반환
			throw new BusinessException(ErrorCode.FORBIDDEN,
				String.format("축제 반경(%.2fkm) 외부에 있습니다. (현재 거리: %.2fkm)", locationRadiusLimit, radius_km));
		}
	}

	// [삭제] public FestivalZoneVarifyResponse getFestivalVerify(...)
	// -> verifyAndGenerateToken으로 대체

	// [삭제] public ChatRoomZoneVarifyResponse getChatroomVerify(...)
	// -> 불필요

	//Redis에서 토큰 정보 가져오기
	public UserTokenDto getLocationInfo(String userId, String contextId) {
		return redisTokenService.getLocationInfo(userId, contextId);
	}
}
