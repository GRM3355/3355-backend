package com.grm3355.zonie.apiserver.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserTokenDto {

	@Schema(description = "사용자 아이디", example = "user:asdf-asdf-asdf-aasdf")
	String userId;

	@Schema(description = "아이피정보", example = "123.456.789.123")
	String clientIp;

	@Schema(description = "기기정보", example = "Device")
	String device;

	@Schema(description = "위도", example = "23.23443")
	double lat;

	@Schema(description = "경도", example = "128.23443")
	double lon;
}
