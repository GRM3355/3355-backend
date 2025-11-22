package com.grm3355.zonie.apiserver.domain.chatroom.enums;

// 일반 채팅 정렬 타입
// 1. 참여자 많은순: default (PART_DESC)
// 2. 채팅방 생성 최신순 (DATE_DESC)
// 나의 채팅방 정렬 타입
// 1. 활성화 최신순 (ACTIVE_DESC)
// 2. 생성일 최신순 (DATE_DESC)
public enum OrderType {
	DATE_ASC,    // 생성일 오래된순
	DATE_DESC,    // 생성일 최신순
	PART_ASC,    // 참여자 적은순
	PART_DESC,    // 참여자 많은순
	ACTIVE_ASC,    // 활성화 오래된순 (마지막 대화 시각)
	ACTIVE_DESC    // 활성화 최신순 (마지막 대화 시각)
}

