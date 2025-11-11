package com.grm3355.zonie.apiserver.domain.festival.enums;

import lombok.Getter;

@Getter
public enum FestivalStatus {
	UPCOMING("예정"),
	ONGOING("진행 중"),
	ALL("전체");

	private final String name;

	FestivalStatus(String name) {
		this.name = name;
	}
}
