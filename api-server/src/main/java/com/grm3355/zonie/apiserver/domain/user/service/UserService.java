package com.grm3355.zonie.apiserver.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.auth.dto.UserProfileResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserQuitResponse;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.auth.util.AESUtil;
import com.grm3355.zonie.apiserver.domain.user.dto.EmailUpdateRequest;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserService {
	private final UserRepository userRepository;
	private final RedisTokenService redisTokenService;
	private final AESUtil aesUtil;

	public UserService(UserRepository userRepository, RedisTokenService redisTokenService, AESUtil aesUtil) {
		this.userRepository = userRepository;
		this.redisTokenService = redisTokenService;
		this.aesUtil = aesUtil;
	}

	@Transactional
	public void updateEmail(String userId, EmailUpdateRequest request) {
		User user = userRepository.getOrThrow(userId);
		user.updateEmail(request.email());
	}

	public UserProfileResponse getUserProfile(String userId) throws Exception {
		User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		String decryptedEmail = null;
		if (user.getAccountEmail() != null) {
			decryptedEmail = aesUtil.decrypt(user.getAccountEmail());
		}

		return new UserProfileResponse(
			user.getUserId(),
			decryptedEmail,
			user.getCreatedAt()
		);
	}

	@Transactional
	public void quit(String userId, UserQuitResponse request) {

		User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
		user.softDelete();
		userRepository.save(user);

		// 보안을 위해 모든 리프레시 토큰 무효화
		redisTokenService.deleteAllTokensForUser(userId);
		log.info("정상적으로 탈퇴처리되었습니다.");
	}
}
