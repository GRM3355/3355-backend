package com.grm3355.zonie.apiserver.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageResponse<T> {

	@Schema(description = "페이지 내용", example = "리스트값을 배열로.")
	private List<T> content;

	@Schema(description = "현재페이지", example = "1")
	private int currentPage;

	@Schema(description = "총페이지", example = "1")
	private int totalPages;

	@Schema(description = "총갯수", example = "10")
	private long totalElements;

	@Schema(description = "페이지블록수", example = "10")
	private int blockSize;

	public PageResponse(Page<T> page, int blockSize) {
		this.content = page.getContent();
		this.currentPage = page.getNumber() + 1;

		this.totalPages = page.getTotalPages();
		this.totalElements = page.getTotalElements();

		int tempEnd = (int)(Math.ceil((double)currentPage / blockSize) * blockSize);
		this.blockSize = blockSize;
	}

}
