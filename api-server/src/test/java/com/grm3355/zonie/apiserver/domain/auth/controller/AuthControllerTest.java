package com.grm3355.zonie.apiserver.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.apiserver.domain.auth.dto.auth.LoginRequest;
import com.grm3355.zonie.apiserver.domain.auth.dto.auth.LoginResponse;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.global.jwt.JwtAccessDeniedHandler;
import com.grm3355.zonie.apiserver.global.jwt.JwtAuthenticationEntryPoint;
import com.grm3355.zonie.apiserver.global.service.RateLimitingService;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.service.AuthService;
import com.grm3355.zonie.apiserver.domain.auth.dto.AuthResponse;
import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("토큰 발행 통합테스트")
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false) //시큐리티 제외
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@MockitoBean
	private RedisTokenService redisTokenService;


    // @Test
    // @DisplayName("입력값 누락 시 400 에러 반환")
    // void registerToken_BadRequest() throws Exception {
    // 	// given - 위도/경도 누락된 경우
    // 	LocationDto invalidDto = null;
    //
    // 	// when & then
    // 	mockMvc.perform(post("/api/v1/auth/token-register")
    // 			.contentType(MediaType.APPLICATION_JSON)
    // 			.content(objectMapper.writeValueAsString(invalidDto)))
    // 		.andExpect(status().isBadRequest());
    // }

    @Test
    void 카카오_OAuth2_로그인을_한다() throws Exception {
        LoginResponse expected = new LoginResponse("accesstoken", "nickname");
        given(authService.login(any(LoginRequest.class)))
                .willReturn(expected);

        String response = mockMvc.perform(get("/api/auth/kakao/callback")
                        .param("code", "code"))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn()
                .getResponse()
                .getContentAsString();
        LoginResponse actual = objectMapper.readValue(response, LoginResponse.class);

        assertThat(actual).isEqualTo(expected);
    }
}
