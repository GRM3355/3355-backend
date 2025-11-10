package com.grm3355.zonie.apiserver.domain.chatroom.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.ColumnDefault;
import org.springframework.validation.annotation.Validated;

import com.grm3355.zonie.apiserver.domain.chatroom.enums.OrderType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class ChatRoomSearchRequest {

	@Schema(description = "페이지번호", example = "1")
	@Digits(integer = 2, fraction = 0, message = "숫자만 가능합니다.")
	//@NotNull(message = "페이지 번호는 필수입니다.")
	@ColumnDefault("'1'")
	private Integer page;

	@Schema(description = "한페이지 데이터 갯수", example = "10", nullable = true)
	@Digits(integer = 2, fraction = 0, message = "숫자만 가능합니다.")
	//@NotNull(message = "페이지 갯수 필수입니다.")
	@ColumnDefault("'10'")
	private Integer pageSize;

	@Schema(description = "정렬이름")
	@Builder.Default
	private OrderType order = OrderType.PART_DESC;

	@Schema(description = "검색어")
	private String keyword;

	public int getPage() {
		return page != null ? page : 1;
	}

	public Integer getPageSize() {
		return pageSize != null ? pageSize : 10;
	}

}
