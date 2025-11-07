package com.grm3355.zonie.apiserver.domain.search.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ListWithCount<T> {
	private long totalCount;
	private List<T> data;

	// Page<T> 기반 생성자
	public ListWithCount(Page<T> page) {
		this.totalCount = page.getTotalElements();
		this.data = page.getContent();
	}
}