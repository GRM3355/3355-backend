package com.grm3355.zonie.commonlib.global.enums;

import java.util.Arrays;

public enum RegionCode {
	SEOUL("1", "서울"),
	INCHEON("2", "인천"),
	DAEJEON("3", "대전"),
	DAEGU("4", "대구"),
	GWANGJU("5", "광주"),
	BUSAN("6", "부산"),
	ULSAN("7", "울산"),
	SEJONG("8", "세종"),
	GYEONGGI("31", "경기"),
	GANGWON("32", "강원"),
	CHUNGBUK("33", "충북"),
	CHUNGNAM("34", "충남"),
	GYEONGBUK("35", "경북"),
	GYEONGNAM("36", "경남"),
	JEONBUK("37", "전북"),
	JEONNAM("38", "전남"),
	JEJU("39", "제주"),
	ETC("0", "기타"); // 지역 코드가 매핑되지 않는 경우를 대비

	private final String code;
	private final String name;

	RegionCode(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public static String getNameByCode(String code) {
		return Arrays.stream(RegionCode.values())
			.filter(rc -> rc.code.equals(code))
			.findFirst()
			.map(rc -> rc.name)
			.orElse(ETC.name);
	}
}