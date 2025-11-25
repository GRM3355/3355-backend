package com.grm3355.zonie.apiserver.domain.auth.domain;

import java.security.SecureRandom;

import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import com.grm3355.zonie.commonlib.global.enums.Role;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserInfo {
	String socialId;
	ProviderType providerType;
	String email;
	String nickname;
	String profileImage;
	String socialIdHash;

	public User toUser() {

		// 아이디 : 랜덤숫자 + kakao + 현재시간(밀리초) + 4자리 랜덤숫자
		SecureRandom random = new SecureRandom();
		int rand = random.nextInt(999999999); // 10자리 랜덤
		String uniqueSuffix = System.currentTimeMillis() + "" + (int)(Math.random() * 9000 + 1000);
		String userId = rand + "kakao" + uniqueSuffix;
		return User.builder()
			.userId(userId)
			.password("null")
			.role(Role.USER)
			.accountEmail(email)
			.providerType(providerType)
			.socialId(socialId)
			.socialIdHash(socialIdHash)
			.build();
	}
}
