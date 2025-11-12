package com.grm3355.zonie.apiserver.domain.festival.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 특정 지역의 축제 개수를 반환하기 위한 DTO
 */
@Getter
@AllArgsConstructor
public class FestivalCountResponse {
	private long count;
}
