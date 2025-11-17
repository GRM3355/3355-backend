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
}
