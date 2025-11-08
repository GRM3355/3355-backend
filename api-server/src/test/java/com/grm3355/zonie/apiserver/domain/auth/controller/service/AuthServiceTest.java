package com.grm3355.zonie.apiserver.domain.auth.controller.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;

import com.grm3355.zonie.apiserver.common.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.auth.dto.AuthResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.AuthService;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.commonlib.domain.auth.JwtTokenProvider;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.enums.Role;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private UserRepository userRepository;
	@Mock
	private RedisTokenService redisTokenService;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private AuthService authService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("register() - 정상적으로 회원가입 및 토큰 발급")
	void register_success() {
		// given
		LocationDto locationDto = new LocationDto(37.123, 127.456);
		when(request.getHeader("device")).thenReturn("android");
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");

		String encodedPassword = "encoded-password";
		when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);

		User mockUser = User.builder()
			.userId("user:1234")
			.password(encodedPassword)
			.role(Role.GUEST)
			.build();
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		UserDetailsImpl mockUserDetails = UserDetailsImpl.build(mockUser);
		Authentication mockAuth = mock(Authentication.class);
		when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(mockAuth);

		when(jwtTokenProvider.createAccessToken(anyString(), any(Role.class)))
			.thenReturn("fake-jwt-token");

		// when
		AuthResponse response = authService.register(locationDto, request);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getAccessToken()).isEqualTo("fake-jwt-token");

		verify(userRepository, times(1)).save(any(User.class));
		verify(redisTokenService, times(1)).generateLocationToken(any(UserTokenDto.class));
		verify(jwtTokenProvider, times(1)).createAccessToken(anyString(), eq(Role.GUEST));
		verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
	}

	@Test
	@DisplayName("generateTokens() - 권한이 없는 경우 예외 발생")
	void generateTokens_fail_noAuthority() {
		// given
		User mockUser = User.builder()
			.userId("user:1111")
			.password("pwd")
			.role(Role.GUEST)
			.build();

		UserDetailsImpl userDetails = spy(UserDetailsImpl.build(mockUser));
		when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());

		UserTokenDto tokenDto = new UserTokenDto("user:1111", 37.1, 127.2);

		// when / then
		org.junit.jupiter.api.Assertions.assertThrows(
			BusinessException.class,
			() -> authService.generateTokens(userDetails, tokenDto)
		);
	}
}