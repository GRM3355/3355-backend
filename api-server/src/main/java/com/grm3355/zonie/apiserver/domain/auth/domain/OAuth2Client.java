package com.grm3355.zonie.apiserver.domain.auth.domain;

import com.grm3355.zonie.commonlib.global.enums.ProviderType;

public interface OAuth2Client {
    String getAccessToken(String code);

    UserInfo getUserInfo(String accessToken);

    ProviderType getProviderType();
}
