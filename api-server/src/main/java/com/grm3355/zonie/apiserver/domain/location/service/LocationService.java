package com.grm3355.zonie.apiserver.domain.location.service;

import org.springframework.stereotype.Service;

import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;

@Service
public class LocationService {

	private final RedisTokenService redisTokenService;

	public LocationService(RedisTokenService redisTokenService) {
		this.redisTokenService = redisTokenService;
	}

	public boolean getFestivalVerify(String token, String festivalId) {
		
		//Redis에서 토큰정보 가져오기
		UserTokenDto userTokenDto = getLocationInfo(token);
		LocationDto locationDto = LocationDto.builder().lat(userTokenDto.getLat()).lon(userTokenDto.getLon()).build();

		//페스티벌 정보 가져오기
		//페스티벌 테이블에서 위도, 경도 가져오기
		LocationDto festivalPosition = locationDto;

		//계산호출
		double position = getDistance(locationDto, festivalPosition);
		return (position <= 1)? true : false;
	}

	public boolean getChatroomVerify(String token, String chatroomId) {

		//Redis에서 토큰정보 가져오기
		UserTokenDto userTokenDto = getLocationInfo(token);
		LocationDto locationDto = LocationDto.builder().lat(userTokenDto.getLat()).lon(userTokenDto.getLon()).build();
		
		//채팅방 정보 가져오기
		//채팅방 테이블에서 위도, 경도 가져오기
		LocationDto chatroomPosition = locationDto;

		//계산호출
		double position = getDistance(locationDto, chatroomPosition);
		return (position <= 1)? true : false;
	}
	
	//Redis에서 토큰 정보 가져오기
	public UserTokenDto getLocationInfo(String token){
		return redisTokenService.getLocationInfo(token);
	}

	//위도 경도, 거리예산
	public static double getDistance(LocationDto location1, LocationDto location2) {

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
		return R * c; // 단위: km
	}	
	
}
