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

}
