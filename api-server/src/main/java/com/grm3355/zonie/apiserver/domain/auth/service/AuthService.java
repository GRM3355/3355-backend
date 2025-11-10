package com.grm3355.zonie.apiserver.domain.auth.service;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.auth.dto.AuthResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.enums.Role;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String PRE_FIX = "user:";
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final RedisTokenService redisTokenService;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public AuthResponse register(LocationDto locationDto) {

		//uuid 생성
		String userId = PRE_FIX + UUID.randomUUID();
		double lat = locationDto.getLat();
		double lon = locationDto.getLon();

		//아이디저장
		String password = passwordEncoder.encode(userId);
		User user = User.builder()
			.userId(userId)
			.password(password)
			.role(Role.GUEST).build();
		userRepository.save(user);

		//사용자정보
		UserTokenDto userTokenDto = UserTokenDto.builder()
			.userId(userId).lat(lat).lon(lon).build();

		//아이디 저장후 인증정보 authentication  정보 가져오기
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(userId, userId)
		);
		UserDetailsImpl userDetails = (UserDetailsImpl)authentication.getPrincipal();

		// --- 로그인 성공 ---
		log.info("사용자 로그인 성공");

		//토큰 생성
		return generateTokens(userDetails, userTokenDto);
	}

	public AuthResponse generateTokens(UserDetailsImpl userDetails, UserTokenDto userTokenDto) {
		// 현재 시스템은 사용자당 단일 권한을 가정하므로, 첫 번째 권한을 가져와 사용합니다.
		// 향후 다중 권한을 지원하려면 이 로직의 수정이 필요합니다.

		String roleName = userDetails.getAuthorities().stream()
			.findFirst()
			.map(GrantedAuthority::getAuthority)
			.orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "사용자 권한 정보를 찾을 수 없습니다."));

		// "ROLE_GUEST" -> "GUEST"
		String roleEnumName = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;

		//액세스 토큰 생성(JWT) - 클라이언트가 저장
		String accessToken = jwtTokenProvider.createAccessToken(userDetails.getUsername(), Role.valueOf(roleEnumName));

		return new AuthResponse(accessToken);
	}
}
