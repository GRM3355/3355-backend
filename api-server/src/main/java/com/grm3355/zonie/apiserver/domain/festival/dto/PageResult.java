package com.grm3355.zonie.apiserver.domain.festival.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

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

	public static <T, R> PageResult<R> of(Page<T> page, Function<? super T, ? extends R> mapper) {
		List<R> mappedItems = (List<R>)page.stream().map(mapper).toList();
		return new PageResult<>(page.getTotalElements(), mappedItems);
	}

}
