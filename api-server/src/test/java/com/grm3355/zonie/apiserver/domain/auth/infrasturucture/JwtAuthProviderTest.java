package com.grm3355.zonie.apiserver.domain.auth.infrasturucture;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

import com.grm3355.zonie.commonlib.domain.user.entity.User;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

@DisplayNameGeneration(ReplaceUnderscores.class)
class JwtAuthProviderTest {
	private static final String SECRET_KEY = "1231231231231231223131231231231231231212312312";
	JwtAuthProvider jwtAuthProvider = new JwtAuthProvider(SECRET_KEY, 360);

	@Test
	@DisplayName("토큰_생성_성공")
	void tokenSuccess() {
		User member = User.builder()
			.userId("s")
			.build();
		JwtParser parser = Jwts.parser()
			.setSigningKey(SECRET_KEY.getBytes())
			.build();

		String token = jwtAuthProvider.provide(member);

		assertThat(parser.isSigned(token))
			.isTrue();
	}
}
