package com.grm3355.zonie.apiserver.domain.auth.domain;

public interface OAuth2Client {
    String getAccessToken(String code);

    UserInfo getUserInfo(String accessToken);
}
