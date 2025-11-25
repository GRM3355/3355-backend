package com.grm3355.zonie.commonlib.global.enums;

import java.util.Arrays;

import lombok.Getter;

@Getter
public enum RegionCode {
	SEOUL("1", "서울", Region.SEOUL), // Region 필드 추가
	INCHEON("2", "인천", Region.GYEONGGI), // 경기/인천
	DAEJEON("3", "대전", Region.CHUNGCHEONG), // 충청/대전/세종
	DAEGU("4", "대구", Region.GYEONGBUK), // 경북/대구/울산
	GWANGJU("5", "광주", Region.JEOLLA), // 전라/광주
	BUSAN("6", "부산", Region.GYEONGNAM), // 경남/부산
	ULSAN("7", "울산", Region.GYEONGBUK), // 경북/대구/울산
	SEJONG("8", "세종", Region.CHUNGCHEONG), // 충청/대전/세종
	GYEONGGI("31", "경기", Region.GYEONGGI), // 경기/인천
	GANGWON("32", "강원", Region.GANGWON),
	CHUNGBUK("33", "충청", Region.CHUNGCHEONG),
	CHUNGNAM("34", "충청", Region.CHUNGCHEONG),
	GYEONGBUK("35", "경북", Region.GYEONGBUK),
	GYEONGNAM("36", "경남", Region.GYEONGNAM),
	JEONBUK("37", "전라", Region.JEOLLA),
	JEONNAM("38", "전라", Region.JEOLLA),
	JEJU("39", "제주", Region.JEJU),
	ETC("0", "기타", null);

	private final String code;
	private final String name;
	private final Region region;    // Region Enum 참조

	RegionCode(String code, String name, Region region) {
		this.code = code;
		this.name = name;
		this.region = region;
	}

	public static String getNameByCode(String code) {
		return Arrays.stream(RegionCode.values())
			.filter(rc -> rc.code.equals(code))
			.findFirst()
			.map(rc -> rc.name)
			.orElse(ETC.name);
	}

	public static String getRegionNameByCode(String code) {
		return Arrays.stream(RegionCode.values())
			.filter(rc -> rc.code.equals(code))
			.findFirst()
			.map(rc -> rc.region != null ? rc.region.name() : ETC.name()) // Region Enum의 이름(SEOUL, GYEONGGI 등) 반환
			.orElse(ETC.name);
	}
}
