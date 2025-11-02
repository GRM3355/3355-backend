package com.grm3355.zonie.apiserver.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserTokenDto{
	String userId;
	String clientIp;
	String device;
	double lat;
	double lon;
}
