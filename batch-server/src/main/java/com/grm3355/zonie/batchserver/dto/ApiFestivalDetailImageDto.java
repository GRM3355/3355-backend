package com.grm3355.zonie.batchserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFestivalDetailImageDto {
	private String contentid;
	private String originimgurl;
	private String imgname;
	private String smallimageurl;
	private String cpyrhtDivCd;
	private String serialnum;
}
