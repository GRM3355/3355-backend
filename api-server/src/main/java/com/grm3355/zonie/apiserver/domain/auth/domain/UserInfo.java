package com.grm3355.zonie.apiserver.domain.auth.domain;

import com.grm3355.zonie.apiserver.domain.auth.dto.auth.KakaoUserInfo;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import com.grm3355.zonie.commonlib.global.enums.Role;

public record UserInfo(
        String socialId,
        ProviderType providerType,
        String nickname,
        String profileImage) {

    public User toUser() {
        return User.builder()
                .userId(null)
                .password(null)
                .role(Role.USER)
                .profileNickName(nickname)
                .accountEmail(null)
                .profileImage(profileImage)
                .provider(providerType)
                .socialId(socialId)
                .build();
    }
}
