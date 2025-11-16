package com.grm3355.zonie.apiserver.domain.auth.domain;

import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import com.grm3355.zonie.commonlib.global.enums.Role;

public record UserInfo(
	String socialId,
	ProviderType providerType,
	String email,
	String nickname,
	String profileImage) {

	public User toUser() {
		// 현재시간(밀리초) + 4자리 랜덤숫자
		String uniqueSuffix = System.currentTimeMillis() + "" + (int)(Math.random() * 9000 + 1000);
		return User.builder()
			.userId(socialId + "kakao" + uniqueSuffix)
			.password("null")
			.role(Role.USER)
			.profileNickName(nickname)
			.accountEmail(email)
			.profileImage(profileImage)
			.providerType(providerType)
			.socialId(socialId)
			.build();
	}
}
