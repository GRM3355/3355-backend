package com.grm3355.zonie.apiserver.domain.auth.infrasturucture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.apiserver.domain.auth.domain.UserInfo;
import com.grm3355.zonie.apiserver.domain.auth.dto.auth.KakaoUserInfo;
import com.grm3355.zonie.apiserver.domain.auth.dto.auth.KakaoUserInfo.KakaoAccount;
import com.grm3355.zonie.apiserver.domain.auth.dto.auth.KakaoUserInfo.KakaoAccount.Profile;
import com.grm3355.zonie.apiserver.global.exception.InternalServerException;
import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.HttpClientErrorException;

@DisplayNameGeneration(ReplaceUnderscores.class)
@SuppressWarnings("NonAsciiCharacters")
@RestClientTest(KakaoOAuth2UserInfoClient.class)
class KakaoOAuth2UserInfoClientTest {

    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Autowired
    KakaoOAuth2UserInfoClient kakaoOAuth2UserInfoClient;

    @Autowired
    MockRestServiceServer mockServer;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 예상치_못한_예외면_서버_에러() {
        mockServer.expect(requestTo(USER_INFO_URL))
                .andRespond(MockRestResponseCreators.withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> kakaoOAuth2UserInfoClient.getUserInfo("accessToken"))
                .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void 성공() throws JsonProcessingException {
        KakaoUserInfo expected = new KakaoUserInfo("id", new KakaoAccount(new Profile("nickname", "imageUrl")));
        mockServer.expect(requestTo(USER_INFO_URL))
                .andRespond(MockRestResponseCreators.withSuccess()
                        .body(objectMapper.writeValueAsString(expected))
                        .contentType(MediaType.APPLICATION_JSON));

        UserInfo actual = kakaoOAuth2UserInfoClient.getUserInfo("accessToken");

        assertSoftly(softly -> {
            softly.assertThat(actual.providerType()).isEqualTo(ProviderType.KAKAO);
            softly.assertThat(actual.socialId()).isEqualTo(expected.id());
            softly.assertThat(actual.nickname()).isEqualTo(expected.kakaoAccount().profile().nickname());
            softly.assertThat(actual.profileImage())
                    .isEqualTo(expected.kakaoAccount().profile().thumbnailImageUrl());
        });
    }
}
