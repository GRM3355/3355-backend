package com.grm3355.zonie.apiserver.domain.festival.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PageResult<T> {
	private long totalCount;    // 전체 아이템 수
	private List<T> items;

	public static <T> PageResult<T> of(Page<T> page) {
		return new PageResult<>(
			page.getTotalElements(),
			page.getContent()
		);
	}
}