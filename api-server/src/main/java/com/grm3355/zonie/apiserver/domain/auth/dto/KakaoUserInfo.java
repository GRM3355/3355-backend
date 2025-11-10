package com.grm3355.zonie.apiserver.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grm3355.zonie.apiserver.domain.auth.domain.UserInfo;
import com.grm3355.zonie.commonlib.global.enums.ProviderType;

public record KakaoUserInfo(
        String id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

    public UserInfo toUserInfo() {
        return new UserInfo(
                id,
                ProviderType.KAKAO,
                kakaoAccount.profile.nickname,
                kakaoAccount.profile.thumbnailImageUrl
        );
    }

    public record KakaoAccount(
            Profile profile
    ) {

        public record Profile(
                String nickname,
                @JsonProperty("thumbnail_image_url") String thumbnailImageUrl
        ) {

        }
    }
}
