package com.grm3355.zonie.apiserver.domain.auth.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.set-cookie")
public class CookieProperties {
	private boolean secure;
	private long maxAge;
	private String sameSite;
	private String domain;

	// getters & setters
	//public boolean isHttpOnly() { return httpOnly; }
	//public void setHttpOnly(boolean httpOnly) { this.httpOnly = httpOnly; }

	//public boolean isSecure() { return secure; }
	//public void setSecure(boolean secure) { this.secure = secure; }

	//public String getSameSite() { return sameSite; }
	//public void setSameSite(String sameSite) { this.sameSite = sameSite; }
}
