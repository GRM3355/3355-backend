package com.grm3355.zonie.apiserver.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserKakaoRequest {

	@Schema(description = "코드값", example = "AkOGS7DaUa3GT8qIUxXi3Da4pUZRUFJY0OX...")
	String code;

}
