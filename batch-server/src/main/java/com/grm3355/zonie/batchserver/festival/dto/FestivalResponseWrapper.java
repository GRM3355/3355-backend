package com.grm3355.zonie.batchserver.festival.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FestivalResponseWrapper {
	private Response response;

	@Getter
	@Setter
	public static class Response {
		private Header header;
		private Body body;
	}

	@Getter
	@Setter
	public static class Header {
		private String resultCode;
		private String resultMsg;
	}

	@Getter
	@Setter
	public static class Body {
		private Items items;
		private int totalCount;
	}

	@Getter
	@Setter
	public static class Items {
		private List<FestivalDto> item;
	}
}