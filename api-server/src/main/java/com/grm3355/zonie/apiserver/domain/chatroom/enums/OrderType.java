package com.grm3355.zonie.apiserver.domain.chatroom.enums;

public enum OrderType {
	DATE_ASC,    // 생성일 오래된순
	DATE_DESC,    // 생성일 최신순
	PART_ASC,    // 참여자 적은순
	PART_DESC,    // 참여자 많은순
	ACTIVE_ASC,    // 활성화 오래된순 (마지막 대화 시각)
	ACTIVE_DESC    // 활성화 최신순 (마지막 대화 시각)
}

