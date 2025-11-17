package com.grm3355.zonie.apiserver.domain.auth.util;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

	private final boolean httpOnly;
	private final boolean secure;
	private final String path;
	private final int maxAge;
	private final String sameSite;
	private final String domain;

	public CookieUtil(
		@Value("${spring.set-cookie.httpOnly}") boolean httpOnly,
		@Value("${spring.set-cookie.secure}") boolean secure,
		@Value("${spring.set-cookie.path}") String path,
		@Value("${spring.set-cookie.maxAge}") int maxAge,
		@Value("${spring.set-cookie.sameSite}") String sameSite,
		@Value("${spring.set-cookie.domain}") String domain
		){
		this.httpOnly = httpOnly;
		this.secure = secure;
		this.path = path;
		this.maxAge = maxAge;
		this.sameSite = sameSite;
		this.domain = domain;
	}

	/**
	 * HttpOnly, Secure 쿠키로 refreshToken 발급
	 *
	 * @param response HttpServletResponse
	 * @param refreshToken 발급할 리프레시 토큰 값
	 */
	public void addRefreshTokenCookie(HttpServletResponse response,
		String refreshToken) {
		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(httpOnly)                 // JS 접근 불가
			.secure(secure)                   // HTTPS 환경이면 true
			.path(path)                      // 모든 경로에서 사용 가능
			.maxAge(maxAge)          // 예: 7일 = 60*60*24*7
			.sameSite(sameSite)               // cross-site 요청 허용
			.sameSite(domain)               // cross-site 요청 허용
			.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}

}