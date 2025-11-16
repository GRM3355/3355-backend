package com.grm3355.zonie.commonlib.global.enums;

import lombok.Getter;

@Getter
public enum Region {
	SEOUL("서울"),
	GYEONGGI("경기/인천"),
	CHUNGCHEONG("충청/대전/세종"),
	GANGWON("강원"),
	GYEONGBUK("경북/대구/울산"),
	GYEONGNAM("경남/부산"),
	JEOLLA("전라/광주"),
	JEJU("제주");

	private final String name;

	Region(String name) {
		this.name = name;
	}


	/**
	 * 한글 지역명 → Enum 이름 변환
	 */
	public static String fromKorean(String areaCode) {
		for (Region region : Region.values()) {
			String[] codes = region.name.split("/"); // 구분자로 분리
			for (String code : codes) {
				if (code.equals(areaCode)) {
					return region.name();
				}
			}
		}
		return areaCode; // 매칭 없으면 그대로 반환
	}

}
