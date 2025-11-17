package com.grm3355.zonie.apiserver.domain.auth.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse{
	String accessToken;
	String refreshToken;

	public LoginResponse(String accessToken, String refreshToken) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
	//String nickname
}
