package com.grm3355.zonie.apiserver.domain.location.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationTokenResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.location.dto.ChatRoomZoneVarifyResponse;
import com.grm3355.zonie.apiserver.domain.location.dto.FestivalZoneVarifyResponse;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

@Service
public class LocationService {

	private final RedisTokenService redisTokenService;
	private final FestivalRepository festivalRepository;
	private final ChatRoomRepository chatRoomRepository;

	public LocationService(RedisTokenService redisTokenService, FestivalRepository festivalRepository,
		ChatRoomRepository chatRoomRepository) {
		this.redisTokenService = redisTokenService;
		this.festivalRepository = festivalRepository;
		this.chatRoomRepository = chatRoomRepository;
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
		return Math.round(distance * 10) / 100.0;
	}


	@Transactional
	public LocationTokenResponse update(LocationDto locationDto, UserDetailsImpl userDetails) {

		String savedToken = userDetails.getUsername();
		boolean value = redisTokenService.updateLocationInfo(locationDto, savedToken);
		String message = value ? "갱신되었습니다." : "갱신에 실패하였습니다.";
		return new LocationTokenResponse(message);
	}


	public FestivalZoneVarifyResponse getFestivalVerify(UserDetailsImpl userDetails, long festivalId) {

		//Redis에서 토큰정보 가져오기
		String userId = userDetails.getUsername();
		UserTokenDto userTokenDto = getLocationInfo(userId);
		LocationDto locationDto = LocationDto.builder().lat(userTokenDto.getLat()).lon(userTokenDto.getLon()).build();

		//페스티벌 테이블에서 위도, 경도 가져오기
		Festival festival = festivalRepository.findByFestivalId(festivalId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관련 축제정보가 없습니다."));
		LocationDto festivalPosition = LocationDto.builder()
			.lat(festival.getPosition().getY())
			.lon(festival.getPosition().getX()).build();

		//계산호출
		double radius_km = getDistanceCalculator(locationDto, festivalPosition);
		boolean accessValue = radius_km <= 1.0;

		return new FestivalZoneVarifyResponse(accessValue, radius_km, festivalId, userId);

	}

	public ChatRoomZoneVarifyResponse getChatroomVerify(UserDetailsImpl userDetails, String chatroomId) {

		//토큰 만료여부 검증
		String userId  = userDetails.getUsername();

		//Redis에서 토큰정보 가져오기
		UserTokenDto userTokenDto = getLocationInfo(userId);
		LocationDto locationDto = LocationDto.builder().lat(userTokenDto.getLat()).lon(userTokenDto.getLon()).build();

		//채팅방 테이블에서 위도, 경도 가져오기
		ChatRoom chatRoom = chatRoomRepository.findByChatRoomId(chatroomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관련 정보가 없습니다."));

		LocationDto chatroomPosition = LocationDto.builder()
			.lat(0.0).lon(0.0)
			// .lat(chatRoom.getPosition().getY())
			// .lon(chatRoom.getPosition().getX())
			.build();

		//계산호출
		double radius_km = getDistanceCalculator(locationDto, chatroomPosition);
		boolean accessValue = radius_km <= 1.0;

		return new ChatRoomZoneVarifyResponse(accessValue, radius_km, chatroomId, userId);

	}

	//Redis에서 토큰 정보 가져오기
	public UserTokenDto getLocationInfo(String token) {
		return redisTokenService.getLocationInfo(token);
	}

}
