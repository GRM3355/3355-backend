package com.grm3355.zonie.apiserver.domain.festival.enums;

// 축제 정렬 타입
// (0) 상태 (진행중 -> 예정, 종료(x))
// 1. 시작일 빠른순 DATE_ASC
// 2. 제목 가나다순 TITLE_ASC
public enum FestivalOrderType {
	DATE_ASC,     // 시작일 오름차순
	DATE_DESC,    // 시작일 내림차순
	TITLE_ASC,   // 제목 가나다순
	TITLE_DESC  // 제목 가나다 역순
}

